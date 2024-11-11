package org.dpsoftware.gui;

import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Constants;
import org.dpsoftware.utilities.CommonUtility;

import javax.swing.*;
import java.awt.*;

public abstract class TrayIconBase {

    // Tray icons
    Image imagePlay, imagePlayCenter, imagePlayLeft, imagePlayRight, imagePlayWaiting, imagePlayWaitingCenter, imagePlayWaitingLeft, imagePlayWaitingRight;
    Image imageStop, imageStopCenter, imageStopLeft, imageStopRight, imageStopOff, imageStopCenterOff, imageStopLeftOff, imageStopRightOff;
    Image imageGreyStop, imageGreyStopCenter, imageGreyStopLeft, imageGreyStopRight;
    public Timer timer;

    /**
     * Initialize images for the tray icon
     */
    public void initializeImages() {
        // load an image
        imagePlay = getImage(Constants.IMAGE_CONTROL_PLAY);
        imagePlayCenter = getImage(Constants.IMAGE_CONTROL_PLAY_CENTER);
        imagePlayLeft = getImage(Constants.IMAGE_CONTROL_PLAY_LEFT);
        imagePlayRight = getImage(Constants.IMAGE_CONTROL_PLAY_RIGHT);
        imagePlayWaiting = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING);
        imagePlayWaitingCenter = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER);
        imagePlayWaitingLeft = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT);
        imagePlayWaitingRight = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT);
        imageStop = getImage(Constants.IMAGE_TRAY_STOP);
        imageStopOff = getImage(Constants.IMAGE_CONTROL_LOGO_OFF);
        imageStopCenter = getImage(Constants.IMAGE_CONTROL_LOGO_CENTER);
        imageStopLeft = getImage(Constants.IMAGE_CONTROL_LOGO_LEFT);
        imageStopRight = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT);
        imageStopCenterOff = getImage(Constants.IMAGE_CONTROL_LOGO_CENTER_OFF);
        imageStopLeftOff = getImage(Constants.IMAGE_CONTROL_LOGO_LEFT_OFF);
        imageStopRightOff = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT_OFF);
        imageGreyStop = getImage(Constants.IMAGE_CONTROL_GREY);
        imageGreyStopCenter = getImage(Constants.IMAGE_CONTROL_GREY_CENTER);
        imageGreyStopLeft = getImage(Constants.IMAGE_CONTROL_GREY_LEFT);
        imageGreyStopRight = getImage(Constants.IMAGE_CONTROL_GREY_RIGHT);
        if (CommonUtility.isSingleDeviceMultiScreen()) {
            imagePlayRight = getImage(Constants.IMAGE_CONTROL_PLAY_RIGHT_GOLD);
            imagePlayWaitingRight = getImage(Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT_GOLD);
            imageStopRight = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD);
            imageStopRightOff = getImage(Constants.IMAGE_CONTROL_LOGO_RIGHT_GOLD_OFF);
            imageGreyStopRight = getImage(Constants.IMAGE_CONTROL_GREY_RIGHT_GOLD);
        }
    }

    /**
     * Create an image from a path
     *
     * @param imgPath image path
     * @return Image
     */
    @SuppressWarnings("all")
    private Image getImage(String imgPath) {
        if (NativeExecutor.isLinux()) {
            return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(imgPath)).getScaledInstance(16, 16, Image.SCALE_DEFAULT);
        } else {
            return Toolkit.getDefaultToolkit().getImage(this.getClass().getResource(imgPath));
        }
    }

}
