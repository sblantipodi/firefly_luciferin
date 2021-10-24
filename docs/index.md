<style>

.footer {

  display: none;

}

.body {

  color: #202020;

  background-color: #F5F5F5;

}

.px-3 {

    padding-right: 30px !important;

    padding-left: 10px !important;

}

.my-5 {

    margin-top: 10px !important;

    margin-bottom: 10px !important;

}

</style>

### In this release:
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.2.3). 
- **MQTT is now optional for FULL firmware**. 
- `Glow Worm Luciferin` FULL firmware now exposes a **Web Interface to control your lights from your browser** without the needs of the `Firefly Luciferin` PC client.   
- `Glow Worm Luciferin` firmware can now be **controlled via standard HTTP methods** (GET/POST).  
- Introducing **Luciferin Modules** for the **Luciferin Official PCB**.  
- Luciferin is now able to detect what is the monitor on your right/center/left position for an easyer configuration. **On some multi monitor configurations it may be necessary to select the monitor again.** To do it go to "Settings -> Mode tab -> Bind to display".
- Fixed a bug that caused flickering on ESP32 when reducing the numbers of LEDs in use.  
- Fixed a bug that caused occasional graphical glitches when right clicking the tray icon (Windows only).  
- Upgrade to Java 17 and JavaFX 17.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.11.2) improves UI during the initial setup.

### In the previous release:
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.1.4). WiFi enhancements/fixes refers to full firmware.
- ***Enriched the "Info" popup*** with a graph that shows the quality of the synchronization between Firefly Luciferin PC software and the Glow Worm Luciferin firmware. Added a graph that shows the WiFi signal strength of the microcontroller in use. A good WiFi signal strength is required for reliable operation.
- ***Added support for DHCP, no need to enter a fixed IP address anymore.***
- ***Added WiFi signal strength info on "devices tab"***, this is useful when using multi devices.
- ***Improved WiFi signal strength by increasing the WiFi output power to +20.5dBm.***
- ***Fixed an heap fragmentation problem that caused severe slow down while using UDP stream.*** This problem occurs randomly after some time of screen capture.
- MQTT username and password are now optional. (if your MQTT server does not require credentials).
- Output Device menu now filters for valid COM ports by hiding the other ports.
- Fixed an error that prevented the bias light effect from starting if LEDs where turned off by an external sources like Home Assistant.
- Fixed a problem with auto update when using different MQTT topics for different devices. (thanks @pblOm)
- Fixed a bug that prevented the automatic black bar detection algorithm from detecting the letterbox mode on big 1080P TVs. (thanks @Marc)
- Fixed a bug that affected the Twinkle effect causing it to freeze at some point.
- Some routers do not display ESP8266 devices in the connected devices list. Fixed.
- [Arduino Bootstrapper](https://github.com/sblantipodi/arduino_bootstrapper/releases) update (v.1.10.3).