param(
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\..\app\src\main\assets\pixel_pet\reference")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# The design board is the visual contract. This source deliberately stores the
# pets as transparent, native-size pixel art instead of resampling screenshots.
# It is safe to edit with any nearest-neighbor pixel editor and rerun.

function Get-StageSize([string]$stage) {
    switch ($stage) {
        "egg" { 16 }
        "baby" { 16 }
        "young" { 32 }
        "adult" { 48 }
        default { throw "Unsupported stage: $stage" }
    }
}

function Get-Color([string]$hex) {
    return [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function Get-Palette([string]$species) {
    switch ($species) {
        "cat" { return @{ Outline = Get-Color "#573B35"; Base = Get-Color "#E87F39"; Shade = Get-Color "#B95535"; Cream = Get-Color "#FFE3B5"; Highlight = Get-Color "#FFF4D8"; Accent = Get-Color "#F4AE55"; Eye = Get-Color "#30242A" } }
        "dog" { return @{ Outline = Get-Color "#5A3C33"; Base = Get-Color "#E57935"; Shade = Get-Color "#A84C32"; Cream = Get-Color "#FFE0B1"; Highlight = Get-Color "#FFF0D2"; Accent = Get-Color "#9B5A45"; Eye = Get-Color "#2E2426" } }
        "bird" { return @{ Outline = Get-Color "#3B5D7D"; Base = Get-Color "#78BCE7"; Shade = Get-Color "#416FA7"; Cream = Get-Color "#F0D37D"; Highlight = Get-Color "#DDF5FF"; Accent = Get-Color "#F19C43"; Eye = Get-Color "#293845" } }
        "penguin" { return @{ Outline = Get-Color "#263650"; Base = Get-Color "#5376B7"; Shade = Get-Color "#31496E"; Cream = Get-Color "#F7F4F1"; Highlight = Get-Color "#D9F1FF"; Accent = Get-Color "#EBA13E"; Eye = Get-Color "#1E283A" } }
        "rabbit" { return @{ Outline = Get-Color "#56505E"; Base = Get-Color "#F0EEF2"; Shade = Get-Color "#AAA5B2"; Cream = Get-Color "#FFF9FE"; Highlight = Get-Color "#FFFFFF"; Accent = Get-Color "#E58E9B"; Eye = Get-Color "#3D3544" } }
        "hamster" { return @{ Outline = Get-Color "#5E4038"; Base = Get-Color "#C88964"; Shade = Get-Color "#8C5243"; Cream = Get-Color "#F7D6B4"; Highlight = Get-Color "#FFF0D8"; Accent = Get-Color "#D37474"; Eye = Get-Color "#2D2020" } }
        default { throw "Unsupported species: $species" }
    }
}

function Convert-GridEdge([double]$value, [double]$scale, [bool]$upper) {
    if ($upper) { return [Math]::Ceiling($value * $scale) }
    return [Math]::Floor($value * $scale)
}

function Fill-GridRect(
    [System.Drawing.Bitmap]$bitmap,
    [double]$scale,
    [double]$x,
    [double]$y,
    [double]$width,
    [double]$height,
    [System.Drawing.Color]$color
) {
    $left = [Math]::Max(0, [int](Convert-GridEdge $x $scale $false))
    $top = [Math]::Max(0, [int](Convert-GridEdge $y $scale $false))
    $right = [Math]::Min($bitmap.Width, [int](Convert-GridEdge ($x + $width) $scale $true))
    $bottom = [Math]::Min($bitmap.Height, [int](Convert-GridEdge ($y + $height) $scale $true))
    for ($py = $top; $py -lt $bottom; $py++) {
        for ($px = $left; $px -lt $right; $px++) {
            $bitmap.SetPixel($px, $py, $color)
        }
    }
}

function Fill-GridEllipse(
    [System.Drawing.Bitmap]$bitmap,
    [double]$scale,
    [double]$centerX,
    [double]$centerY,
    [double]$radiusX,
    [double]$radiusY,
    [System.Drawing.Color]$color
) {
    $left = [Math]::Max(0, [int](Convert-GridEdge ($centerX - $radiusX - 1) $scale $false))
    $top = [Math]::Max(0, [int](Convert-GridEdge ($centerY - $radiusY - 1) $scale $false))
    $right = [Math]::Min($bitmap.Width, [int](Convert-GridEdge ($centerX + $radiusX + 1) $scale $true))
    $bottom = [Math]::Min($bitmap.Height, [int](Convert-GridEdge ($centerY + $radiusY + 1) $scale $true))
    for ($py = $top; $py -lt $bottom; $py++) {
        for ($px = $left; $px -lt $right; $px++) {
            $logicalX = ($px + 0.5) / $scale
            $logicalY = ($py + 0.5) / $scale
            $inside = ((($logicalX - $centerX) / $radiusX) * (($logicalX - $centerX) / $radiusX)) + ((($logicalY - $centerY) / $radiusY) * (($logicalY - $centerY) / $radiusY))
            if ($inside -le 1.0) { $bitmap.SetPixel($px, $py, $color) }
        }
    }
}

function Draw-GridLine(
    [System.Drawing.Bitmap]$bitmap,
    [double]$scale,
    [double]$fromX,
    [double]$fromY,
    [double]$toX,
    [double]$toY,
    [double]$thickness,
    [System.Drawing.Color]$color
) {
    $steps = [Math]::Max([Math]::Abs($toX - $fromX), [Math]::Abs($toY - $fromY))
    $steps = [Math]::Max(1, [int][Math]::Ceiling($steps))
    for ($index = 0; $index -le $steps; $index++) {
        $fraction = $index / [double]$steps
        Fill-GridRect $bitmap $scale ($fromX + (($toX - $fromX) * $fraction)) ($fromY + (($toY - $fromY) * $fraction)) $thickness $thickness $color
    }
}

function Draw-OutlinedEllipse(
    [System.Drawing.Bitmap]$bitmap,
    [double]$scale,
    [double]$centerX,
    [double]$centerY,
    [double]$radiusX,
    [double]$radiusY,
    [System.Drawing.Color]$outline,
    [System.Drawing.Color]$fill
) {
    Fill-GridEllipse $bitmap $scale $centerX $centerY ($radiusX + 1) ($radiusY + 1) $outline
    Fill-GridEllipse $bitmap $scale $centerX $centerY $radiusX $radiusY $fill
}

function Draw-Egg([System.Drawing.Bitmap]$bitmap, [double]$scale, [string]$species, $palette) {
    Draw-OutlinedEllipse $bitmap $scale 16 18 7 10 $palette.Outline $palette.Cream
    Fill-GridEllipse $bitmap $scale 15 16 4 7 $palette.Highlight
    Fill-GridRect $bitmap $scale 10 24 12 1 $palette.Shade
    switch ($species) {
        "cat" {
            Fill-GridRect $bitmap $scale 13 10 2 2 $palette.Accent
            Fill-GridRect $bitmap $scale 18 12 2 2 $palette.Accent
            Fill-GridRect $bitmap $scale 15 19 2 1 $palette.Shade
        }
        "dog" {
            Fill-GridEllipse $bitmap $scale 16 14 4 4 $palette.Base
            Fill-GridRect $bitmap $scale 14 17 4 2 $palette.Cream
            Fill-GridRect $bitmap $scale 15 17 2 1 $palette.Eye
        }
        "bird" {
            Fill-GridEllipse $bitmap $scale 16 18 6 9 $palette.Base
            Fill-GridRect $bitmap $scale 12 12 1 2 $palette.Highlight
            Fill-GridRect $bitmap $scale 19 15 2 2 $palette.Highlight
            Fill-GridRect $bitmap $scale 15 22 2 1 $palette.Highlight
        }
        "penguin" {
            Fill-GridRect $bitmap $scale 10 15 12 3 $palette.Shade
            Fill-GridEllipse $bitmap $scale 16 20 4 5 $palette.Cream
            Fill-GridRect $bitmap $scale 15 18 2 1 $palette.Accent
        }
        "rabbit" {
            Fill-GridEllipse $bitmap $scale 16 18 6 9 $palette.Accent
            Fill-GridRect $bitmap $scale 12 14 2 2 $palette.Highlight
            Fill-GridRect $bitmap $scale 18 18 2 2 $palette.Highlight
            Fill-GridRect $bitmap $scale 14 23 2 1 $palette.Cream
        }
        "hamster" {
            Fill-GridEllipse $bitmap $scale 16 18 6 9 $palette.Base
            Fill-GridRect $bitmap $scale 12 13 1 2 $palette.Shade
            Fill-GridRect $bitmap $scale 19 16 1 1 $palette.Shade
            Fill-GridRect $bitmap $scale 15 23 2 1 $palette.Cream
        }
    }
}

function Draw-Cat([System.Drawing.Bitmap]$bitmap, [double]$scale, [string]$stage, $p) {
    $adult = $stage -eq "adult"
    $young = $stage -eq "young"
    $headY = if ($adult) { 12 } elseif ($young) { 14 } else { 16 }
    $bodyY = if ($adult) { 29 } elseif ($young) { 23 } else { 22 }
    $headRadius = if ($adult) { 8 } elseif ($young) { 7 } else { 5 }
    $bodyRadius = if ($adult) { 10 } elseif ($young) { 8 } else { 6 }
    Draw-OutlinedEllipse $bitmap $scale 16 $bodyY $bodyRadius ($bodyRadius - 2) $p.Outline $p.Base
    Draw-OutlinedEllipse $bitmap $scale 16 $headY $headRadius ($headRadius - 1) $p.Outline $p.Base
    Fill-GridRect $bitmap $scale (16 - $headRadius) ($headY - $headRadius - 2) 4 5 $p.Outline
    Fill-GridRect $bitmap $scale (17 + $headRadius - 4) ($headY - $headRadius - 2) 4 5 $p.Outline
    Fill-GridRect $bitmap $scale (16 - $headRadius + 1) ($headY - $headRadius - 1) 2 3 $p.Accent
    Fill-GridRect $bitmap $scale (17 + $headRadius - 3) ($headY - $headRadius - 1) 2 3 $p.Accent
    Fill-GridEllipse $bitmap $scale 16 ($headY + 3) ($headRadius - 2) 3 $p.Cream
    Fill-GridRect $bitmap $scale 12 ($headY + 1) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 19 ($headY + 1) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 15 ($headY + 4) 2 1 $p.Eye
    Fill-GridRect $bitmap $scale 13 ($bodyY + 2) 2 2 $p.Cream
    Fill-GridRect $bitmap $scale 18 ($bodyY + 2) 2 2 $p.Cream
    Draw-GridLine $bitmap $scale (23 + $bodyRadius / 3) ($bodyY + 2) 28 ($bodyY - 2) 2 $p.Outline
    Draw-GridLine $bitmap $scale (24 + $bodyRadius / 3) ($bodyY + 2) 28 ($bodyY - 2) 1 $p.Base
    if ($adult) {
        Fill-GridRect $bitmap $scale 12 26 8 2 $p.Cream
        Fill-GridRect $bitmap $scale 9 21 2 1 $p.Highlight
        Fill-GridRect $bitmap $scale 22 18 2 1 $p.Highlight
        Fill-GridRect $bitmap $scale 7 31 2 2 $p.Accent
    }
}

function Draw-Dog([System.Drawing.Bitmap]$bitmap, [double]$scale, [string]$stage, $p) {
    $adult = $stage -eq "adult"
    $young = $stage -eq "young"
    $headY = if ($adult) { 13 } elseif ($young) { 15 } else { 17 }
    $bodyY = if ($adult) { 28 } elseif ($young) { 23 } else { 22 }
    $headRadius = if ($adult) { 8 } elseif ($young) { 7 } else { 5 }
    $bodyRadius = if ($adult) { 10 } elseif ($young) { 8 } else { 6 }
    Draw-OutlinedEllipse $bitmap $scale 16 $bodyY $bodyRadius ($bodyRadius - 3) $p.Outline $p.Base
    Draw-OutlinedEllipse $bitmap $scale 16 $headY $headRadius ($headRadius - 1) $p.Outline $p.Base
    Fill-GridRect $bitmap $scale (16 - $headRadius - 2) ($headY - 2) 4 7 $p.Outline
    Fill-GridRect $bitmap $scale (16 + $headRadius - 2) ($headY - 2) 4 7 $p.Outline
    Fill-GridRect $bitmap $scale (16 - $headRadius - 1) ($headY - 1) 2 5 $p.Shade
    Fill-GridRect $bitmap $scale (16 + $headRadius - 1) ($headY - 1) 2 5 $p.Shade
    Fill-GridEllipse $bitmap $scale 16 ($headY + 3) ($headRadius - 2) 3 $p.Cream
    Fill-GridRect $bitmap $scale 12 ($headY + 1) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 19 ($headY + 1) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 15 ($headY + 4) 2 1 $p.Eye
    Fill-GridRect $bitmap $scale 14 ($bodyY + 2) 4 3 $p.Cream
    Draw-GridLine $bitmap $scale 24 ($bodyY + 1) 29 ($bodyY - 2) 2 $p.Outline
    Draw-GridLine $bitmap $scale 24 ($bodyY + 1) 29 ($bodyY - 2) 1 $p.Base
    if ($adult) {
        Fill-GridRect $bitmap $scale 9 23 13 3 $p.Accent
        Fill-GridRect $bitmap $scale 10 24 11 1 $p.Cream
        Fill-GridRect $bitmap $scale 23 17 2 3 $p.Shade
    }
}

function Draw-Bird([System.Drawing.Bitmap]$bitmap, [double]$scale, [string]$stage, $p) {
    $adult = $stage -eq "adult"
    $young = $stage -eq "young"
    $bodyY = if ($adult) { 24 } elseif ($young) { 21 } else { 22 }
    $radius = if ($adult) { 9 } elseif ($young) { 8 } else { 6 }
    Draw-OutlinedEllipse $bitmap $scale 16 $bodyY $radius ($radius - 2) $p.Outline $p.Base
    Fill-GridEllipse $bitmap $scale 17 ($bodyY + 3) ($radius - 3) 4 $p.Cream
    Fill-GridRect $bitmap $scale 20 ($bodyY - 3) 2 2 $p.Eye
    Fill-GridRect $bitmap $scale 23 ($bodyY - 1) 3 2 $p.Accent
    Fill-GridEllipse $bitmap $scale 10 ($bodyY + 1) 5 4 $p.Outline
    Fill-GridEllipse $bitmap $scale 10 ($bodyY + 1) 4 3 $p.Shade
    Fill-GridRect $bitmap $scale 13 ($bodyY + 1) 3 2 $p.Highlight
    Draw-GridLine $bitmap $scale 13 ($bodyY + 6) 10 ($bodyY + 9) 1 $p.Outline
    Draw-GridLine $bitmap $scale 18 ($bodyY + 6) 20 ($bodyY + 9) 1 $p.Outline
    if ($adult) {
        Draw-GridLine $bitmap $scale 10 23 2 15 3 $p.Outline
        Draw-GridLine $bitmap $scale 10 23 2 15 2 $p.Shade
        Draw-GridLine $bitmap $scale 20 22 30 14 3 $p.Outline
        Draw-GridLine $bitmap $scale 20 22 30 14 2 $p.Shade
        Fill-GridRect $bitmap $scale 5 16 4 1 $p.Highlight
        Fill-GridRect $bitmap $scale 24 14 4 1 $p.Highlight
        Fill-GridRect $bitmap $scale 12 8 2 2 $p.Highlight
    }
}

function Draw-Penguin([System.Drawing.Bitmap]$bitmap, [double]$scale, [string]$stage, $p) {
    $adult = $stage -eq "adult"
    $young = $stage -eq "young"
    $bodyY = if ($adult) { 25 } elseif ($young) { 22 } else { 22 }
    $radiusX = if ($adult) { 10 } elseif ($young) { 8 } else { 6 }
    $radiusY = if ($adult) { 13 } elseif ($young) { 10 } else { 7 }
    Draw-OutlinedEllipse $bitmap $scale 16 $bodyY $radiusX $radiusY $p.Outline $p.Shade
    Fill-GridEllipse $bitmap $scale 16 ($bodyY + 3) ($radiusX - 3) ($radiusY - 4) $p.Cream
    Fill-GridEllipse $bitmap $scale 16 ($bodyY - 5) 6 5 $p.Base
    Fill-GridRect $bitmap $scale 13 ($bodyY - 6) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 18 ($bodyY - 6) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 15 ($bodyY - 4) 2 1 $p.Accent
    Fill-GridEllipse $bitmap $scale 7 ($bodyY + 1) 3 5 $p.Outline
    Fill-GridEllipse $bitmap $scale 25 ($bodyY + 1) 3 5 $p.Outline
    Fill-GridRect $bitmap $scale 10 ($bodyY + $radiusY - 1) 4 2 $p.Accent
    Fill-GridRect $bitmap $scale 18 ($bodyY + $radiusY - 1) 4 2 $p.Accent
    if ($adult) {
        Fill-GridRect $bitmap $scale 11 17 10 3 $p.Accent
        Fill-GridRect $bitmap $scale 9 20 4 2 $p.Accent
        Fill-GridRect $bitmap $scale 13 7 2 3 $p.Accent
        Fill-GridRect $bitmap $scale 15 5 2 5 $p.Highlight
        Fill-GridRect $bitmap $scale 17 7 2 3 $p.Accent
    }
}

function Draw-Rabbit([System.Drawing.Bitmap]$bitmap, [double]$scale, [string]$stage, $p) {
    $adult = $stage -eq "adult"
    $young = $stage -eq "young"
    $bodyY = if ($adult) { 28 } elseif ($young) { 23 } else { 22 }
    $bodyRadius = if ($adult) { 10 } elseif ($young) { 8 } else { 6 }
    $earHeight = if ($adult) { 16 } elseif ($young) { 12 } else { 7 }
    Draw-OutlinedEllipse $bitmap $scale 16 $bodyY $bodyRadius ($bodyRadius - 2) $p.Outline $p.Base
    Draw-OutlinedEllipse $bitmap $scale 16 ($bodyY - 8) ($bodyRadius - 2) ($bodyRadius - 3) $p.Outline $p.Base
    Draw-OutlinedEllipse $bitmap $scale 11 ($bodyY - 13) 3 $earHeight $p.Outline $p.Base
    Draw-OutlinedEllipse $bitmap $scale 21 ($bodyY - 13) 3 $earHeight $p.Outline $p.Base
    Fill-GridEllipse $bitmap $scale 11 ($bodyY - 13) 1 ($earHeight - 2) $p.Accent
    Fill-GridEllipse $bitmap $scale 21 ($bodyY - 13) 1 ($earHeight - 2) $p.Accent
    Fill-GridRect $bitmap $scale 12 ($bodyY - 9) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 19 ($bodyY - 9) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 15 ($bodyY - 6) 2 1 $p.Accent
    Fill-GridEllipse $bitmap $scale 24 ($bodyY + 2) 3 3 $p.Cream
    Fill-GridRect $bitmap $scale 11 ($bodyY + 5) 3 2 $p.Shade
    Fill-GridRect $bitmap $scale 19 ($bodyY + 5) 3 2 $p.Shade
    if ($adult) {
        Fill-GridRect $bitmap $scale 13 6 6 2 $p.Accent
        Fill-GridRect $bitmap $scale 15 4 2 4 $p.Highlight
        Fill-GridRect $bitmap $scale 23 20 2 7 $p.Accent
        Fill-GridRect $bitmap $scale 25 19 3 2 $p.Cream
    }
}

function Draw-Hamster([System.Drawing.Bitmap]$bitmap, [double]$scale, [string]$stage, $p) {
    $adult = $stage -eq "adult"
    $young = $stage -eq "young"
    $bodyY = if ($adult) { 27 } elseif ($young) { 23 } else { 22 }
    $radius = if ($adult) { 11 } elseif ($young) { 8 } else { 6 }
    Draw-OutlinedEllipse $bitmap $scale 16 $bodyY $radius ($radius - 1) $p.Outline $p.Base
    Fill-GridEllipse $bitmap $scale 16 ($bodyY + 3) ($radius - 3) 5 $p.Cream
    Fill-GridEllipse $bitmap $scale 8 ($bodyY - 7) 3 3 $p.Outline
    Fill-GridEllipse $bitmap $scale 24 ($bodyY - 7) 3 3 $p.Outline
    Fill-GridEllipse $bitmap $scale 8 ($bodyY - 7) 2 2 $p.Accent
    Fill-GridEllipse $bitmap $scale 24 ($bodyY - 7) 2 2 $p.Accent
    Fill-GridRect $bitmap $scale 12 ($bodyY - 3) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 19 ($bodyY - 3) 1 2 $p.Eye
    Fill-GridRect $bitmap $scale 15 ($bodyY + 1) 2 1 $p.Accent
    Fill-GridRect $bitmap $scale 9 ($bodyY + 1) 2 2 $p.Cream
    Fill-GridRect $bitmap $scale 21 ($bodyY + 1) 2 2 $p.Cream
    if ($adult) {
        Fill-GridRect $bitmap $scale 13 9 6 2 $p.Accent
        Fill-GridRect $bitmap $scale 15 7 2 4 $p.Highlight
        Fill-GridRect $bitmap $scale 8 21 2 7 $p.Shade
        Fill-GridRect $bitmap $scale 22 21 2 7 $p.Shade
    }
}

function Draw-HamsterWheel([string]$path, $p) {
    $bitmap = [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        Draw-OutlinedEllipse $bitmap 1.0 16 15 13 13 $p.Outline $p.Shade
        Draw-OutlinedEllipse $bitmap 1.0 16 15 10 10 $p.Outline ([System.Drawing.Color]::Transparent)
        Fill-GridRect $bitmap 1.0 14 1 4 2 $p.Highlight
        Fill-GridRect $bitmap 1.0 4 15 2 2 $p.Highlight
        Fill-GridRect $bitmap 1.0 26 15 2 2 $p.Highlight
        Fill-GridRect $bitmap 1.0 14 27 4 2 $p.Highlight
        Fill-GridRect $bitmap 1.0 13 28 6 3 $p.Outline
        Fill-GridRect $bitmap 1.0 14 28 4 2 $p.Accent
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

function Draw-Pet([string]$species, [string]$stage, [string]$directory) {
    $size = Get-StageSize $stage
    $scale = $size / 32.0
    $palette = Get-Palette $species
    $bitmap = [System.Drawing.Bitmap]::new($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        if ($stage -eq "egg") {
            Draw-Egg $bitmap $scale $species $palette
        } else {
            switch ($species) {
                "cat" { Draw-Cat $bitmap $scale $stage $palette }
                "dog" { Draw-Dog $bitmap $scale $stage $palette }
                "bird" { Draw-Bird $bitmap $scale $stage $palette }
                "penguin" { Draw-Penguin $bitmap $scale $stage $palette }
                "rabbit" { Draw-Rabbit $bitmap $scale $stage $palette }
                "hamster" { Draw-Hamster $bitmap $scale $stage $palette }
            }
        }
        $bitmap.Save((Join-Path $directory "${species}_${stage}.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$species = @("cat", "dog", "bird", "penguin", "rabbit", "hamster")
$stages = @("egg", "baby", "young", "adult")
foreach ($pet in $species) {
    foreach ($stage in $stages) {
        Draw-Pet $pet $stage $OutputDirectory
    }
}

# The high-form hamster is draggable; its wheel is a separate habitat prop.
Copy-Item (Join-Path $OutputDirectory "hamster_adult.png") (Join-Path $OutputDirectory "hamster_adult_body.png") -Force
Draw-HamsterWheel (Join-Path $OutputDirectory "hamster_wheel.png") (Get-Palette "hamster")
Write-Output "Redrew transparent reference Sprites in $OutputDirectory"
