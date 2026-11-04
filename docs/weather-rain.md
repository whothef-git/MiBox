# Home and Weather rain

The Weather card's rainy atmosphere uses near-vertical tapered streaks at several depths, speeds and opacities. Slowly drifting, layered noise clouds and a restrained day/night rain tint sit behind the existing glass forecast panels. The original wallpaper and forecast layout remain in place. Static ring-shaped droplets have been removed. Falling drops now trigger brief bottom-edge splashes: four small ballistic droplets and a flattening ripple fade after each impact, with density following rain intensity. These reuse existing particles and paints and appear only during reported rain. Home shares the same per-refresh particle motion and adds impacts on the visible app tiles.

Only the current local weather condition code activates rain; hourly/daily rain probabilities do not. Density varies from drizzle/light rain through moderate and heavy rain. Missing/stale current weather and clear conditions do not invent rainfall. Existing non-rain effects and the weather provider are retained.

Rain redraws follow display vsync, and positions use elapsed time with double-precision motion calculations rather than absolute float uptime. Drops wrap outside the viewport. A small gradient streak bitmap is reused, and the seamless 256 × 128 cloud texture is generated once on a short background thread. Draw-time paints, particle seeds and rectangles are reused. No new framework, network endpoint or bundled video asset is needed.

The static sky and forecast views use temporary hardware layers while Weather is visible. Those caches are released when leaving Weather or pausing the activity. Rain frame scheduling also stops when inactive; the cloud worker is interrupted when the view is detached. Home uses a separate transparent foreground rain view; Prayer Time and Smart Home do not display either rain overlay.

Validation on Sony Android TV, 31 October 2026:

- Debug and optimized normal APKs built successfully.
- All 37 `WeatherRainChecks` passed, covering condition classification, intensity, missing data, actual timed rain motion, bottom splash placement/movement, clear conditions and hidden-state behavior. Conditions in these checks are fixtures, not claimed live observations.
- Drizzle and heavy rain were shown with the existing labelled debug preview and recorded on the actual Weather card. Forecast readings were not replaced with simulated readings.
- Sampled unrecorded frame timing was around 17–18ms on this TV. Heavy-rain UI draw work had a median of about 0.84ms and render-command work about 2.25ms after caching. Screen recording adds substantial overhead; this is not a guarantee of locked 60fps or an exhaustive performance benchmark.
- Preview overrides automatically expire and are disabled in normal builds. The final installed normal APK follows live weather conditions.

Run the targeted checks with `:app:assembleDebugAndroidTest -PtestRunner=tv.widgetv.home.WeatherRainChecks`, then `am instrument -w tv.widgetv.home.test/tv.widgetv.home.WeatherRainChecks`.

## Home dock and wet app tiles

The Home dock bottom margin is 10dp instead of 22dp, moving its contents 24px down at the target 1920 � 1080 / 320dpi resolution. Tile size, spacing and header positions are unchanged. The visible focus underline retains about 23px of bottom clearance.

Home uses the Weather renderer's display-vsync scheduling, elapsed-time fall speeds, sideways drift and full-screen particle cycles. It does not use a separate 30fps cap. Foreground drops terminate at visible dock-card tops and create brief splashes; background drops pass behind the artwork and produce bottom-screen splashes. Collision bounds follow the actual visible cards rather than fixed app names or coordinates. The existing Weather clouds and tint remain on Weather only.

During current reported rain, each app gains 36 cached surface beads, fine water trails and three slowly sliding droplets. The static wet texture is a card foreground included in the temporary hardware-cached dock layer; moving beads remain in the rain view. Local reflected light fades to transparent before tile edges. There is no full-tile color wash or straight highlight border, preventing an extra rectangular edge on dark Nuvio or beIN Sports artwork. The existing rectangular app artwork itself retains its original shape.

Dry or unavailable conditions clear the wet foreground; leaving Home also clears it. Home rain pauses with the activity and does not change remote focus, launch apps, replace weather readings or send smart-device commands. Temporary dock/sky caches are released when no longer needed.

Validation on Sony Android TV, 4 November 2026:

- Debug, instrumentation and optimized release builds succeeded. The final optimized edge-fix APK was installed for review.
- All 54 `WeatherRainChecks` passed, including the existing forecast/rain checks, tile-top and dock-gap collision selection, hidden-dock behavior, cloudy Home transparency, wet-texture bounds, sliding beads, dry-state removal and transparent tile perimeters.
- Nuvio and beIN Sports were inspected and recorded during a clearly labeled debug rain preview. The simulation was cleared afterward; the normal installed build follows the provider's current condition code.
- A short unrecorded cached extra-wet preview sample measured 12ms median frame time and 0.54% janky frames. This sample preceded the transparent-edge fix and is not a locked-frame-rate guarantee. No smart-device commands were tested.
