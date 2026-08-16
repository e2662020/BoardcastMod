# BoardcastMod

A client-side Fabric mod for **Minecraft 1.21**. It combines the useful parts of
Scoreboard Helper, Scoreboard Overhaul, OBS Overlay and a chat-regex logger into
one configurable mod.

## Features

1. **Real-time scoreboard CSV export**
   - Polls the scoreboard every tick by default and performs an atomic file
     rewrite as soon as any objective/holder/score changes.
   - Exports **all objectives and all score holders**, not only the sidebar.
   - Full CSV columns: `objective, objective_display, holder,
     holder_display, score, criterion, render_type, score_display[,
     timestamp]`.
   - Also writes a minimal Scoreboard Helper style CSV
     (`objective,holder,score`) to
     `.minecraft/config/boardcastmod/scoreboard_helper.csv`.
   - Default full output: `.minecraft/config/boardcastmod/scoreboard.csv`.
   - Force an immediate export with `/boardcastmod export`.

2. **Scoreboard Overhaul style sidebar + OBS overlay compatibility**
   - Replaces the vanilla sidebar renderer with a configurable one.
   - Title, holder, score and background colours are editable in the config UI.
   - Position, margins, min/max width, row height, max rows and text shadow are
      configurable. Scoreboard Overhaul style 1K/1M compact values are optional.
   - If `obs_overlay` / `obs-overlay` is installed, the native sidebar is
     automatically hidden so OBS can draw the scoreboard itself without double
     rendering. This can be disabled in the settings.
   - `/boardcastmod sidebar hide|show|toggle` also controls sidebar visibility.

3. **Health as hearts**
   - Health objectives can be shown as hearts (`NUMBER`, `HEARTS`, `AUTO`).
   - One heart equals 2 HP. Half hearts are supported:
     - `ICONS`: full hearts are drawn in the full-heart colour; the last half
       heart uses the half-heart colour.
     - `NUMBER_WITH_HEART`: renders values such as `3.5 ♥`.
   - AUTO enables hearts for the vanilla hearts render type or objectives whose
     name/criterion contains `health`, `hp`, `heart`, `生命`, `血量`, `血`.

4. **Timer scoreboards**
   - Timer objectives are detected by keywords (`timer`, `time`, `clock`,
     `计时`, `倒计时`, `cd`) and by criterion name.
   - The raw score is still exported to CSV every tick, so timer updates are
     transmitted in real time.
   - One-key switch to seconds: press **N** (configurable in vanilla controls)
     or use `/boardcastmod timer seconds`.
   - Supports raw ticks, decimal seconds (`12.5s`) and `mm:ss` clock format.
   - If the server timer is already in seconds, disable "Timer source is ticks".

5. **Client chat regex capture**
   - Captures GAME/overlay messages, player chat, system messages and outgoing
     chat input.
   - If any configured Java regex matches (`Matcher.find`), the whole message
     is appended to a txt file.
   - Default pattern is `.*` (capture everything). Default output:
     `.minecraft/config/boardcastmod/chat_capture.txt`.
   - Timestamp, case-insensitivity and automatic file rotation are configurable.

## Settings UI

- Uses **Cloth Config API** (required dependency).
- Fully integrated with **ModMenu**: open Mods -> BoardcastMod -> settings.
- Config file: `.minecraft/config/boardcastmod.json`.

## Commands

| Command | Action |
| --- | --- |
| `/boardcastmod export` / `csv` | Force scoreboard CSV export next tick |
| `/boardcastmod sidebar hide` | Hide the native sidebar |
| `/boardcastmod sidebar show` | Show the native sidebar |
| `/boardcastmod sidebar toggle` | Toggle sidebar visibility |
| `/boardcastmod timer seconds` | Toggle seconds mode for timer scoreboards |
| `/boardcastmod timer clock` | Toggle `mm:ss` clock formatting |

## Key bindings

- `N` — toggle timer seconds mode (category "BoardcastMod" in vanilla controls).

## Build

Requires JDK 21. The wrapper uses Gradle 9.4.

Windows:

```bat
gradlew.bat build
```

macOS / Linux:

```sh
chmod +x gradlew
./gradlew build
```

If the wrapper jar is missing, the scripts try to download it once. You can
also open this folder directly in IntelliJ IDEA and use IDEA's Gradle import.

The remapped jar is generated at `build/libs/BoardcastMod-1.0.0.jar`.

## Reference repositories

The mod combines concepts from Scoreboard Helper, Scoreboard Overhaul,
zziger/obs-overlay and a chat regex logger. Clone helpers for the referenced
repositories are available at `scripts/clone-reference-repos.bat` (Windows)
and `scripts/clone-reference-repos.sh` (macOS/Linux).
