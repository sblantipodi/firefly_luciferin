/*
  CommonBinding.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2024  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.gui.bindings;

import org.dpsoftware.MainSingleton;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.config.Enums;
import org.dpsoftware.utilities.CommonUtility;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Common methods for jextracted bindings
 */
public class CommonBinding {

    /**
     * Get absolute path of an image, used for native access
     *
     * @param imgStr relative path
     * @return absolute path
     */
    public static String getIconPath(String imgStr) {
        if (NativeExecutor.isSystemTraySupported()) {
            String imgAbsolutePath = Objects.requireNonNull(CommonBinding.class.getResource(imgStr)).getPath()
                    .replace(Constants.JAVA_PREFIX, "").replace(Constants.FILE_PREFIX, "")
                    .split(Constants.FAT_JAR_NAME)[0] + Constants.CLASSES + imgStr;
            if (Files.exists(Paths.get(imgAbsolutePath))) {
                imgStr = imgAbsolutePath;
            } else {
                imgStr = Objects.requireNonNull(CommonBinding.class.getResource(imgStr)).getPath();
            }
        }
        return imgStr;
    }

    /**
     * Useful logic to choose a tray icon
     *
     * @param playerStatus player status
     * @return image path
     */
    public String computeImageToUse(Enums.PlayerStatus playerStatus) {
        String imagePlayRight = Constants.IMAGE_CONTROL_PLAY_RIGHT;
        String imagePlayWaitingRight = Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT;
        String imageStopRight = Constants.IMAGE_CONTROL_LOGO_RIGHT;
        String imageStopRightOff = Constants.IMAGE_CONTROL_LOGO_RIGHT_OFF;
        String imageGreyStopRight = Constants.IMAGE_CONTROL_GREY_RIGHT;
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            imagePlayRight = Constants.IMAGE_CONTROL_PLAY_RIGHT_GOLD;
            imagePlayWaitingRight = Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT_GOLD;
            imageStopRight = Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD;
            imageStopRightOff = Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD_OFF;
            imageGreyStopRight = Constants.IMAGE_CONTROL_GREY_RIGHT_GOLD;
        }
        return switch (playerStatus) {
            case PLAY ->
                    setImage(Constants.IMAGE_CONTROL_PLAY, imagePlayRight, Constants.IMAGE_CONTROL_PLAY_LEFT, Constants.IMAGE_CONTROL_PLAY_CENTER);
            case PLAY_WAITING ->
                    setImage(Constants.IMAGE_CONTROL_PLAY_WAITING, imagePlayWaitingRight, Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT, Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER);
            case STOP ->
                    setImage(Constants.IMAGE_TRAY_STOP, imageStopRight, Constants.IMAGE_CONTROL_LOGO_LEFT, Constants.IMAGE_CONTROL_LOGO_CENTER);
            case GREY ->
                    setImage(Constants.IMAGE_CONTROL_GREY, imageGreyStopRight, Constants.IMAGE_CONTROL_GREY_LEFT, Constants.IMAGE_CONTROL_GREY_CENTER);
            case OFF ->
                    setImage(Constants.IMAGE_CONTROL_LOGO_OFF, imageStopRightOff, Constants.IMAGE_CONTROL_LOGO_LEFT_OFF, Constants.IMAGE_CONTROL_LOGO_CENTER_OFF);
        };
    }

    /**
     * Set image
     *
     * @param imagePlay       image
     * @param imagePlayRight  image
     * @param imagePlayLeft   image
     * @param imagePlayCenter image
     * @return tray image
     */
    @SuppressWarnings("Duplicates")
    public String setImage(String imagePlay, String imagePlayRight, String imagePlayLeft, String imagePlayCenter) {
        String img = "";
        switch (MainSingleton.getInstance().whoAmI) {
            case 1 -> {
                if ((MainSingleton.getInstance().config.getMultiMonitor() == 1)) {
                    img = imagePlay;
                } else {
                    img = imagePlayRight;
                }
            }
            case 2 -> {
                if ((MainSingleton.getInstance().config.getMultiMonitor() == 2)) {
                    img = imagePlayLeft;
                } else {
                    img = imagePlayCenter;
                }
            }
            case 3 -> img = imagePlayLeft;
        }
        return img;
    }

}
