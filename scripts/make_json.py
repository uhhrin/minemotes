#!/usr/bin/env python3
"""Generate Minemotes font providers + emotes.json from frame folders.

Expected layout (after convert.bat):
  src/main/resources/assets/minemotes/textures/emotes/<emoteName>/<emoteName>_001.png
  ...

Writes:
  src/main/resources/assets/minemotes/font/default.json
  src/main/resources/assets/minemotes/emotes.json

Run from the repository root:
  python scripts/make_json.py
"""

from __future__ import annotations

import json
import os
from pathlib import Path

MODID = "minemotes"
REPO_ROOT = Path(__file__).resolve().parents[1]
ASSETS = REPO_ROOT / "src" / "main" / "resources" / "assets" / MODID
BASE_DIR = ASSETS / "textures" / "emotes"
FONT_OUTPUT = ASSETS / "font" / "default.json"
ANIM_OUTPUT = ASSETS / "emotes.json"

# Private-use area start for emote glyphs
UNICODE_BASE = 0xE000

# Match in-game sizing used by Minemotes
ASCENT = 8
HEIGHT = 9


def main() -> None:
    if not BASE_DIR.is_dir():
        raise SystemExit(
            f"Missing emote frames directory:\n  {BASE_DIR}\n"
            "Add converted frame folders first (see README)."
        )

    unicode_counter = UNICODE_BASE
    providers: list[dict] = [
        {
            "type": "space",
            "advances": {
                "\uF801": -1,
            },
        }
    ]
    animations: dict[str, list[str]] = {}

    for emote in sorted(os.listdir(BASE_DIR)):
        emote_path = BASE_DIR / emote
        if not emote_path.is_dir():
            continue

        frames = sorted(f for f in os.listdir(emote_path) if f.lower().endswith(".png"))
        if not frames:
            continue

        frame_chars: list[str] = []
        for frame in frames:
            char = chr(unicode_counter)
            providers.append(
                {
                    "type": "bitmap",
                    "file": f"{MODID}:emotes/{emote}/{frame}",
                    "ascent": ASCENT,
                    "height": HEIGHT,
                    "chars": [char],
                }
            )
            frame_chars.append(char)
            unicode_counter += 1

        animations[emote] = frame_chars

    FONT_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    FONT_OUTPUT.write_text(json.dumps({"providers": providers}, indent=4) + "\n", encoding="utf-8")
    ANIM_OUTPUT.write_text(json.dumps(animations, indent=4) + "\n", encoding="utf-8")

    print(f"Generated {len(animations)} emotes")
    print(f" - {FONT_OUTPUT.relative_to(REPO_ROOT)}")
    print(f" - {ANIM_OUTPUT.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
