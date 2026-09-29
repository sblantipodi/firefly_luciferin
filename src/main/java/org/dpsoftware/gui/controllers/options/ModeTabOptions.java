/*
  ModeTabOptions.java

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
package org.dpsoftware.gui.controllers.options;

import javafx.application.Platform;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Enums;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.ManagerSingleton;
import org.dpsoftware.utilities.CaptureDeviceUtilities;
import org.dpsoftware.utilities.CommonUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Options shared by the JavaFX Mode tab and the web settings page.
 */
public final class ModeTabOptions {

    /**
     * Prevents instantiation of this option provider.
     */
    private ModeTabOptions() {
    }

    /**
     * Returns the scaling percentages offered by the Mode tab.
     *
     * @return the available scaling percentages
     */
    public static List<String> scalingRatios() {
        return Arrays.stream(Enums.ScalingRatio.values()).map(Enums.ScalingRatio::getScalingRatio).toList();
    }

    /**
     * Returns display names in the same order as their monitor indices.
     *
     * @param displayManager the display manager used to enumerate monitors
     * @return the ordered display names
     */
    public static List<String> displayNames(DisplayManager displayManager) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < displayManager.displayNumber(); i++) {
            names.add(displayManager.getDisplayName(i));
        }
        return names;
    }

    /**
     * Finds the friendly names of available external video capture devices.
     *
     * @return distinct external device names
     */
    public static List<String> externalCaptureDeviceNames() {
        List<CaptureDeviceUtilities.CaptureDevice> devices = new ArrayList<>(ManagerSingleton.getInstance().getCaptureDevices());
        devices.addAll(CaptureDeviceUtilities.discover());
        return devices.stream().map(CaptureDeviceUtilities.CaptureDevice::getFriendlyName)
                .filter(name -> name != null && !name.isBlank()).distinct().toList();
    }

    /**
     * Sends available external device names to the JavaFX thread after discovery.
     * An empty list is sent immediately so the selector can initialize.
     *
     * @param onComplete receives the initial and discovered device names
     */
    public static void loadExternalCaptureDeviceNames(Consumer<List<String>> onComplete) {
        CommonUtility.delayMilliseconds(() -> {
            List<String> names = externalCaptureDeviceNames();
            if (!names.isEmpty()) {
                Platform.runLater(() -> onComplete.accept(names));
            }
        }, 10);
        onComplete.accept(new ArrayList<>());
    }

    /**
     * Returns the SIMD choices supported by the current CPU vector length.
     *
     * @param supportedSpeciesLength preferred CPU vector length
     * @return supported SIMD choices, including automatic and disabled
     */
    public static List<Enums.SimdAvxOption> simdOptions(int supportedSpeciesLength) {
        List<Enums.SimdAvxOption> options = new ArrayList<>();
        options.add(Enums.SimdAvxOption.AUTO);
        if (supportedSpeciesLength >= 16) {
            options.add(Enums.SimdAvxOption.AVX512);
        }
        if (supportedSpeciesLength >= 8) {
            options.add(Enums.SimdAvxOption.AVX256);
            options.add(Enums.SimdAvxOption.AVX);
        }
        options.add(Enums.SimdAvxOption.DISABLED);
        return options;
    }

    /**
     * Returns capture methods available for the current OS and source type.
     *
     * @param externalSource whether the selected source is an external video device
     * @return available capture methods with automatic selection first
     */
    public static List<Configuration.CaptureMethod> captureMethods(boolean externalSource) {
        List<Configuration.CaptureMethod> methods = new ArrayList<>();
        methods.add(Configuration.CaptureMethod.AUTO);
        if (NativeExecutor.isWindows()) {
            if (externalSource) {
                methods.add(Configuration.CaptureMethod.WIN_USB_VIDEO);
            } else {
                methods.addAll(List.of(Configuration.CaptureMethod.DDUPL_DX12,
                        Configuration.CaptureMethod.DDUPL_DX11, Configuration.CaptureMethod.WinAPI,
                        Configuration.CaptureMethod.CPU));
            }
        } else if (NativeExecutor.isMac()) {
            methods.add(Configuration.CaptureMethod.AVFVIDEOSRC);
        } else if (externalSource) {
            methods.addAll(List.of(Configuration.CaptureMethod.USB_VIDEO,
                    Configuration.CaptureMethod.USB_VIDEO_OPENGL,
                    Configuration.CaptureMethod.USB_VIDEO_NVIDIA,
                    Configuration.CaptureMethod.USB_VIDEO_AMD_HIP,
                    Configuration.CaptureMethod.USB_VIDEO_AMD_INTEL));
        } else if (NativeExecutor.isWayland()) {
            methods.addAll(List.of(Configuration.CaptureMethod.PIPEWIREXDG,
                    Configuration.CaptureMethod.PIPEWIREXDG_OPENGL,
                    Configuration.CaptureMethod.PIPEWIREXDG_NVIDIA,
                    Configuration.CaptureMethod.PIPEWIREXDG_AMD_HIP,
                    Configuration.CaptureMethod.PIPEWIREXDG_AMD_INTEL));
        } else {
            methods.addAll(List.of(Configuration.CaptureMethod.XIMAGESRC,
                    Configuration.CaptureMethod.XIMAGESRC_NVIDIA));
        }
        return methods;
    }
}
