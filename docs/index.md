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

- ***Hotfix release:*** Fixed an issue that prevents 21:9 screen resolutions to correctly capture the screen. No firmware upgrade needed.


### In the previous release:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.9.7).
- **Performance improvements and optimizations. Reduced system memory and VRAM usage**.
- Luciferin can now be easily integrated into your preferred Home Automation system like `Home Assistant`. MQTT Auto Discovery is now fully supported and **Firefly Luciferin is now able to add/upgrade/remove all the supported MQTT entities in one click**. If you used the manual integration previously, please remove it before using the automatic method. (Closes #101 and #94)
- Aspect ratio can now be changed via MQTT / Home Assistant. (Closes #83)
- Tooltips on tray icons now shows the name of the device in use, this will help you better understand which device is controlled by the Luciferian instance. (Closes #83)
- If power saving option is enabled, Luciferin now turns off the lights when you shut down your PC or when the screen saver is active. (Closes #102)
- Added a selector to choose the desired effect via Home Assistant. (Closes #105)
- Once Firefly Luciferin is properly paired with a Glow Worm device, any changes made in Firefly are now automatically transferred to the Glow Worm device.
- MQTT topic can now be configured via Web Interface.
- Framerate can now be fine tuned with 1 FPS precision.
- Audio samplerate is now configurable via the config file, this is an advanced option for devices where auto detection does not work as expected.
- Web installer has been updated for better compatibility during WiFi provisioning.
- ESP32 suffered from occasional flickering when driving LEDs via USB with full firmware, fixed.
- When starting up the computer Luciferin may throw an error saying it is unable to connect to the network. Fixed.
- Arduino Bootstrapper update (v.1.13.5).
- Updated to the latest Arduino Core for both ESP32 and ESP8266.
- Upgrade GitHub Actions to use Node.js 16.
