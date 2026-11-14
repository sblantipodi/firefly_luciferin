/*
  EnumsTest.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2026  Davide Perini  (https://github.com/sblantipodi)

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for Enum lookup methods (findByValue, findByExtendedVal).
 */
class EnumsTest {

    // --- BaudRate ---

    @Test
    void baudRate_findByValue_knownValues() {
        assertEquals(Enums.BaudRate.BAUD_RATE_115200, Enums.BaudRate.findByValue(8));
        assertEquals(Enums.BaudRate.BAUD_RATE_230400, Enums.BaudRate.findByValue(1));
        assertEquals(Enums.BaudRate.BAUD_RATE_921600, Enums.BaudRate.findByValue(4));
        assertEquals(Enums.BaudRate.BAUD_RATE_2000000, Enums.BaudRate.findByValue(7));
        assertEquals(Enums.BaudRate.BAUD_RATE_6000000, Enums.BaudRate.findByValue(10));
    }

    @Test
    void baudRate_findByValue_unknownValueReturnsNull() {
        assertNull(Enums.BaudRate.findByValue(999));
    }

    @Test
    void baudRate_findByExtendedVal() {
        assertEquals(Enums.BaudRate.BAUD_RATE_115200, Enums.BaudRate.findByExtendedVal("115200"));
        assertEquals(Enums.BaudRate.BAUD_RATE_460800, Enums.BaudRate.findByExtendedVal("460800"));
        assertNull(Enums.BaudRate.findByExtendedVal("999999"));
    }

    // --- SmoothingTarget ---

    @Test
    void smoothingTarget_findByValue() {
        assertEquals(Enums.SmoothingTarget.TARGET_30_FPS, Enums.SmoothingTarget.findByValue(30));
        assertEquals(Enums.SmoothingTarget.TARGET_60_FPS, Enums.SmoothingTarget.findByValue(60));
        assertEquals(Enums.SmoothingTarget.TARGET_120_FPS, Enums.SmoothingTarget.findByValue(120));
        assertNull(Enums.SmoothingTarget.findByValue(999));
    }

    @Test
    void smoothingTarget_findByExtendedVal() {
        assertEquals(Enums.SmoothingTarget.TARGET_60_FPS,
                Enums.SmoothingTarget.findByExtendedVal("60 FPS"));
        assertNull(Enums.SmoothingTarget.findByExtendedVal("invalid"));
    }

    // --- FrameGeneration ---

    @Test
    void frameGeneration_findByValue() {
        assertEquals(Enums.FrameGeneration.DISABLED, Enums.FrameGeneration.findByValue(0));
        assertEquals(Enums.FrameGeneration.FI_2X, Enums.FrameGeneration.findByValue(30));
        assertEquals(Enums.FrameGeneration.FI_4X, Enums.FrameGeneration.findByValue(15));
        assertEquals(Enums.FrameGeneration.FI_12X, Enums.FrameGeneration.findByValue(5));
        assertNull(Enums.FrameGeneration.findByValue(999));
    }

    // --- ColorOrder ---

    @Test
    void colorOrder_findByValue() {
        assertEquals(Enums.ColorOrder.GRB_GRBW, Enums.ColorOrder.findByValue(1));
        assertEquals(Enums.ColorOrder.RGB_RGBW, Enums.ColorOrder.findByValue(2));
        assertEquals(Enums.ColorOrder.BGR_BGRW, Enums.ColorOrder.findByValue(3));
        assertEquals(Enums.ColorOrder.WBRG, Enums.ColorOrder.findByValue(24));
        assertNull(Enums.ColorOrder.findByValue(0));
        assertNull(Enums.ColorOrder.findByValue(25));
    }

    // --- BrightnessLimiter ---

    @Test
    void brightnessLimiter_findByValue() {
        assertEquals(Enums.BrightnessLimiter.BRIGHTNESS_LIMIT_DISABLED,
                Enums.BrightnessLimiter.findByValue(1.0F));
        assertEquals(Enums.BrightnessLimiter.BRIGHTNESS_LIMIT_50,
                Enums.BrightnessLimiter.findByValue(0.5F));
        assertEquals(Enums.BrightnessLimiter.BRIGHTNESS_LIMIT_90,
                Enums.BrightnessLimiter.findByValue(0.9F));
        assertNull(Enums.BrightnessLimiter.findByValue(0.25F));
    }

    // --- SimdAvxOption ---

    @Test
    void simdAvxOption_findByValue() {
        assertEquals(Enums.SimdAvxOption.AUTO, Enums.SimdAvxOption.findByValue(0));
        assertEquals(Enums.SimdAvxOption.AVX512, Enums.SimdAvxOption.findByValue(1));
        assertEquals(Enums.SimdAvxOption.AVX256, Enums.SimdAvxOption.findByValue(2));
        assertEquals(Enums.SimdAvxOption.AVX, Enums.SimdAvxOption.findByValue(3));
        assertEquals(Enums.SimdAvxOption.DISABLED, Enums.SimdAvxOption.findByValue(4));
        assertNull(Enums.SimdAvxOption.findByValue(5));
    }

    // --- EthernetBoards ---

    @Test
    void ethernetBoards_findByValue() {
        assertEquals(Enums.EthernetBoards.ETH_BOARD_QUINLED_32,
                Enums.EthernetBoards.findByValue(1));
        assertEquals(Enums.EthernetBoards.ETH_BOARD_WT32,
                Enums.EthernetBoards.findByValue(3));
        assertEquals(Enums.EthernetBoards.ETH_BOARD_GLEDOPTO,
                Enums.EthernetBoards.findByValue(9));
        assertNull(Enums.EthernetBoards.findByValue(999));
    }

    // --- ResamplingFactor ---

    @Test
    void resamplingFactor_findByValue() {
        assertEquals(Enums.ResamplingFactor.NATIVE, Enums.ResamplingFactor.findByValue(1));
        assertEquals(Enums.ResamplingFactor.VERY_GOOD, Enums.ResamplingFactor.findByValue(2));
        assertEquals(Enums.ResamplingFactor.BALANCED, Enums.ResamplingFactor.findByValue(4));
        assertEquals(Enums.ResamplingFactor.POOR, Enums.ResamplingFactor.findByValue(12));
        assertNull(Enums.ResamplingFactor.findByValue(99));
    }

    // --- CpuGpuLoadThreshold ---

    @Test
    void cpuGpuLoadThreshold_findByValue() {
        assertEquals(Enums.CpuGpuLoadThreshold.CPU_GPU_THRESHOLD_DISABLED,
                Enums.CpuGpuLoadThreshold.findByValue(0));
        assertEquals(Enums.CpuGpuLoadThreshold.CPU_GPU_THRESHOLD_50,
                Enums.CpuGpuLoadThreshold.findByValue(50));
        assertEquals(Enums.CpuGpuLoadThreshold.CPU_GPU_THRESHOLD_100,
                Enums.CpuGpuLoadThreshold.findByValue(100));
        assertNull(Enums.CpuGpuLoadThreshold.findByValue(55));
    }

    // --- LdrInterval ---

    @Test
    void ldrInterval_findByValue() {
        assertEquals(Enums.LdrInterval.CONTINUOUS, Enums.LdrInterval.findByValue(0));
        assertEquals(Enums.LdrInterval.MINUTES_10, Enums.LdrInterval.findByValue(10));
        assertEquals(Enums.LdrInterval.MINUTES_60, Enums.LdrInterval.findByValue(60));
        assertEquals(Enums.LdrInterval.MINUTES_120, Enums.LdrInterval.findByValue(120));
        assertNull(Enums.LdrInterval.findByValue(45));
    }

    // --- ColorEnum navigation ---

    @Test
    void colorEnum_nextAndPrevNavigation() {
        assertEquals(Enums.ColorEnum.YELLOW, Enums.ColorEnum.RED.next());
        assertEquals(Enums.ColorEnum.GREEN, Enums.ColorEnum.YELLOW.next());
        assertEquals(Enums.ColorEnum.YELLOW, Enums.ColorEnum.GREEN.prev());
        assertEquals(Enums.ColorEnum.RED, Enums.ColorEnum.MASTER.next());
        assertEquals(Enums.ColorEnum.MAGENTA, Enums.ColorEnum.RED.prev());
    }
}
