// SPDX-License-Identifier: GPL-3.0-or-later
package com.github.leaf.hyperportal.ocr;

import android.os.BadParcelableException;
import android.os.Parcel;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire reader for the AICR V2 result verified with AICR 4.12.26 / 4.13.2.
 * This does not load vendor classes or depend on a hidden vendor API.
 */
final class AicrV2ResultReader {
    private static final String LOCATION = "com.xiaomi.aicr.vision.ocr.OCRRes$Location";

    private AicrV2ResultReader() {
    }

    static AicrOcrResult readResult(Parcel reply) {
        if (!present(reply)) {
            return null;
        }
        List<AicrOcrResult.Line> lines = new ArrayList<>();
        array(reply, () -> {
            array(reply, () -> {
                Location coordinates = location(reply);
                String text = reply.readString();
                lines.add(new AicrOcrResult.Line(text,
                        coordinates == null ? null : coordinates.polygon,
                        coordinates == null ? null : coordinates.box));
                reply.readInt(); // line id
                array(reply, () -> {
                    location(reply);
                    reply.readString(); // character
                    reply.readFloat(); // confidence
                });
                reply.readFloat(); // line confidence
            });
            reply.readString(); // paragraph text
        });
        String text = reply.readString();
        array(reply, () -> {
            location(reply);
            reply.readString(); // entity text
            reply.readInt(); // type
            reply.readInt(); // line id
            reply.readInt(); // start
            reply.readInt(); // end
        });
        return new AicrOcrResult(text, lines);
    }

    private static boolean present(Parcel parcel) {
        int marker = parcel.readInt();
        if (marker != 0 && marker != 1) {
            throw new BadParcelableException("AICR V2: invalid presence marker " + marker);
        }
        return marker == 1;
    }

    private static void array(Parcel parcel, Runnable readItem) {
        int count = parcel.readInt();
        if (count < -1 || count > parcel.dataAvail() / Integer.BYTES) {
            throw new BadParcelableException("AICR V2: invalid array length " + count);
        }
        for (int i = 0; i < count; i++) {
            if (present(parcel)) {
                readItem.run();
            }
        }
    }

    private static Location location(Parcel parcel) {
        String name = parcel.readString();
        if (name == null) {
            return null;
        }
        if (!LOCATION.equals(name)) {
            throw new BadParcelableException("AICR V2: unexpected location type " + name);
        }
        return new Location(parcel.createFloatArray(), parcel.createFloatArray());
    }

    private static final class Location {
        final float[] polygon;
        final float[] box;

        Location(float[] polygon, float[] box) {
            this.polygon = polygon;
            this.box = box;
        }
    }
}
