package com.github.leaf.hyperportal;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Content-Portal-like main page.
 *
 * The patched AES Bridge starts this Activity with:
 *   viewContent  (String, optional)
 *   viewBitmap   (Bitmap, optional)
 *   catchType    (int)
 *   sourcePkg    (String, optional)
 */
public class MainActivity extends Activity {

    private static final String TAG = "DragShareBridge";
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");

    private TextView mSourceView;
    private TextView mTextView;
    private ImageView mImageView;
    private TextView mEmptyView;
    private LinearLayout mActionsView;
    private TextView mOpenAction;

    private String mText;
    private Bitmap mBitmap;
    private Uri mSavedImageUri;
    private File mLocalFile;
    private String mFileName;
    private String mMimeType;
    private long mFileSize;
    private Uri mSavedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            startForegroundService(new Intent(this, PortalService.class).putExtra("keepAlive", true));
        } catch (Throwable ignored) {
        }
        buildUi();
        render(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        render(intent);
    }

    private void buildUi() {
        int padding = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(Color.rgb(242, 243, 245));

        TextView title = new TextView(this);
        title.setText("内容传送门");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTextColor(Color.rgb(20, 20, 20));
        title.setGravity(Gravity.START);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        mSourceView = new TextView(this);
        mSourceView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        mSourceView.setTextColor(Color.rgb(100, 100, 100));
        LinearLayout.LayoutParams sourceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sourceParams.topMargin = dp(4);
        root.addView(mSourceView, sourceParams);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(16), 0, dp(8));

        mTextView = new TextView(this);
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        mTextView.setTextColor(Color.rgb(30, 30, 30));
        mTextView.setTextIsSelectable(true);
        mTextView.setLineSpacing(dp(5), 1.0f);
        mTextView.setPadding(dp(16), dp(16), dp(16), dp(16));
        mTextView.setBackground(cardBackground());
        content.addView(mTextView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        mImageView = new ImageView(this);
        mImageView.setAdjustViewBounds(true);
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImageView.setPadding(dp(12), dp(12), dp(12), dp(12));
        mImageView.setBackground(cardBackground());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        imageParams.topMargin = dp(12);
        content.addView(mImageView, imageParams);

        mEmptyView = new TextView(this);
        mEmptyView.setText("等待长按内容…\n\n在任意应用里长按文字或图片，内容会显示在这里。");
        mEmptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        mEmptyView.setTextColor(Color.rgb(120, 120, 120));
        mEmptyView.setGravity(Gravity.CENTER);
        mEmptyView.setPadding(0, dp(100), 0, 0);
        content.addView(mEmptyView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        mActionsView = new LinearLayout(this);
        mActionsView.setOrientation(LinearLayout.HORIZONTAL);
        mActionsView.setGravity(Gravity.CENTER_VERTICAL);
        mActionsView.setPadding(0, dp(8), 0, 0);
        HorizontalScrollView actionScroll = new HorizontalScrollView(this);
        actionScroll.setHorizontalScrollBarEnabled(false);
        actionScroll.addView(mActionsView, new HorizontalScrollView.LayoutParams(
                HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                HorizontalScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(actionScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        addAction("复制", v -> copyText());
        addAction("分享", v -> shareContent());
        addAction("搜索", v -> searchText());
        mOpenAction = addAction("打开链接", v -> openLink());
        addAction("保存图片", v -> saveImage());
        addAction("关闭", v -> finish());

        setContentView(root);
    }

    private TextView addAction(String label, View.OnClickListener listener) {
        TextView action = new TextView(this);
        action.setText(label);
        action.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        action.setTextColor(Color.rgb(20, 90, 180));
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(16), dp(10), dp(16), dp(10));
        action.setBackground(actionBackground());
        action.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.rightMargin = dp(8);
        mActionsView.addView(action, params);
        return action;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.WHITE);
        drawable.setCornerRadius(dp(18));
        return drawable;
    }

    private GradientDrawable actionBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(232, 240, 254));
        drawable.setCornerRadius(dp(22));
        return drawable;
    }

    private void render(Intent intent) {
        mText = intent == null ? null : intent.getStringExtra("viewContent");
        mBitmap = null;
        mLocalFile = null;
        mFileName = null;
        mMimeType = null;
        mFileSize = -1;
        try {
            Object extra = intent == null ? null : intent.getParcelableExtra("viewBitmap");
            if (extra instanceof Bitmap) {
                mBitmap = (Bitmap) extra;
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "read bitmap failed", throwable);
        }
        try {
            Object pfdExtra = intent == null ? null : intent.getParcelableExtra("filePfd");
            if (pfdExtra instanceof ParcelFileDescriptor) {
                mFileName = intent.getStringExtra("fileName");
                mMimeType = intent.getStringExtra("mimeType");
                mFileSize = intent.getLongExtra("fileSize", -1);
                mLocalFile = copyPfd((ParcelFileDescriptor) pfdExtra);
                if (mMimeType != null && mMimeType.startsWith("image/") && mLocalFile != null) {
                    mBitmap = BitmapFactory.decodeFile(mLocalFile.getPath());
                }
            }
        } catch (Throwable throwable) {
            Log.w(TAG, "read file failed", throwable);
        }
        String source = intent == null ? null : intent.getStringExtra("sourcePkg");
        int catchType = intent == null ? 0 : intent.getIntExtra("catchType", 0);
        mSavedImageUri = null;
        mSavedFileUri = null;

        boolean hasText = !TextUtils.isEmpty(mText);
        boolean hasBitmap = mBitmap != null;
        boolean hasFile = mLocalFile != null;
        boolean hasLink = (hasText && URL_PATTERN.matcher(mText).find()) || hasFile;

        String type = hasFile ? "文件" : (catchType == 2 ? "图片" : "文字");
        mSourceView.setText(TextUtils.isEmpty(source)
                ? "来源：未知 · " + type
                : "来源：" + source + " · " + type);

        if (hasFile && !hasText) {
            mText = "文件：" + mFileName + "\n大小：" + formatSize(mFileSize);
        }
        mTextView.setText(hasText ? mText : "");
        mTextView.setVisibility(hasText ? View.VISIBLE : View.GONE);
        mImageView.setImageBitmap(mBitmap);
        mImageView.setVisibility(hasBitmap ? View.VISIBLE : View.GONE);
        mEmptyView.setVisibility(!hasText && !hasBitmap && !hasFile ? View.VISIBLE : View.GONE);
        mActionsView.setVisibility(hasText || hasBitmap || hasFile ? View.VISIBLE : View.GONE);
        mOpenAction.setText(hasFile ? "打开文件" : "打开链接");
        mOpenAction.setVisibility(hasLink ? View.VISIBLE : View.GONE);

        Log.i(TAG, "render text=" + (hasText ? mText.length() + " chars" : "null")
                + " bitmap=" + (hasBitmap ? mBitmap.getWidth() + "x" + mBitmap.getHeight() : "null")
                + " file=" + (hasFile ? mFileName + " " + mFileSize : "null")
                + " source=" + source
                + " catchType=" + catchType);
    }

    private File copyPfd(ParcelFileDescriptor pfd) {
        try {
            String suffix = mFileName != null && mFileName.contains(".")
                    ? mFileName.substring(mFileName.lastIndexOf('.')) : ".bin";
            File target = File.createTempFile("dragshare_", suffix, getCacheDir());
            try (InputStream input = new FileInputStream(pfd.getFileDescriptor());
                 OutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) > 0) {
                    output.write(buffer, 0, count);
                }
            }
            return target;
        } catch (Throwable throwable) {
            Log.w(TAG, "copyPfd failed", throwable);
            return null;
        }
    }

    private String formatSize(long size) {
        if (size < 0) {
            return "未知";
        }
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", size / 1024f);
        }
        return String.format(Locale.US, "%.1f MB", size / (1024f * 1024f));
    }

    private void copyText() {
        if (mLocalFile != null || TextUtils.isEmpty(mText)) {
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("content", mText));
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show();
    }

    private void shareContent() {
        Intent intent;
        if (mLocalFile != null) {
            Uri uri = ensureFileUri();
            if (uri == null) {
                return;
            }
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType(TextUtils.isEmpty(mMimeType) ? "*/*" : mMimeType);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(ClipData.newUri(getContentResolver(), "file", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (!TextUtils.isEmpty(mText)) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mText);
        } else if (mBitmap != null) {
            Uri uri = ensureImageUri();
            if (uri == null) {
                return;
            }
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setClipData(ClipData.newUri(getContentResolver(), "image", uri));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            return;
        }
        startActivity(Intent.createChooser(intent, "分享内容"));
    }

    private void searchText() {
        if (mLocalFile != null || TextUtils.isEmpty(mText)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, mText);
            startActivity(intent);
        } catch (Throwable throwable) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.bing.com/search?q=" + Uri.encode(mText)));
            startActivity(intent);
        }
    }

    private void openLink() {
        if (mLocalFile != null) {
            Uri uri = ensureFileUri();
            if (uri != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setDataAndType(uri, TextUtils.isEmpty(mMimeType) ? "*/*" : mMimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivity(intent);
                } catch (Throwable throwable) {
                    Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        if (TextUtils.isEmpty(mText)) {
            return;
        }
        Matcher matcher = URL_PATTERN.matcher(mText);
        if (matcher.find()) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(matcher.group())));
            } catch (Throwable throwable) {
                Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImage() {
        if (mLocalFile != null) {
            if (ensureFileUri() != null) {
                Toast.makeText(this, "已保存到下载目录", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (ensureImageUri() != null) {
            Toast.makeText(this, "已保存到相册", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri ensureFileUri() {
        if (mSavedFileUri != null) {
            return mSavedFileUri;
        }
        if (mLocalFile == null) {
            return null;
        }
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME,
                TextUtils.isEmpty(mFileName) ? "DragShare_file" : mFileName);
        values.put(MediaStore.Downloads.MIME_TYPE,
                TextUtils.isEmpty(mMimeType) ? "application/octet-stream" : mMimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/DragShare");
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return null;
        }
        try (InputStream input = new FileInputStream(mLocalFile);
             OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) > 0) {
                output.write(buffer, 0, count);
            }
            mSavedFileUri = uri;
            return uri;
        } catch (Throwable throwable) {
            Log.w(TAG, "ensureFileUri failed", throwable);
            return null;
        }
    }

    private Uri ensureImageUri() {
        if (mSavedImageUri != null) {
            return mSavedImageUri;
        }
        if (mBitmap == null) {
            return null;
        }
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "DragShare_" + System.currentTimeMillis() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DragShare");
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            return null;
        }
        try (OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null || !mBitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                return null;
            }
            mSavedImageUri = uri;
            return uri;
        } catch (Throwable throwable) {
            Log.w(TAG, "save image failed", throwable);
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
