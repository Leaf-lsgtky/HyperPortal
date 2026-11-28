// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock

import android.content.Context
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.github.leaf.hyperportal.system.PortalSystemUiModule
import java.io.FileNotFoundException

private const val AUTHORITY = "com.android.systemui.fileprovider"
private const val PATH = "hyperportal_transfer_dock"

internal fun dockItemUri(item: TransferDockItem): Uri? {
    if (item.filePath != null) {
        return Uri.Builder().scheme("content").authority(AUTHORITY)
            .appendPath(PATH).appendPath(item.id).build()
    }
    return item.uriString?.takeIf { it.startsWith(PortalSystemUiModule.INCOMING_URI_PREFIX) }?.let(Uri::parse)
}

internal object TransferDockFiles {
    @JvmStatic
    fun handles(uri: Uri?): Boolean =
        uri?.authority == AUTHORITY && uri.pathSegments.size == 2 && uri.pathSegments.first() == PATH

    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun open(context: Context, uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Transfer dock files are read-only")
        val (_, file) = TransferDockManager.getInstance(context).storedItem(uri.lastPathSegment.orEmpty())
            ?: throw FileNotFoundException("Transfer dock file unavailable")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    @JvmStatic
    fun query(context: Context, uri: Uri, projection: Array<String>?): MatrixCursor {
        val columns = (projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
            .filter { it == OpenableColumns.DISPLAY_NAME || it == OpenableColumns.SIZE }
        val cursor = MatrixCursor(columns.toTypedArray(), 1)
        val (item, file) = TransferDockManager.getInstance(context).storedItem(uri.lastPathSegment.orEmpty())
            ?: return cursor
        cursor.addRow(columns.map { if (it == OpenableColumns.DISPLAY_NAME) item.title else file.length() })
        return cursor
    }

    @JvmStatic
    fun mimeType(context: Context, uri: Uri): String? =
        TransferDockManager.getInstance(context).storedItem(uri.lastPathSegment.orEmpty())?.first?.mimeType
}
