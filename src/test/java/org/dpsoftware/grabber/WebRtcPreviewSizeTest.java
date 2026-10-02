package org.dpsoftware.grabber;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WebRtcPreviewSizeTest {

    @ParameterizedTest
    @CsvSource({
            "1920, 1080, 1280, 720",
            "960, 540, 1280, 720",
            "480, 270, 1280, 720",
            "240, 135, 1280, 720",
            "3840, 2160, 1280, 720",
            "860, 360, 1280, 534",
            "480, 360, 960, 720",
            "270, 480, 404, 720"
    })
    void fitsPreviewToBoundingBoxRegardlessOfCaptureSize(int width, int height,
                                                         int expectedWidth, int expectedHeight) {
        assertArrayEquals(new int[]{expectedWidth, expectedHeight},
                WebRtcStreamer.previewSize(width, height));
    }
}
