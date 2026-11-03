// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.transferdock;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

/**
 * ColorOS TransferDock light/dark palette.
 *
 * The reference APK uses the COUI light panel in day mode and the COUI dark
 * panel at night (common_item_bg_color becomes #404040, panel text becomes
 * white).  Keep the palette in one place so the panel, footer and scrim all
 * switch together with the system configuration.
 */
final class TransferDockTheme {

    final boolean night;
    final int panelFill;
    final int panelStroke;
    final int primaryText;
    final int secondaryText;
    final int cardBackground;
    final int cardStroke;
    final int cardText;
    final int cardSecondaryText;
    final int footerBackground;
    final int footerStroke;
    final int footerText;
    final int divider;
    final int emptyIcon;
    final int pressedOverlay;

    private TransferDockTheme(boolean night, boolean blurAvailable) {
        this.night = night;
        if (night) {
            panelFill = Color.argb(blurAvailable ? 230 : 242, 28, 28, 30);
            panelStroke = Color.argb(38, 255, 255, 255);
            primaryText = Color.argb(242, 255, 255, 255);
            secondaryText = Color.argb(140, 255, 255, 255);
            cardBackground = Color.rgb(64, 64, 64);
            cardStroke = Color.argb(31, 255, 255, 255);
            cardText = Color.argb(242, 255, 255, 255);
            cardSecondaryText = Color.argb(128, 255, 255, 255);
            footerBackground = Color.argb(51, 255, 255, 255);
            footerStroke = Color.argb(26, 255, 255, 255);
            footerText = Color.argb(242, 255, 255, 255);
            divider = Color.argb(31, 255, 255, 255);
            emptyIcon = Color.argb(64, 255, 255, 255);
            pressedOverlay = Color.argb(36, 255, 255, 255);
        } else {
            panelFill = Color.argb(blurAvailable ? 230 : 247, 242, 242, 242);
            panelStroke = Color.argb(26, 0, 0, 0);
            primaryText = Color.argb(230, 0, 0, 0);
            secondaryText = Color.argb(153, 0, 0, 0);
            cardBackground = Color.WHITE;
            cardStroke = Color.argb(20, 0, 0, 0);
            cardText = Color.rgb(64, 64, 64);
            cardSecondaryText = Color.argb(128, 0, 0, 0);
            footerBackground = Color.argb(232, 255, 255, 255);
            footerStroke = Color.argb(20, 0, 0, 0);
            footerText = Color.argb(230, 0, 0, 0);
            divider = Color.argb(15, 0, 0, 0);
            emptyIcon = Color.argb(51, 0, 0, 0);
            pressedOverlay = Color.argb(20, 0, 0, 0);
        }
    }

    static boolean isNight(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static TransferDockTheme resolve(Context context, boolean blurAvailable) {
        return new TransferDockTheme(isNight(context), blurAvailable);
    }

    static int scrimColor(Context context) {
        return scrimColor(isNight(context));
    }

    static int scrimColor(boolean night) {
        return night ? Color.argb(102, 0, 0, 0) : Color.argb(51, 0, 0, 0);
    }
}
