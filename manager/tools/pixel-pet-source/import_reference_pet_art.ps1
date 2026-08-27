param(
    [Parameter(Mandatory = $true)]
    [string]$ImagePath,
    [string]$RabbitImagePath = "",
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\..\app\src\main\assets\pixel_pet\reference"),
    [string[]]$OnlySpecies = @()
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# The approved boards contain one row per growth stage. These tight crop boxes
# deliberately exclude board captions, arrows, palette swatches, and the
# sample thumbnails around each pet before nearest-neighbor placement on the
# transparent high-detail game grid. The final row maps to the app's Adult stage.
$canonicalGrid = 32
$targetSize = $canonicalGrid
$samples = [ordered]@{
    cat_egg = @(126, 148, 52, 66, 6, 3, 20, 26)
    dog_egg = @(306, 148, 52, 66, 6, 3, 20, 26)
    bird_egg = @(496, 148, 52, 66, 6, 3, 20, 26)
    penguin_egg = @(676, 148, 52, 66, 6, 3, 20, 26)
    rabbit_egg = @(852, 148, 52, 66, 6, 3, 20, 26)
    cat_baby = @(118, 218, 60, 58, 4, 6, 24, 22)
    dog_baby = @(294, 218, 70, 58, 4, 6, 24, 22)
    bird_baby = @(486, 218, 64, 58, 4, 6, 24, 22)
    penguin_baby = @(674, 218, 58, 62, 5, 5, 22, 24)
    rabbit_baby = @(857, 218, 52, 62, 5, 5, 22, 24)
    cat_young = @(116, 278, 64, 70, 2, 2, 28, 29)
    dog_young = @(284, 278, 84, 70, 2, 2, 28, 29)
    bird_young = @(474, 278, 88, 70, 2, 2, 28, 29)
    penguin_young = @(670, 278, 68, 70, 2, 2, 28, 29)
    rabbit_young = @(852, 278, 58, 70, 2, 2, 28, 29)
    cat_adult = @(110, 356, 78, 118, 1, 1, 30, 30)
    dog_adult = @(272, 356, 108, 118, 1, 1, 30, 30)
    bird_adult = @(458, 352, 118, 126, 1, 2, 30, 27)
    penguin_adult = @(660, 356, 88, 116, 1, 1, 30, 30)
    rabbit_adult = @(848, 356, 72, 116, 1, 1, 30, 30)
    hamster_egg = @(852, 148, 52, 66, 6, 3, 20, 26)
    hamster_baby = @(846, 218, 68, 62, 5, 5, 22, 24)
    hamster_young = @(840, 278, 72, 70, 2, 2, 28, 29)
    hamster_adult = @(820, 350, 110, 124, 1, 1, 30, 30)
    hamster_wheel = @(818, 350, 112, 124, 1, 1, 30, 30)
}

function Get-StageCanvasSize([string]$lifeStage) {
    switch ($lifeStage) {
        "egg" { 32 }
        "baby" { 48 }
        "young" { 64 }
        "adult" { 96 }
        # The habitat compositor expects the detached wheel on the standard
        # 32px grid, independently from the adult pet's 48px artboard.
        "wheel" { 64 }
        default { throw "Unsupported pixel pet stage: $lifeStage" }
    }
}

# The supplied design boards use a parchment-colored presentation background.
# Remove only that edge-connected matte after cropping. Do not apply a generic
# animal mask: it would cut species-specific ears, tails, wings, and wheels.
function Test-BoardMatteColor([System.Drawing.Color]$color) {
    if ($color.A -eq 0) { return $false }
    # The board paper is a pale parchment around rgb(239, 215, 167). Do not
    # classify arbitrary warm colors as paper: cat and dog coats use orange
    # pixels that otherwise disappear during the edge-connected flood fill.
    return $color.R -ge 210 -and
        $color.G -ge 180 -and
        $color.B -ge 120 -and
        $color.R -ge ($color.G + 12) -and
        $color.G -ge ($color.B + 12)
}

function Clear-BoardMatte([System.Drawing.Bitmap]$bitmap) {
    $width = $bitmap.Width
    $height = $bitmap.Height
    $visited = [bool[,]]::new($width, $height)
    $queue = [System.Collections.Generic.Queue[System.Drawing.Point]]::new()

    function Add-Seed([int]$x, [int]$y) {
        if ($visited[$x, $y] -or -not (Test-BoardMatteColor $bitmap.GetPixel($x, $y))) { return }
        $visited[$x, $y] = $true
        $queue.Enqueue([System.Drawing.Point]::new($x, $y))
    }

    for ($x = 0; $x -lt $width; $x++) {
        Add-Seed $x 0
        Add-Seed $x ($height - 1)
    }
    for ($y = 1; $y -lt ($height - 1); $y++) {
        Add-Seed 0 $y
        Add-Seed ($width - 1) $y
    }

    while ($queue.Count -gt 0) {
        $point = $queue.Dequeue()
        $bitmap.SetPixel($point.X, $point.Y, [System.Drawing.Color]::Transparent)
        foreach ($offset in @(
            @(-1, -1), @(0, -1), @(1, -1),
            @(-1, 0),              @(1, 0),
            @(-1, 1),  @(0, 1),  @(1, 1)
        )) {
            $nextX = $point.X + $offset[0]
            $nextY = $point.Y + $offset[1]
            if ($nextX -lt 0 -or $nextX -ge $width -or $nextY -lt 0 -or $nextY -ge $height -or $visited[$nextX, $nextY]) {
                continue
            }
            if (-not (Test-BoardMatteColor $bitmap.GetPixel($nextX, $nextY))) { continue }
            $visited[$nextX, $nextY] = $true
            $queue.Enqueue([System.Drawing.Point]::new($nextX, $nextY))
        }
    }
}

function Get-ColorDistanceSquared([System.Drawing.Color]$left, [System.Drawing.Color]$right) {
    $red = $left.R - $right.R
    $green = $left.G - $right.G
    $blue = $left.B - $right.B
    return ($red * $red + $green * $green + $blue * $blue)
}

# The supplied boards are exported screenshots, so their apparent pixels still
# contain anti-aliasing and JPEG-like shade noise. Convert every authored,
# opaque Sprite to a small deterministic palette after scaling. Skipping this
# for larger stages leaves thousands of near-identical colors in the final
# PNG; FilterQuality.None cannot make those half-tones look like deliberate
# pixel art on an LKM card.
function Reduce-PixelPalette([System.Drawing.Bitmap]$bitmap, [int]$maximumColors) {
    $pixels = [System.Collections.Generic.List[System.Drawing.Color]]::new()
    $counts = @{}
    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            $color = $bitmap.GetPixel($x, $y)
            if ($color.A -eq 0) { continue }
            $pixels.Add($color)
            $key = "$($color.R),$($color.G),$($color.B)"
            $counts[$key] = 1 + [int]($counts[$key] ?? 0)
        }
    }
    if ($pixels.Count -eq 0 -or $counts.Count -le $maximumColors) { return }

    $candidates = $counts.GetEnumerator() |
        ForEach-Object {
            $rgb = $_.Key.Split(',')
            [pscustomobject]@{
                Color = [System.Drawing.Color]::FromArgb(255, [int]$rgb[0], [int]$rgb[1], [int]$rgb[2])
                Count = [int]$_.Value
            }
        } |
        Sort-Object -Property Count -Descending
    $palette = [System.Collections.Generic.List[System.Drawing.Color]]::new()
    $palette.Add($candidates[0].Color)
    while ($palette.Count -lt $maximumColors -and $palette.Count -lt $candidates.Count) {
        $best = $null
        $bestScore = -1.0
        foreach ($candidate in $candidates) {
            $nearest = [double]::PositiveInfinity
            foreach ($paletteColor in $palette) {
                $nearest = [Math]::Min($nearest, [double](Get-ColorDistanceSquared $candidate.Color $paletteColor))
            }
            $score = $nearest * $candidate.Count
            if ($score -gt $bestScore) {
                $best = $candidate
                $bestScore = $score
            }
        }
        if ($null -eq $best) { break }
        $palette.Add($best.Color)
        $candidates = @($candidates | Where-Object {
            (Get-ColorDistanceSquared $_.Color $best.Color) -ne 0
        })
    }

    # A few Lloyd iterations stabilize the representative coat, shadow, and
    # highlight colors without introducing interpolation between grid cells.
    for ($iteration = 0; $iteration -lt 2; $iteration++) {
        $sumR = New-Object 'long[]' $palette.Count
        $sumG = New-Object 'long[]' $palette.Count
        $sumB = New-Object 'long[]' $palette.Count
        $samples = New-Object 'int[]' $palette.Count
        foreach ($color in $pixels) {
            $nearestIndex = 0
            $nearestDistance = [double]::PositiveInfinity
            for ($index = 0; $index -lt $palette.Count; $index++) {
                $distance = Get-ColorDistanceSquared $color $palette[$index]
                if ($distance -lt $nearestDistance) {
                    $nearestIndex = $index
                    $nearestDistance = $distance
                }
            }
            $sumR[$nearestIndex] += $color.R
            $sumG[$nearestIndex] += $color.G
            $sumB[$nearestIndex] += $color.B
            $samples[$nearestIndex]++
        }
        for ($index = 0; $index -lt $palette.Count; $index++) {
            if ($samples[$index] -gt 0) {
                $palette[$index] = [System.Drawing.Color]::FromArgb(
                    255,
                    [Math]::Round($sumR[$index] / $samples[$index]),
                    [Math]::Round($sumG[$index] / $samples[$index]),
                    [Math]::Round($sumB[$index] / $samples[$index])
                )
            }
        }
    }

    for ($y = 0; $y -lt $bitmap.Height; $y++) {
        for ($x = 0; $x -lt $bitmap.Width; $x++) {
            $color = $bitmap.GetPixel($x, $y)
            if ($color.A -eq 0) { continue }
            $nearestColor = $palette[0]
            $nearestDistance = [double]::PositiveInfinity
            foreach ($paletteColor in $palette) {
                $distance = Get-ColorDistanceSquared $color $paletteColor
                if ($distance -lt $nearestDistance) {
                    $nearestColor = $paletteColor
                    $nearestDistance = $distance
                }
            }
            $bitmap.SetPixel($x, $y, $nearestColor)
        }
    }
}

function Color-DistanceSquared([System.Drawing.Color]$left, [System.Drawing.Color]$right) {
    $red = $left.R - $right.R
    $green = $left.G - $right.G
    $blue = $left.B - $right.B
    return ($red * $red + $green * $green + $blue * $blue)
}

# Crops often keep a large screenshot-colored component inside an otherwise
# transparent 32px Sprite. Start from opaque pixels next to transparency, then
# remove only a large same-color component. This preserves isolated outlines,
# highlights, and the pet's enclosed pixels.
function Clear-ScreenshotMatte([System.Drawing.Bitmap]$bitmap) {
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
                    # The approved boards use warm paper and warm fur. Keep
                    # this import threshold conservative; the silhouette mask
                    # below handles outer board pixels without erasing coats.
                    if ($candidate.A -eq 0 -or (Color-DistanceSquared $seed $candidate) -gt 1444) { continue }
                    $visited[$nx, $ny] = $true
                    $queue.Enqueue([System.Drawing.Point]::new($nx, $ny))
                }
            }
            if ($component.Count -ge $minimumArea -and $component.Count * 8 -ge $opaqueCount) {
                foreach ($point in $component) { $bitmap.SetPixel($point.X, $point.Y, [System.Drawing.Color]::Transparent) }
            }
        }
    }
}

function Test-Ellipse([double]$x, [double]$y, [double]$centerX, [double]$centerY, [double]$radiusX, [double]$radiusY) {
    $horizontal = ($x - $centerX) / $radiusX
    $vertical = ($y - $centerY) / $radiusY
    return ($horizontal * $horizontal + $vertical * $vertical -le 1.0)
}

# Board paper can share a color with eggshells and white fur. Constrain every
# import to the hand-authored 32px silhouette before it reaches the app. The
# source colors remain untouched inside the mask, while captions and paper
# pixels outside it are guaranteed transparent.
function Test-PetSilhouette([string]$species, [string]$lifeStage, [double]$x, [double]$y) {
    $x = $x * $canonicalGrid / $targetSize
    $y = $y * $canonicalGrid / $targetSize
    $body = switch ($lifeStage) {
        "egg" { Test-Ellipse $x $y 16 16 10.5 14 }
        "baby" { Test-Ellipse $x $y 16 18 10.5 10.5 }
        "young" { Test-Ellipse $x $y 16 17 12.5 13.5 }
        default { Test-Ellipse $x $y 16 17 14.5 14.5 }
    }
    if ($lifeStage -eq "egg") { return $body }

    $ears = $false
    $tail = $false
    switch ($species) {
        "cat" {
            $ears = ($x -ge 6 -and $x -le 12 -and $y -ge 2 -and $y -le 11) -or ($x -ge 20 -and $x -le 26 -and $y -ge 2 -and $y -le 11)
            $tail = ($x -ge 23 -and $x -le 30 -and $y -ge 17 -and $y -le 27)
        }
        "dog" {
            $ears = ($x -ge 5 -and $x -le 11 -and $y -ge 6 -and $y -le 15) -or ($x -ge 21 -and $x -le 27 -and $y -ge 6 -and $y -le 15)
            $tail = ($x -ge 23 -and $x -le 31 -and $y -ge 17 -and $y -le 27)
        }
        "rabbit" {
            $ears = ($x -ge 7 -and $x -le 13 -and $y -ge 0 -and $y -le 13) -or ($x -ge 19 -and $x -le 25 -and $y -ge 0 -and $y -le 13)
            $tail = ($x -ge 23 -and $x -le 29 -and $y -ge 19 -and $y -le 26)
        }
        "bird" {
            $body = $body -or (Test-Ellipse $x $y 16 17 16 12)
            $tail = ($x -ge 1 -and $x -le 8 -and $y -ge 17 -and $y -le 26) -or ($x -ge 24 -and $x -le 31 -and $y -ge 17 -and $y -le 26)
        }
        "penguin" {
            $body = $body -or (Test-Ellipse $x $y 16 17 11.5 15.5)
            $ears = ($lifeStage -eq "adult" -and $x -ge 12 -and $x -le 20 -and $y -ge 0 -and $y -le 5)
        }
        "hamster" {
            $body = $body -or (Test-Ellipse $x $y 16 17 15.5 15.5)
            $ears = ($x -ge 6 -and $x -le 11 -and $y -ge 7 -and $y -le 13) -or ($x -ge 21 -and $x -le 26 -and $y -ge 7 -and $y -le 13)
        }
    }
    return ($body -or $ears -or $tail)
}

function Restore-EggCore([System.Drawing.Bitmap]$sprite, [System.Drawing.Bitmap]$source) {
    for ($y = 0; $y -lt $targetSize; $y++) {
        for ($x = 0; $x -lt $targetSize; $x++) {
            $logicalX = $x * $canonicalGrid / $targetSize
            $logicalY = $y * $canonicalGrid / $targetSize
            if (Test-Ellipse $logicalX $logicalY 16 16 8.5 11.5) {
                $sprite.SetPixel($x, $y, $source.GetPixel($x, $y))
            }
        }
    }
}

function Clip-ToPetSilhouette([System.Drawing.Bitmap]$sprite, [string]$species, [string]$lifeStage) {
    for ($y = 0; $y -lt $targetSize; $y++) {
        for ($x = 0; $x -lt $targetSize; $x++) {
            if (-not (Test-PetSilhouette $species $lifeStage $x $y)) {
                $sprite.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            }
        }
    }
}

if (-not (Test-Path -LiteralPath $ImagePath)) { throw "Reference image does not exist: $ImagePath" }
if (-not [string]::IsNullOrWhiteSpace($RabbitImagePath) -and -not (Test-Path -LiteralPath $RabbitImagePath)) {
    throw "Rabbit reference image does not exist: $RabbitImagePath"
}
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
foreach ($legacyName in @(
    "cat_model.png",
    "dog_model.png",
    "bird_model.png",
    "penguin_model.png",
    "rabbit_model.png"
)) {
    $legacyPath = Join-Path $OutputDirectory $legacyName
    if (Test-Path -LiteralPath $legacyPath) {
        Remove-Item -LiteralPath $legacyPath -Force
    }
}
$source = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $ImagePath).Path)
$rabbitSource = if ([string]::IsNullOrWhiteSpace($RabbitImagePath)) {
    $null
} else {
    [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $RabbitImagePath).Path)
}
try {
    foreach ($entry in $samples.GetEnumerator()) {
        $species = $entry.Key.Split('_', 2)[0]
        $lifeStage = $entry.Key.Split('_', 2)[1]
        if ($OnlySpecies.Count -gt 0 -and $species -notin $OnlySpecies) { continue }
        # The primary board contains cat, dog, bird, penguin, and hamster.
        # Rabbit lives on the companion board in the same supplied art style.
        $sourceForEntry = if ($species -eq "rabbit" -and $null -ne $rabbitSource) { $rabbitSource } else { $source }
        $targetSize = Get-StageCanvasSize $lifeStage
        $scale = $targetSize / [double]$canonicalGrid
        $x = [int]$entry.Value[0]
        $y = [int]$entry.Value[1]
        $cropWidth = [int]$entry.Value[2]
        $cropHeight = [int]$entry.Value[3]
        $destinationX = [Math]::Round([int]$entry.Value[4] * $scale)
        $destinationY = [Math]::Round([int]$entry.Value[5] * $scale)
        $destinationWidth = [Math]::Max(1, [Math]::Round([int]$entry.Value[6] * $scale))
        $destinationHeight = [Math]::Max(1, [Math]::Round([int]$entry.Value[7] * $scale))
        if ($x + $cropWidth -gt $sourceForEntry.Width -or $y + $cropHeight -gt $sourceForEntry.Height) {
            throw "Reference crop for $($entry.Key) exceeds the supplied image"
        }
        $sourceCrop = [System.Drawing.Bitmap]::new($cropWidth, $cropHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $sourceGraphics = [System.Drawing.Graphics]::FromImage($sourceCrop)
        try {
            $sourceGraphics.DrawImage(
                $sourceForEntry,
                [System.Drawing.Rectangle]::new(0, 0, $cropWidth, $cropHeight),
                [System.Drawing.Rectangle]::new($x, $y, $cropWidth, $cropHeight),
                [System.Drawing.GraphicsUnit]::Pixel
            )
        } finally {
            $sourceGraphics.Dispose()
        }
        try {
            # Remove paper while the original pixels are still intact. Doing
            # this after resampling blends the board color into the outline.
            Clear-BoardMatte $sourceCrop
            $sprite = [System.Drawing.Bitmap]::new($targetSize, $targetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                for ($row = 0; $row -lt $destinationHeight; $row++) {
                    for ($column = 0; $column -lt $destinationWidth; $column++) {
                        $sourceX = [Math]::Floor(($column + 0.5) * $cropWidth / $destinationWidth)
                        $sourceY = [Math]::Floor(($row + 0.5) * $cropHeight / $destinationHeight)
                        $sprite.SetPixel($destinationX + $column, $destinationY + $row, $sourceCrop.GetPixel($sourceX, $sourceY))
                    }
                }
                # Preserve the authored silhouette exactly. Only the board
                # paper connected to a crop edge is removed; geometry masks
                # would cut wings, tails, crowns, and high-form accessories.
                Clear-BoardMatte $sprite
                $paletteSize = switch ($lifeStage) {
                    "egg" { 10 }
                    "baby" { 12 }
                    # Preserve facial, limb, wing, and coat detail while
                    # removing screenshot-only anti-aliasing.
                    "young" { 22 }
                    "adult" { 30 }
                    "wheel" { 16 }
                    default { 18 }
                }
                Reduce-PixelPalette $sprite $paletteSize
                if ($entry.Key -eq "hamster_wheel") {
                    # The high-form board places the pet inside its wheel.
                    # Keep only the wheel shell in the habitat layer.
                    for ($maskY = 8; $maskY -le 28; $maskY++) {
                        for ($maskX = 8; $maskX -le 24; $maskX++) {
                            $sprite.SetPixel($maskX, $maskY, [System.Drawing.Color]::Transparent)
                        }
                    }
                }
                $sprite.Save((Join-Path $OutputDirectory "$($entry.Key).png"), [System.Drawing.Imaging.ImageFormat]::Png)
            } finally {
                $sprite.Dispose()
            }
        } finally {
            $sourceCrop.Dispose()
        }
    }

    # The high-form hamster is deliberately shown inside a wheel on the
    # approved board. The wheel is a fixed habitat prop, whereas the pet must
    # remain draggable. Reuse the board's standalone adult hamster body for
    # the 48px high-form canvas and keep the separately imported wheel below
    # the pet layer.
    if ($OnlySpecies.Count -eq 0 -or "hamster" -in $OnlySpecies) {
        $youngPath = Join-Path $OutputDirectory "hamster_young.png"
        if (-not (Test-Path -LiteralPath $youngPath)) {
            throw "Hamster body source was not generated: $youngPath"
        }
        $youngSprite = [System.Drawing.Bitmap]::new($youngPath)
        $adultCanvasSize = Get-StageCanvasSize "adult"
        $adultBody = [System.Drawing.Bitmap]::new($adultCanvasSize, $adultCanvasSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $bodyGraphics = [System.Drawing.Graphics]::FromImage($adultBody)
        try {
            $bodyGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $bodyGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $bodyGraphics.DrawImage(
                $youngSprite,
                [System.Drawing.Rectangle]::new(0, 0, $adultCanvasSize, $adultCanvasSize),
                [System.Drawing.Rectangle]::new(0, 0, $youngSprite.Width, $youngSprite.Height),
                [System.Drawing.GraphicsUnit]::Pixel
            )
            $adultBody.Save((Join-Path $OutputDirectory "hamster_adult_body.png"), [System.Drawing.Imaging.ImageFormat]::Png)
        } finally {
            $bodyGraphics.Dispose()
            $adultBody.Dispose()
            $youngSprite.Dispose()
        }
    }
} finally {
    $source.Dispose()
    if ($null -ne $rabbitSource) { $rabbitSource.Dispose() }
}

Write-Output "Imported reference pixel pet Sprites to $OutputDirectory"
