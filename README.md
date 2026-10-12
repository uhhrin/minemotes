# Minemotes

Fabric mod for **custom animated emotes** in Minecraft chat and on signs. Type `:emoteName:` (with autocomplete) and the matching glyph is rendered from your resource pack.

> **Bring your own art.** This repository ships the mod + tooling only. Third-party / streamer emote packs are **not** included — add your own licensed assets locally.

## Demo

[Watch the demo video](https://drive.google.com/file/d/14liOpzW26ULEAkPRE2s1u7LZPDlVFtcv/view?usp=sharing)

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Loader | Fabric Loader ≥ 0.18.4 |
| APIs | Fabric API, Fabric Language Kotlin |
| Java (dev / CI) | 21+ |

## Install (players)

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.1.
2. Drop into `mods/`:
   - Fabric API
   - Fabric Language Kotlin
   - `minemotes-<version>.jar` from [Releases](https://github.com/uhhrin/minemotes/releases)
3. Install the matching **emote resource pack** (also from Releases, or build your own — see below) and enable it in **Options → Resource Packs**.
4. Restart the client (and the server if you use dedicated). Emote rewriting runs on both sides for chat/signs.

Without a resource pack that defines the same glyphs, emote names still rewrite to private-use characters — they just won’t look like pictures.

## Features

- `:name:` chat rewriting with animated multi-frame emotes
- Sign text support (edit screen autocomplete + rendered signs)
- Client autocomplete / picker while typing
- Feature-based mod core so more systems can be added later

## Build

```bash
./gradlew build
```

Outputs:

- Mod jar: `build/libs/minemotes-<version>.jar`
- Emote pack zip: `./gradlew buildEmoteResourcePack` → `build/resourcepack/minemotes-emotes-resource-pack.zip`

Optional SHA-1 of the pack (useful for server `server.properties` forced packs):

```bash
./gradlew printEmoteResourcePackSha1
```

---

## Adding custom emotes (step by step)

You need **ffmpeg** on your PATH ([download](https://ffmpeg.org/download.html)) and **Python 3**.

### 1. Collect source images (local only)

Put your own PNG / GIF / WEBP files in:

```text
scripts/raw_emotes/
```

Naming rules:

- File name (without extension) becomes the emote name used in chat, e.g. `wave.gif` → `:wave:`
- Use only art you have rights to distribute (or keep it private and never commit it)
- This folder is **gitignored** on purpose — do not push raw sources

### 2. Convert sources into frame PNGs

From `scripts/raw_emotes/`:

```bat
convert.bat
```

What it does:

- PNG → one `32×32` frame: `<name>/<name>_001.png`
- GIF / WEBP → frames at 6 fps, scaled to `32×32`

Example result:

```text
scripts/raw_emotes/wave/wave_001.png
scripts/raw_emotes/wave/wave_002.png
scripts/raw_emotes/smile/smile_001.png
```

### 3. Copy frames into the mod assets

Copy each emote folder into:

```text
src/main/resources/assets/minemotes/textures/emotes/
```

So you end up with:

```text
src/main/resources/assets/minemotes/textures/emotes/wave/wave_001.png
src/main/resources/assets/minemotes/textures/emotes/wave/wave_002.png
src/main/resources/assets/minemotes/textures/emotes/smile/smile_001.png
```

### 4. Generate font + emote registry JSON

From the **repository root**:

```bash
python scripts/make_json.py
```

This writes:

- `src/main/resources/assets/minemotes/font/default.json` — bitmap font providers (Private Use Area starting at `U+E000`) plus a `-1` advance negative-space glyph
- `src/main/resources/assets/minemotes/emotes.json` — map of `emoteName → [frame glyphs…]` used for animation

### 5. Build the mod and the resource pack

```bash
./gradlew build buildEmoteResourcePack
```

- Install the jar in `mods/`
- Install `build/resourcepack/minemotes-emotes-resource-pack.zip` as a resource pack (client), or host it for a server forced pack

### 6. Use in-game

1. Enable the resource pack
2. Open chat and type `:wav` — autocomplete should list matches
3. Confirm with Enter / Tab (picker behavior) so `:wave:` becomes the emote glyph
4. Animated emotes cycle frames client-side while the base glyph is shown

---

## Emote pack layout (reference)

```text
minemotes-emotes-resource-pack.zip
├── pack.mcmeta
├── pack.png
└── assets/
    ├── minecraft/font/default.json    # references minemotes:default
    └── minemotes/
        ├── font/default.json          # bitmap providers
        ├── emotes.json                # name → glyphs
        └── textures/emotes/<name>/*.png
```

The Gradle task `buildEmoteResourcePack` assembles this from `src/main/resources/assets` + `resourcepack/pack.mcmeta`.

## Project layout

```text
src/main|client/...     Fabric mod (Kotlin + small Java mixins)
scripts/raw_emotes/     Local sources only (gitignored) + convert.bat
scripts/make_json.py    Generates font + emotes.json
resourcepack/           pack.mcmeta template for the zip
```

## Releases

Pushing a tag matching `v*` (example: `v1.0.0`) runs GitHub Actions, builds the mod + resource pack, and publishes a GitHub Release with both artifacts.

## License

[CC0 1.0](LICENSE) — code and tooling. **Your emote art remains yours** (or the original rights holder’s). Do not commit assets you do not own.
