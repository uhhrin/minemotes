@echo off
setlocal enabledelayedexpansion

REM Convert PNG / GIF / WEBP sources in this folder into per-emote frame folders.
REM Requires ffmpeg on PATH: https://ffmpeg.org/download.html
REM
REM Output example:
REM   smile\smile_001.png
REM   wave\wave_001.png
REM   wave\wave_002.png
REM
REM Afterwards, copy those folders into:
REM   src\main\resources\assets\minemotes\textures\emotes\

echo Processing emotes in %CD% ...

for %%f in (*.*) do (
    set "filename=%%~nf"
    set "ext=%%~xf"

    if /I "!ext!"==".png" (
        mkdir "!filename!" 2>nul
        echo Processing PNG: %%f
        ffmpeg -y -i "%%f" -vf "scale=32:32" "!filename!\!filename!_001.png"
    )

    if /I "!ext!"==".gif" (
        mkdir "!filename!" 2>nul
        echo Processing GIF: %%f
        ffmpeg -y -i "%%f" -vf "fps=6,scale=32:32" "!filename!\!filename!_%%03d.png"
    )

    if /I "!ext!"==".webp" (
        mkdir "!filename!" 2>nul
        echo Processing WEBP: %%f
        ffmpeg -y -analyzeduration 100M -probesize 100M -i "%%f" -vf "fps=6,scale=32:32" "!filename!\!filename!_%%03d.png"
    )
)

echo Done!
echo Next: copy the generated folders into src\main\resources\assets\minemotes\textures\emotes\
pause
