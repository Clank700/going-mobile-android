@echo off
echo extract-game-assets.bat is an old name.
echo It now runs prepare-game.bat, which extracts assets AND prepares source.
echo.
call "%~dp0prepare-game.bat" %*
