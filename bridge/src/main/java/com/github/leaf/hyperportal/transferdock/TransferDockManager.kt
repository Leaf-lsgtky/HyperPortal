// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TransferDockManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "TransferDockManager"
        private const val MAX_ITEMS = 100

        @Volatile
        private var instance: TransferDockManager? = null

        @JvmStatic
        fun getInstance(context: Context): TransferDockManager {
            return instance ?: synchronized(this) {
                instance ?: TransferDockManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val baseDir = File(context.filesDir, "transfer_dock").apply { mkdirs() }
    private val mediaDir = File(baseDir, "media").apply { mkdirs() }
    private val dataFile = File(baseDir, "items.json")
    private val lock = Any()
    private val items = ArrayList<TransferDockItem>()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        synchronized(lock) {
            items.clear()
            if (!dataFile.exists()) return
            try {
                val jsonStr = AtomicFile(dataFile).readFully().toString(Charsets.UTF_8)
                val jsonArr = JSONArray(jsonStr)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    items.add(TransferDockItem.fromJson(obj))
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to load transfer dock items", e)
            }
        }
    }

    private fun saveToDisk() {
        synchronized(lock) {
            val json = JSONArray()
            for (item in items) json.put(item.toJson())
            val atomic = AtomicFile(dataFile)
            val stream = atomic.startWrite()
            try {
                stream.write(json.toString().toByteArray(Charsets.UTF_8))
                atomic.finishWrite(stream)
            } catch (e: Exception) {
                atomic.failWrite(stream)
                throw e
            }
        }
    }

    fun getItems(): List<TransferDockItem> {
        synchronized(lock) {
            return ArrayList(items)
        }
    }

    fun getItemCount(): Int {
        synchronized(lock) {
            return items.size
        }
    }

    fun saveText(text: String, sourcePkg: String?): TransferDockItem {
        val trimmed = text.trim()
        val title = if (trimmed.length > 50) trimmed.substring(0, 50) + "..." else trimmed
        val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
        val item = TransferDockItem(
            type = TransferDockItem.TYPE_TEXT,
            text = text,
            title = title,
            detail = timeStr,
            mimeType = "text/plain",
            sourcePackage = sourcePkg
        )
        addItem(item)
        return item
    }

    fun saveBitmap(bitmap: Bitmap, sourcePkg: String?): TransferDockItem? {
        val id = UUID.randomUUID().toString()
        val imageFile = File(mediaDir, "img_${id}.png")
        val thumbFile = File(mediaDir, "thumb_${id}.jpg")

        try {
            FileOutputStream(imageFile).use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
        } catch (e: Exception) {
            imageFile.delete()
            Log.e(TAG, "Failed to save transfer dock bitmap", e)
            return null
        }
        try {
            // Generate compact thumbnail for fast stack preview
            val maxThumb = 320
            val scale = Math.min(1.0f, maxThumb.toFloat() / Math.max(bitmap.width, bitmap.height))
            val thumbW = Math.max(1, (bitmap.width * scale).toInt())
            val thumbH = Math.max(1, (bitmap.height * scale).toInt())
            val thumbBmp = Bitmap.createScaledBitmap(bitmap, thumbW, thumbH, true)
            FileOutputStream(thumbFile).use { out ->
                thumbBmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (thumbBmp != bitmap) {
                thumbBmp.recycle()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to cache transfer dock bitmap", e)
        }

        val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
        val item = TransferDockItem(
            id = id,
            type = TransferDockItem.TYPE_IMAGE,
            filePath = imageFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            title = "图片",
            detail = "${bitmap.width}×${bitmap.height} · $timeStr",
            mimeType = "image/png",
            sourcePackage = sourcePkg
        )
        addItem(item)
        return item
    }

    fun saveFile(uri: Uri, mimeType: String?, fileName: String?, sourcePkg: String?): TransferDockItem? {
        val id = UUID.randomUUID().toString()
        val mime = mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        val displayName = fileName ?: "文件"
        val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
        val extension = extensionForMime(mime)
        val target = File(mediaDir, "file_${id}.$extension")
        // SystemUI staging files live in a small LRU cache. Copy the original
        // bytes into the dock's own media dir; storing the content:// URI here
        // made the item disappear after a few more drags pruned the cache.
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            stream.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy transfer dock file", e)
            target.delete()
            return null
        }
        val isImage = mime.startsWith("image/")
        val thumbnail = if (isImage) {
            createThumbnail(target, id)
        } else {
            null
        }
        val item = TransferDockItem(
            id = id,
            type = if (isImage) TransferDockItem.TYPE_IMAGE else TransferDockItem.TYPE_FILE,
            filePath = target.absolutePath,
            title = displayName,
            detail = timeStr,
            mimeType = mime,
            thumbnailPath = thumbnail,
            sourcePackage = sourcePkg
        )
        addItem(item)
        return item
    }

    private fun createThumbnail(source: File, id: String): String? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / sample > 640 || bounds.outHeight / sample > 640) {
                sample *= 2
            }
            val decoded = BitmapFactory.decodeFile(
                source.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null
            val maxThumb = 320
            val scale = Math.min(1.0f, maxThumb.toFloat() / Math.max(decoded.width, decoded.height))
            val thumbW = Math.max(1, (decoded.width * scale).toInt())
            val thumbH = Math.max(1, (decoded.height * scale).toInt())
            val thumb = Bitmap.createScaledBitmap(decoded, thumbW, thumbH, true)
            val file = File(mediaDir, "thumb_${id}.jpg")
            FileOutputStream(file).use { out ->
                thumb.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            if (thumb !== decoded) {
                thumb.recycle()
            }
            decoded.recycle()
            file.absolutePath
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to create transfer dock thumbnail", e)
            null
        }
    }

    private fun addItem(item: TransferDockItem) {
        synchronized(lock) {
            items.add(0, item)
            val removed = if (items.size > MAX_ITEMS) items.removeAt(items.lastIndex) else null
            try {
                saveToDisk()
            } catch (e: Exception) {
                items.removeAt(0)
                if (removed != null) items.add(removed)
                cleanupItemFiles(item)
                throw e
            }
            if (removed != null) cleanupItemFiles(removed)
        }
    }

    fun removeItem(id: String) {
        synchronized(lock) {
            val index = items.indexOfFirst { it.id == id }
            if (index < 0) return
            val item = items.removeAt(index)
            try {
                saveToDisk()
            } catch (e: Exception) {
                items.add(index, item)
                Log.e(TAG, "Failed to remove transfer dock item", e)
                return
            }
            cleanupItemFiles(item)
        }
    }

    fun clearAll() {
        synchronized(lock) {
            val removed = ArrayList(items)
            items.clear()
            try {
                saveToDisk()
            } catch (e: Exception) {
                items.addAll(removed)
                Log.e(TAG, "Failed to clear transfer dock items", e)
                return
            }
            removed.forEach(::cleanupItemFiles)
        }
    }

    internal fun storedItem(id: String): Pair<TransferDockItem, File>? = synchronized(lock) {
        val item = items.firstOrNull { it.id == id } ?: return null
        val file = item.filePath?.let(::File) ?: return null
        if (file.parentFile != mediaDir || !file.isFile) return null
        item to file
    }

    fun exportItemUri(item: TransferDockItem): Uri? {
        if (item.type == TransferDockItem.TYPE_TEXT) {
            return null
        }
        var mime = item.mimeType.ifBlank { "application/octet-stream" }
        if (item.type == TransferDockItem.TYPE_IMAGE
            && (mime.isBlank() || mime == "text/plain" || mime == "application/octet-stream")
        ) {
            mime = "image/png"
        }
        val extension = extensionForMime(mime)
        val name = "transfer_${item.id}.$extension"
        if (context.packageName != "com.android.systemui") {
            Log.w(TAG, "exportItemUri called outside SystemUI")
            return null
        }
        return try {
            val dir = com.github.leaf.hyperportal.system.PortalSystemUiModule.getIncomingDir(context)
            val targetFile = File(dir, name)
            FileOutputStream(targetFile).use { stream ->
                when {
                    !TextUtils.isEmpty(item.filePath) -> {
                        val srcFile = File(item.filePath!!)
                        if (!srcFile.isFile) return null
                        srcFile.inputStream().use { it.copyTo(stream) }
                    }
                    !TextUtils.isEmpty(item.uriString) -> {
                        val src = Uri.parse(item.uriString!!)
                        if ("file" == src.scheme) {
                            val srcFile = File(src.path ?: return null)
                            if (!srcFile.isFile) return null
                            srcFile.inputStream().use { it.copyTo(stream) }
                        } else {
                            val input = context.contentResolver.openInputStream(src) ?: return null
                            input.use { it.copyTo(stream) }
                        }
                    }
                    else -> return null
                }
            }
            val uri = Uri.parse(com.github.leaf.hyperportal.system.PortalSystemUiModule.INCOMING_URI_PREFIX + Uri.encode(name))
            com.github.leaf.hyperportal.system.PortalSystemUiModule.grantReadToCommonTargets(context, uri)
            uri
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to export item URI in SystemUI", e)
            null
        }
    }

    private fun extensionForMime(mime: String): String {
        return when {
            mime == "image/png" -> "png"
            mime == "image/jpeg" -> "jpg"
            mime == "image/webp" -> "webp"
            mime == "image/gif" -> "gif"
            else -> MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(mime)
                ?.takeIf { it.isNotBlank() }
                ?: "bin"
        }
    }

    private fun cleanupItemFiles(item: TransferDockItem) {
        item.filePath?.let { runCatching { File(it).delete() } }
        item.thumbnailPath?.let { runCatching { File(it).delete() } }
    }
}
