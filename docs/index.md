<style>
.footer {
  display: none;
}
.px-3 {
  padding-right: 30px !important;
  padding-left: 10px !important;
}
.my-5 {
  margin-top: 10px !important;
  margin-bottom: 10px !important;
}
strong {
  font-weight: bold;
}
a {
  font-weight: bold;
  color: #E19A00FF;
}
</style>
<img align="right" width="100" height="100" src="https://raw.githubusercontent.com/sblantipodi/firefly_luciferin/master/data/img/luciferin_logo.png">

### In this release

- ***Hotfix release: This issue affects only Firefly Luciferin; no firmware update is required.***
- Reverted to Java 24 due to compatibility issues:
  - Some users experienced startup crashes on Windows caused by permission issues with temporary native files. If you are among the users experiencing this issue, please download the installer manually and install it over your current installation. Other user can use the automatic update offered by Luciferin. Closes [#354](https://github.com/sblantipodi/firefly_luciferin/issues/354).
  - Project cannot build on Linux with Java 25 from Temurin.

### In the previous release:

- ***Hotfix release: This issue affects only Firefly Luciferin; no firmware update is required.***
- **Added a setting to adjust screen capture quality.** Balanced is recommended for most users, while higher quality
  offers more precision at the cost of higher resource usage, and lower quality reduces the load on the system. This is
  particularly useful on lower-end hardware or
  in [multi-screen](https://github.com/sblantipodi/firefly_luciferin/wiki/Multi-monitor-support#screen-capture-quality)
  setups with high resolutions.
- **Installation improvements** (Windows only – not required on Linux):
    - Added a checkbox option to launch Firefly Luciferin immediately
      after [installation](https://github.com/sblantipodi/firefly_luciferin/wiki/Installers-and-binaries).
    - Enhanced
      the [auto-update process](https://github.com/sblantipodi/firefly_luciferin/wiki/Luciferin-update-management): no
      manual confirmation is needed in the UI anymore, and the app now starts
    automatically once the update is complete. This will take effect starting with the next update.
- Fixed a Linux-only issue that prevented Firefly Luciferin from running when multiple installation types were present
  on the system, such as .deb/.rpm, Snap, or Flatpak.
- Java/JavaFX 25, libs update, code refactor to avoid using deprecated methods, CI/CD pipeline improvements.

### Two Versions Ago:

- ***Hotfix release: This issue only affects Firefly Luciferin; there is no need to update the firmware.***
- Luciferin restarted after seemingly random periods of time. Fixed. Closes [#324](https://github.com/sblantipodi/firefly_luciferin/issues/324).
- Reduced latency in screen capture on Linux Wayland.
- Prevented invalid values from being entered during
  the [Single Device Multi Screen](https://github.com/sblantipodi/firefly_luciferin/wiki/Multi-monitor-support) setup
  configuration.
- Fixed an issue that caused incorrect colors when using
  the [smoothing](https://github.com/sblantipodi/firefly_luciferin/wiki/Smoothing-color-transitions) effect on Linux
  Wayland.
- Fixed an issue where settings could not be opened on Flatpak when an update was available.



[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
