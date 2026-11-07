// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
                val jsonStr = dataFile.readText(Charsets.UTF_8)
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
            try {
                val jsonArr = JSONArray()
                for (item in items) {
                    jsonArr.put(item.toJson())
                }
                dataFile.writeText(jsonArr.toString(), Charsets.UTF_8)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to save transfer dock items", e)
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

    fun saveBitmap(bitmap: Bitmap, sourcePkg: String?): TransferDockItem {
        val id = UUID.randomUUID().toString()
        val imageFile = File(mediaDir, "img_${id}.png")
        val thumbFile = File(mediaDir, "thumb_${id}.jpg")

        try {
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
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

    fun saveFile(uri: Uri, mimeType: String?, fileName: String?, sourcePkg: String?): TransferDockItem {
        val id = UUID.randomUUID().toString()
        val displayName = fileName ?: "文件"
        val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
        val item = TransferDockItem(
            id = id,
            type = TransferDockItem.TYPE_FILE,
            uriString = uri.toString(),
            title = displayName,
            detail = timeStr,
            mimeType = mimeType ?: "*/*",
            sourcePackage = sourcePkg
        )
        addItem(item)
        return item
    }

    private fun addItem(item: TransferDockItem) {
        synchronized(lock) {
            items.add(0, item) // Newest at top
            while (items.size > MAX_ITEMS) {
                val removed = items.removeAt(items.size - 1)
                cleanupItemFiles(removed)
            }
            saveToDisk()
        }
    }

    fun removeItem(id: String) {
        synchronized(lock) {
            val it = items.iterator()
            while (it.hasNext()) {
                val item = it.next()
                if (item.id == id) {
                    it.remove()
                    cleanupItemFiles(item)
                    break
                }
            }
            saveToDisk()
        }
    }

    fun clearAll() {
        synchronized(lock) {
            for (item in items) {
                cleanupItemFiles(item)
            }
            items.clear()
            saveToDisk()
        }
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
