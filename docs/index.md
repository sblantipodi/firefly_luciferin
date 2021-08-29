[comment]: <> (<style>)

[comment]: <> (.footer {)

[comment]: <> (  display: none;)

[comment]: <> (})

[comment]: <> (.body {)

[comment]: <> (  color: #202020;)

[comment]: <> (  background-color: #F5F5F5;)

[comment]: <> (})

[comment]: <> (.px-3 {)

[comment]: <> (    padding-right: 30px !important;)

[comment]: <> (    padding-left: 10px !important;)

[comment]: <> (})

[comment]: <> (.my-5 {)

[comment]: <> (    margin-top: 10px !important;)

[comment]: <> (    margin-bottom: 10px !important;)

[comment]: <> (})

[comment]: <> (</style>)

### In this release:
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.1.4)
- ***Added support for DHCP, no need to enter the microcontroller's IP address anymore.***
- ***MQTT username and password are now optional.*** (if your MQTT server does not require credentials).
- ***Enriched the "Info" popup*** with a graph that shows the quality of the synchronization between Firefly Luciferin PC software and the Glow Worm Luciferin firmware. Added a graph that shows the WiFi signal strength of the microcontroller in use. A good WiFi signal strength is required for reliable operation.
- ***Added WiFi signal strength info on "devices tab"***, this is useful when using multi devices.
- ***Improved WiFi signal strenght by highering the  WiFi output power to +20.5dBm.***
- ***Fixed an heap fragmentation problem that caused severe slow down while using UDP stream.*** This problem occurs randomly after some time of screen capture.
- Fixed an error that prevented the bias light effect from starting if LEDs where turned off by an external sources like Home Assistant.
- Fixed a problem with auto update when using different MQTT topics for different devices.
- Fixed a bug that affected the Twinkle effect causing it to freeze at some point. 
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.9.2).


### In the previous release:
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.0.1)
- ***Launching Luciferin Official PCBs.*** If you don't want to design your own PCB and you have a soldering iron, you might find ***Luciferin's official PCB*** interesting. If you have an existing setup or want to design your own PCB, you can do it.
- ***DMA mode (Direct Memory Access) and UART mode is now supported.*** Please use the right GPIO to enable these modes. GPIO2 is now the default pin.  
- ***UDP wireless stream has been added to the MQTT stream*** and now it's the default option for wireless streaming.
- ***Massive performance increase.*** Thanks to DMA/UART and UDP stream you can now run 500+LEDs at 60+FPS and 200LEDs at 144+FPS. Frametime has been widely reduced. Wireless stream is now 3 times faster on ESP8266 and 7 times faster on ESP32. ESP8266 continue to be the recommended option due to a more mature ecosystem.
- ***Added two power saving features:***
    - Turn off LEDs for long inactivity period
    -  Add a relay to cut power to the LED strip
- ***Added support for themes***, "Dark theme" added. 
- After this update, all upcoming updates will be ***notified with a complete changelog on what's new***.
- ***Added new light effects:***
    - Fire
    - Twinkle
    - Chase rainbow
- Fixed an issue that prevented capture to start and stop by double clicking the tray icon.
- When you change the total number of LEDs in use, the microcontroller gets the update instantly and optimizes performance for the number of LEDs in use, no need to restart the microcontroller.
- This is a major update, it will erase ESP32 devices, please reconfigure them.
- Preparing the support for ESP32-C3 and ESP32-S2.
