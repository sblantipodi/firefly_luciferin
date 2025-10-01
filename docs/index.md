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

-
***This update is exclusive to Firefly Luciferin. No firmware update required if running Glow Worm Luciferin v5.22.4.***
- **Added a setting to adjust screen capture quality.** Balanced is recommended for most users, while higher quality
  offers more precision at the cost of higher resource usage, and lower quality reduces the load on the system.
- **Installation improvements** (Windows only – not required on Linux):
    - Added a checkbox option to launch Firefly Luciferin immediately after installation.
  - Enhanced the auto-update process: no manual confirmation is needed in the UI anymore, and the app now starts
    automatically once the update is complete. This will take effect starting with the next update.
- Fixed a Linux-only issue that prevented Firefly Luciferin from running when multiple installation types were present
  on the system, such as .deb/.rpm, Snap, or Flatpak.
- Java/JavaFX 25, libs update, code refactor to avoid using deprecated methods, CI/CD pipeline improvements.

### In the previous release:

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

### Two Versions Ago:

- ***Breaking changes***: requires `Glow Worm Luciferin` firmware (v5.22.4).
- **[Smoothing](https://github.com/sblantipodi/firefly_luciferin/wiki/Smoothing-color-transitions): better frame time stability during high GPU load.**
- Added an option
  to [invert the relay GPIO signal](https://github.com/sblantipodi/firefly_luciferin/wiki/Power-saving-features#add-a-relay-to-cut-power-to-the-led-strip),
  useful for supporting devices such
  as [Gledopto](https://github.com/sblantipodi/firefly_luciferin/wiki/Compatible-Hardware#pre-build-boards-support).
- Implemented [automatic profile activation](https://github.com/sblantipodi/firefly_luciferin/wiki/Profiles) when
  specific processes are running, or when CPU/GPU reach a certain load, or when a fullscreen app is detected. Useful,
  for example, to switch between a normal and a gaming profile.
  Closes [#311](https://github.com/sblantipodi/firefly_luciferin/issues/311).
- Led configuration: allow entering a value from 0 to 95% instead of selecting from a list, for more precise gap
  adjustment.
- Small UI improvements.
- Fixed reduced brightness when
  using [EMA color transition smoothing](https://github.com/sblantipodi/firefly_luciferin/wiki/Smoothing-color-transitions).
- Smoothing was not working
  on [Single Device Multi Screen](https://github.com/sblantipodi/firefly_luciferin/wiki/Multi-monitor-support) setup.
  Fixed.
- Fixed an issue that prevented correct LED alignment with the screen when using
  the [Single Device Multi Screen](https://github.com/sblantipodi/firefly_luciferin/wiki/Multi-monitor-support) setup.
- [Home Assistant](https://github.com/sblantipodi/firefly_luciferin/wiki/Home-Automation-configs) logs were cluttered
  with warnings due to changes in their entities. This has been fixed.
  Closes [#318](https://github.com/sblantipodi/firefly_luciferin/issues/318).
- Luciferin stops working after the screen turns off due to power saving on Linux. Fixed.
  Closes [#288](https://github.com/sblantipodi/firefly_luciferin/issues/288).
- Updated to Java/JavaFX 24 and other libraries. Deprecated code has been updated and warnings have been resolved.


[Click here for the complete changelog of previous versions.](https://github.com/sblantipodi/firefly_luciferin/releases)
