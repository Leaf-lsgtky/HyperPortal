// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import org.json.JSONObject
import java.util.UUID

data class TransferDockItem(
    val id: String = UUID.randomUUID().toString(),
    val type: Int, // 0 = TEXT, 1 = IMAGE, 2 = FILE
    val text: String? = null,
    val uriString: String? = null,
    val filePath: String? = null,
    val title: String,
    val detail: String = "",
    val mimeType: String = "text/plain",
    val timestamp: Long = System.currentTimeMillis(),
    val thumbnailPath: String? = null,
    val sourcePackage: String? = null
) {
    companion object {
        const val TYPE_TEXT = 0
        const val TYPE_IMAGE = 1
        const val TYPE_FILE = 2

        fun fromJson(json: JSONObject): TransferDockItem {
            return TransferDockItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                type = json.optInt("type", TYPE_TEXT),
                text = if (json.has("text")) json.optString("text") else null,
                uriString = if (json.has("uriString")) json.optString("uriString") else null,
                filePath = if (json.has("filePath")) json.optString("filePath") else null,
                title = json.optString("title", ""),
                detail = json.optString("detail", ""),
                mimeType = json.optString("mimeType", "text/plain"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                thumbnailPath = if (json.has("thumbnailPath")) json.optString("thumbnailPath") else null,
                sourcePackage = if (json.has("sourcePackage")) json.optString("sourcePackage") else null
            )
        }
    }

    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("type", type)
        text?.let { json.put("text", it) }
        uriString?.let { json.put("uriString", it) }
        filePath?.let { json.put("filePath", it) }
        json.put("title", title)
        json.put("detail", detail)
        json.put("mimeType", mimeType)
        json.put("timestamp", timestamp)
        thumbnailPath?.let { json.put("thumbnailPath", it) }
        sourcePackage?.let { json.put("sourcePackage", it) }
        return json
    }
}
