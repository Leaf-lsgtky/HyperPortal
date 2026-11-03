// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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
        if (item.type != TransferDockItem.TYPE_IMAGE || item.filePath == null) {
            return null
        }
        return try {
            val srcFile = File(item.filePath)
            if (!srcFile.exists()) return null
            val incomingDir = File(context.cacheDir, "incoming").apply { mkdirs() }
            val destFile = File(incomingDir, "transfer_${item.id}.png")
            if (!destFile.exists() || destFile.length() != srcFile.length()) {
                srcFile.copyTo(destFile, overwrite = true)
            }
            val uri = Uri.parse("content://com.github.leaf.hyperportal.files/incoming/transfer_${item.id}.png")
            try {
                val targets = arrayOf(
                    "com.miui.notes", "com.tencent.mm", "com.tencent.mobileqq",
                    "com.sina.weibo", "org.telegram.messenger", "com.coolapk.market",
                    "com.android.fileexplorer", "com.zhihu.android", "com.xingin.xhs",
                    "com.baidu.tieba", "com.android.systemui"
                )
                for (pkg in targets) {
                    try {
                        context.grantUriPermission(pkg, uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (ignored: Throwable) {}
                }
            } catch (ignored: Throwable) {}
            uri
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to export item URI", e)
            null
        }
    }

    private fun cleanupItemFiles(item: TransferDockItem) {
        item.filePath?.let { runCatching { File(it).delete() } }
        item.thumbnailPath?.let { runCatching { File(it).delete() } }
    }
}
