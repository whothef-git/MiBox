# Al Quran Verse of the Day

The Prayer Time card replaces its active MyHadith section with **AL QURAN VERSE OF
THE DAY**. Original Arabic and Malay translation are fetched directly over HTTPS
from [Mengaji.my](https://www.mengaji.my/api-docs/). The provider identifies the
translation as Sheikh Abdullah Basmeih's Tafsir Pimpinan Ar-Rahman. This is a
third-party Quran API, not the official JAKIM prayer-time service.

The documented API needs no account, API key or external backend. `GET
https://api.mengaji.my/v1/random` returns the verse, Arabic, Malay, surah name and
reference together. If it repeats the previously shown ID, a single documented
`GET /v1/ayat/{id}` for the following global verse prevents an immediate repeat.
There is no retry loop, no bundled verse collection and no generated translation.
The API wording is preserved. Literal backslashes escaping quotation marks are removed after JSON decoding; the quotation marks and words remain unchanged.

## Rotation and offline behavior

- Rotation follows the current official JAKIM SGR01 prayer schedule: Subuh, Zohor,
  Asar, Maghrib and Isyak. Syuruk and midnight do not trigger a change.
- The interval is identified by the most recent prayer's actual timestamp. Before
  Subuh it remains the previous day's Isyak interval. Opening/reopening or restarting
  the app in the same interval keeps the same verse.
- Fetching happens only when Prayer Time is opened or remains visible when a new
  interval begins. Existing minute ticks and JAKIM refresh callbacks update the card;
  no new background service or polling loop is introduced.
- The latest fetched verse and interval are cached privately. It remains available
  offline within its interval. An earlier interval's content is not silently presented
  as the new verse. A new interval without Internet shows an unavailable message and
  retries no more than once per minute while visible.
- If official prayer times are unavailable, the card waits for them rather than
  presenting calculated fallback times as JAKIM rotation boundaries.

## Layout and transport

The full-width card, prayer timeline and 6dp heading/content gap are retained.
Heading and Malay text use Alexandria Regular. Arabic is fetched as pre-rendered
Madani artwork using the exact surah and ayah from the text response:
`https://cdn.islamic.network/quran/images/high-resolution/{surah}_{ayah}.png`.
The format is documented by [AlQuran Cloud / Islamic Network](https://alquran.cloud/cdn).
It avoids the incompatible Quran combining-mark mapping observed when the API's
Unicode text was shaped using the bundled Hafs font. It is not a Quran.com API.

Only transparent margins are cropped. Gray edge coverage is preserved when tinting
ink to the existing ivory color; diacritics and the verse-number ornament stay in
the source artwork. The Arabic has its own size budget, independent of Malay
text auto-sizing. Compact gaps and reclaimed bottom padding let the translation
use more space. The reference remains below it. Long translations use smaller
text to fit without scrolling or ellipsis. The provider footer stays hidden.

Artwork loads off the main thread, only for the displayed verse, and only the
latest image is held in memory. No Quran image collection is bundled or stored
on disk. After an app restart, Arabic artwork requires Internet even if the
current interval's text metadata remains cached. An artwork failure shows a clear
unavailable message rather than falling back to the mismatched font. Retry is
limited to once per minute while visible. The older Hafs font and its license
remain in source for the existing text tests; active rendering uses CDN artwork.
Image responses are limited to 2MiB and six million decoded pixels, with timeouts
and redirects disabled. Provider availability is an external dependency.

The client validates required fields and reference ranges, allows only the documented
provider URLs, refuses redirects, limits responses to 128KiB and uses connection/read
timeouts. Default TLS chain and hostname validation remain enabled. The API is an
online dependency; no availability guarantee or JAKIM content endorsement is implied.

## Checks

`PrayerVerseChecks` covers the five boundaries, midnight continuity, Syuruk exclusion,
original-text parsing, missing data, repeat prevention, stable interval cache, network
failure/backoff and short/long bilingual text fitting. It also fetches a real verse over
HTTPS directly from the Sony TV. Mock fixtures are confined to tests, never shown as
live content. No smart-home controls are exercised.

Build with `:app:assembleDebugAndroidTest -PtestRunner=tv.widgetv.home.PrayerVerseChecks`
and invoke `tv.widgetv.home.test/tv.widgetv.home.PrayerVerseChecks` using instrumentation.
The previous MyHadith provider/tests remain as historical source; the launcher no longer
instantiates that provider. Release shrinking removes unused code/resources.

Verified on 30 October 2026: optimized and debug builds passed; all 31 targeted checks
passed on the Sony Android 10 TV, including a real provider HTTPS request. The actual
Prayer Time screen displayed both scripts and the reference without scrolling. A cold
app reopen retained identical verse text and reference, with no crash entries for the
running release process. Prayer-boundary timing was simulated in automated checks;
testing did not wait for all five real prayer times. Other tabs and device controls
were outside this targeted validation. The preview APK is outside Git as
`../Octahedron-Quran-Verse.apk`; stable main is preserved in `../backups/pre-quran-verse/`.

31 October typography update: 34 checks passed on the TV, including actual API content
for 1:1 and 2:282 fitting completely with the new font, spacing and inline reference.
The optimized APK was installed and its Prayer Time screen visually inspected. No
crash entries were present for the running release process. The earlier Quran preview
is preserved in `../backups/pre-quran-typography/`.

## Madani artwork update — 4 November 2026

Actual TV previews covered 37:116, 2:255, 112:1, 49:13, 13:28, 16:90 and
31:17. The approved medium-long layout keeps the Arabic prominent and enlarges
the Malay translation using the available lower space. Temporary verse overrides
were removed; normal prayer-interval rotation is preserved.

`MadaniArtworkChecks` covers exact artwork URLs, invalid references, transparent
margin cropping, antialias coverage, quotation-escape cleanup, real HTTPS retrieval
and short/long bilingual fitting, including 2:282. These checks validate transport
and rendering; they are not an audit of the provider's full Quran corpus.

Final validation: debug, instrumentation and optimized release builds succeeded;
22 Madani artwork checks passed on the Sony TV, including live HTTPS and the
quotation-escape fixture. No smart-home commands were sent.
