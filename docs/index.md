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
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.4.9).
- **Introducing the perfect case for the Official Printed Circuit Board for Luciferin.** Thank you @pbl0m for this wonderful work!
- **Luciferin can now be easily translated in your favourite language.**   
  Do you want to help with the translations? 
  Current available languages (more languages coming soon):
  - Spanish (thanks @morganflint)
  - German (thanks @Maaaaarc)
  - Hungarian (thanks @kopidoo)
  - Italian (thanks @sblantipodi)
  - Russian (thanks @whakru, @kreanmire)
- **Added a new effect**: Stereo VU Meter.
- Added the possibility to configure the number of leds via Web Interface.
- First LED offset can now be easily configured via presets which calculate the right value automatically.
- Check for default audio device on Native or Software Audio capture.
- Improved input validation, some values can cause exceptions at runtime.
- Linux version does not support the tray icon, Firefly Luciferin now starts minimized.
- Linux version now shows only one icon on multi monitor setup.
- Fixed a problem that caused DNS IP being interpreted as a subnet mask value.
- Fixed an issue in the Web Interface that prevents to correctly enter MQTT settings.
- Fixed a problem that make ESP8266 to bootloop on some routers when using DHCP.
- Mitigation to LOGBACK-1591 vulnerability.
- Fixed a problem that caused stuttering when using Rainbow Music Mode.
- Fixed an issue that prevented night mode from activating when using Full firmware without MQTT.
- Fixed an issue that prevented a third monitor from working properly when using Full firmware without MQTT.
- Arduino Bootstrapper update (v.1.12.6).

### In the previous release:
- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.3.1).
- **Added an option menu to the Web Interface** where you can change all firmware related settings without the needs of reflashing the microcontroller.
- Added "Info Graphics" button. Linux only, the feature was already available in Windows.
- Added the possibility to start and stop the Bias Light effect via Web Interface.
- ESP8266 goes out of memory when serving Web Interface while driving many LEDs. Fixed.
- Fixed an error that caused white temperature to reset to default after WiFi disconnection.
- Fixed an error that prevented ESP32 to start the bias light effect when using a static IP address with MQTT disabled.
- Various improvements on ESP32, updated ESP-IDF to the latest 4.3.1.
- Arduino Bootstrapper update (v.1.11.4) memory optimizations.  
