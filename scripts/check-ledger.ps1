param([string]$FilmWordRoot = 'D:\03_work\09_android\FilmWord')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$javaBin = Join-Path $FilmWordRoot '.jdk\jdk-17\bin'
$outputDirectory = Join-Path $projectRoot 'artifacts\ledger-check'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$sourceDirectory = Join-Path $projectRoot 'app\src\main\java\com\simaoqingnian\myaccount'
$sources = @('Ledger.java','IconCatalog.java','EntrySession.java') | ForEach-Object { Join-Path $sourceDirectory $_ }
& (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -d $outputDirectory @sources (Join-Path $PSScriptRoot 'checks\LedgerCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'Ledger compilation failed.' }
& (Join-Path $javaBin 'java.exe') -cp $outputDirectory LedgerCheck
if ($LASTEXITCODE -ne 0) { throw 'Ledger checks failed.' }
$jsonCache = Join-Path $env:USERPROFILE '.gradle\caches\modules-2\files-2.1\org.json\json'
$jsonJar = Get-ChildItem -LiteralPath $jsonCache -Recurse -Filter 'json-20240303.jar' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($null -eq $jsonJar) { throw 'Host backup checks require a cached org.json 20240303 jar.' }
$classpath = $outputDirectory + ';' + $jsonJar.FullName
& (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -cp $classpath -d $outputDirectory (Join-Path $sourceDirectory 'BackupCodec.java') (Join-Path $PSScriptRoot 'checks\BackupCheck.java')
if ($LASTEXITCODE -ne 0) { throw 'Backup check compilation failed.' }
& (Join-Path $javaBin 'java.exe') -cp $classpath BackupCheck
if ($LASTEXITCODE -ne 0) { throw 'Backup checks failed.' }
