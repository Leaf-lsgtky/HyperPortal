// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast

/**
 * A transparent, exported trampoline used for image shares started from the
 * SystemUI LSPosed module.
 *
 * HyperOS blocks background activity launches from SystemUI
 * ("Background activity launch blocked", BAL_BLOCK). SystemUI can start this
 * exported Activity directly; once it is resumed it has a visible window and
 * is allowed to start the target app's ACTION_SEND Activity.
 */
class ShareProxyActivity : Activity() {

    private var dispatched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setDimAmount(0f)
        setContentView(FrameLayout(this))
    }

    override fun onResume() {
        super.onResume()
        if (dispatched) {
            return
        }
        dispatched = true
        window.decorView.postDelayed({ dispatchShare() }, 80L)
    }

    private fun dispatchShare() {
        val target = intent.getStringExtra(PortalShareContract.EXTRA_TARGET_PACKAGE) ?: ""
        val targetActivity = intent.getStringExtra(PortalShareContract.EXTRA_TARGET_ACTIVITY) ?: ""
        val uriString = intent.getStringExtra(PortalShareContract.EXTRA_IMAGE_URI)
        val fallbackMime = intent.getStringExtra(PortalShareContract.EXTRA_IMAGE_MIME)
        val text = intent.getStringExtra(PortalShareContract.EXTRA_SHARE_TEXT)
        val isChooser = intent.getBooleanExtra("isChooser", false)
            || "system:chooser" == target
            || "android" == target

        if (isChooser) {
            try {
                val share = Intent(Intent.ACTION_SEND)
                share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (!TextUtils.isEmpty(uriString)) {
                    val uri = Uri.parse(uriString)
                    val mime = resolveMime(uri, fallbackMime)
                    share.type = mime
                    share.putExtra(Intent.EXTRA_STREAM, uri)
                    share.clipData = ClipData.newUri(contentResolver, "image", uri)
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (!TextUtils.isEmpty(text)) {
                        share.putExtra(Intent.EXTRA_TEXT, text)
                    }
                    Log.i(TAG, "start system chooser image share uri=$uri mime=$mime")
                } else if (!TextUtils.isEmpty(text)) {
                    share.type = "text/plain"
                    share.putExtra(Intent.EXTRA_TEXT, text)
                    Log.i(TAG, "start system chooser text share chars=${text?.length ?: 0}")
                } else {
                    Log.w(TAG, "empty share payload for chooser")
                    finishAndRemoveTask()
                    return
                }
                val chooser = Intent.createChooser(share, "分享到")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(chooser)
            } catch (error: Throwable) {
                Log.w(TAG, "start chooser failed", error)
                Toast.makeText(this, "无法打开系统分享", Toast.LENGTH_SHORT).show()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                finishAndRemoveTask()
                overridePendingTransition(0, 0)
            }, 120L)
            return
        }

        if (TextUtils.isEmpty(target)) {
            Log.w(TAG, "missing target")
            finishAndRemoveTask()
            return
        }
        try {
            val share = Intent(Intent.ACTION_SEND)
            share.setPackage(target)
            if (!TextUtils.isEmpty(targetActivity)) {
                share.component = android.content.ComponentName(target, targetActivity)
            }
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!TextUtils.isEmpty(uriString)) {
                val uri = Uri.parse(uriString)
                val mime = resolveMime(uri, fallbackMime)
                share.type = mime
                share.putExtra(Intent.EXTRA_STREAM, uri)
                share.clipData = ClipData.newUri(contentResolver, "image", uri)
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    grantUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (error: Throwable) {
                    Log.w(TAG, "grant failed target=$target uri=$uri", error)
                }
                Log.i(TAG, "start image share target=$target uri=$uri mime=$mime")
            } else if (!TextUtils.isEmpty(text)) {
                share.type = "text/plain"
                share.putExtra(Intent.EXTRA_TEXT, text)
                Log.i(TAG, "start text share target=$target chars=${text?.length ?: 0}")
            } else {
                Log.w(TAG, "empty share payload target=$target")
                finishAndRemoveTask()
                return
            }
            startActivity(share)
        } catch (error: Throwable) {
            Log.w(TAG, "start share failed target=$target", error)
            Toast.makeText(this, "无法分享图片", Toast.LENGTH_SHORT).show()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            finishAndRemoveTask()
            overridePendingTransition(0, 0)
        }, 120L)
    }

    private fun resolveMime(uri: Uri, fallback: String?): String {
        try {
            contentResolver.getType(uri)?.let { return it }
        } catch (_: Throwable) {
        }
        return if (TextUtils.isEmpty(fallback)) "image/png" else fallback!!
    }

    companion object {
        private const val TAG = "HyperPortal/ShareProxy"
    }
}
