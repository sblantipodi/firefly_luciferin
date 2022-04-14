package org.dpsoftware.managers.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LedMatrixInfo {

    int screenWidth;
    int screenHeight;
    int bottomRightLed;
    int rightLed;
    int topLed;
    int leftLed;
    int bottomLeftLed;
    int bottomRowLed;
    String splitBottomRow;
    String grabberTopBottom;
    String grabberSide;
    String gapTypeTopBottom;
    String gapTypeSide;

}
