/*
  EyeCareOptions.java

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

import com.fasterxml.jackson.databind.JsonNode;
import org.dpsoftware.MainSingleton;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.managers.NetworkManager;
import org.dpsoftware.managers.SerialManager;
import org.dpsoftware.managers.dto.LdrDto;
import org.dpsoftware.managers.dto.TcpResponse;
import org.dpsoftware.utilities.CommonUtility;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Eye care choices and LDR programming shared by JavaFX and web settings.
 */
public final class EyeCareOptions {

    /**
     * Prevents instantiation.
     */
    private EyeCareOptions() {
    }

    /**
     * Returns the brightness limits offered by the eye care dialog.
     *
     * @return brightness limiter choices
     */
    public static List<Enums.BrightnessLimiter> brightnessLimiters() {
        return Arrays.asList(Enums.BrightnessLimiter.values());
    }

    /**
     * Returns the LDR reading intervals offered by the eye care dialog.
     *
     * @return LDR interval choices
     */
    public static List<Enums.LdrInterval> ldrIntervals() {
        return Arrays.asList(Enums.LdrInterval.values());
    }

    /**
     * Returns minimum LED brightness choices in ten percent steps.
     *
     * @return minimum brightness percentages
     */
    public static List<Integer> minimumBrightnessValues() {
        return IntStream.rangeClosed(1, 10).map(step -> step * 10).boxed().toList();
    }

    /**
     * Returns the luminosity threshold values offered by the spinner.
     *
     * @return thresholds from zero to fifty percent
     */
    public static List<Integer> luminosityThresholdValues() {
        return IntStream.rangeClosed(0, 50).boxed().toList();
    }

    /**
     * Reads the LDR state from full firmware, as the JavaFX dialog does.
     *
     * @return current device controls, or empty when the device is unavailable
     */
    public static Optional<LdrControls> firmwareLdrControls() {
        try {
            TcpResponse response = NetworkManager.publishToTopic(Constants.HTTP_LDR, "", true);
            return response == null ? Optional.empty() : parseFirmwareLdr(response.getResponse());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a firmware LDR response without replacing valid saved settings with malformed data.
     *
     * @param response firmware JSON response
     * @return parsed controls, or empty for an incomplete response
     */
    public static Optional<LdrControls> parseFirmwareLdr(String response) {
        if (response == null || response.isBlank()) {
            return Optional.empty();
        }
        JsonNode data = CommonUtility.fromJsonToObject(response);
        if (data == null || !data.hasNonNull(Constants.HTTP_LDR_ENABLED)
                || !data.hasNonNull(Constants.HTTP_LDR_TURNOFF)
                || !data.hasNonNull(Constants.HTTP_LDR_INTERVAL)
                || !data.hasNonNull(Constants.HTTP_LDR_MIN)) {
            return Optional.empty();
        }
        try {
            int interval = Integer.parseInt(data.path(Constants.HTTP_LDR_INTERVAL).asText());
            int minimum = Integer.parseInt(data.path(Constants.HTTP_LDR_MIN).asText());
            if (Enums.LdrInterval.findByValue(interval) == null
                    || (minimum != 0 && !minimumBrightnessValues().contains(minimum))) {
                return Optional.empty();
            }
            return Optional.of(new LdrControls("1".equals(data.path(Constants.HTTP_LDR_ENABLED).asText()),
                    "1".equals(data.path(Constants.HTTP_LDR_TURNOFF).asText()), interval, minimum));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Applies and validates the LDR settings selected in either interface.
     *
     * @param config   configuration to update
     * @param enabled  whether the LDR is enabled
     * @param turnOff  whether LEDs turn off during readings
     * @param interval reading interval in minutes
     * @param minimum  minimum LED brightness percentage
     */
    public static void applyLdrControls(Configuration config, boolean enabled, boolean turnOff, int interval, int minimum) {
        if (Enums.LdrInterval.findByValue(interval) == null) {
            throw new IllegalArgumentException("Invalid LDR interval");
        }
        if (minimum != 0 && !minimumBrightnessValues().contains(minimum)) {
            throw new IllegalArgumentException("Invalid minimum LED brightness");
        }
        config.setEnableLDR(enabled);
        config.setLdrTurnOff(turnOff);
        config.setLdrInterval(interval);
        config.setLdrMin(minimum);
    }

    /**
     * Checks whether the LDR controls changed and need to be sent to the device.
     *
     * @param before saved configuration
     * @param after  updated configuration
     * @return true when any LDR setting changed
     */
    public static boolean ldrSettingsChanged(Configuration before, Configuration after) {
        return before.isEnableLDR() != after.isEnableLDR()
                || before.isLdrTurnOff() != after.isLdrTurnOff()
                || before.getLdrInterval() != after.getLdrInterval()
                || before.getLdrMin() != after.getLdrMin();
    }

    /**
     * Builds the firmware command for saving, calibrating or resetting the LDR.
     *
     * @param config selected LDR settings
     * @param action firmware action: 2 calibrate, 3 reset, 4 save
     * @return command to send to the device
     */
    public static LdrDto ldrCommand(Configuration config, int action) {
        if (action < 2 || action > 4) {
            throw new IllegalArgumentException("Invalid LDR action");
        }
        applyLdrControls(config, config.isEnableLDR(), config.isLdrTurnOff(), config.getLdrInterval(), config.getLdrMin());
        LdrDto dto = new LdrDto();
        dto.setLdrEnabled(config.isEnableLDR());
        dto.setLdrTurnOff(config.isLdrTurnOff());
        dto.setLdrInterval(config.getLdrInterval());
        dto.setLdrMin(config.getLdrMin());
        dto.setLdrAction(action);
        return dto;
    }

    /**
     * Sends an LDR command using the firmware or legacy serial transport.
     *
     * @param config     selected LDR settings
     * @param action     firmware action: 2 calibrate, 3 reset, 4 save
     * @param sendSerial sends legacy serial parameters when needed
     * @return firmware response, or null for legacy serial devices
     */
    public static TcpResponse programLdr(Configuration config, int action, Runnable sendSerial) {
        LdrDto dto = ldrCommand(config, action);
        Configuration runtime = MainSingleton.getInstance().config;
        applyLdrControls(runtime, dto.isLdrEnabled(), dto.isLdrTurnOff(), dto.getLdrInterval(), dto.getLdrMin());
        MainSingleton.getInstance().ldrAction = action;
        if (config.isFullFirmware()) {
            return NetworkManager.publishToTopic(NetworkManager.getTopic(Constants.HTTP_SET_LDR),
                    CommonUtility.toJsonString(dto), true);
        }
        sendSerial.run();
        return null;
    }

    /**
     * Sends an LDR command from the web interface using the current saved LED color.
     *
     * @param config selected LDR settings
     * @param action firmware action: 2 calibrate, 3 reset, 4 save
     * @return firmware response, or null for legacy serial devices
     */
    public static TcpResponse programLdr(Configuration config, int action) {
        return programLdr(config, action, () -> {
            String[] rgb = config.getColorChooser().split(",");
            new SerialManager().sendSerialParams(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]),
                    Integer.parseInt(rgb[2]));
        });
    }

    /**
     * Calibrates with the LEDs temporarily off when the LDR setting requires it.
     *
     * @param config  selected LDR settings
     * @param action  firmware action
     * @param turnOff temporarily turns off the LEDs
     * @param turnOn  restores the LEDs after calibration
     * @param program sends the LDR command
     * @return firmware response, or null for legacy serial devices
     */
    public static TcpResponse programWithCalibrationLighting(Configuration config, int action,
                                                             Runnable turnOff, Runnable turnOn,
                                                             java.util.function.Supplier<TcpResponse> program) {
        boolean ledWasOn = MainSingleton.getInstance().config.isToggleLed();
        boolean restoreLed = action == 2 && config.isLdrTurnOff() && ledWasOn;
        if (restoreLed) {
            turnOff.run();
        }
        try {
            if (action == 2 && ledWasOn) {
                CommonUtility.sleepSeconds(4);
            }
            return program.get();
        } finally {
            if (restoreLed) {
                CommonUtility.sleepSeconds(2);
                turnOn.run();
            }
        }
    }

    /**
     * Current LDR controls reported by the connected firmware.
     */
    public record LdrControls(boolean enabled, boolean turnOff, int interval, int minimum) {
    }
}
