param(
    [string]$MasterDirectory = (Join-Path $PSScriptRoot "v5-masters"),
    [string]$MasterSourceDirectory = (Join-Path $PSScriptRoot "v5-masters-src"),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "v5")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# This is the editable authoring step for v5. It starts from 24 transparent,
# native-resolution semantic pixel masters and writes every timing cel as a
# standalone image region. The runtime packer remains a validator only.
$speciesNames = @("penguin", "dog", "cat", "bird", "rabbit", "hamster")
$stageNames = @("egg", "baby", "young", "adult")
$stageSizes = @(16, 16, 32, 48)
$stageBaselines = @(14, 14, 30, 46)
$frameCounts = @(8, 10, 10, 10, 10, 10, 6, 6, 6, 6, 6, 6, 6)
$slots = @("head", "back", "hand", "neck", "tail", "trail")
$colors = [ordered]@{
    o = [System.Drawing.Color]::FromArgb(0xFF, 0x24, 0x21, 0x2B)
    b = [System.Drawing.Color]::FromArgb(0xFF, 0xC8, 0xA2, 0x7C)
    s = [System.Drawing.Color]::FromArgb(0xFF, 0x76, 0x6A, 0x82)
    c = [System.Drawing.Color]::FromArgb(0xFF, 0xF6, 0xEA, 0xD7)
    h = [System.Drawing.Color]::FromArgb(0xFF, 0xFF, 0xFF, 0xFF)
    a = [System.Drawing.Color]::FromArgb(0xFF, 0xFF, 0xAB, 0x76)
    m = [System.Drawing.Color]::FromArgb(0xFF, 0xD2, 0x7C, 0x9E)
    r = [System.Drawing.Color]::FromArgb(0xFF, 0x9E, 0xDE, 0xFA)
    e = [System.Drawing.Color]::FromArgb(0xFF, 0x30, 0x2A, 0x36)
    x = [System.Drawing.Color]::FromArgb(0xFF, 0x17, 0x13, 0x1D)
}
$symbolByArgb = @{}
foreach ($entry in $colors.GetEnumerator()) {
    $symbolByArgb[[int]$entry.Value.ToArgb()] = [char]$entry.Key
}

function Cell-Key([int]$x, [int]$y) { return "$x,$y" }

function New-PixelMap {
    return [System.Collections.Generic.Dictionary[string, char]]::new()
}

function Get-PixelPriority([char]$symbol) {
    switch ([string]$symbol) {
        'o' { return 100 }
        'x' { return 96 }
        'e' { return 92 }
        'a' { return 84 }
        'm' { return 82 }
        'h' { return 74 }
        'c' { return 66 }
        's' { return 58 }
        'r' { return 50 }
        default { return 40 }
    }
}

function Copy-PixelMap($source) {
    $copy = New-PixelMap
    foreach ($entry in $source.GetEnumerator()) { $copy[$entry.Key] = $entry.Value }
    return $copy
}

function Put-Pixel($map, [int]$size, [int]$x, [int]$y, [char]$symbol) {
    if ($x -ge 0 -and $x -lt $size -and $y -ge 0 -and $y -lt $size) {
        $map[(Cell-Key $x $y)] = $symbol
    }
}

function Put-PixelByPriority($map, [int]$size, [int]$x, [int]$y, [char]$symbol) {
    if ($x -lt 0 -or $x -ge $size -or $y -lt 0 -or $y -ge $size) { return }
    $key = Cell-Key $x $y
    if (-not $map.ContainsKey($key) -or (Get-PixelPriority $symbol) -ge (Get-PixelPriority $map[$key])) {
        $map[$key] = $symbol
    }
}

function Clamp-PixelX([int]$x, [int]$size) {
    return [Math]::Max(1, [Math]::Min($size - 2, $x))
}

function Remove-Pixel($map, [int]$x, [int]$y) {
    [void]$map.Remove((Cell-Key $x $y))
}

function Fill-Pixels($map, [int]$size, [int]$x, [int]$y, [int]$width, [int]$height, [char]$symbol) {
    for ($py = $y; $py -lt ($y + $height); $py++) {
        for ($px = $x; $px -lt ($x + $width); $px++) {
            Put-Pixel $map $size $px $py $symbol
        }
    }
}

function Draw-PixelLine($map, [int]$size, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [char]$symbol) {
    $steps = [Math]::Max([Math]::Abs($x1 - $x0), [Math]::Abs($y1 - $y0))
    if ($steps -eq 0) { Put-Pixel $map $size $x0 $y0 $symbol; return }
    for ($step = 0; $step -le $steps; $step++) {
        $x = [int][Math]::Round($x0 + (($x1 - $x0) * $step / [double]$steps))
        $y = [int][Math]::Round($y0 + (($y1 - $y0) * $step / [double]$steps))
        Put-Pixel $map $size $x $y $symbol
    }
}

function Get-PixelBounds($map) {
    if ($map.Count -eq 0) { return $null }
    $minX = [int]::MaxValue; $maxX = -1; $minY = [int]::MaxValue; $maxY = -1
    foreach ($key in $map.Keys) {
        $parts = $key.Split(',')
        $x = [int]$parts[0]; $y = [int]$parts[1]
        $minX = [Math]::Min($minX, $x); $maxX = [Math]::Max($maxX, $x)
        $minY = [Math]::Min($minY, $y); $maxY = [Math]::Max($maxY, $y)
    }
    return [PSCustomObject]@{
        MinX = $minX; MaxX = $maxX; MinY = $minY; MaxY = $maxY
        Width = $maxX - $minX + 1; Height = $maxY - $minY + 1
        CenterX = [int][Math]::Round(($minX + $maxX) / 2.0)
        CenterY = [int][Math]::Round(($minY + $maxY) / 2.0)
    }
}

function Shift-PixelMap($map, [int]$size, [int]$dx, [int]$dy) {
    if ($dx -eq 0 -and $dy -eq 0) { return }
    $bounds = Get-PixelBounds $map
    $dx = [Math]::Max(1 - $bounds.MinX, [Math]::Min(($size - 2) - $bounds.MaxX, $dx))
    $dy = [Math]::Max(1 - $bounds.MinY, [Math]::Min(($size - 2) - $bounds.MaxY, $dy))
    $shifted = New-PixelMap
    foreach ($entry in $map.GetEnumerator()) {
        $parts = $entry.Key.Split(',')
        Put-Pixel $shifted $size ([int]$parts[0] + $dx) ([int]$parts[1] + $dy) $entry.Value
    }
    $map.Clear()
    foreach ($entry in $shifted.GetEnumerator()) { $map[$entry.Key] = $entry.Value }
}

function Shift-PixelRegion($map, [int]$size, [int]$maxY, [int]$dx, [int]$dy) {
    $moving = [System.Collections.Generic.List[object]]::new()
    foreach ($entry in @($map.GetEnumerator())) {
        $parts = $entry.Key.Split(',')
        if ([int]$parts[1] -le $maxY) {
            $moving.Add([PSCustomObject]@{ Key = $entry.Key; X = [int]$parts[0]; Y = [int]$parts[1]; Value = $entry.Value })
        }
    }
    if ($moving.Count -eq 0) { return }
    $minX = ($moving | ForEach-Object X | Measure-Object -Minimum).Minimum
    $maxX = ($moving | ForEach-Object X | Measure-Object -Maximum).Maximum
    $minY = ($moving | ForEach-Object Y | Measure-Object -Minimum).Minimum
    $maxMovingY = ($moving | ForEach-Object Y | Measure-Object -Maximum).Maximum
    $dx = [Math]::Max(1 - $minX, [Math]::Min(($size - 2) - $maxX, $dx))
    $dy = [Math]::Max(1 - $minY, [Math]::Min(($size - 2) - $maxMovingY, $dy))
    foreach ($cell in $moving) { [void]$map.Remove($cell.Key) }
    foreach ($cell in $moving) { Put-Pixel $map $size ($cell.X + $dx) ($cell.Y + $dy) $cell.Value }
}

function Shift-LowerLimb($map, [int]$size, $bounds, [bool]$left, [int]$dx, [int]$dy) {
    $thresholdY = $bounds.MinY + [int][Math]::Round($bounds.Height * 0.68)
    $moving = [System.Collections.Generic.List[object]]::new()
    foreach ($entry in @($map.GetEnumerator())) {
        $parts = $entry.Key.Split(',')
        $x = [int]$parts[0]; $y = [int]$parts[1]
        $onSide = if ($left) { $x -le $bounds.CenterX } else { $x -gt $bounds.CenterX }
        if ($y -ge $thresholdY -and $onSide) {
            $moving.Add([PSCustomObject]@{ Key = $entry.Key; X = $x; Y = $y; Value = $entry.Value })
        }
    }
    if ($moving.Count -eq 0) { return }
    $minX = ($moving | ForEach-Object X | Measure-Object -Minimum).Minimum
    $maxX = ($moving | ForEach-Object X | Measure-Object -Maximum).Maximum
    $minY = ($moving | ForEach-Object Y | Measure-Object -Minimum).Minimum
    $maxY = ($moving | ForEach-Object Y | Measure-Object -Maximum).Maximum
    $dx = [Math]::Max(1 - $minX, [Math]::Min(($size - 2) - $maxX, $dx))
    $dy = [Math]::Max(1 - $minY, [Math]::Min(($size - 2) - $maxY, $dy))
    foreach ($cell in $moving) { [void]$map.Remove($cell.Key) }
    foreach ($cell in $moving) { Put-PixelByPriority $map $size ($cell.X + $dx) ($cell.Y + $dy) $cell.Value }
}

function Mirror-PixelMap($source, [int]$size, [int]$pivotX) {
    $result = New-PixelMap
    foreach ($entry in $source.GetEnumerator()) {
        $parts = $entry.Key.Split(',')
        $x = [int]$parts[0]; $y = [int]$parts[1]
        Put-Pixel $result $size ($pivotX * 2 - 1 - $x) $y $entry.Value
    }
    return $result
}

function Normalize-PixelMapHorizontal($map, [int]$size) {
    $bounds = Get-PixelBounds $map
    if ($null -eq $bounds) { throw "Empty authored pixel frame" }
    $dx = if ($bounds.MinX -lt 1) {
        1 - $bounds.MinX
    } elseif ($bounds.MaxX -gt ($size - 2)) {
        ($size - 2) - $bounds.MaxX
    } else { 0 }
    Shift-PixelMap $map $size $dx 0
}

function Assert-PixelMapSafe($map, [int]$size, [string]$name) {
    $bounds = Get-PixelBounds $map
    if ($null -eq $bounds) { throw "Empty authored pixel frame: $name" }
    if ($bounds.MinX -lt 1 -or $bounds.MaxX -gt ($size - 2)) {
        $edgePixels = @($map.GetEnumerator() | Where-Object {
            $x = [int]$_.Key.Split(',')[0]
            $x -lt 1 -or $x -gt ($size - 2)
        } | Sort-Object Key | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join ';'
        throw "Frame crossed its horizontal action margin: $name bounds=$($bounds.MinX)..$($bounds.MaxX) edge=$edgePixels"
    }
    if ($bounds.MinY -lt 1 -or $bounds.MaxY -gt ($size - 2)) {
        throw "Frame crossed its vertical artboard: $name bounds=$($bounds.MinY)..$($bounds.MaxY)"
    }
}

function Normalize-PixelMap($map, [int]$size, [int]$baseline) {
    $bounds = Get-PixelBounds $map
    if ($null -eq $bounds) { throw "Empty authored pixel frame" }
    Shift-PixelMap $map $size 0 ($baseline - $bounds.MaxY)
    $bounds = Get-PixelBounds $map
    $dx = if ($bounds.MinX -lt 1) {
        1 - $bounds.MinX
    } elseif ($bounds.MaxX -gt ($size - 2)) {
        ($size - 2) - $bounds.MaxX
    } else { 0 }
    Shift-PixelMap $map $size $dx 0
}

function Read-NativeMaster([string]$species, [string]$stage, [int]$size, [int]$baseline) {
    $path = Join-Path $MasterDirectory "${species}_${stage}.png"
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing native master: $path" }
    $bitmap = [System.Drawing.Bitmap]::new($path)
    try {
        if ($bitmap.Width -ne $size -or $bitmap.Height -ne $size) {
            throw "Native master must be ${size}x${size}: $path"
        }
        $map = New-PixelMap
        for ($y = 0; $y -lt $size; $y++) {
            for ($x = 0; $x -lt $size; $x++) {
                $pixel = $bitmap.GetPixel($x, $y)
                if ($pixel.A -eq 0) { continue }
                if ($pixel.A -ne 0xFF) { throw "Semi-transparent master pixel: $path/$x/$y" }
                $symbol = $symbolByArgb[[int]$pixel.ToArgb()]
                if ($null -eq $symbol) { throw "Non-semantic master pixel: $path/$x/$y" }
                Put-Pixel $map $size $x $y $symbol
            }
        }
        Normalize-PixelMap $map $size $baseline
        return $map
    } finally {
        $bitmap.Dispose()
    }
}

function New-BackFacing($source, [int]$size, [string]$species, [int]$stageIndex) {
    $map = Copy-PixelMap $source
    $bounds = Get-PixelBounds $map
    $faceBottom = $bounds.MinY + [int][Math]::Round($bounds.Height * 0.42)
    foreach ($entry in @($map.GetEnumerator())) {
        $parts = $entry.Key.Split(',')
        $y = [int]$parts[1]
        if ($y -le $faceBottom -and $entry.Value -in @('c', 'h', 'a', 'e', 'x', 'r', 'm')) {
            $map[$entry.Key] = if (($y + [int]$parts[0]) % 4 -eq 0) { 's' } else { 'b' }
        }
    }
    $center = $bounds.CenterX
    $stripeTop = $bounds.MinY + [Math]::Max(2, [int]($bounds.Height * 0.18))
    $stripeBottom = $bounds.MinY + [int]($bounds.Height * 0.48)
    switch ($species) {
        'cat' { Draw-PixelLine $map $size $center $stripeTop $center $stripeBottom 's' }
        'dog' { Fill-Pixels $map $size ($center - 1) $stripeTop 3 ([Math]::Max(2, $stripeBottom - $stripeTop)) 'c' }
        'bird' { Draw-PixelLine $map $size ($center - 3) ($stripeTop + 2) $center $stripeBottom 's'; Draw-PixelLine $map $size ($center + 3) ($stripeTop + 2) $center $stripeBottom 's' }
        'penguin' { Fill-Pixels $map $size ($center - 2) $stripeTop 5 ([Math]::Max(2, $stripeBottom - $stripeTop)) 's' }
        'rabbit' { Fill-Pixels $map $size ($center - 1) ($stripeTop + 1) 3 2 'c' }
        'hamster' { Fill-Pixels $map $size ($center - 2) ($stripeTop + 2) 5 2 's' }
    }
    return $map
}

function New-SideFacing($source, [int]$size, [string]$species, [int]$stageIndex, [int]$direction) {
    if ($direction -notin @(-1, 1)) { throw "Invalid side-facing direction: $direction" }
    $bounds = Get-PixelBounds $source
    $map = New-PixelMap
    $center = ($size - 1) / 2.0
    $headBottom = $bounds.MinY + [int][Math]::Round($bounds.Height * 0.43)
    $headLead = [Math]::Max(1, [int][Math]::Round($size / 32.0))
    foreach ($entry in $source.GetEnumerator()) {
        $parts = $entry.Key.Split(',')
        $x = [int]$parts[0]; $y = [int]$parts[1]
        if ($y -le $headBottom -and $entry.Value -in @('e', 'x')) { continue }
        $lead = if ($y -le $headBottom) { $headLead * $direction } else { 0 }
        $targetX = [int][Math]::Round($center + ($x - $center) * 0.80 + $lead)
        Put-PixelByPriority $map $size $targetX $y $entry.Value
    }
    Normalize-PixelMap $map $size ($stageBaselines[$stageIndex])
    $sideBounds = Get-PixelBounds $map
    $unit = [Math]::Max(1, [int][Math]::Round($size / 32.0))
    $eyeY = $sideBounds.MinY + [Math]::Max(2, [int][Math]::Round($sideBounds.Height * 0.24))
    $eyeX = if ($direction -lt 0) {
        [Math]::Max(2, $sideBounds.MinX + [Math]::Max(1, $unit))
    } else {
        [Math]::Min($size - 3, $sideBounds.MaxX - [Math]::Max(1, $unit))
    }
    Put-Pixel $map $size $eyeX $eyeY 'x'
    $frontEdge = if ($direction -lt 0) { $sideBounds.MinX } else { $sideBounds.MaxX }
    $backEdge = if ($direction -lt 0) { $sideBounds.MaxX } else { $sideBounds.MinX }
    $frontOutside = [Math]::Max(1, [Math]::Min($size - 2, $frontEdge + $direction))
    switch ($species) {
        'cat' {
            $tailStartX = Clamp-PixelX ($backEdge - $direction * $unit) $size
            $tailEndX = Clamp-PixelX ($backEdge - $direction * 2 * $unit) $size
            Draw-PixelLine $map $size $tailStartX ($sideBounds.CenterY + 2 * $unit) $tailEndX ($sideBounds.CenterY - 2 * $unit) 's'
            Put-Pixel $map $size $frontOutside ($eyeY + $unit) 'a'
        }
        'dog' {
            $tailStartX = Clamp-PixelX ($backEdge - $direction * 2 * $unit) $size
            $tailEndX = Clamp-PixelX ($backEdge - $direction * 3 * $unit) $size
            Draw-PixelLine $map $size $tailStartX $sideBounds.CenterY $tailEndX ($sideBounds.CenterY - 2 * $unit) 's'
            $muzzleX = if ($direction -lt 0) { $frontEdge } else { $frontEdge - 2 * $unit }
            $muzzleWidth = 1 + $unit
            $muzzleX = [Math]::Max(1, [Math]::Min($size - 1 - $muzzleWidth, $muzzleX))
            Fill-Pixels $map $size $muzzleX ($eyeY + $unit) $muzzleWidth $unit 'c'
        }
        'bird' {
            $wingStartX = Clamp-PixelX ($sideBounds.CenterX - $direction * $unit) $size
            $wingEndX = Clamp-PixelX ($backEdge - $direction * $unit) $size
            Draw-PixelLine $map $size $wingStartX $sideBounds.CenterY $wingEndX ($sideBounds.CenterY + 2 * $unit) 's'
            Put-Pixel $map $size $frontOutside ($eyeY + $unit) 'a'
        }
        'penguin' {
            Fill-Pixels $map $size ($sideBounds.CenterX - $unit) ($sideBounds.CenterY - $unit) (2 * $unit + 1) (3 * $unit) 'c'
            Put-Pixel $map $size $frontOutside ($eyeY + $unit) 'a'
        }
        'rabbit' {
            Shift-PixelRegion $map $size ($sideBounds.MinY + [int]($sideBounds.Height * 0.28)) ($direction * $unit) 0
            Put-Pixel $map $size $frontOutside ($eyeY + $unit) 'c'
        }
        'hamster' {
            $cheekX = if ($direction -lt 0) { $frontEdge } else { $frontEdge - 2 * $unit }
            $cheekWidth = 2 * $unit
            $cheekX = [Math]::Max(1, [Math]::Min($size - 1 - $cheekWidth, $cheekX))
            Fill-Pixels $map $size $cheekX ($eyeY + $unit) $cheekWidth (2 * $unit) 'c'
            Put-Pixel $map $size $frontOutside ($eyeY + $unit) 'm'
        }
    }
    Normalize-PixelMap $map $size ($stageBaselines[$stageIndex])
    return $map
}

function New-FacingMap($master, [int]$size, [string]$species, [int]$stageIndex, [int]$facing) {
    if ($stageIndex -eq 0) { return Copy-PixelMap $master }
    $pivot = [int]($size / 2)
    switch ($facing) {
        0 { return Copy-PixelMap $master }
        1 { return New-BackFacing $master $size $species $stageIndex }
        2 { return New-SideFacing $master $size $species $stageIndex -1 }
        3 { return New-SideFacing $master $size $species $stageIndex 1 }
    }
}

function Add-ClosedEyes($map, [int]$size, $bounds, [int]$facing) {
    if ($facing -eq 1) { return }
    $y = $bounds.MinY + [Math]::Max(2, [int]($bounds.Height * 0.23))
    $spread = [Math]::Max(1, [int]($bounds.Width * 0.10))
    Put-Pixel $map $size ($bounds.CenterX - $spread) $y 'o'
    Put-Pixel $map $size ($bounds.CenterX + $spread) $y 'o'
}

function Add-SpeciesMotion($map, [int]$size, [string]$species, [int]$action, [int]$facing, [int]$phase) {
    if ($action -notin @(0, 1, 3, 5, 8, 9, 10)) { return }
    $bounds = Get-PixelBounds $map
    $swing = if ($phase % 2 -eq 0) { -1 } else { 1 }
    $unit = [Math]::Max(1, [int][Math]::Round($size / 32.0))
    switch ($species) {
        'cat' {
            $tailX = if ($facing -eq 2) { $bounds.MaxX } else { $bounds.MinX }
            $tailEndX = [Math]::Max(1, [Math]::Min($size - 2, $tailX - $swing))
            Draw-PixelLine $map $size $tailX ($bounds.MaxY - 3 * $unit) $tailEndX ($bounds.MaxY - 7 * $unit) 'b'
        }
        'dog' {
            $tailX = if ($facing -eq 2) { $bounds.MaxX } else { $bounds.MinX }
            $tailEndX = [Math]::Max(1, [Math]::Min($size - 2, $tailX - 2 * $swing))
            Draw-PixelLine $map $size $tailX ($bounds.CenterY + 2 * $unit) $tailEndX ($bounds.CenterY - 2 * $unit) 's'
        }
        'bird' {
            $lift = if ($action -in @(1, 3, 9)) { 3 * $unit } else { $unit }
            Draw-PixelLine $map $size ($bounds.CenterX - 2 * $unit) $bounds.CenterY ($bounds.MinX + $unit) ($bounds.CenterY - $lift) 's'
            Draw-PixelLine $map $size ($bounds.CenterX + 2 * $unit) $bounds.CenterY ($bounds.MaxX - $unit) ($bounds.CenterY - $lift) 's'
        }
        'penguin' {
            Draw-PixelLine $map $size ($bounds.MinX + 2 * $unit) $bounds.CenterY $bounds.MinX ($bounds.CenterY + $swing * $unit) 's'
            Draw-PixelLine $map $size ($bounds.MaxX - 2 * $unit) $bounds.CenterY $bounds.MaxX ($bounds.CenterY - $swing * $unit) 's'
        }
        'rabbit' {
            if ($action -in @(1, 3, 9)) {
                Shift-PixelRegion $map $size ($bounds.MinY + [int]($bounds.Height * 0.35)) $swing 0
            }
        }
        'hamster' {
            if ($facing -ne 1) {
                Put-Pixel $map $size ($bounds.CenterX - 2 * $unit) ($bounds.CenterY + $unit) 'm'
                Put-Pixel $map $size ($bounds.CenterX + 2 * $unit) ($bounds.CenterY + $unit) 'm'
            }
        }
    }
}

function Flatten-SleepPose($source, [int]$size, [int]$baseline) {
    $bounds = Get-PixelBounds $source
    $target = New-PixelMap
    $heightScale = 0.52
    $widthScale = 1.08
    foreach ($entry in $source.GetEnumerator()) {
        $parts = $entry.Key.Split(',')
        $x = [int]$parts[0]; $y = [int]$parts[1]
        # Keep the authored contour inside the one-pixel action margin while
        # widening the resting silhouette into a tucked sleeping pose.
        $newX = [Math]::Max(1, [Math]::Min($size - 2, $bounds.CenterX + [int][Math]::Round(($x - $bounds.CenterX) * $widthScale)))
        $newY = $baseline - [int][Math]::Round(($bounds.MaxY - $y) * $heightScale)
        Put-PixelByPriority $target $size $newX $newY $entry.Value
    }
    return $target
}

function Add-ActionPose($map, [int]$size, [int]$baseline, [string]$species, [int]$stageIndex, [int]$action, [int]$facing, [int]$frame) {
    $count = $frameCounts[$action]
    $phase = $frame % $count
    $bounds = Get-PixelBounds $map
    $unit = [Math]::Max(1, [int][Math]::Round($size / 32.0))
    $frontSign = if ($facing -eq 2) { -1 } else { 1 }
    switch ($action) {
        0 {
            Shift-PixelMap $map $size 0 (@(0, 0, -1, -1, 0, 0, 0, 0)[$phase])
            if ($phase -in @(5, 6)) { Add-ClosedEyes $map $size (Get-PixelBounds $map) $facing }
        }
        1 {
            Shift-PixelMap $map $size 0 (@(0, -1, -1, 0, 0, -1, -1, 0, 0, 0)[$phase] * $unit)
            $stride = @(-1, 0, 1, 1, 0, -1, -1, 0, 1, 0)[$phase] * $unit
            $walkingBounds = Get-PixelBounds $map
            Shift-LowerLimb $map $size $walkingBounds $true $stride 0
            Shift-LowerLimb $map $size $walkingBounds $false (-$stride) 0
            if ($phase -in @(2, 7)) {
                Put-Pixel $map $size ($walkingBounds.CenterX - $stride) $baseline 's'
            }
        }
        2 {
            $headLimit = $bounds.MinY + [int]($bounds.Height * 0.48)
            $headLean = @(1, 1, 0, 1, 1, 1, 0, -1, -1, 0)[$phase] * $unit
            Shift-PixelRegion $map $size $headLimit ($frontSign * $headLean) $(if ($phase % 3 -eq 1) { $unit } else { 0 })
            $current = Get-PixelBounds $map
            $foodX = if ($frontSign -lt 0) {
                if ($current.MinX -gt 1) { $current.MinX - 1 - $unit } else { $current.MaxX + 1 }
            } else {
                if ($current.MaxX -lt ($size - 2)) { $current.MaxX + 1 } else { $current.MinX - 1 - $unit }
            }
            $foodX = [Math]::Max(1, [Math]::Min($size - 2 - $unit, $foodX))
            $foodY = $current.MinY + [int]($current.Height * 0.48) + ($phase % 2)
            if ($phase -in 0..8) {
                Fill-Pixels $map $size $foodX $foodY (1 + $unit) (1 + $unit) 'a'
                Draw-PixelLine $map $size $current.CenterX ($current.CenterY + $unit) $foodX ($foodY + $unit) 'c'
            }
        }
        3 {
            $jump = @(0, -1, -2, -3, -2, -1, 0, -1, -2, 0)[$phase] * $unit
            Shift-PixelMap $map $size 0 $jump
            $happyBounds = Get-PixelBounds $map
            $happySway = @(0, -1, 0, 1, 0, -1, 0, 1, 0, -1)[$phase] * $unit
            $upperLimit = $happyBounds.MinY + [int][Math]::Round($happyBounds.Height * 0.62)
            Shift-PixelRegion $map $size $upperLimit $happySway 0
            $happyBounds = Get-PixelBounds $map
            Fill-Pixels $map $size ($happyBounds.CenterX - 2 * $unit) $baseline (4 * $unit + 1) 1 's'
            $rightSpace = ($size - 2) - $happyBounds.MaxX
            $leftSpace = $happyBounds.MinX - 1
            $sparkleX = if ($rightSpace -gt 0) {
                $happyBounds.MaxX + 1 + [Math]::Min($phase % 3, $rightSpace - 1)
            } elseif ($leftSpace -gt 0) {
                $happyBounds.MinX - 1 - [Math]::Min($phase % 3, $leftSpace - 1)
            } else {
                Clamp-PixelX ($happyBounds.CenterX + $frontSign * (3 + ($phase % 3)) * $unit) $size
            }
            $sparkleY = [Math]::Max(1, $happyBounds.MinY + ($phase % 2) * $unit)
            Put-Pixel $map $size $sparkleX $sparkleY $(if ($phase % 2 -eq 0) { 'h' } else { 'm' })
            $expressionX = Clamp-PixelX ($happyBounds.CenterX + $frontSign * $unit) $size
            $expressionY = [Math]::Max(1, [Math]::Min($size - 2, $happyBounds.MinY + 2 * $unit))
            Put-Pixel $map $size $expressionX $expressionY $(if ($phase % 2 -eq 0) { 'h' } else { 'a' })
        }
        4 {
            $sleep = Flatten-SleepPose $map $size $baseline
            $map.Clear(); foreach ($entry in $sleep.GetEnumerator()) { $map[$entry.Key] = $entry.Value }
            Add-ClosedEyes $map $size (Get-PixelBounds $map) $facing
            $rest = Get-PixelBounds $map
            $bubbleX = Clamp-PixelX ($rest.CenterX + (2 + ($phase % 3)) * $unit) $size
            $bubbleY = [Math]::Max(1, $rest.MinY - 1 - [int]($phase / 4))
            Put-Pixel $map $size $bubbleX $bubbleY 'h'
        }
        5 {
            Shift-PixelMap $map $size ($frontSign * @(-1, 0, 1, 1, 0, -1, -1, 0, 1, 0)[$phase] * $unit) (@(0, -1, 0, 0, 0, -1, 0, 0, -1, 0)[$phase] * $unit)
            $current = Get-PixelBounds $map
            Put-Pixel $map $size ([Math]::Max(1, [Math]::Min($size - 2, $current.CenterX + $frontSign * [Math]::Max(3, [int]($current.Width * 0.4))))) ([Math]::Max(1, $current.MinY + ($phase % 3))) 'r'
        }
        6 {
            if ($stageIndex -eq 0) {
                $crackX = $bounds.CenterX + @(-1, 0, 1, 0, -1, 1)[$phase]
                Draw-PixelLine $map $size $crackX ($bounds.MinY + 2) ($crackX - 1) ($bounds.CenterY + 1) 'o'
                if ($phase -ge 3) { Shift-PixelRegion $map $size ($bounds.MinY + [int]($bounds.Height * 0.35)) ($phase - 2) (-1 * ($phase - 2)) }
            } else {
                $rise = @(0, -1, -2, -1, 0, -1)[$phase] * $unit
                Shift-PixelMap $map $size 0 $rise
                Fill-Pixels $map $size ($bounds.CenterX - $unit) $baseline (2 * $unit + 1) 1 's'
                Put-Pixel $map $size ([Math]::Min($size - 2, $bounds.MaxX + 1)) ([Math]::Max(1, $bounds.MinY + $phase)) 'h'
            }
        }
        7 {
            Shift-PixelMap $map $size (@(0, -1, 1, -1, 1, 0)[$phase] * $unit) 0
            $current = Get-PixelBounds $map
            if ($facing -ne 1) {
                Fill-Pixels $map $size ($current.CenterX - 2 * $unit) ($current.MinY + 2 * $unit) $unit $unit 'h'
                Fill-Pixels $map $size ($current.CenterX + $unit) ($current.MinY + 2 * $unit) $unit $unit 'h'
            }
            Put-Pixel $map $size ([Math]::Max(1, $current.MinX - 1)) ([Math]::Max(1, $current.MinY + $phase)) 'r'
        }
        8 {
            Shift-PixelRegion $map $size ($bounds.MinY + [int]($bounds.Height * 0.45)) (@(0, -1, 0, 1, 0, -1)[$phase] * $unit) (-1 * $unit)
            $current = Get-PixelBounds $map
            $heartX = Clamp-PixelX ($current.CenterX - 3 * $unit + $phase * $unit) $size
            Put-Pixel $map $size $heartX ([Math]::Max(1, $current.MinY - 1)) 'm'
        }
        9 {
            $jump = @(0, -1, -2, -3, -2, 0)[$phase] * $unit
            Shift-PixelMap $map $size ($frontSign * $(if ($phase -in @(2, 3)) { $unit } else { 0 })) $jump
            $toySize = 1 + $unit
            $toyX = [Math]::Max(1, [Math]::Min($size - 1 - $toySize, $bounds.CenterX + (2 + ($phase % 3)) * $unit))
            $toyY = [Math]::Max(1, $baseline - (2 + ($phase % 2)) * $unit)
            Fill-Pixels $map $size $toyX $toyY $toySize $toySize $(if ($phase % 2 -eq 0) { 'a' } else { 'm' })
            Put-Pixel $map $size $bounds.CenterX $baseline 's'
            Put-Pixel $map $size $bounds.CenterX $bounds.CenterY $(if ($phase % 2 -eq 0) { 'h' } else { 'a' })
        }
        10 {
            $lookLift = @(0, -1, 0, -1, 0, -1)[$phase] * $unit
            Shift-PixelRegion $map $size ($bounds.MinY + [int]($bounds.Height * 0.42)) ($frontSign * $unit) (-1 * $unit + $lookLift)
            $current = Get-PixelBounds $map
            $signalX = if ($frontSign -gt 0 -and $current.MaxX -lt ($size - 2)) {
                $current.MaxX + 1
            } elseif ($current.MinX -gt 1) {
                $current.MinX - 1
            } else {
                $size - 2
            }
            Put-Pixel $map $size $signalX ([Math]::Max(1, $current.MinY + ($phase % 3))) 'r'
        }
        11 {
            $current = Get-PixelBounds $map
            $handX = $current.CenterX + $frontSign * [Math]::Max(2, [int]($current.Width * 0.18))
            $faceY = $current.MinY + [Math]::Max(2, [int]($current.Height * 0.30))
            Draw-PixelLine $map $size $current.CenterX ($current.CenterY + $unit) $handX ($faceY + ($phase % 2)) 'c'
            Put-Pixel $map $size ([Math]::Max(1, $current.MinX + $phase * $unit)) ([Math]::Max(1, $current.MinY - 1)) 'h'
        }
        12 {
            $current = Get-PixelBounds $map
            if ($facing -ne 1) { Fill-Pixels $map $size ($current.CenterX - $unit) ($current.MinY + [int]($current.Height * 0.35)) (1 + $unit) $unit 'x' }
            $waveX = [Math]::Min($size - 2, $current.MaxX + 1 + ($phase % 2))
            $waveY = [Math]::Max(1, $current.MinY + $phase * $unit)
            Put-Pixel $map $size $waveX $waveY 'a'
        }
    }
    Add-SpeciesMotion $map $size $species $action $facing $phase
}

function Get-FrameAnchors($map, [int]$size, [int]$baseline, [string]$species, [int]$stageIndex, [int]$action, [int]$facing, [int]$frame) {
    $bounds = Get-PixelBounds $map
    $side = if ($facing -eq 2) { -1 } elseif ($facing -eq 3) { 1 } else { 0 }
    $phase = $frame % $frameCounts[$action]
    $speciesOffset = switch ($species) {
        'cat' { @(-1, 0) }
        'dog' { @(1, 0) }
        'bird' { @(0, -1) }
        'penguin' { @(0, 0) }
        'rabbit' { @(1, -2) }
        'hamster' { @(-1, 1) }
    }
    $actionHeadX = if ($action -in @(2, 10, 11)) { $side + $(if ($facing -eq 0) { 1 } else { 0 }) } else { 0 }
    $actionHeadY = if ($action -in @(2, 10, 11)) { -1 - ($phase % 2) } else { 0 }
    $headX = [Math]::Max(0, [Math]::Min($size - 1, $bounds.CenterX + $side * [Math]::Max(1, [int]($bounds.Width * 0.12)) + $speciesOffset[0] + $actionHeadX))
    $headY = [Math]::Max(0, [Math]::Min($size - 1, $bounds.MinY + [int]($bounds.Height * 0.22) + $speciesOffset[1] + $actionHeadY))
    $handY = [Math]::Max(0, [Math]::Min($size - 1, $bounds.MinY + [int]($bounds.Height * 0.62) + $(if ($action -in @(2, 11)) { -1 - ($phase % 2) } else { 0 })))
    $frontLayer = if ($facing -eq 1) { 0 } else { 2 }
    $backX = [int]($bounds.CenterX - $side)
    $backY = [int][Math]::Min($size - 1, $bounds.CenterY + 1)
    $handX = [int][Math]::Max(0, [Math]::Min($size - 1, $bounds.CenterX + $side * 2 + $(if ($phase % 2 -eq 0) { -1 } else { 1 })))
    $neckY = [int][Math]::Min($size - 1, $headY + [Math]::Max(1, [int]($bounds.Height * 0.18)))
    $neckLayer = if ($facing -eq 1) { 0 } else { 1 }
    $tailX = [int][Math]::Max(0, [Math]::Min($size - 1, $bounds.CenterX - $side * [Math]::Max(2, [int]($bounds.Width * 0.34))))
    $tailY = [int][Math]::Min($size - 1, $bounds.MinY + [int]($bounds.Height * 0.68))
    $trailX = [int][Math]::Max(0, [Math]::Min($size - 1, $bounds.CenterX + $(if ($phase % 2 -eq 0) { -1 } else { 1 })))
    return [ordered]@{
        head = @($headX, $headY, $frontLayer)
        back = @($backX, $backY, 0)
        hand = @($handX, $handY, $frontLayer)
        neck = @($headX, $neckY, $neckLayer)
        tail = @($tailX, $tailY, 0)
        trail = @($trailX, $baseline, 0)
    }
}

function Write-StageSheet([string]$species, [int]$stageIndex) {
    $stage = $stageNames[$stageIndex]
    $size = $stageSizes[$stageIndex]
    $baseline = $stageBaselines[$stageIndex]
    $master = Read-NativeMaster $species $stage $size $baseline
    $facingMasters = @(
        for ($facingIndex = 0; $facingIndex -lt 4; $facingIndex++) {
            New-FacingMap $master $size $species $stageIndex $facingIndex
        }
    )
    $bitmap = [System.Drawing.Bitmap]::new($size * 20, $size * 20, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $records = [System.Collections.Generic.List[object]]::new()
    $index = 0
    try {
        for ($action = 0; $action -lt $frameCounts.Count; $action++) {
            for ($facing = 0; $facing -lt 4; $facing++) {
                for ($frame = 0; $frame -lt $frameCounts[$action]; $frame++) {
                    $map = Copy-PixelMap $facingMasters[$facing]
                    Add-ActionPose $map $size $baseline $species $stageIndex $action $facing $frame
                    if ($action -in @(3, 9)) {
                        Normalize-PixelMapHorizontal $map $size
                    } else {
                        Normalize-PixelMap $map $size $baseline
                    }
                    Assert-PixelMapSafe $map $size "$species/$stage/$action/$facing/$frame"
                    $baseX = ($index % 20) * $size
                    $baseY = [int][Math]::Floor($index / 20) * $size
                    foreach ($entry in $map.GetEnumerator()) {
                        $parts = $entry.Key.Split(',')
                        $bitmap.SetPixel($baseX + [int]$parts[0], $baseY + [int]$parts[1], $colors[[string]$entry.Value])
                    }
                    $records.Add([ordered]@{
                        index = $index; stage = $stageIndex; action = $action; facing = $facing; frame = $frame
                        width = $size; height = $size; pivotX = [int]($size / 2); baselineY = $baseline
                        anchors = Get-FrameAnchors $map $size $baseline $species $stageIndex $action $facing $frame
                        provenance = "authored-v5-$species-$stage-semantic-pixel-v2"
                    })
                    $index++
                }
            }
        }
        if ($index -ne 400) { throw "Unexpected frame count for $species/${stage}: $index" }
        $imagePath = Join-Path $OutputDirectory "${species}_${stage}.png"
        $metadataPath = Join-Path $OutputDirectory "${species}_${stage}.json"
        $bitmap.Save($imagePath, [System.Drawing.Imaging.ImageFormat]::Png)
        $metadata = [ordered]@{
            version = 5; format = 3; species = $species; stage = $stage
            canvasSize = $size; columns = 20; rows = 20; frames = $records
        }
        [System.IO.File]::WriteAllText($metadataPath, ($metadata | ConvertTo-Json -Depth 8), [System.Text.Encoding]::UTF8)
    } finally {
        $bitmap.Dispose()
    }
}

[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
foreach ($species in $speciesNames) {
    for ($stageIndex = 0; $stageIndex -lt $stageNames.Count; $stageIndex++) {
        Write-StageSheet $species $stageIndex
    }
}

$masterEntries = [ordered]@{}
foreach ($species in $speciesNames) {
    $masterEntries[$species] = [ordered]@{}
    for ($stageIndex = 0; $stageIndex -lt $stageNames.Count; $stageIndex++) {
        $stage = $stageNames[$stageIndex]
        $file = Join-Path $MasterDirectory "${species}_${stage}.png"
        $source = Join-Path $MasterSourceDirectory "${species}_${stage}.px"
        if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Missing editable master source: $source"
        }
        $masterEntries[$species][$stage] = [ordered]@{
            source = [System.IO.Path]::GetFileName($source)
            sourceSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $source).Hash.ToLowerInvariant()
            image = [System.IO.Path]::GetFileName($file)
            canvasSize = $stageSizes[$stageIndex]
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant()
        }
    }
}
$masterManifest = [ordered]@{
    schemaVersion = 2
    sourceVersion = 5
    provenance = "editable-semantic-pixel-rows"
    palette = @($colors.Keys)
    species = $masterEntries
}
[System.IO.File]::WriteAllText(
    (Join-Path $MasterDirectory "manifest.json"),
    ($masterManifest | ConvertTo-Json -Depth 7),
    [System.Text.Encoding]::UTF8
)

Write-Output "Redrew 24 native v5 Sprite sheets from semantic pixel masters"
