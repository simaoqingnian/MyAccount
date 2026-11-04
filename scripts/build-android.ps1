param(
    [string]$FilmWordRoot = 'D:\03_work\09_android\FilmWord',
    [string]$GradleCacheRoot = '',
    [switch]$Online
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkPath = Join-Path $FilmWordRoot '.jdk\jdk-17'
$sdkPath = Join-Path $FilmWordRoot '.android-sdk'
$cachePath = $GradleCacheRoot
if ([string]::IsNullOrWhiteSpace($cachePath)) {
    $userCache = Join-Path $env:USERPROFILE '.gradle'
    if (Test-Path -LiteralPath (Join-Path $userCache 'caches\modules-2\files-2.1\com.android.tools.lint\lint-gradle\31.6.1')) { $cachePath = $userCache }
    else { $cachePath = Join-Path $FilmWordRoot '.gradle' }
}
if (!(Test-Path -LiteralPath (Join-Path $jdkPath 'bin\java.exe'))) { throw 'FilmWord JDK 17 was not found.' }
if (!(Test-Path -LiteralPath (Join-Path $sdkPath 'platforms\android-35\android.jar'))) { throw 'FilmWord Android SDK 35 was not found.' }
$env:JAVA_HOME = $jdkPath
$env:ANDROID_HOME = $sdkPath
$env:ANDROID_SDK_ROOT = $sdkPath
$env:GRADLE_USER_HOME = $cachePath
$sdkProperty = $sdkPath.Replace('\','/')
[System.IO.File]::WriteAllText((Join-Path $projectRoot 'local.properties'), "sdk.dir=$sdkProperty`n", [System.Text.UTF8Encoding]::new($false))
$signingDirectory = Join-Path $projectRoot '.android'
$signingPath = Join-Path $signingDirectory 'debug.keystore'
if (!(Test-Path -LiteralPath $signingPath)) {
    New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
    & (Join-Path $jdkPath 'bin\keytool.exe') -genkeypair -keystore $signingPath -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Heji Debug, O=MyAccount, C=CN'
    if ($LASTEXITCODE -ne 0) { throw 'Could not create project debug signing key.' }
}
$gradleArguments = @('--console=plain', '--no-daemon', "-Dorg.gradle.java.home=$jdkPath", ':app:assembleDebug', ':app:lintDebug')
if (!$Online) { $gradleArguments = @('--offline') + $gradleArguments }
Push-Location $projectRoot
try {
    $gradleLauncher = $null
    foreach ($distributionRoot in @((Join-Path $cachePath 'wrapper\dists\gradle-8.7-bin'),(Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-8.7-bin'))) {
        if (Test-Path -LiteralPath $distributionRoot) {
            $gradleLauncher = Get-ChildItem -LiteralPath $distributionRoot -Recurse -File -Filter 'gradle-launcher-8.7.jar' | Select-Object -First 1
            if ($null -ne $gradleLauncher) { break }
        }
    }
    if ($null -ne $gradleLauncher) {
        & (Join-Path $jdkPath 'bin\java.exe') -Xmx64m -Xms64m -classpath $gradleLauncher.FullName org.gradle.launcher.GradleMain @gradleArguments
    } else {
        & (Join-Path $projectRoot 'gradlew.bat') @gradleArguments
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed: $LASTEXITCODE" }
    $artifactDirectory = Join-Path $projectRoot 'artifacts'
    New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $artifactDirectory 'Heji-0.1.0-debug.apk') -Force
    Get-FileHash -LiteralPath (Join-Path $artifactDirectory 'Heji-0.1.0-debug.apk') -Algorithm SHA256
} finally { Pop-Location }
