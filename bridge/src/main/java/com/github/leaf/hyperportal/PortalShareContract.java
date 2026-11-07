// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal;

/**
 * Cross-process contract for image shares that SystemUI cannot start itself.
 *
 * SystemUI owns the staging FileProvider, so normal shares are dispatched
 * directly. This contract is only the fallback for an external provider URI
 * (for example a legacy app-provider URI or a third-party content URI): a
 * transparent trampoline in HyperPortal receives a visible window and then
 * starts the target's ACTION_SEND activity.
 */
public final class PortalShareContract {

    public static final String EXTRA_TARGET_PACKAGE = "targetPackage";
    public static final String EXTRA_TARGET_ACTIVITY = "targetActivity";
    public static final String EXTRA_IMAGE_URI = "imageUri";
    public static final String EXTRA_IMAGE_MIME = "imageMime";
    public static final String EXTRA_SHARE_TEXT = "shareText";

    public static final String PORTAL_PACKAGE = "com.github.leaf.hyperportal";
    public static final String SHARE_PROXY_ACTIVITY =
            "com.github.leaf.hyperportal.ShareProxyActivity";

    private PortalShareContract() {
    }
}
