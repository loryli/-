# Installs a headless Android toolchain: JDK 21, Android cmdline-tools,
# platform-tools, API 35 platform + build-tools, emulator and one x86_64 system image.
# Idempotent. curl-only downloads with hard timeouts (Invoke-WebRequest hangs here).

$ErrorActionPreference = 'Continue'   # native tools (java, sdkmanager, adb) write to stderr
$ProgressPreference = 'SilentlyContinue'

function Log([string]$m) { '{0} {1}' -f (Get-Date -Format 'HH:mm:ss'), $m }

function Get-File([string]$Url, [string]$Out) {
    if (Test-Path $Out) { Remove-Item $Out -Force }
    Log "GET $Url"
    & curl.exe -sSL --fail --retry 2 --retry-delay 3 --connect-timeout 20 --max-time 1800 -o $Out $Url
    if ($LASTEXITCODE -ne 0) { throw "curl exit $LASTEXITCODE for $Url" }
    if (-not (Test-Path $Out) -or (Get-Item $Out).Length -le 0) { throw "empty file for $Url" }
    Log ('  -> {0:N1} MB' -f ((Get-Item $Out).Length / 1MB))
}

$SdkRoot = 'C:\Android\Sdk'
$JdkRoot = 'C:\Android\jdk21'
$Tmp     = Join-Path $env:TEMP 'android-bootstrap'
New-Item -ItemType Directory -Force -Path $Tmp, $SdkRoot | Out-Null

foreach ($d in 'C', 'D') {
    try {
        $di = New-Object System.IO.DriveInfo($d)
        Log ('DRIVE {0}: free {1:N1} GB / total {2:N1} GB' -f $d, ($di.AvailableFreeSpace / 1GB), ($di.TotalSize / 1GB))
    }
    catch { Log "DRIVE $d unavailable: $($_.Exception.Message)" }
}

# ---------- 1. JDK 21 ----------
if (-not (Test-Path (Join-Path $JdkRoot 'bin\java.exe'))) {
    $zip = Join-Path $Tmp 'jdk21.zip'

    # Tsinghua mirror: read the directory listing and pick the newest 21 zip.
    $tsinghua = @()
    try {
        $idx = Join-Path $Tmp 'tuna-index.html'
        & curl.exe -sSL --fail --connect-timeout 20 --max-time 60 -o $idx 'https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/windows/'
        if ($LASTEXITCODE -eq 0 -and (Test-Path $idx)) {
            $names = Select-String -Path $idx -Pattern 'OpenJDK21U-jdk_x64_windows_hotspot_[0-9._]+\.zip' -AllMatches |
                ForEach-Object { $_.Matches } | ForEach-Object { $_.Value } | Sort-Object -Unique | Sort-Object -Descending
            if ($names) { $tsinghua = @("https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/windows/$($names[0])") }
            Log ('tuna listing: ' + ($(if ($names) { $names[0] } else { 'no match' })))
        }
    }
    catch { Log "tuna listing failed: $($_.Exception.Message)" }

    $candidates = @(
        'https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.zip'
    ) + $tsinghua + @(
        'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse'
    )

    $got = $false
    if ((Test-Path $zip) -and (Get-Item $zip).Length -gt 100MB) {
        Log ('reusing cached jdk zip {0:N1} MB' -f ((Get-Item $zip).Length / 1MB))
        $got = $true
    }
    foreach ($u in $candidates) {
        if ($got) { break }
        try { Get-File $u $zip; $got = $true; Log "JDK source OK: $u"; break }
        catch { Log "JDK source failed: $($_.Exception.Message)" }
    }
    if (-not $got) { throw 'JDK 21: every source failed' }

    $stage = Join-Path $Tmp 'jdk-stage'
    if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $stage -Force
    $src = Get-ChildItem $stage -Directory | Where-Object Name -like 'jdk-*' | Select-Object -First 1
    if (-not $src) { throw 'JDK archive layout unexpected' }
    if (Test-Path $JdkRoot) { Remove-Item $JdkRoot -Recurse -Force }
    Move-Item $src.FullName $JdkRoot
    Log "JDK installed to $JdkRoot"
}
$env:JAVA_HOME = $JdkRoot
$env:PATH = "$JdkRoot\bin;$env:PATH"
Log ('java: ' + ((& "$JdkRoot\bin\java.exe" -version 2>&1) -join ' | '))

# ---------- 2. cmdline-tools ----------
$toolsRoot = Join-Path $SdkRoot 'cmdline-tools'
$binDir    = Join-Path $toolsRoot 'latest\bin'
if (-not (Test-Path (Join-Path $binDir 'sdkmanager.bat'))) {
    $candidates = @(
        'commandlinetools-win-13114758_latest.zip',
        'commandlinetools-win-12266719_latest.zip',
        'commandlinetools-win-11076708_latest.zip'
    )
    $zip = Join-Path $Tmp 'cmdline-tools.zip'
    $got = $false
    foreach ($c in $candidates) {
        try { Get-File "https://dl.google.com/android/repository/$c" $zip; $got = $true; Log "cmdline-tools OK: $c"; break }
        catch { Log "cmdline-tools failed: $($_.Exception.Message)" }
    }
    if (-not $got) { throw 'cmdline-tools: every candidate URL failed' }

    $stage = Join-Path $Tmp 'ct-stage'
    if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $stage -Force
    New-Item -ItemType Directory -Force -Path $toolsRoot | Out-Null
    if (Test-Path (Join-Path $toolsRoot 'latest')) { Remove-Item (Join-Path $toolsRoot 'latest') -Recurse -Force }
    Move-Item (Join-Path $stage 'cmdline-tools') (Join-Path $toolsRoot 'latest')
    Log 'cmdline-tools installed'
}

$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot
$sdkmanager = Join-Path $binDir 'sdkmanager.bat'

# ---------- 3. licenses + packages ----------
Log 'accepting SDK licenses'
$answers = (1..80 | ForEach-Object { 'y' }) -join "`n"
$answers | & $sdkmanager --sdk_root=$SdkRoot --licenses 2>&1 | Select-Object -Last 3 | ForEach-Object { Log "LIC $_" }

$packages = @(
    'platform-tools',
    'platforms;android-35',
    'build-tools;35.0.0',
    'emulator',
    'cmdline-tools;latest',
    'system-images;android-35;google_apis;x86_64'
)
Log ('installing: ' + ($packages -join ', '))
& $sdkmanager --sdk_root=$SdkRoot @packages 2>&1 | ForEach-Object { if ($_ -match '\S') { Log "SDK $_" } }

# ---------- 4. user environment ----------
[Environment]::SetEnvironmentVariable('JAVA_HOME', $JdkRoot, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', $SdkRoot, 'User')
[Environment]::SetEnvironmentVariable('ANDROID_SDK_ROOT', $SdkRoot, 'User')
$userPath = [Environment]::GetEnvironmentVariable('PATH', 'User')
foreach ($p in @("$JdkRoot\bin", "$SdkRoot\platform-tools", "$binDir", "$SdkRoot\emulator")) {
    if ($userPath -notlike "*$p*") { $userPath = "$userPath;$p" }
}
[Environment]::SetEnvironmentVariable('PATH', $userPath, 'User')
Log 'user env written (JAVA_HOME, ANDROID_HOME, ANDROID_SDK_ROOT, PATH)'

# ---------- 5. verify ----------
Log '--- VERIFY ---'
$adb = Join-Path $SdkRoot 'platform-tools\adb.exe'
if (Test-Path $adb) { Log ('adb: ' + ((& $adb version 2>&1) -join ' | ')) } else { Log 'adb: MISSING' }
Log ('java: ' + ((& "$JdkRoot\bin\java.exe" -version 2>&1)[0]))
& $sdkmanager --sdk_root=$SdkRoot --list_installed 2>&1 | Where-Object { $_ -match '\S' } | ForEach-Object { Log "INST $_" }
Log 'DONE'
