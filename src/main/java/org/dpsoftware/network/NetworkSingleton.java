/*
  NetworkSingleton.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright © 2020 - 2025  Davide Perini  (https://github.com/sblantipodi)

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
package org.dpsoftware.network;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dpsoftware.MainSingleton;

/**
 * Network singleton used to share common data
 */
@Getter
@Setter
@NoArgsConstructor
public class NetworkSingleton {

    @Getter
    private final static NetworkSingleton instance;

    static {
        instance = new NetworkSingleton();
    }

    public boolean udpBroadcastReceiverRunning = false;
    public MessageClient msgClient;
    public boolean closeServer = false;
    public int totalLedNum = MainSingleton.getInstance().ledNumber;
    public MessageServer messageServer;

}

