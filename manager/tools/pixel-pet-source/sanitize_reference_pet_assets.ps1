param(
    [string]$AssetDirectory = (Join-Path $PSScriptRoot "..\..\app\src\main\assets\pixel_pet\reference"),
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Color-DistanceSquared([System.Drawing.Color]$left, [System.Drawing.Color]$right) {
    $red = $left.R - $right.R
    $green = $left.G - $right.G
    $blue = $left.B - $right.B
    return ($red * $red + $green * $green + $blue * $blue)
}

function Get-ScreenshotMatte([System.Drawing.Bitmap]$bitmap) {
    $width = $bitmap.Width
    $height = $bitmap.Height
    $visited = [bool[,]]::new($width, $height)
    $opaqueCount = 0
    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            if ($bitmap.GetPixel($x, $y).A -gt 0) { $opaqueCount++ }
        }
    }
    $minimumArea = [Math]::Max(24, [Math]::Floor(($width * $height) / 8))
    $removed = [System.Collections.Generic.List[System.Drawing.Point]]::new()
    foreach ($startY in 0..($height - 1)) {
        foreach ($startX in 0..($width - 1)) {
            if ($visited[$startX, $startY]) { continue }
            $seed = $bitmap.GetPixel($startX, $startY)
            if ($seed.A -eq 0) { continue }
            $touchesTransparent = $false
            foreach ($offset in @(@(-1, 0), @(1, 0), @(0, -1), @(0, 1))) {
                $nx = $startX + $offset[0]
                $ny = $startY + $offset[1]
                if ($nx -lt 0 -or $nx -ge $width -or $ny -lt 0 -or $ny -ge $height -or $bitmap.GetPixel($nx, $ny).A -eq 0) {
                    $touchesTransparent = $true
                    break
                }
            }
            if (-not $touchesTransparent) { continue }

            $queue = [System.Collections.Generic.Queue[System.Drawing.Point]]::new()
            $component = [System.Collections.Generic.List[System.Drawing.Point]]::new()
            $queue.Enqueue([System.Drawing.Point]::new($startX, $startY))
            $visited[$startX, $startY] = $true
            while ($queue.Count -gt 0) {
                $point = $queue.Dequeue()
                $component.Add($point)
                foreach ($offset in @(@(-1, 0), @(1, 0), @(0, -1), @(0, 1))) {
                    $nx = $point.X + $offset[0]
                    $ny = $point.Y + $offset[1]
                    if ($nx -lt 0 -or $nx -ge $width -or $ny -lt 0 -or $ny -ge $height -or $visited[$nx, $ny]) { continue }
                    $candidate = $bitmap.GetPixel($nx, $ny)
                    if ($candidate.A -eq 0 -or (Color-DistanceSquared $seed $candidate) -gt 5625) { continue }
                    $visited[$nx, $ny] = $true
                    $queue.Enqueue([System.Drawing.Point]::new($nx, $ny))
                }
            }
            if ($component.Count -ge $minimumArea -and $component.Count * 8 -ge $opaqueCount) {
                foreach ($point in $component) { $removed.Add($point) }
            }
        }
    }
    return $removed
}

$paths = Get-ChildItem -LiteralPath $AssetDirectory -Filter "*.png" -File | Sort-Object Name
if ($paths.Count -eq 0) { throw "No PNG assets found: $AssetDirectory" }

$summary = foreach ($path in $paths) {
    $temporary = $null
    $removedPixels = 0
    $bitmap = [System.Drawing.Bitmap]::new($path.FullName)
    try {
        $matte = @(Get-ScreenshotMatte $bitmap)
        $removedPixels = $matte.Count
        if (-not $CheckOnly -and $matte.Count -gt 0) {
            foreach ($point in $matte) { $bitmap.SetPixel($point.X, $point.Y, [System.Drawing.Color]::Transparent) }
            $temporary = "$($path.FullName).tmp.png"
            $bitmap.Save($temporary, [System.Drawing.Imaging.ImageFormat]::Png)
        }
    } finally {
        $bitmap.Dispose()
    }
    if ($temporary -ne $null) {
        Move-Item -LiteralPath $temporary -Destination $path.FullName -Force
    }
    [PSCustomObject]@{ Asset = $path.Name; RemovedPixels = $removedPixels }
}

$summary | Format-Table -AutoSize
if ($CheckOnly) {
    Write-Output "Check only: no assets were changed."
} else {
    Write-Output "Sanitized transparent reference Sprite assets in $AssetDirectory"
}
