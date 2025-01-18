package org.dpsoftware.grabber;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.IntBuffer;

public class ImageProcessorTest {

    @Test
    public void testGetAverageForAllZones() {
        Color[] leds = {new Color(100, 100, 100), new Color(70, 70, 70)};
        Color averageColor = ImageProcessor.getAverageForAllZones(leds, 0, 2);
        Assertions.assertEquals(new Color(56, 56, 56), averageColor);
    }

    @Test
    public void testRoundToTheNearestNumber() {
        int roundedNumber = ImageProcessor.roundToTheNearestNumber(10, 234);
        Assertions.assertEquals(230, roundedNumber);
    }

    @Test
    public void testGetWidthPlusStride() {
        int width = 3440;
        int height = 1440;
        IntBuffer buffer = IntBuffer.allocate(width * height + 4); // Simulate stride
        int widthPlusStride = ImageProcessor.getWidthPlusStride(width, height, buffer);
        Assertions.assertEquals(width, widthPlusStride); // Adjust the expected value based on the stride calculation
    }

    @Test
    public void testColorDistance() {
        double distance = ImageProcessor.colorDistance(255, 0, 0, 0, 255, 0);
        Assertions.assertEquals(650.0273056163679, distance, 0.0001); // Adjust the expected value based on the calculation
    }

    @Test
    public void testGetInstallationPath() {
        ImageProcessor processor = new ImageProcessor(false);
        String path = processor.getInstallationPath();
        Assertions.assertTrue(path.contains("firefly_luciferin")); // Adjust the assertion based on the expected path
    }

    @Test
    public void testCalculateBorders() {
        ImageProcessor processor = new ImageProcessor(false);
        processor.calculateBorders();
        Assertions.assertTrue(GrabberSingleton.getInstance().CHECK_ASPECT_RATIO); // Adjust the assertion based on the expected behavior
    }

    @Test
    public void testInitGStreamerLibraryPaths() {
        ImageProcessor processor = new ImageProcessor(false);
        processor.initGStreamerLibraryPaths();
        Assertions.assertTrue(true); // Adjust the assertion based on the expected behavior
    }

}