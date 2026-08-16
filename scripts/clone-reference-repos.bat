@echo off
setlocal
set ROOT=%~dp0reference-repos
if not exist "%ROOT%" mkdir "%ROOT%"
cd /d "%ROOT%"

git clone --depth 1 https://github.com/TmallKing1/ScoreboardHelper.git
git clone --depth 1 https://github.com/wifi-left/ScoreboardDataBackupMod.git
git clone --depth 1 https://github.com/iL6hua/Minecraft-Digging-Leaderboard.git
git clone --depth 1 https://gitlab.com/HorrificDev/scoreboard-overhaul.git
git clone --depth 1 https://github.com/shedaniel/cloth-config.git
git clone --depth 1 https://github.com/TerraformersMC/ModMenu.git
git clone --depth 1 https://github.com/zziger/obs-overlay.git
git clone --depth 1 https://github.com/BlackShadowHRD/boti_timer.git
git clone --depth 1 https://github.com/BouncingElf10/timelesslib.git

echo Done. Non-git references:
echo   https://modrinth.com/mod/playerhp
echo   https://www.curseforge.com/minecraft/mc-mods/display-hearts
echo   https://modrinth.com/mod/callout
echo   https://modrinth.com/mod/mc-looper
echo   https://metamods.ru/mods/advancedchatlog
endlocal
