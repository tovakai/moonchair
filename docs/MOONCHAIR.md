# Moonchair

Moonberry theme foundation for Lawnchair 16.

This branch adds a dedicated palette that ignores wallpaper/Monet style presets, fixed Compose Material 3 roles, and Launcher3 tone swatches. Fresh installs default to dark plum surfaces, the existing **Meow** icon mask, and the existing **two-row dock**. Settings retain the ability to switch themes, icon shapes and dock rows. Existing saved preferences take precedence over defaults.

The original **Meow** shape and **Dock rows** slider were already present upstream; no duplicated implementation was introduced.

## Not yet implemented: pinned home widgets

The requested Symfonium widget must remain in a launcher-owned stationary layer across workspace pages, keep touch and RemoteViews updates, survive restore/restart, and stay invisible outside the homescreen. Overlay permissions or copying widgets to every page are not acceptable substitutes. This requires Launcher3 workspace lifecycle, model persistence and drag/resize integration.

## Test status

Unbuilt/unverified on Android in this environment. Review and device testing required before replacing a working launcher.
