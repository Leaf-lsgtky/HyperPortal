package com.dragshare.portal;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Write-only staging provider.
 *
 * AES runs in the target app process, so it cannot write directly into the
 * portal's private cache. This provider exposes a single write-only
 * "incoming/" namespace. The bridge streams the original file here first and
 * then sends only the cache file name to PortalService, avoiding both the
 * 1MB Binder limit and the Android 14 "no file descriptors in startService"
 * restriction.
 */
public class PortalFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.dragshare.portal.files";
    public static final String INCOMING = "incoming";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("..")) {
            throw new FileNotFoundException("bad name");
        }
        File dir = new File(getContext().getCacheDir(), INCOMING);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new FileNotFoundException("cannot create cache dir");
        }
        File file = new File(dir, name);
        if ("r".equals(mode)) {
            if (!file.isFile()) {
                throw new FileNotFoundException("missing " + name);
            }
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        if (!"w".equals(mode) && !"wt".equals(mode)) {
            throw new FileNotFoundException("bad mode");
        }
        return ParcelFileDescriptor.open(file,
                ParcelFileDescriptor.MODE_CREATE
                        | ParcelFileDescriptor.MODE_WRITE_ONLY
                        | ParcelFileDescriptor.MODE_TRUNCATE);
    }

    @Override
    public String getType(Uri uri) {
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
