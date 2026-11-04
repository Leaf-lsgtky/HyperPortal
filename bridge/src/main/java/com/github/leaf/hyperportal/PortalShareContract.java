// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal;

/**
 * Cross-process contract for asking the HyperPortal app to perform an image share.
 *
 * The SystemUI LSPosed module runs under com.android.systemui's UID. It cannot reliably
 * grant a URI owned by PortalFileProvider, so image shares are dispatched by PortalService
 * (which owns the provider) instead of calling startActivity() directly from SystemUI.
 */
public final class PortalShareContract {

    public static final String ACTION_DISPATCH_IMAGE_SHARE =
            "com.github.leaf.hyperportal.DISPATCH_IMAGE_SHARE";
    public static final String ACTION_OCR_SEGMENT =
            "com.github.leaf.hyperportal.OCR_SEGMENT";
    public static final String EXTRA_OCR_IMAGE_URI = "ocrImageUri";
    public static final String EXTRA_OCR_TOUCH_X = "ocrTouchX";
    public static final String EXTRA_OCR_TOUCH_Y = "ocrTouchY";

    public static final String EXTRA_TARGET_PACKAGE = "targetPackage";
    public static final String EXTRA_TARGET_ACTIVITY = "targetActivity";
    public static final String EXTRA_IMAGE_URI = "imageUri";
    public static final String EXTRA_IMAGE_MIME = "imageMime";
    public static final String EXTRA_SHARE_TEXT = "shareText";

    public static final String PORTAL_PACKAGE = "com.github.leaf.hyperportal";
    public static final String PORTAL_SERVICE =
            "com.github.leaf.hyperportal.PortalService";
    public static final String SHARE_PROXY_ACTIVITY =
            "com.github.leaf.hyperportal.ShareProxyActivity";

    private PortalShareContract() {
    }
}
