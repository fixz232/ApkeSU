param(
    [string]$SourceDirectory = (Join-Path $PSScriptRoot "v3"),
    [string]$PreviewPath = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# Adult master v1 is intentionally authored as pixel cells, then written into
# the editable 32x32 PNG sheets. The resulting PNG and anchor JSON remain the
# source of truth for later hand edits in Aseprite or any nearest-neighbor editor.
$grid = 32
$columns = 40
$sheetSize = $grid * $columns
$speciesNames = @("penguin", "dog", "cat", "bird", "rabbit")
$actionFrameCounts = @(8, 10, 10, 10, 10, 10, 6, 6, 6, 6, 6, 6, 6)
$adultStage = 3
$coreActions = @(0, 1, 2, 4) # Idle, Walking, Eating, Sleeping
$palette = @(
    "00000000", "FF24212B", "FFC8A27C", "FF766A82", "FFF6EAD7", "FFFFFFFF",
    "FFFFAB76", "FFD27C9E", "FF9EDEFA", "FF302A36", "FF17131D"
) | ForEach-Object { [Convert]::ToUInt32($_, 16) }

function Clamp-Grid([int]$value) { [Math]::Max(0, [Math]::Min($grid - 1, $value)) }

function New-Art { @{} }

function Set-Pixel([hashtable]$art, [int]$x, [int]$y, [int]$color) {
    if ($x -ge 0 -and $x -lt $grid -and $y -ge 0 -and $y -lt $grid) {
        $art["$x,$y"] = $color
    }
}

function Fill-Rect([hashtable]$art, [int]$x, [int]$y, [int]$width, [int]$height, [int]$color) {
    for ($row = $y; $row -lt $y + $height; $row++) {
        for ($column = $x; $column -lt $x + $width; $column++) {
            Set-Pixel $art $column $row $color
        }
    }
}

function Fill-Oval([hashtable]$art, [int]$centerX, [int]$centerY, [int]$radiusX, [int]$radiusY, [int]$color) {
    for ($y = $centerY - $radiusY; $y -le $centerY + $radiusY; $y++) {
        for ($x = $centerX - $radiusX; $x -le $centerX + $radiusX; $x++) {
            $distance = (($x - $centerX) * ($x - $centerX) / [double]($radiusX * $radiusX)) +
                (($y - $centerY) * ($y - $centerY) / [double]($radiusY * $radiusY))
            if ($distance -le 1.0) { Set-Pixel $art $x $y $color }
        }
    }
}

function Draw-Oval([hashtable]$art, [int]$centerX, [int]$centerY, [int]$radiusX, [int]$radiusY, [int]$fill) {
    Fill-Oval $art $centerX $centerY $radiusX $radiusY 1
    Fill-Oval $art $centerX $centerY ([Math]::Max(1, $radiusX - 1)) ([Math]::Max(1, $radiusY - 1)) $fill
}

function Draw-Line([hashtable]$art, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [int]$color) {
    $steps = [Math]::Max([Math]::Abs($x1 - $x0), [Math]::Abs($y1 - $y0))
    for ($step = 0; $step -le $steps; $step++) {
        $fraction = if ($steps -eq 0) { 0 } else { $step / [double]$steps }
        Set-Pixel $art ([Math]::Round($x0 + ($x1 - $x0) * $fraction)) ([Math]::Round($y0 + ($y1 - $y0) * $fraction)) $color
    }
}

function Draw-Triangle([hashtable]$art, [int]$centerX, [int]$bottomY, [int]$height, [int]$color) {
    for ($row = 0; $row -lt $height; $row++) {
        $half = [Math]::Floor($row / 2)
        for ($x = $centerX - $half; $x -le $centerX + $half; $x++) {
            Set-Pixel $art $x ($bottomY - $row) $color
        }
    }
}

function Mirror-Art([hashtable]$art) {
    $mirrored = New-Art
    foreach ($entry in $art.GetEnumerator()) {
        $parts = $entry.Key.Split(',')
        Set-Pixel $mirrored (31 - [int]$parts[0]) ([int]$parts[1]) ([int]$entry.Value)
    }
    return $mirrored
}

function Get-FrameIndex([int]$action, [int]$facing, [int]$frame) {
    $offset = $adultStage * 400
    for ($index = 0; $index -lt $action; $index++) { $offset += $actionFrameCounts[$index] * 4 }
    return $offset + $facing * $actionFrameCounts[$action] + $frame
}

function Get-ActionPose([int]$action, [int]$frame) {
    $walk = @(-2, -1, 0, 1, 2, 1, 0, -1, -2, -1)
    $breath = @(0, 0, -1, 0, 1, 0, -1, 0, 0, 1)
    [PSCustomObject]@{
        Bob = if ($action -eq 0) { @(0, 0, -1, 0, 0, 1, 0, -1)[$frame] } else { 0 }
        Stride = if ($action -eq 1) { $walk[$frame] } else { 0 }
        Breath = if ($action -eq 4) { $breath[$frame] } else { 0 }
        Nibble = if ($action -eq 2) { $frame % 5 } else { 0 }
        Blink = $action -eq 0 -and $frame -in @(5, 6)
    }
}

function Draw-Feet([hashtable]$art, [string]$species, [int]$centerX, [int]$baseline, [int]$stride, [int]$facing) {
    $lead = [Math]::Sign($stride)
    $leftY = $baseline - [Math]::Abs([Math]::Min(0, $stride))
    $rightY = $baseline - [Math]::Abs([Math]::Max(0, $stride))
    switch ($species) {
        "bird" {
            Fill-Rect $art ($centerX - 4 + $lead) $leftY 3 1 6
            Fill-Rect $art ($centerX + 1 + $lead) $rightY 3 1 6
        }
        "penguin" {
            Fill-Rect $art ($centerX - 4 + $lead) $leftY 3 1 6
            Fill-Rect $art ($centerX + 1 + $lead) $rightY 3 1 6
        }
        default {
            Fill-Rect $art ($centerX - 5 + $lead) ($leftY - 1) 3 2 1
            Fill-Rect $art ($centerX - 4 + $lead) $leftY 2 1 4
            Fill-Rect $art ($centerX + 2 + $lead) ($rightY - 1) 3 2 1
            Fill-Rect $art ($centerX + 2 + $lead) $rightY 2 1 4
        }
    }
}

function Draw-IdleMotionCue([hashtable]$art, [string]$species, [int]$facing, [int]$frame) {
    # These are body-part poses, not floating effect particles. Keeping them
    # in the source frame gives each species a recognizable idle rhythm.
    $swing = @(0, 1, 2, 1, 0, -1, -2, -1)[$frame % 8]
    switch ($species) {
        "cat" {
            $x = if ($facing -eq 2) { 25 } elseif ($facing -eq 3) { 6 } else { 24 }
            Draw-Line $art $x 23 ($x + [Math]::Sign($swing)) (20 - [Math]::Abs($swing)) 1
            Set-Pixel $art ($x + [Math]::Sign($swing)) (20 - [Math]::Abs($swing)) 2
        }
        "dog" {
            $x = if ($facing -eq 2) { 26 } elseif ($facing -eq 3) { 5 } else { 24 }
            Draw-Line $art $x 24 ($x + [Math]::Sign($swing) * 2) (24 + $swing) 1
            Set-Pixel $art ($x + [Math]::Sign($swing) * 2) (24 + $swing) 2
        }
        "bird" {
            $leftWing = 9 - [Math]::Max(0, $swing)
            $rightWing = 23 + [Math]::Max(0, -$swing)
            Draw-Line $art 10 22 $leftWing (20 + [Math]::Abs($swing)) 3
            Draw-Line $art 22 22 $rightWing (20 + [Math]::Abs($swing)) 3
            Draw-Line $art 10 21 6 (17 + ($frame % 8)) 1
            Set-Pixel $art 6 (17 + ($frame % 8)) 3
        }
        "rabbit" {
            $earY = 4 - [Math]::Abs($swing)
            Draw-Line $art 11 11 (11 + [Math]::Sign($swing)) $earY 1
            Draw-Line $art 21 11 (21 - [Math]::Sign($swing)) $earY 1
        }
        "penguin" {
            $leftWingY = 22 + $swing
            $rightWingY = 22 - $swing
            Draw-Line $art 10 20 7 $leftWingY 3
            Draw-Line $art 22 20 25 $rightWingY 3
            Set-Pixel $art 7 $leftWingY 1
            Set-Pixel $art 25 $rightWingY 1
        }
    }
}

function Draw-FrontAdult([hashtable]$art, [string]$species, [int]$action, [int]$frame) {
    $pose = Get-ActionPose $action $frame
    $cx = 16
    $top = 10 + $pose.Bob
    $baseline = 29
    if ($action -eq 4) { Draw-SleepingAdult $art $species 0 $frame; return }
    switch ($species) {
        "cat" {
            Draw-Triangle $art 11 ($top + 2) 5 1; Draw-Triangle $art 21 ($top + 2) 5 1
            Draw-Triangle $art 11 ($top + 1) 3 2; Draw-Triangle $art 21 ($top + 1) 3 2
            Draw-Oval $art $cx ($top + 7) 7 6 2
            Fill-Oval $art $cx ($top + 9) 4 3 4
            Fill-Oval $art $cx 22 6 7 2
            Fill-Oval $art $cx 22 5 6 2
            Fill-Rect $art 11 23 2 4 3; Fill-Rect $art 19 23 2 4 3
            Draw-Line $art 22 24 (25 + ($frame % 3)) (21 - ($frame % 2)) 1
            Draw-Line $art 23 23 (25 + ($frame % 3)) (21 - ($frame % 2)) 2
            if (-not $pose.Blink) { Set-Pixel $art 13 ($top + 7) 9; Set-Pixel $art 19 ($top + 7) 9 }
            Set-Pixel $art 16 ($top + 10) 6; Set-Pixel $art 15 ($top + 11) 3; Set-Pixel $art 17 ($top + 11) 3
        }
        "dog" {
            Fill-Rect $art 8 ($top + 5) 3 7 1; Fill-Rect $art 9 ($top + 6) 2 5 3
            Fill-Rect $art 21 ($top + 5) 3 7 1; Fill-Rect $art 21 ($top + 6) 2 5 3
            Draw-Oval $art $cx ($top + 8) 7 6 2
            Fill-Oval $art $cx ($top + 10) 4 3 4
            Fill-Oval $art $cx 22 6 7 2
            Fill-Rect $art 11 23 2 4 3; Fill-Rect $art 19 23 2 4 3
            Draw-Line $art 22 24 (26 - ($frame % 3)) (24 + ($frame % 2)) 1
            Draw-Line $art 23 24 (26 - ($frame % 3)) (24 + ($frame % 2)) 2
            if (-not $pose.Blink) { Set-Pixel $art 13 ($top + 8) 9; Set-Pixel $art 19 ($top + 8) 9 }
            Set-Pixel $art 16 ($top + 9) 10; Fill-Rect $art 15 ($top + 11) 3 1 6
        }
        "bird" {
            Draw-Oval $art $cx ($top + 7) 6 6 2
            Draw-Oval $art $cx 21 7 8 2
            Fill-Oval $art $cx 23 5 5 4
            Fill-Oval $art 10 22 3 4 3; Fill-Oval $art 22 22 3 4 3
            Fill-Rect $art 15 ($top + 9) 3 2 6
            if (-not $pose.Blink) { Set-Pixel $art 13 ($top + 7) 9; Set-Pixel $art 19 ($top + 7) 9 }
            Set-Pixel $art 16 ($top + 6) 5
        }
        "rabbit" {
            Draw-Oval $art 11 ($top + 3) 3 7 1; Fill-Oval $art 11 ($top + 3) 2 6 4
            Draw-Oval $art 21 ($top + 3) 3 7 1; Fill-Oval $art 21 ($top + 3) 2 6 4
            Fill-Rect $art 11 ($top - 2) 1 8 3; Fill-Rect $art 21 ($top - 2) 1 8 3
            Draw-Oval $art $cx ($top + 9) 7 6 2
            Fill-Oval $art $cx 22 6 7 2
            Fill-Oval $art $cx 24 4 4 4
            if (-not $pose.Blink) { Set-Pixel $art 13 ($top + 8) 9; Set-Pixel $art 19 ($top + 8) 9 }
            Set-Pixel $art 16 ($top + 11) 6
        }
        "penguin" {
            Draw-Oval $art $cx ($top + 9) 7 8 1
            Fill-Oval $art $cx ($top + 9) 6 7 2
            Fill-Oval $art $cx ($top + 12) 4 5 4
            Fill-Oval $art 9 22 2 4 3; Fill-Oval $art 23 22 2 4 3
            Fill-Rect $art 15 ($top + 8) 3 2 6
            if (-not $pose.Blink) { Set-Pixel $art 13 ($top + 7) 9; Set-Pixel $art 19 ($top + 7) 9 }
            Set-Pixel $art 16 ($top + 5) 5
        }
    }
    Draw-Feet $art $species $cx $baseline $pose.Stride 0
    if ($action -eq 0) { Draw-IdleMotionCue $art $species 0 $frame }
    if ($action -eq 1) {
        Set-Pixel $art (7 + ($frame % 6)) (28 - ($frame % 2)) 8
        Set-Pixel $art (5 + $frame) (29 - ($frame % 3)) 8
    }
    if ($action -eq 2) { Draw-EatingDetails $art $species 0 $frame }
}

function Draw-BackAdult([hashtable]$art, [string]$species, [int]$action, [int]$frame) {
    if ($action -eq 4) { Draw-SleepingAdult $art $species 1 $frame; return }
    $pose = Get-ActionPose $action $frame
    $cx = 16; $top = 10 + $pose.Bob
    switch ($species) {
        "cat" {
            Draw-Triangle $art 11 ($top + 2) 5 1; Draw-Triangle $art 21 ($top + 2) 5 1
            Draw-Triangle $art 11 ($top + 1) 3 2; Draw-Triangle $art 21 ($top + 1) 3 2
            Draw-Oval $art $cx ($top + 7) 7 6 3; Draw-Oval $art $cx 22 6 7 2
            Draw-Line $art 21 24 (25 + ($frame % 3)) 20 1; Draw-Line $art 22 24 (25 + ($frame % 3)) 20 2
        }
        "dog" {
            Fill-Rect $art 8 ($top + 5) 3 7 1; Fill-Rect $art 21 ($top + 5) 3 7 1
            Draw-Oval $art $cx ($top + 8) 7 6 3; Draw-Oval $art $cx 22 6 7 2
            Draw-Line $art 22 24 (26 - ($frame % 3)) (25 + ($frame % 2)) 1
        }
        "bird" { Draw-Oval $art $cx ($top + 8) 6 7 2; Draw-Oval $art $cx 22 7 8 2; Fill-Oval $art 10 22 3 4 3; Fill-Oval $art 22 22 3 4 3 }
        "rabbit" {
            Draw-Oval $art 11 ($top + 3) 3 7 1; Fill-Oval $art 11 ($top + 3) 2 6 3
            Draw-Oval $art 21 ($top + 3) 3 7 1; Fill-Oval $art 21 ($top + 3) 2 6 3
            Draw-Oval $art $cx ($top + 10) 7 6 3; Draw-Oval $art $cx 22 6 7 2; Set-Pixel $art 24 24 4
        }
        "penguin" { Draw-Oval $art $cx ($top + 9) 7 8 1; Fill-Oval $art $cx ($top + 9) 6 7 2; Fill-Oval $art 9 22 2 4 3; Fill-Oval $art 23 22 2 4 3 }
    }
    Draw-Feet $art $species $cx 29 $pose.Stride 1
    if ($action -eq 0) { Draw-IdleMotionCue $art $species 1 $frame }
    if ($action -eq 1) {
        Set-Pixel $art (24 - ($frame % 6)) (28 - ($frame % 2)) 8
        Set-Pixel $art (26 - $frame) (29 - ($frame % 3)) 8
    }
    if ($action -eq 2) { Draw-EatingDetails $art $species 1 $frame }
}

function Draw-LeftAdult([hashtable]$art, [string]$species, [int]$action, [int]$frame) {
    if ($action -eq 4) { Draw-SleepingAdult $art $species 2 $frame; return }
    $pose = Get-ActionPose $action $frame
    $top = 10 + $pose.Bob
    switch ($species) {
        "cat" {
            Draw-Triangle $art 12 ($top + 2) 5 1; Draw-Triangle $art 18 ($top + 2) 4 1
            Draw-Oval $art 14 ($top + 8) 6 6 2; Draw-Oval $art 19 22 7 6 2
            Fill-Oval $art 12 ($top + 10) 3 2 4; Set-Pixel $art 10 ($top + 8) 9
            Draw-Line $art 24 23 (28 - ($frame % 3)) (19 + ($frame % 2)) 1; Draw-Line $art 23 23 (27 - ($frame % 3)) 20 2
        }
        "dog" {
            Fill-Rect $art 15 ($top + 5) 4 7 1; Fill-Rect $art 15 ($top + 6) 3 5 3
            Draw-Oval $art 13 ($top + 8) 7 6 2; Draw-Oval $art 20 22 7 6 2
            Fill-Rect $art 6 ($top + 10) 5 3 4; Set-Pixel $art 9 ($top + 8) 9
            Draw-Line $art 25 24 (28 - ($frame % 3)) (25 + ($frame % 2)) 1
        }
        "bird" {
            Draw-Oval $art 13 ($top + 8) 6 6 2; Draw-Oval $art 19 21 7 8 2
            Fill-Oval $art 19 23 4 5 4; Fill-Oval $art 23 22 4 4 3
            Fill-Rect $art 5 ($top + 9) 4 2 6; Set-Pixel $art 10 ($top + 7) 9
        }
        "rabbit" {
            Draw-Oval $art 14 ($top + 2) 3 8 1; Fill-Oval $art 14 ($top + 2) 2 7 4
            Draw-Oval $art 11 ($top + 9) 7 6 2; Draw-Oval $art 19 22 7 6 2
            Set-Pixel $art 8 ($top + 8) 9; Draw-Oval $art 25 23 3 3 4
        }
        "penguin" {
            Draw-Oval $art 14 ($top + 9) 6 7 1; Fill-Oval $art 14 ($top + 9) 5 6 2
            Draw-Oval $art 20 22 7 7 1; Fill-Oval $art 20 22 6 6 2
            Fill-Oval $art 18 23 4 5 4; Fill-Rect $art 6 ($top + 10) 4 2 6; Set-Pixel $art 10 ($top + 7) 9
        }
    }
    Draw-Feet $art $species 17 29 $pose.Stride 2
    if ($action -eq 0) { Draw-IdleMotionCue $art $species 2 $frame }
    if ($action -eq 1) {
        Set-Pixel $art (26 - ($frame % 5)) (28 - ($frame % 2)) 8
        Set-Pixel $art (27 - $frame) (29 - ($frame % 3)) 8
    }
    if ($action -eq 2) { Draw-EatingDetails $art $species 2 $frame }
}

function Draw-EatingDetails([hashtable]$art, [string]$species, [int]$facing, [int]$frame) {
    # Ten authored positions: food starts near the hand, reaches the mouth,
    # then leaves a different crumb path while the pet settles.
    $nibble = $frame
    $foodX = if ($facing -eq 2) { 7 + $nibble } elseif ($facing -eq 3) { 24 - $nibble } else { 17 + ($nibble % 4) }
    $foodY = 24 - [Math]::Floor($nibble / 2)
    Fill-Rect $art $foodX $foodY 2 2 6
    Set-Pixel $art ($foodX + 1) ($foodY - 1) 5
    $handX = if ($facing -eq 2) { 11 + [Math]::Floor($nibble / 2) } elseif ($facing -eq 3) { 21 - [Math]::Floor($nibble / 2) } else { 18 + ($nibble % 3) }
    $crumbDelta = if ($facing -eq 2) { 2 } else { -1 }
    Draw-Line $art $handX 24 $foodX ($foodY + 1) 4
    if ($species -eq "bird") { Set-Pixel $art ($foodX - 1) $foodY 6 }
    Set-Pixel $art ($foodX + $crumbDelta) ($foodY - 1 - ($nibble % 2)) 5
    $outerCrumbX = if ($facing -eq 2) { 4 } elseif ($facing -eq 3) { 27 } else { 27 }
    Set-Pixel $art $outerCrumbX (14 + $nibble) 6
}

function Draw-SleepingAdult([hashtable]$art, [string]$species, [int]$facing, [int]$frame) {
    $breath = (Get-ActionPose 4 $frame).Breath
    $cx = if ($facing -eq 2) { 14 } elseif ($facing -eq 3) { 18 } else { 16 }
    $bodyY = 24 + $breath
    switch ($species) {
        "cat" {
            Draw-Oval $art $cx $bodyY 9 5 2; Fill-Oval $art ($cx - 3) ($bodyY - 1) 4 3 4
            Draw-Line $art ($cx + 4) $bodyY ($cx + 8) ($bodyY - 4) 1; Draw-Line $art ($cx + 5) $bodyY ($cx + 8) ($bodyY - 4) 2
            Set-Pixel $art ($cx - 4) ($bodyY - 1) 9
        }
        "dog" {
            Draw-Oval $art $cx $bodyY 9 5 2; Draw-Oval $art ($cx - 4) ($bodyY - 1) 4 4 2
            Fill-Rect $art ($cx - 6) ($bodyY - 2) 2 4 3; Set-Pixel $art ($cx - 5) ($bodyY - 1) 9
            Draw-Line $art ($cx + 4) $bodyY ($cx + 8) ($bodyY + 1) 1
        }
        "bird" {
            Draw-Oval $art $cx $bodyY 8 5 2; Fill-Oval $art $cx ($bodyY + 1) 5 3 4
            Fill-Oval $art ($cx + 4) ($bodyY - 1) 4 3 3; Set-Pixel $art ($cx - 4) ($bodyY - 1) 9
        }
        "rabbit" {
            Draw-Oval $art $cx $bodyY 9 5 2; Draw-Oval $art ($cx - 4) ($bodyY - 1) 4 4 2
            Draw-Line $art ($cx - 5) ($bodyY - 3) ($cx - 4) ($bodyY - 8) 1
            Draw-Line $art ($cx - 2) ($bodyY - 3) ($cx - 1) ($bodyY - 8) 1
            Set-Pixel $art ($cx - 5) ($bodyY - 1) 9
        }
        "penguin" {
            Draw-Oval $art $cx $bodyY 8 5 1; Fill-Oval $art $cx $bodyY 7 4 2
            Fill-Oval $art ($cx - 1) ($bodyY + 1) 4 3 4; Fill-Rect $art ($cx - 6) ($bodyY + 4) 3 1 6
            Set-Pixel $art ($cx - 4) ($bodyY - 1) 9
        }
    }
    $dreamX = if ($facing -eq 2) { 19 } elseif ($facing -eq 3) { 8 } else { 23 }
    $dreamY = 14 - [Math]::Floor($frame / 3)
    Set-Pixel $art ($dreamX + ($frame % 3)) $dreamY 5
    Set-Pixel $art ($dreamX + 1 + ($frame % 2)) ($dreamY - 1) 5
    if ($frame % 2 -eq 0) { Set-Pixel $art ($dreamX + 2) ($dreamY - 2) 5 }
    Set-Pixel $art ($dreamX - 1 + $frame) (18 - $frame) 5
}

function Draw-AdultFrame([string]$species, [int]$action, [int]$facing, [int]$frame) {
    $art = New-Art
    if ($facing -eq 3) {
        $art = Draw-AdultFrame $species $action 2 $frame
        return Mirror-Art $art
    }
    switch ($facing) {
        0 { Draw-FrontAdult $art $species $action $frame }
        1 { Draw-BackAdult $art $species $action $frame }
        2 { Draw-LeftAdult $art $species $action $frame }
    }
    return $art
}

function Get-Bounds([hashtable]$art) {
    $points = @($art.Keys | ForEach-Object {
        $parts = $_.Split(',')
        [PSCustomObject]@{ X = [int]$parts[0]; Y = [int]$parts[1] }
    })
    [PSCustomObject]@{
        MinX = ($points | Measure-Object X -Minimum).Minimum
        MaxX = ($points | Measure-Object X -Maximum).Maximum
        MinY = ($points | Measure-Object Y -Minimum).Minimum
        MaxY = ($points | Measure-Object Y -Maximum).Maximum
        CenterX = [Math]::Round((($points | Measure-Object X -Minimum).Minimum + ($points | Measure-Object X -Maximum).Maximum) / 2.0)
    }
}

function Get-AdultAnchors([hashtable]$art, [string]$species, [int]$action, [int]$facing, [int]$frame) {
    $bounds = Get-Bounds $art
    $left = $facing -eq 2
    $right = $facing -eq 3
    $headX = if ($left) { $bounds.MinX + 3 } elseif ($right) { $bounds.MaxX - 3 } else { $bounds.CenterX }
    $handX = if ($left) { $bounds.MinX + 4 } elseif ($right) { $bounds.MaxX - 4 } else { $bounds.CenterX + 3 }
    $backX = if ($left) { $bounds.MaxX - 2 } elseif ($right) { $bounds.MinX + 2 } else { $bounds.MaxX - 2 }
    $tailX = if ($species -eq "penguin") { $bounds.CenterX } elseif ($left) { $bounds.MaxX - 1 } elseif ($right) { $bounds.MinX + 1 } else { $bounds.MaxX - 1 }
    $handLift = if ($action -eq 2) { -2 - ($frame % 2) } elseif ($action -eq 1) { if ($frame % 2 -eq 0) { -1 } else { 1 } } else { 0 }
    $headLayer = if ($facing -eq 1) { 0 } else { 2 }
    $handLayer = if ($facing -eq 1 -or $action -eq 4) { 0 } else { 2 }
    $neckLayer = if ($facing -eq 1) { 0 } else { 1 }
    $trailDelta = if ($frame % 2 -eq 0) { -2 } else { 2 }
    [ordered]@{
        head = @((Clamp-Grid $headX), (Clamp-Grid ($bounds.MinY + 2)), $headLayer)
        back = @((Clamp-Grid $backX), (Clamp-Grid ($bounds.MinY + [Math]::Floor(($bounds.MaxY - $bounds.MinY) * 0.55))), 0)
        hand = @((Clamp-Grid $handX), (Clamp-Grid ($bounds.MaxY - 5 + $handLift)), $handLayer)
        neck = @((Clamp-Grid $headX), (Clamp-Grid ($bounds.MinY + 6)), $neckLayer)
        tail = @((Clamp-Grid $tailX), (Clamp-Grid ($bounds.MaxY - 5 + ($frame % 2))), 0)
        trail = @((Clamp-Grid ($bounds.CenterX + $trailDelta)), (Clamp-Grid ($bounds.MaxY - 1)), 0)
    }
}

function Load-BitmapBytes([string]$path) {
    $bitmap = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $path).Path)
    try {
        if ($bitmap.Width -ne $sheetSize -or $bitmap.Height -ne $sheetSize) { throw "Unexpected editable sheet size: $path" }
        $rect = [System.Drawing.Rectangle]::new(0, 0, $sheetSize, $sheetSize)
        $data = $bitmap.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $bytes = [byte[]]::new($sheetSize * $sheetSize * 4)
            [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
            # Do not let PowerShell enumerate ByteArray through the pipeline;
            # callers must keep the same mutable image buffer across all frames.
            return ,$bytes
        } finally { $bitmap.UnlockBits($data) }
    } finally { $bitmap.Dispose() }
}

function Save-BitmapBytes([string]$path, [byte[]]$bytes) {
    $bitmap = [System.Drawing.Bitmap]::new($sheetSize, $sheetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $rect = [System.Drawing.Rectangle]::new(0, 0, $sheetSize, $sheetSize)
        $data = $bitmap.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::WriteOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try { [System.Runtime.InteropServices.Marshal]::Copy($bytes, 0, $data.Scan0, $bytes.Length) } finally { $bitmap.UnlockBits($data) }
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $bitmap.Dispose() }
}

function Write-Art([byte[]]$bytes, [int]$frameIndex, [hashtable]$art) {
    $baseX = ($frameIndex % $columns) * $grid
    $baseY = [Math]::Floor($frameIndex / $columns) * $grid
    for ($y = 0; $y -lt $grid; $y++) {
        for ($x = 0; $x -lt $grid; $x++) {
            $offset = (($baseY + $y) * $sheetSize + $baseX + $x) * 4
            $bytes[$offset] = 0; $bytes[$offset + 1] = 0; $bytes[$offset + 2] = 0; $bytes[$offset + 3] = 0
        }
    }
    foreach ($entry in $art.GetEnumerator()) {
        $parts = $entry.Key.Split(',')
        $color = $palette[[int]$entry.Value]
        $x = $baseX + [int]$parts[0]; $y = $baseY + [int]$parts[1]
        $offset = ($y * $sheetSize + $x) * 4
        $bytes[$offset] = [byte]($color -band 0xFF)
        $bytes[$offset + 1] = [byte](($color -shr 8) -band 0xFF)
        $bytes[$offset + 2] = [byte](($color -shr 16) -band 0xFF)
        $bytes[$offset + 3] = [byte](($color -shr 24) -band 0xFF)
    }
}

function Write-Preview([string]$path) {
    $tile = 96
    $preview = [System.Drawing.Bitmap]::new($tile * 4, $tile * $speciesNames.Count, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($preview)
    try {
        $graphics.Clear([System.Drawing.Color]::FromArgb(255, 20, 23, 30))
        for ($species = 0; $species -lt $speciesNames.Count; $species++) {
            $source = [System.Drawing.Bitmap]::new((Join-Path $SourceDirectory ($speciesNames[$species] + ".png")))
            try {
                for ($column = 0; $column -lt $coreActions.Count; $column++) {
                    $frameIndex = Get-FrameIndex $coreActions[$column] 0 0
                    $sourceRect = [System.Drawing.Rectangle]::new(($frameIndex % $columns) * $grid, [Math]::Floor($frameIndex / $columns) * $grid, $grid, $grid)
                    $destination = [System.Drawing.Rectangle]::new($column * $tile + 16, $species * $tile + 16, 64, 64)
                    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                    $graphics.DrawImage($source, $destination, $sourceRect, [System.Drawing.GraphicsUnit]::Pixel)
                }
            } finally { $source.Dispose() }
        }
        [System.IO.Directory]::CreateDirectory((Split-Path -Parent $path)) | Out-Null
        $preview.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $preview.Dispose()
    }
}

foreach ($speciesIndex in 0..($speciesNames.Count - 1)) {
    $species = $speciesNames[$speciesIndex]
    $imagePath = Join-Path $SourceDirectory "$species.png"
    $anchorsPath = Join-Path $SourceDirectory "$species.anchors.json"
    if (-not (Test-Path -LiteralPath $imagePath) -or -not (Test-Path -LiteralPath $anchorsPath)) {
        throw "Missing editable source for $species"
    }
    $bytes = Load-BitmapBytes $imagePath
    $anchors = @(Get-Content -Raw -LiteralPath $anchorsPath | ConvertFrom-Json)
    if ($anchors.Count -ne 1600) { throw "Invalid anchor count for $species" }
    foreach ($action in $coreActions) {
        for ($facing = 0; $facing -lt 4; $facing++) {
            for ($frame = 0; $frame -lt $actionFrameCounts[$action]; $frame++) {
                $art = Draw-AdultFrame $species $action $facing $frame
                $index = Get-FrameIndex $action $facing $frame
                Write-Art $bytes $index $art
                $anchors[$index] = [PSCustomObject]@{
                    index = $index
                    anchors = Get-AdultAnchors $art $species $action $facing $frame
                }
            }
        }
    }
    Save-BitmapBytes $imagePath $bytes
    [System.IO.File]::WriteAllText($anchorsPath, ($anchors | ConvertTo-Json -Depth 5), [System.Text.UTF8Encoding]::new($false))
    Write-Output "Redrew adult master frames for $species"
}

if (-not [string]::IsNullOrWhiteSpace($PreviewPath)) { Write-Preview $PreviewPath }
Write-Output "Adult master v1 redraw completed"
