@echo off
:: Attende 5 secondi prima di iniziare
:: timeout /t 5 /nobreak >nul

:CHECK_INSTALL
:: Conta quanti processi msiexec.exe sono attivi
set /a count=0
for /f %%A in ('tasklist ^| find /I /C "msiexec.exe"') do set count=%%A

if %count% GEQ 2 (
    echo Installazione in corso, attendo...
    timeout /t 5 /nobreak >nul
    goto CHECK_INSTALL
)

:: Se non c'è installazione, esegue il comando passato come argomento
if "%~1"=="" (
    echo Errore: Nessun comando specificato.
    exit /b 1
)

echo Eseguo il comando: %*
%*
