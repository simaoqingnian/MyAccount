param([string]$FilmWordRoot = 'D:\03_work\09_android\FilmWord')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaBin = Join-Path $FilmWordRoot '.jdk\jdk-17\bin'
$outputDirectory = Join-Path $projectRoot 'artifacts\ledger-check'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
& (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -d $outputDirectory (Join-Path $projectRoot 'app\src\main\java\com\simaoqingnian\myaccount\Ledger.java') (Join-Path $PSScriptRoot 'checks\LedgerCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'Ledger compilation failed.' }
& (Join-Path $javaBin 'java.exe') -cp $outputDirectory LedgerCheck
if ($LASTEXITCODE -ne 0) { throw 'Ledger checks failed.' }
