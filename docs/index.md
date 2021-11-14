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
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.3.1).
- **Added an option menu to the Web Interface** where you can change all firmware related settings without the needs of reflashing the microcontroller.
- Added "Info Graphics" button. Linux only, the feature was already available in Windows.
- Added the possibility to start and stop the Bias Light effect via Web Interface.
- ESP8266 goes out of memory when serving Web Interface while driving many LEDs. Fixed.
- Fixed an error that caused white temperature to reset to default after WiFi disconnection.
- Fixed an error that prevented ESP32 to start the bias light effect when using a static IP address with MQTT disabled.
- Various improvements on ESP32, updated ESP-IDF to the latest 4.3.1.
- Arduino Bootstrapper update (v.1.11.4) memory optimizations.  
  
### In the previous release:
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.2.3).
- **MQTT is now optional for FULL firmware**.
- `Glow Worm Luciferin` FULL firmware now exposes a **Web Interface to control your lights from your browser** without the needs of the `Firefly Luciferin` PC client.
- `Glow Worm Luciferin` firmware can now be **controlled via standard HTTP methods** (GET/POST).
- Introducing **Luciferin Modules** for the **Luciferin Official PCB**.
- Luciferin is now able to detect what is the monitor on your right/center/left position for an easyer configuration. **On some multi monitor configurations it may be necessary to select the monitor again.** To do it go to "Settings -> Mode tab -> Bind to display".
- Fixed a bug that caused flickering on ESP32 when reducing the numbers of LEDs in use.
- Fixed a bug that caused occasional graphical glitches when right clicking the tray icon (Windows only).
- Upgrade to Java 17 and JavaFX 17.
- Arduino Bootstrapper update (v.1.11.2) improves UI during the initial setup.