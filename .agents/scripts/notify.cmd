@echo off
set TITLE=%~1
if "%TITLE%"=="" set TITLE=Task Done
set MSG=%~2
if "%MSG%"=="" set MSG=Finished requested changes

start "" powershell -NoProfile -Command "Add-Type -AssemblyName System.Speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('%MSG%'); Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('%MSG%', '%TITLE%')"
