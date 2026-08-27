param(
    [string]$CoreBoard = (Join-Path $PSScriptRoot "design-boards\core-stages.png"),
    [string]$HamsterBoard = (Join-Path $PSScriptRoot "design-boards\hamster-stages.png"),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "design-draft-v1")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# These rectangles are documented source-board coordinates, not inferred at
# runtime. Keeping them here makes the user-approved board editable and the
# generated runtime assets reproducible.
$coreFrames = @{
    cat = @{
        egg = @(120, 144, 58, 72); baby = @(120, 216, 60, 60)
        young = @(110, 276, 80, 82); adult = @(104, 360, 94, 118)
    }
    dog = @{
        egg = @(314, 144, 58, 72); baby = @(304, 216, 72, 60)
        young = @(292, 276, 100, 82); adult = @(302, 360, 90, 112)
    }
    bird = @{
        egg = @(496, 144, 56, 72); baby = @(500, 216, 58, 60)
        young = @(490, 276, 82, 82); adult = @(448, 346, 134, 126)
    }
    penguin = @{
        egg = @(684, 144, 54, 72); baby = @(684, 216, 56, 60)
        young = @(672, 276, 82, 82); adult = @(678, 348, 74, 124)
    }
    rabbit = @{
        egg = @(852, 144, 70, 72); baby = @(864, 216, 60, 60)
        young = @(858, 276, 76, 82); adult = @(860, 348, 74, 124)
    }
}

$hamsterFrames = @{
    egg = @(854, 144, 64, 72); baby = @(850, 216, 76, 60)
    young = @(854, 276, 76, 82)
    # The only high-form hamster board pose is seated inside its wheel. Keep
    # that wheel as habitat furniture and use the board's complete standalone
    # adult pose in the larger runtime canvas rather than ship a cropped pet.
    adult = @(854, 276, 76, 82)
}

$hamsterWheel = @(844, 360, 104, 116)

$targetSizes = @{ egg = 32; baby = 48; young = 64; adult = 96 }
$boardMatte = [System.Drawing.Color]::FromArgb(255, 239, 211, 150)

function Color-DistanceSquared([System.Drawing.Color]$left, [System.Drawing.Color]$right) {
    $red = $left.R - $right.R
    $green = $left.G - $right.G
    $blue = $left.B - $right.B
    return ($red * $red) + ($green * $green) + ($blue * $blue)
}

function Is-BoardMatte([System.Drawing.Color]$color) {
    if ($color.A -eq 0) { return $true }
    # The cream cat egg and white rabbit fur are close to the board color.
    # Keep this threshold deliberately tight; detached board texture is
    # removed later as tiny components, but authored pale pixels must stay.
    return (Color-DistanceSquared $color $boardMatte) -le 5625
}

function Clear-ConnectedMatte([System.Drawing.Bitmap]$bitmap) {
    $width = $bitmap.Width
    $height = $bitmap.Height
    $visited = New-Object 'bool[,]' $width, $height
    $queue = [System.Collections.Generic.Queue[System.Drawing.Point]]::new()
    for ($x = 0; $x -lt $width; $x++) {
        $queue.Enqueue([System.Drawing.Point]::new($x, 0))
        $queue.Enqueue([System.Drawing.Point]::new($x, $height - 1))
    }
    for ($y = 1; $y -lt ($height - 1); $y++) {
        $queue.Enqueue([System.Drawing.Point]::new(0, $y))
        $queue.Enqueue([System.Drawing.Point]::new($width - 1, $y))
    }
    while ($queue.Count -gt 0) {
        $point = $queue.Dequeue()
        if ($point.X -lt 0 -or $point.Y -lt 0 -or $point.X -ge $width -or $point.Y -ge $height) { continue }
        if ($visited[$point.X, $point.Y]) { continue }
        $visited[$point.X, $point.Y] = $true
        if (-not (Is-BoardMatte $bitmap.GetPixel($point.X, $point.Y))) { continue }
        $bitmap.SetPixel($point.X, $point.Y, [System.Drawing.Color]::Transparent)
        $queue.Enqueue([System.Drawing.Point]::new($point.X - 1, $point.Y))
        $queue.Enqueue([System.Drawing.Point]::new($point.X + 1, $point.Y))
        $queue.Enqueue([System.Drawing.Point]::new($point.X, $point.Y - 1))
        $queue.Enqueue([System.Drawing.Point]::new($point.X, $point.Y + 1))
    }
}

function Is-StrongModelInk([System.Drawing.Color]$color) {
    if ($color.A -eq 0) { return $false }
    $maximum = [Math]::Max($color.R, [Math]::Max($color.G, $color.B))
    $distance = Color-DistanceSquared $color $boardMatte
    # The screenshot board contains saturated gold texture. Only the dark
    # hand-drawn outline is a reliable model seed; fur, feathers, eggshells,
    # and highlights are recovered later from the outline's closed region.
    return $distance -ge 1500 -and $maximum -le 185
}

function Keep-DesignContours([System.Drawing.Bitmap]$bitmap, [int]$radius) {
    $width = $bitmap.Width
    $height = $bitmap.Height
    $keep = New-Object 'bool[,]' $width, $height
    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            if (-not (Is-StrongModelInk $bitmap.GetPixel($x, $y))) { continue }
            # Close the small anti-aliasing gaps in the screenshot's original
            # dark outline. All authored interior colours are then recovered
            # from the original crop, so pale fur and eggshell never become a
            # colour-key casualty.
            for ($offsetY = -$radius; $offsetY -le $radius; $offsetY++) {
                for ($offsetX = -$radius; $offsetX -le $radius; $offsetX++) {
                    if (($offsetX * $offsetX) + ($offsetY * $offsetY) -gt ($radius * $radius)) { continue }
                    $targetX = $x + $offsetX
                    $targetY = $y + $offsetY
                    if ($targetX -ge 0 -and $targetY -ge 0 -and $targetX -lt $width -and $targetY -lt $height) { $keep[$targetX, $targetY] = $true }
                }
            }
        }
    }
    $outside = New-Object 'bool[,]' $width, $height
    $queue = [System.Collections.Generic.Queue[System.Drawing.Point]]::new()
    for ($x = 0; $x -lt $width; $x++) {
        $queue.Enqueue([System.Drawing.Point]::new($x, 0))
        $queue.Enqueue([System.Drawing.Point]::new($x, $height - 1))
    }
    for ($y = 1; $y -lt ($height - 1); $y++) {
        $queue.Enqueue([System.Drawing.Point]::new(0, $y))
        $queue.Enqueue([System.Drawing.Point]::new($width - 1, $y))
    }
    while ($queue.Count -gt 0) {
        $point = $queue.Dequeue()
        if ($point.X -lt 0 -or $point.Y -lt 0 -or $point.X -ge $width -or $point.Y -ge $height) { continue }
        if ($outside[$point.X, $point.Y] -or $keep[$point.X, $point.Y]) { continue }
        $outside[$point.X, $point.Y] = $true
        $queue.Enqueue([System.Drawing.Point]::new($point.X - 1, $point.Y))
        $queue.Enqueue([System.Drawing.Point]::new($point.X + 1, $point.Y))
        $queue.Enqueue([System.Drawing.Point]::new($point.X, $point.Y - 1))
        $queue.Enqueue([System.Drawing.Point]::new($point.X, $point.Y + 1))
    }
    for ($y = 0; $y -lt $height; $y++) {
        for ($x = 0; $x -lt $width; $x++) {
            if (-not $outside[$x, $y]) { $keep[$x, $y] = $true }
            if (-not $keep[$x, $y]) { $bitmap.SetPixel($x, $y, [System.Drawing.Color]::Transparent) }
        }
    }
}

function Keep-PrimaryComponent([System.Drawing.Bitmap]$bitmap) {
    $width = $bitmap.Width
    $height = $bitmap.Height
    $visited = New-Object 'bool[,]' $width, $height
    $components = [System.Collections.Generic.List[System.Collections.Generic.List[System.Drawing.Point]]]::new()
    for ($startY = 0; $startY -lt $height; $startY++) {
        for ($startX = 0; $startX -lt $width; $startX++) {
            if ($visited[$startX, $startY] -or $bitmap.GetPixel($startX, $startY).A -eq 0) { continue }
            $queue = [System.Collections.Generic.Queue[System.Drawing.Point]]::new()
            $component = [System.Collections.Generic.List[System.Drawing.Point]]::new()
            $queue.Enqueue([System.Drawing.Point]::new($startX, $startY))
            $visited[$startX, $startY] = $true
            while ($queue.Count -gt 0) {
                $point = $queue.Dequeue()
                $component.Add($point)
                for ($offsetY = -1; $offsetY -le 1; $offsetY++) {
                    for ($offsetX = -1; $offsetX -le 1; $offsetX++) {
                        if ($offsetX -eq 0 -and $offsetY -eq 0) { continue }
                        $nextX = $point.X + $offsetX
                        $nextY = $point.Y + $offsetY
                        if ($nextX -lt 0 -or $nextY -lt 0 -or $nextX -ge $width -or $nextY -ge $height) { continue }
                        if ($visited[$nextX, $nextY] -or $bitmap.GetPixel($nextX, $nextY).A -eq 0) { continue }
                        $visited[$nextX, $nextY] = $true
                        $queue.Enqueue([System.Drawing.Point]::new($nextX, $nextY))
                    }
                }
            }
            $components.Add($component)
        }
    }
    if ($components.Count -le 1) { return }
    $primaryIndex = 0
    for ($index = 1; $index -lt $components.Count; $index++) {
        if ($components[$index].Count -gt $components[$primaryIndex].Count) { $primaryIndex = $index }
    }
    for ($index = 0; $index -lt $components.Count; $index++) {
        if ($index -eq $primaryIndex) { continue }
        $component = $components[$index]
        foreach ($point in $component) { $bitmap.SetPixel($point.X, $point.Y, [System.Drawing.Color]::Transparent) }
    }
}

function Get-OpaqueBounds([System.Drawing.Bitmap]$bitmap) {
    $minX = $bitmap.Width
    $minY = $bitmap.Height
    $maxX = -1
    $maxY = -1
    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            if ($bitmap.GetPixel($x, $y).A -eq 0) { continue }
            $minX = [Math]::Min($minX, $x)
            $minY = [Math]::Min($minY, $y)
            $maxX = [Math]::Max($maxX, $x)
            $maxY = [Math]::Max($maxY, $y)
        }
    }
    if ($maxX -lt $minX) { throw "No opaque design pixels found" }
    return @($minX, $minY, $maxX, $maxY)
}

function Restore-EggInterior([System.Drawing.Bitmap]$cleaned, [System.Drawing.Bitmap]$source) {
    $bounds = Get-OpaqueBounds $cleaned
    $width = $bounds[2] - $bounds[0] + 1
    $height = $bounds[3] - $bounds[1] + 1
    $centerX = ($bounds[0] + $bounds[2]) / 2.0
    # The low source-board rows contain a small ground shadow. Bias the egg
    # body upward so the recovery mask remains inside the drawn shell.
    $centerY = $bounds[1] + ($height * 0.44)
    $radiusX = [Math]::Max(1.0, ($width * 0.44))
    $radiusY = [Math]::Max(1.0, ($height * 0.42))
    for ($y = $bounds[1]; $y -le $bounds[3]; $y++) {
        for ($x = $bounds[0]; $x -le $bounds[2]; $x++) {
            if ($cleaned.GetPixel($x, $y).A -ne 0) { continue }
            $distance = (($x - $centerX) * ($x - $centerX) / ($radiusX * $radiusX)) + (($y - $centerY) * ($y - $centerY) / ($radiusY * $radiusY))
            if ($distance -gt 1.0) { continue }
            $color = $source.GetPixel($x, $y)
            $cleaned.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $color.R, $color.G, $color.B))
        }
    }
}

function Posterize-DesignSprite([System.Drawing.Bitmap]$bitmap) {
    # The supplied boards are enlarged screenshots, so each intended pixel can
    # carry several near-identical anti-alias colours. Group close colours into
    # five-step RGB buckets, then use each bucket's average source colour. The
    # result keeps the original warm/cool palette instead of imposing a new one.
    $buckets = @{}
    foreach ($y in 0..($bitmap.Height - 1)) {
        foreach ($x in 0..($bitmap.Width - 1)) {
            $color = $bitmap.GetPixel($x, $y)
            if ($color.A -eq 0) { continue }
            $key = "{0}:{1}:{2}" -f [int]($color.R / 64), [int]($color.G / 64), [int]($color.B / 64)
            if (-not $buckets.ContainsKey($key)) { $buckets[$key] = [long[]]@(0, 0, 0, 0) }
            $entry = $buckets[$key]
            $entry[0] += $color.R
            $entry[1] += $color.G
            $entry[2] += $color.B
            $entry[3]++
        }
    }
    foreach ($y in 0..($bitmap.Height - 1)) {
        foreach ($x in 0..($bitmap.Width - 1)) {
            $color = $bitmap.GetPixel($x, $y)
            if ($color.A -eq 0) { continue }
            $key = "{0}:{1}:{2}" -f [int]($color.R / 64), [int]($color.G / 64), [int]($color.B / 64)
            $entry = $buckets[$key]
            $bitmap.SetPixel(
                $x,
                $y,
                [System.Drawing.Color]::FromArgb(
                    $color.A,
                    [int]($entry[0] / $entry[3]),
                    [int]($entry[1] / $entry[3]),
                    [int]($entry[2] / $entry[3])
                )
            )
        }
    }
}

function Copy-Crop([System.Drawing.Bitmap]$source, [int[]]$rect) {
    $crop = [System.Drawing.Bitmap]::new($rect[2], $rect[3], [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt $rect[3]; $y++) {
        for ($x = 0; $x -lt $rect[2]; $x++) {
            $crop.SetPixel($x, $y, $source.GetPixel($rect[0] + $x, $rect[1] + $y))
        }
    }
    return $crop
}

function Fit-DesignSprite([System.Drawing.Bitmap]$crop, [int]$targetSize) {
    # The supplied boards are presentation screenshots. Derive alpha from the
    # original dark pixel-art contour, then retain the user-art crop colours
    # inside that contour.
    $original = $crop.Clone()
    try {
        $contourRadius = if ($targetSize -le 48) { 3 } else { 2 }
        Keep-DesignContours $crop $contourRadius
        if ($targetSize -eq 32) { Restore-EggInterior $crop $original }
        if ($targetSize -gt 48) { Clear-ConnectedMatte $crop }
        # Tight source crops can still include a disconnected label, palette, or
        # board ornament. The pet itself is the dominant connected silhouette.
        Keep-PrimaryComponent $crop
        $bounds = Get-OpaqueBounds $crop
        $sourceWidth = $bounds[2] - $bounds[0] + 1
        $sourceHeight = $bounds[3] - $bounds[1] + 1
        $available = $targetSize - 4
        $scale = [Math]::Min($available / [double]$sourceWidth, $available / [double]$sourceHeight)
        $drawWidth = [Math]::Max(1, [int][Math]::Round($sourceWidth * $scale))
        $drawHeight = [Math]::Max(1, [int][Math]::Round($sourceHeight * $scale))
        $left = [int][Math]::Floor(($targetSize - $drawWidth) / 2.0)
        # Preserve a shared visual foot baseline with a small safety margin.
        $top = $targetSize - 2 - $drawHeight
        $sprite = [System.Drawing.Bitmap]::new($targetSize, $targetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            for ($y = 0; $y -lt $drawHeight; $y++) {
                $sourceY = $bounds[1] + [Math]::Min($sourceHeight - 1, [int][Math]::Floor($y * $sourceHeight / [double]$drawHeight))
                for ($x = 0; $x -lt $drawWidth; $x++) {
                    $sourceX = $bounds[0] + [Math]::Min($sourceWidth - 1, [int][Math]::Floor($x * $sourceWidth / [double]$drawWidth))
                    $sprite.SetPixel($left + $x, $top + $y, $crop.GetPixel($sourceX, $sourceY))
                }
            }
            Posterize-DesignSprite $sprite
            return $sprite
        } catch {
            $sprite.Dispose()
            throw
        }
    } catch {
        throw
    } finally {
        $original.Dispose()
    }
}

function Save-Sprite([System.Drawing.Bitmap]$board, [int[]]$rect, [int]$targetSize, [string]$path) {
    $crop = Copy-Crop $board $rect
    try {
        $sprite = Fit-DesignSprite $crop $targetSize
        try { $sprite.Save($path, [System.Drawing.Imaging.ImageFormat]::Png) } finally { $sprite.Dispose() }
    } finally { $crop.Dispose() }
}

function Write-ContactSheet([string]$directory) {
    $names = @("cat", "dog", "bird", "penguin", "rabbit", "hamster")
    $stages = @("egg", "baby", "young", "adult")
    $sheet = [System.Drawing.Bitmap]::new(1152, 864, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($sheet)
    try {
        $graphics.Clear([System.Drawing.Color]::FromArgb(255, 31, 36, 46))
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $font = [System.Drawing.Font]::new("Consolas", 16)
        try {
            for ($row = 0; $row -lt $names.Count; $row++) {
                for ($column = 0; $column -lt $stages.Count; $column++) {
                    $name = $names[$row]
                    $stage = $stages[$column]
                    $source = [System.Drawing.Bitmap]::FromFile((Join-Path $directory "${name}_${stage}.png"))
                    try {
                        $cellLeft = 20 + $column * 280
                        $cellTop = 12 + $row * 142
                        $graphics.DrawImage($source, [System.Drawing.Rectangle]::new($cellLeft + 80, $cellTop, 96, 96))
                        $graphics.DrawString("${name}_${stage}", $font, [System.Drawing.Brushes]::White, $cellLeft, $cellTop + 104)
                    } finally { $source.Dispose() }
                }
            }
        } finally { $font.Dispose() }
        $sheet.Save((Join-Path $directory "contact-sheet.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $sheet.Dispose()
    }
}

if (-not (Test-Path -LiteralPath $CoreBoard)) { throw "Core design board does not exist: $CoreBoard" }
if (-not (Test-Path -LiteralPath $HamsterBoard)) { throw "Hamster design board does not exist: $HamsterBoard" }
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$core = [System.Drawing.Bitmap]::FromFile($CoreBoard)
$hamster = [System.Drawing.Bitmap]::FromFile($HamsterBoard)
try {
    foreach ($species in $coreFrames.Keys) {
        foreach ($stage in $targetSizes.Keys) {
            Save-Sprite $core $coreFrames[$species][$stage] $targetSizes[$stage] (Join-Path $OutputDirectory "${species}_${stage}.png")
        }
    }
    foreach ($stage in $targetSizes.Keys) {
        Save-Sprite $hamster $hamsterFrames[$stage] $targetSizes[$stage] (Join-Path $OutputDirectory "hamster_${stage}.png")
    }
    # The high-form board frames the hamster with a wheel. The draggable pet
    # and the habitat furniture deliberately remain independent assets.
    Save-Sprite $hamster $hamsterFrames.adult $targetSizes.adult (Join-Path $OutputDirectory "hamster_adult_body.png")
    Save-Sprite $hamster $hamsterWheel $targetSizes.young (Join-Path $OutputDirectory "hamster_wheel.png")
    Write-ContactSheet $OutputDirectory
} finally {
    $core.Dispose()
    $hamster.Dispose()
}

Write-Output "Imported user design board sprites to $OutputDirectory"
