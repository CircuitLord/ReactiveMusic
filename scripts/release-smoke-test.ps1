param(
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$Gradlew = Join-Path $Root "gradlew.bat"
$OutputRoot = Join-Path $Root "build\smoke-test"
$InstanceRoot = Join-Path $OutputRoot "instances"
$CacheRoot = Join-Path $env:LOCALAPPDATA "ReactiveMusic\smoke-test"
$MinecraftRoot = Join-Path $CacheRoot "minecraft"
$DownloadRoot = Join-Path $CacheRoot "downloads"
$HeadlessMcVersion = "2.10.0"
$RuntimeTestVersion = "4.5.1"
$HeadlessMc = Join-Path $CacheRoot "headlessmc-launcher-$HeadlessMcVersion.jar"
$Java17 = "C:\Program Files\Java\jdk-17\bin\java.exe"
$Java21 = "C:\Program Files\Java\jdk-21\bin\java.exe"
$StartedAt = Get-Date

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;

public static class ProcessAudioMuter
{
    public static bool Mute(int processId)
    {
        IMMDeviceEnumerator deviceEnumerator = null;
        IMMDevice device = null;
        IAudioSessionManager2 manager = null;
        IAudioSessionEnumerator sessions = null;
        bool muted = false;
        try
        {
            deviceEnumerator = (IMMDeviceEnumerator)new MMDeviceEnumerator();
            deviceEnumerator.GetDefaultAudioEndpoint(EDataFlow.Render, ERole.Multimedia, out device);
            Guid managerId = typeof(IAudioSessionManager2).GUID;
            object managerObject;
            device.Activate(ref managerId, 23, IntPtr.Zero, out managerObject);
            manager = (IAudioSessionManager2)managerObject;
            manager.GetSessionEnumerator(out sessions);
            int count;
            sessions.GetCount(out count);
            for (int i = 0; i < count; i++)
            {
                IAudioSessionControl2 control = null;
                try
                {
                    sessions.GetSession(i, out control);
                    int sessionProcessId;
                    control.GetProcessId(out sessionProcessId);
                    if (sessionProcessId == processId)
                    {
                        ISimpleAudioVolume volume = control as ISimpleAudioVolume;
                        if (volume != null)
                        {
                            Guid context = Guid.Empty;
                            volume.SetMute(true, ref context);
                            muted = true;
                        }
                    }
                }
                finally
                {
                    if (control != null) Marshal.ReleaseComObject(control);
                }
            }
        }
        finally
        {
            if (sessions != null) Marshal.ReleaseComObject(sessions);
            if (manager != null) Marshal.ReleaseComObject(manager);
            if (device != null) Marshal.ReleaseComObject(device);
            if (deviceEnumerator != null) Marshal.ReleaseComObject(deviceEnumerator);
        }
        return muted;
    }

    private enum EDataFlow { Render, Capture, All }
    private enum ERole { Console, Multimedia, Communications }

    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")]
    private class MMDeviceEnumerator { }

    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDeviceEnumerator
    {
        int NotImpl1();
        [PreserveSig] int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice device);
    }

    [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IMMDevice
    {
        [PreserveSig] int Activate(ref Guid id, int clsCtx, IntPtr activationParams, [MarshalAs(UnmanagedType.IUnknown)] out object iface);
    }

    [ComImport, Guid("77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IAudioSessionManager2
    {
        int NotImpl1();
        int NotImpl2();
        [PreserveSig] int GetSessionEnumerator(out IAudioSessionEnumerator sessions);
    }

    [ComImport, Guid("E2F5BB11-0570-40CA-ACDD-3AA01277DEE8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IAudioSessionEnumerator
    {
        [PreserveSig] int GetCount(out int count);
        [PreserveSig] int GetSession(int index, out IAudioSessionControl2 session);
    }

    [ComImport, Guid("BFB7FF88-7239-4FC9-8FA2-07C950BE9C6D"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IAudioSessionControl2
    {
        int NotImpl0();
        [PreserveSig] int GetDisplayName([MarshalAs(UnmanagedType.LPWStr)] out string value);
        [PreserveSig] int SetDisplayName([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid context);
        [PreserveSig] int GetIconPath([MarshalAs(UnmanagedType.LPWStr)] out string value);
        [PreserveSig] int SetIconPath([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid context);
        [PreserveSig] int GetGroupingParam(out Guid value);
        [PreserveSig] int SetGroupingParam(ref Guid value, ref Guid context);
        int NotImpl1();
        int NotImpl2();
        [PreserveSig] int GetSessionIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string value);
        [PreserveSig] int GetSessionInstanceIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string value);
        [PreserveSig] int GetProcessId(out int processId);
        [PreserveSig] int IsSystemSoundsSession();
        [PreserveSig] int SetDuckingPreference(bool optOut);
    }

    [ComImport, Guid("87CE5498-68D6-44E5-9215-6DA47EF883D8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface ISimpleAudioVolume
    {
        [PreserveSig] int SetMasterVolume(float level, ref Guid context);
        [PreserveSig] int GetMasterVolume(out float level);
        [PreserveSig] int SetMute(bool muted, ref Guid context);
        [PreserveSig] int GetMute(out bool muted);
    }
}
'@

$Targets = @(
    [pscustomobject]@{ Minecraft = "1.19.2"; Loader = "fabric"; LoaderVersion = "0.16.14"; RuntimeLoader = "fabric"; Java = 17; FabricApi = "0.77.0+1.19.2" }
    [pscustomobject]@{ Minecraft = "1.19.2"; Loader = "forge"; LoaderVersion = "43.5.2"; RuntimeLoader = "lexforge"; Java = 17; FabricApi = $null }
    [pscustomobject]@{ Minecraft = "1.20.1"; Loader = "fabric"; LoaderVersion = "0.16.14"; RuntimeLoader = "fabric"; Java = 17; FabricApi = "0.92.6+1.20.1" }
    [pscustomobject]@{ Minecraft = "1.20.1"; Loader = "forge"; LoaderVersion = "47.3.0"; RuntimeLoader = "lexforge"; Java = 17; FabricApi = $null }
    [pscustomobject]@{ Minecraft = "1.21.1"; Loader = "fabric"; LoaderVersion = "0.16.14"; RuntimeLoader = "fabric"; Java = 21; FabricApi = "0.116.7+1.21.1" }
    [pscustomobject]@{ Minecraft = "1.21.1"; Loader = "neoforge"; LoaderVersion = "21.1.66"; RuntimeLoader = "neoforge"; Java = 21; FabricApi = $null }
    [pscustomobject]@{ Minecraft = "1.21.11"; Loader = "fabric"; LoaderVersion = "0.18.4"; RuntimeLoader = "fabric"; Java = 21; FabricApi = "0.141.3+1.21.11" }
)

function Write-Status {
    param([string]$Message, [ConsoleColor]$Color = [ConsoleColor]::Gray)

    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $Message" -ForegroundColor $Color
}

function Get-ForwardSlashPath {
    param([string]$Path)

    return $Path.Replace("\", "/")
}

function Get-File {
    param([string]$Url, [string]$Path)

    if (-not (Test-Path $Path)) {
        Write-Status "Downloading $(Split-Path -Leaf $Path)" Cyan
        Invoke-WebRequest -UseBasicParsing $Url -OutFile $Path
    }
}

function Invoke-Hmc {
    param([string[]]$Arguments)

    Push-Location $CacheRoot
    try {
        & $Java21 -jar $HeadlessMc --command @Arguments | Out-Host
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($exitCode -ne 0) {
        throw "HeadlessMC command failed: $($Arguments -join ' ')"
    }
}

function Get-Profile {
    param($Target)

    $versions = Join-Path $MinecraftRoot "versions"
    if (-not (Test-Path $versions)) {
        return $null
    }

    $matches = Get-ChildItem $versions -Directory | Where-Object {
        if ($Target.Loader -eq "fabric") {
            $_.Name -like "fabric-loader-$($Target.LoaderVersion)-$($Target.Minecraft)"
        } elseif ($Target.Loader -eq "forge") {
            $_.Name -like "$($Target.Minecraft)-forge-$($Target.LoaderVersion)"
        } else {
            $_.Name -like "neoforge-$($Target.LoaderVersion)"
        }
    }
    return $matches | Select-Object -First 1
}

function Install-Target {
    param($Target)

    $label = "$($Target.Minecraft) $($Target.Loader)"
    $profile = Get-Profile $Target
    if ($null -ne $profile) {
        Write-Status "$label installation cached" Green
        return $profile.Name
    }

    Write-Status "Installing $label" Cyan
    Invoke-Hmc @("download", $Target.Minecraft)
    if ($Target.Loader -eq "fabric") {
        Invoke-Hmc @("fabric", $Target.Minecraft, "--uid", $Target.LoaderVersion, "--java", "$($Target.Java)")
    } elseif ($Target.Loader -eq "forge") {
        $installerName = "forge-$($Target.Minecraft)-$($Target.LoaderVersion)-installer.jar"
        $installer = Join-Path $DownloadRoot $installerName
        Get-File "https://maven.minecraftforge.net/net/minecraftforge/forge/$($Target.Minecraft)-$($Target.LoaderVersion)/$installerName" $installer
        Push-Location $CacheRoot
        try {
            & $Java17 -jar $installer --installClient $MinecraftRoot | Out-Host
            $forgeExitCode = $LASTEXITCODE
        } finally {
            Pop-Location
        }
        if ($forgeExitCode -ne 0) {
            throw "Forge installer failed for $label"
        }
    } else {
        Invoke-Hmc @($Target.Loader, $Target.Minecraft, "--uid", $Target.LoaderVersion)
    }

    $profile = Get-Profile $Target
    if ($null -eq $profile) {
        throw "HeadlessMC did not create a profile for $label"
    }
    return $profile.Name
}

function New-HardLink {
    param([string]$Source, [string]$Target)

    if (Test-Path $Target) {
        Remove-Item $Target -Force
    }
    New-Item -ItemType HardLink -Path $Target -Target $Source | Out-Null
}

function Stage-Target {
    param($Target, [string]$ModVersion)

    $key = "$($Target.Minecraft)-$($Target.Loader)"
    $instance = Join-Path $InstanceRoot $key
    $mods = Join-Path $instance "mods"
    New-Item -ItemType Directory -Force $mods,(Join-Path $instance "resourcepacks") | Out-Null
    Get-ChildItem $mods -File -ErrorAction SilentlyContinue | Remove-Item -Force
    foreach ($logName in @("latest.log", "debug.log")) {
        $oldLog = Join-Path $instance "logs\$logName"
        if (Test-Path $oldLog) {
            Remove-Item $oldLog -Force
        }
    }

    $modJar = Join-Path $Root "build\libs\$ModVersion\$($Target.Loader)\reactivemusic-$($Target.Loader)-$ModVersion+$($Target.Minecraft).jar"
    if (-not (Test-Path $modJar)) {
        throw "Missing release artifact: $modJar"
    }
    New-HardLink $modJar (Join-Path $mods (Split-Path -Leaf $modJar))

    $runtimeName = "mc-runtime-test-$($Target.Minecraft)-$RuntimeTestVersion-$($Target.RuntimeLoader)-release.jar"
    $runtimeJar = Join-Path $DownloadRoot $runtimeName
    Get-File "https://github.com/headlesshq/mc-runtime-test/releases/download/$RuntimeTestVersion/$runtimeName" $runtimeJar
    New-HardLink $runtimeJar (Join-Path $mods $runtimeName)

    if ($null -ne $Target.FabricApi) {
        $fabricApiName = "fabric-api-$($Target.FabricApi).jar"
        $fabricApiJar = Join-Path $DownloadRoot $fabricApiName
        Get-File "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$($Target.FabricApi)/$fabricApiName" $fabricApiJar
        New-HardLink $fabricApiJar (Join-Path $mods $fabricApiName)
    }

    @("onboardAccessibility:false", "pauseOnLostFocus:false") | Set-Content (Join-Path $instance "options.txt")
    return $instance
}

function Start-Target {
    param($Target, [string]$Profile, [string]$Instance)

    $key = "$($Target.Minecraft)-$($Target.Loader)"
    $stdout = Join-Path $OutputRoot "$key.stdout.log"
    $stderr = Join-Path $OutputRoot "$key.stderr.log"
    $gameDir = Get-ForwardSlashPath $Instance
    $arguments = @(
        "-Dhmc.gamedir=$gameDir"
        "-jar"
        $HeadlessMc
        "--command"
        "launch"
        $Profile
        "-lwjgl"
    )
    $runner = Join-Path $CacheRoot "runners\$key"
    $runnerConfig = Join-Path $runner "HeadlessMC"
    New-Item -ItemType Directory -Force $runnerConfig | Out-Null
    Copy-Item (Join-Path $CacheRoot "HeadlessMC\config.properties") (Join-Path $runnerConfig "config.properties") -Force
    $process = Start-Process -FilePath $Java21 -ArgumentList $arguments -WorkingDirectory $runner -PassThru `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    return [pscustomobject]@{
        Target = $Target
        Key = $key
        Instance = $Instance
        Process = $process
        Stdout = $stdout
        Stderr = $stderr
        StartedAt = Get-Date
        Complete = $false
        AudioMuted = $false
    }
}

function Mute-TargetAudio {
    param($Run, $Processes)

    $pending = [System.Collections.Generic.Queue[int]]::new()
    $pending.Enqueue($Run.Process.Id)
    while ($pending.Count -gt 0) {
        $parentId = $pending.Dequeue()
        foreach ($process in $Processes | Where-Object { $_.ParentProcessId -eq $parentId }) {
            $pending.Enqueue([int]$process.ProcessId)
            if ([ProcessAudioMuter]::Mute([int]$process.ProcessId)) {
                if (-not $Run.AudioMuted) {
                    Write-Status "$($Run.Key) audio session muted" Green
                    $Run.AudioMuted = $true
                }
            }
        }
    }
}

function Stop-ProcessTree {
    param($Process)

    $Process.Refresh()
    if (-not $Process.HasExited) {
        & taskkill /PID $Process.Id /T /F *> $null
        $Process.WaitForExit()
    }
}

function Complete-Target {
    param($Run, [bool]$TimedOut)

    if ($TimedOut) {
        Stop-ProcessTree $Run.Process
    } else {
        $Run.Process.WaitForExit()
    }

    $latestLog = Join-Path $Run.Instance "logs\latest.log"
    $log = if (Test-Path $latestLog) { Get-Content $latestLog -Raw } else { "" }
    $launcherLog = if (Test-Path $Run.Stdout) { Get-Content $Run.Stdout -Raw } else { "" }
    $exitMatch = [regex]::Match($launcherLog, "Minecraft exited with code: (-?\d+)")
    $minecraftExitCode = if ($exitMatch.Success) { [int]$exitMatch.Groups[1].Value } else { $null }
    $errors = $log -split "`r?`n" | Where-Object { $_ -match "ERROR|FATAL|Exception|crash" }
    Set-Content (Join-Path $OutputRoot "$($Run.Key).errors.log") -Value $errors

    $passed = -not $TimedOut -and $minecraftExitCode -eq 0 `
        -and $log.Contains("Initializing Reactive Music...") `
        -and $log.Contains("Playing ") `
        -and $log.Contains("Successfully finished.")
    $detail = if ($TimedOut) {
        "timed out after ${TimeoutSeconds}s"
    } elseif ($null -eq $minecraftExitCode) {
        "HeadlessMC did not report a Minecraft exit code"
    } elseif ($minecraftExitCode -ne 0) {
        "Minecraft exited with code $minecraftExitCode"
    } elseif (-not $log.Contains("Initializing Reactive Music...")) {
        "ReactiveMusic did not initialize"
    } elseif (-not $log.Contains("Playing ")) {
        "ReactiveMusic did not start a song"
    } elseif (-not $log.Contains("Successfully finished.")) {
        "runtime helper did not finish"
    } else {
        "world and chunk loaded, runtime helper completed"
    }

    $color = if ($passed) { [ConsoleColor]::Green } else { [ConsoleColor]::Red }
    $status = if ($passed) { "PASS" } else { "FAIL" }
    Write-Status "$status $($Run.Key): $detail" $color
    $Run.Complete = $true
    return [pscustomobject]@{ Target = $Run.Key; Status = $status; Detail = $detail }
}

foreach ($java in @($Java17, $Java21)) {
    if (-not (Test-Path $java)) {
        throw "Required Java executable not found: $java"
    }
}

New-Item -ItemType Directory -Force $OutputRoot,$InstanceRoot,$CacheRoot,$MinecraftRoot,$DownloadRoot,(Join-Path $CacheRoot "HeadlessMC") | Out-Null
Get-File "https://github.com/headlesshq/headlessmc/releases/download/$HeadlessMcVersion/headlessmc-launcher-$HeadlessMcVersion.jar" $HeadlessMc

$javaPaths = "$(Get-ForwardSlashPath $Java17);$(Get-ForwardSlashPath $Java21)"
@(
    "hmc.java.versions=$javaPaths"
    "hmc.java.auto=false"
    "hmc.gamedir=$(Get-ForwardSlashPath (Join-Path $CacheRoot 'game'))"
    "hmc.mcdir=$(Get-ForwardSlashPath $MinecraftRoot)"
    "hmc.offline=true"
    "hmc.rethrow.launch.exceptions=true"
    "hmc.exit.on.failed.command=true"
    "hmc.assets.dummy=true"
    "hmc.jline.enabled=false"
    "hmc.jvmargs=-Xmx2G -Djava.awt.headless=true -DMcRuntimeGameTest=false"
) | Set-Content (Join-Path $CacheRoot "HeadlessMC\config.properties")

Write-Status "Resetting active project" Cyan
& $Gradlew "Reset active project"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Status "Building production artifacts" Cyan
& $Gradlew chiseledBuild --no-build-cache
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$modVersionLine = Get-Content (Join-Path $Root "gradle.properties") | Where-Object { $_ -like "mod.version=*" }
$modVersion = $modVersionLine.Split("=", 2)[1]
$profiles = @{}
foreach ($target in $Targets) {
    $profiles["$($target.Minecraft)-$($target.Loader)"] = Install-Target $target
}

Write-Status "Launching all seven production clients in parallel" Cyan
$runs = foreach ($target in $Targets) {
    $key = "$($target.Minecraft)-$($target.Loader)"
    $instance = Stage-Target $target $modVersion
    Start-Target $target $profiles[$key] $instance
}

$results = [System.Collections.Generic.List[object]]::new()
$nextHeartbeat = Get-Date
while (@($runs | Where-Object { -not $_.Complete }).Count -gt 0) {
    $processes = Get-CimInstance Win32_Process
    foreach ($run in @($runs | Where-Object { -not $_.Complete })) {
        Mute-TargetAudio $run $processes
        $run.Process.Refresh()
        $elapsed = [int]((Get-Date) - $run.StartedAt).TotalSeconds
        if ($run.Process.HasExited) {
            $results.Add((Complete-Target $run $false))
        } elseif ($elapsed -ge $TimeoutSeconds) {
            $results.Add((Complete-Target $run $true))
        }
    }

    $pending = @($runs | Where-Object { -not $_.Complete })
    if ($pending.Count -gt 0) {
        if ((Get-Date) -ge $nextHeartbeat) {
            $state = $pending | ForEach-Object { "$($_.Key) $([int]((Get-Date) - $_.StartedAt).TotalSeconds)s" }
            Write-Status "Running: $($state -join ', ')"
            $nextHeartbeat = (Get-Date).AddSeconds(5)
        }
        Start-Sleep -Seconds 1
    }
}

$duration = [int]((Get-Date) - $StartedAt).TotalSeconds
$summary = $results | Sort-Object Target | Format-Table -AutoSize | Out-String
$summary += "`nDuration: $([TimeSpan]::FromSeconds($duration).ToString('mm\:ss'))`n"
$summary | Set-Content (Join-Path $OutputRoot "summary.txt")
Write-Host $summary
Write-Status "Logs: $OutputRoot"

if ($results.Status -contains "FAIL") {
    exit 1
}
