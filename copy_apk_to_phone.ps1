$shell = New-Object -ComObject Shell.Application
$thisPc = $shell.Namespace(17)
$deviceItem = $null
foreach ($item in $thisPc.Items()) {
    if ($item.Name -like "*Redmi Note 7 Pro*") {
        $deviceItem = $item
        break
    }
}

if (-not $deviceItem) {
    Write-Error "Redmi Note 7 Pro not found in This PC."
    exit 1
}

$apkCandidate1 = "C:\Users\jiten\jAnitGravity\jFindMe\jButterFly\release\jButterFly-release.apk"
$apkCandidate2 = "C:\Users\jiten\jAnitGravity\jFindMe\jButterFly\build\outputs\apk\release\jButterFly-release.apk"
$apkPath = if (Test-Path $apkCandidate1) { $apkCandidate1 } elseif (Test-Path $apkCandidate2) { $apkCandidate2 } else { $null }

if (-not $apkPath) {
    Write-Error "APK file not found in jButterFly/release or jButterFly/build/outputs/apk/release."
    exit 1
}

$deviceFolder = $deviceItem.GetFolder
$downloadFolder = $null

foreach ($sub in $deviceFolder.Items()) {
    if ($sub.Name -like "*Internal*") {
        $internalFolder = $sub.GetFolder
        foreach ($dir in $internalFolder.Items()) {
            if ($dir.Name -eq "Download") {
                $downloadFolder = $dir.GetFolder
                break
            }
        }
    }
}

if (-not $downloadFolder) {
    Write-Error "Could not locate 'Download' directory on Redmi Note 7 Pro."
    exit 1
}

Write-Host "Copying $apkPath to Redmi Note 7 Pro -> Download..." -ForegroundColor Cyan
$downloadFolder.CopyHere($apkPath, 16) # 16 = Respond "Yes to All" for any dialog
Start-Sleep -Seconds 3

Write-Host "Verifying files in phone Download directory..." -ForegroundColor Cyan
foreach ($f in $downloadFolder.Items()) {
    Write-Host "  -> $($f.Name) ($($f.Size) bytes)" -ForegroundColor Green
}
