# Current Octahedron animated previews and screenshots

The README embeds six-second loops recorded directly on the TV on **1 November 2026, around 18:12–18:13 Malaysia time**. GIFs are encoded at **960 × 540, 15fps**, from 1920 × 1080 screen recordings:

- [Animated Home](octahedron-home.gif): ambient dock lighting, ticking seconds and the transition into gold Home-tab focus.
- [Animated Prayer Time](octahedron-prayer-time.gif): the current Quran card, ticking seconds and normal-speed sky.
- [Animated Weather](octahedron-weather.gif): current-condition falling rain, drifting clouds and bottom-edge splashes over the forecast layout.
- [Animated Smart Home](octahedron-smart-home.gif): the current device grid and ticking clock; no control is activated.

These loops use normal time and live provider conditions, with no weather override. Sky travel and moon-phase changes are too slow to demonstrate in six seconds. The Moon is not visible in these daytime captures; the clips do not simulate moonrise, accelerate phases or imply that illumination changes each loop. Looping naturally returns the captured clock to its starting value. Screen recording and GIF encoding are presentation samples, not a frame-rate benchmark.

## Earlier accelerated sky demonstration

[Moon-and-sun timelapse](octahedron-sky-timelapse.gif) is derived from the saved `Octahedron-moon-sun-timelapse.mp4` captured on the Sony TV on **31 October 2026 around 01:10**. It shows the approved textured Moon and Sun on accelerated night/day arcs. The saved recording is about 105 seconds; this 800 × 450, 12fps GIF plays it six times faster, in about 18 seconds.

This is a **historical demonstration**, preceding the later clock-seconds, moon-phase-label and gold-tab changes. It is not the current UI capture or a live moonrise/illumination sequence. The original preview accelerated celestial motion only, and its temporary code was removed from the normal launcher. The original MP4 remains in the local diagnostics archive outside Git.

## Original-resolution stills

All four current screenshots are direct **1920 × 1080 Sony BRAVIA captures from 1 November 2026**, around 18:07–18:08 Malaysia time:

- [Home](octahedron-home.png): local seconds, Saudi clock, Malay Hijri date, textured sun, original wallpaper and app dock.
- [Prayer Time](octahedron-prayer-time.png): official JAKIM SGR01 times, Arabic Quran verse in Uthmanic script, Malay translation and surah/ayah reference.
- [Weather](octahedron-weather.png): Sunrise, Sunset and Next Rain; eight hourly forecasts with rain chance and precipitation in millimetres; five-day outlook; current-condition rain and bottom splashes.
- [Smart Home](octahedron-smart-home.png): five Daikin ACs, two Tapo plugs and one Kasa power-strip card with three outlet states. Online/offline states are those reported at capture time.

Each capture has its screen tab focused, showing calendar-gold text and the short underline. The text-to-underline gap was verified at 25px on this TV. The tab focus design has no pill or glow.

The installed app is **Octahedron v3.0**, `versionCode 13`, package `tv.widgetv.home`. Its implementation matches main commit **`4b649e3`**, including approved changes through PR #15. No app version bump accompanies this documentation update.

The installed APK was pulled read-only and its SHA-256 matched the approved local `Octahedron-Calendar-Gold-Tabs.apk` exactly:

- Size: **5,479,775 bytes**.
- SHA-256: `064cce29e067b99bae8040a2cda15d4b35afab1582330b4973771d2870c733cd`.

These are actual UI captures, not mockups. Clocks, calendar content, weather and device states reflect capture time. Weather was not overridden, and no smart-device control commands were sent. The TV display was woken for capture and navigation returned to Home afterward. Generated APKs remain outside Git.

Other screenshot filenames in this directory are historical WidgeTV captures retained for older documentation; they are not the current Octahedron preview.
