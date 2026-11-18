$shell = New-Object -ComObject Shell.Application
$thisPc = $shell.Namespace(17)
$deviceItem = $null
foreach ($item in $thisPc.Items()) {
    if ($item.Name -like "*Redmi Note 7 Pro*") {
        $deviceItem = $item
        break
    }
}

if ($deviceItem) {
    Write-Host "Found Device: $($deviceItem.Name)"
    $deviceFolder = $deviceItem.GetFolder
    foreach ($sub in $deviceFolder.Items()) {
        Write-Host "  Storage: $($sub.Name)"
        if ($sub.Name -like "*Internal*") {
            $internalFolder = $sub.GetFolder
            foreach ($dir in $internalFolder.Items()) {
                if ($dir.Name -in @("Download", "Documents")) {
                    Write-Host "    Folder: $($dir.Name)"
                }
            }
        }
    }
} else {
    Write-Host "Redmi Note 7 Pro not found in This PC."
}
