// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Fast, offline QR code decoder using ZXing.
 * Optimizes performance by downsampling images to ~500px before decoding.
 */
public final class QrCodeScanner {

    private static final String TAG = "HyperPortal/QrScanner";
    private static final int MAX_DECODE_DIMENSION = 500;

    private QrCodeScanner() {
    }

    @Nullable
    public static String scan(@Nullable Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        long startTime = System.currentTimeMillis();
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width <= 0 || height <= 0) {
                return null;
            }

            Bitmap target = bitmap;
            boolean needRecycle = false;
            if (width > MAX_DECODE_DIMENSION || height > MAX_DECODE_DIMENSION) {
                float scale = Math.min(
                        (float) MAX_DECODE_DIMENSION / width,
                        (float) MAX_DECODE_DIMENSION / height);
                int dstW = Math.max(1, Math.round(width * scale));
                int dstH = Math.max(1, Math.round(height * scale));
                target = Bitmap.createScaledBitmap(bitmap, dstW, dstH, false);
                needRecycle = (target != bitmap);
            }

            int w = target.getWidth();
            int h = target.getHeight();
            int[] pixels = new int[w * h];
            target.getPixels(pixels, 0, w, 0, 0, w, h);
            if (needRecycle) {
                target.recycle();
            }

            RGBLuminanceSource source = new RGBLuminanceSource(w, h, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            long elapsed = System.currentTimeMillis() - startTime;
            if (result != null && result.getText() != null && !result.getText().isEmpty()) {
                Log.i(TAG, "QR code recognized in " + elapsed + "ms: " + result.getText());
                return result.getText();
            }
        } catch (Throwable ignored) {
            // No barcode found or decoding failed
        }
        return null;
    }

    @Nullable
    public static String scan(Context context, @Nullable Uri uri) {
        if (context == null || uri == null) {
            return null;
        }
        try {
            // First decode bounds only
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, opts);
            }
            if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                return null;
            }

            // Calculate sample size for ~500px max dimension
            int sampleSize = 1;
            int maxDim = Math.max(opts.outWidth, opts.outHeight);
            while (maxDim / (sampleSize * 2) >= MAX_DECODE_DIMENSION) {
                sampleSize *= 2;
            }

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bmp;
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                bmp = BitmapFactory.decodeStream(is, null, opts);
            }
            if (bmp == null) {
                return null;
            }
            String result = scan(bmp);
            bmp.recycle();
            return result;
        } catch (Throwable t) {
            Log.w(TAG, "scan uri failed: " + uri, t);
            return null;
        }
    }
}
