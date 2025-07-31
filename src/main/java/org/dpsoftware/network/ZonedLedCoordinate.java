package org.dpsoftware.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.dpsoftware.config.Enums;

import java.awt.*;

@Getter
@Setter
@AllArgsConstructor
public class ZonedLedCoordinate {

    private int monitorNumber;
    private Enums.PossibleZones zone;
    private Color color;

}
