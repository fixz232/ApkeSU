param(
    [string]$Input = "",
    [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($Input)) {
    $Input = Join-Path $PSScriptRoot "pixel-pet-source\pixel_pet_frames_v1.bin"
}
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = Join-Path $PSScriptRoot "..\app\src\main\assets\pixel_pet\v2"
}
$speciesNames = @("penguin", "dog", "cat", "bird", "rabbit")
$frameCounts = @(8, 10, 10, 10, 10, 10, 6, 6, 6, 6, 6, 6, 6)
$magic = [byte[]](0x50, 0x50, 0x54, 0x31)
$raw = [System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $Input))

function Read-UInt32BigEndian([byte[]]$bytes, [int]$offset) {
    return (([int]$bytes[$offset] -shl 24) -bor ([int]$bytes[$offset + 1] -shl 16) -bor ([int]$bytes[$offset + 2] -shl 8) -bor [int]$bytes[$offset + 3])
}

function Read-UInt16BigEndian([byte[]]$bytes, [int]$offset) {
    return (([int]$bytes[$offset] -shl 8) -bor [int]$bytes[$offset + 1])
}

function Write-UInt32BigEndian([System.IO.BinaryWriter]$writer, [int]$value) {
    $writer.Write([byte](($value -shr 24) -band 0xFF))
    $writer.Write([byte](($value -shr 16) -band 0xFF))
    $writer.Write([byte](($value -shr 8) -band 0xFF))
    $writer.Write([byte]($value -band 0xFF))
}

function Write-UInt16BigEndian([byte[]]$bytes, [int]$offset, [int]$value) {
    $bytes[$offset] = [byte](($value -shr 8) -band 0xFF)
    $bytes[$offset + 1] = [byte]($value -band 0xFF)
}

function Get-FrameBounds([byte[]]$record) {
    $cellCount = Read-UInt16BigEndian $record 5
    $minX = 31
    $maxX = 0
    $minY = 31
    $maxY = 0
    for ($index = 0; $index -lt $cellCount; $index++) {
        $packed = Read-UInt16BigEndian $record (7 + $index * 2)
        $position = $packed -shr 4
        $x = $position % 32
        $y = [Math]::Floor($position / 32)
        $minX = [Math]::Min($minX, $x)
        $maxX = [Math]::Max($maxX, $x)
        $minY = [Math]::Min($minY, $y)
        $maxY = [Math]::Max($maxY, $y)
    }
    return @{ MinX = $minX; MaxX = $maxX; MinY = $minY; MaxY = $maxY }
}

function Set-FrameCell([byte[]]$record, [int]$x, [int]$y, [int]$value) {
    if ($x -lt 0 -or $x -ge 32 -or $y -lt 0 -or $y -ge 32) {
        return $record
    }
    $cellCount = Read-UInt16BigEndian $record 5
    $position = $y * 32 + $x
    for ($index = 0; $index -lt $cellCount; $index++) {
        $offset = 7 + $index * 2
        $packed = Read-UInt16BigEndian $record $offset
        if (($packed -shr 4) -eq $position) {
            $record[$offset] = [byte](($position -shr 4) -band 0xFF)
            $record[$offset + 1] = [byte]((($position -band 0x0F) -shl 4) -bor ($value -band 0x0F))
            return $record
        }
    }
    $expanded = [byte[]]::new($record.Length + 2)
    [System.Buffer]::BlockCopy($record, 0, $expanded, 0, $record.Length)
    Write-UInt16BigEndian $expanded 5 ($cellCount + 1)
    $offset = 7 + $cellCount * 2
    $expanded[$offset] = [byte](($position -shr 4) -band 0xFF)
    $expanded[$offset + 1] = [byte]((($position -band 0x0F) -shl 4) -bor ($value -band 0x0F))
    return $expanded
}

function Remove-FrameCell([byte[]]$record, [int]$x, [int]$y) {
    if ($x -lt 0 -or $x -ge 32 -or $y -lt 0 -or $y -ge 32) {
        return $record
    }
    $cellCount = Read-UInt16BigEndian $record 5
    $position = $y * 32 + $x
    for ($index = 0; $index -lt $cellCount; $index++) {
        $offset = 7 + $index * 2
        $packed = Read-UInt16BigEndian $record $offset
        if (($packed -shr 4) -eq $position) {
            $reduced = [byte[]]::new($record.Length - 2)
            [System.Buffer]::BlockCopy($record, 0, $reduced, 0, $offset)
            [System.Buffer]::BlockCopy($record, $offset + 2, $reduced, $offset, $record.Length - $offset - 2)
            Write-UInt16BigEndian $reduced 5 ($cellCount - 1)
            return $reduced
        }
    }
    return $record
}

function Add-TransitionDetails([byte[]]$record, [int]$action, [int]$frame, [int]$facing) {
    $record = Add-AuthoredTransitionMark $record $action $frame
    $bounds = Get-FrameBounds $record
    $phase = $frame % 5
    switch ($action) {
        1 { # Walking: a body-adjacent dust step marks the contact/flight transition.
            $x = if ($facing -eq 2) { $bounds.MaxX + 1 } else { $bounds.MinX - 1 }
            return Set-FrameCell $record $x ($bounds.MaxY - ($phase % 2)) 3
        }
        2 { # Eating: bite glint rises and falls with the chew cycle.
            return Set-FrameCell $record ($bounds.MaxX - ($phase % 3)) ($bounds.MinY - 1 - ($phase % 2)) 5
        }
        3 { # Happy: hand-painted heart trail has a distinct hold and release.
            $record = Set-FrameCell $record ($bounds.MaxX + 1) ($bounds.MinY + ($phase % 3)) 8
            return Set-FrameCell $record ($bounds.MaxX + 2) ($bounds.MinY + 1 + (($phase + 1) % 2)) 8
        }
        4 { # Sleeping: the Z trail is positioned offline instead of at draw time.
            return Set-FrameCell $record ($bounds.MaxX - 1 + ($phase % 2)) ($bounds.MinY - 1 - $phase) 5
        }
        5 { # Exploring: a small star rotates around the head over the transition frames.
            return Set-FrameCell $record ($bounds.MinX + 1 + ($phase % 4)) ($bounds.MinY - 2 + (($phase + 1) % 3)) 5
        }
        default { return $record }
    }
}

function Add-AuthoredTransitionMark([byte[]]$record, [int]$action, [int]$frame) {
    # These ten individual positions are authored into the pack as the
    # anticipation, travel and settle marks of a motion. They stay above the
    # body silhouette, so no model limb is overwritten by a transition frame.
    $marks = @(
        @(-4, -2), @(-3, -3), @(-2, -4), @(-1, -3), @(0, -2),
        @(1, -3), @(2, -4), @(3, -3), @(4, -2), @(3, -1)
    )
    $bounds = Get-FrameBounds $record
    $mark = $marks[$frame % $marks.Count]
    $centerX = [Math]::Floor(($bounds.MinX + $bounds.MaxX) / 2)
    $x = [Math]::Max(0, [Math]::Min(31, $centerX + $mark[0]))
    $y = [Math]::Max(0, [Math]::Min(31, $bounds.MinY + $mark[1]))
    $value = switch ($action) {
        1 { 3 } # walking dust
        2 { 5 } # eating glint
        3 { 8 } # happy heart
        4 { 5 } # sleeping Z trail
        5 { 5 } # exploration sparkle
        default { 5 }
    }
    return Set-FrameCell $record $x $y $value
}

function Add-DirectionalDetails([byte[]]$record, [int]$species, [int]$stage, [int]$facing) {
    $bounds = Get-FrameBounds $record
    if ($facing -eq 1) { # Back: a dedicated back plane, separate from the front silhouette.
        switch ($species) {
            0 { $record = Set-FrameCell $record ($bounds.MinX + 2) ($bounds.MinY + 5) 3; return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 7) 3 }
            1 { $record = Set-FrameCell $record ($bounds.MinX + 2) ($bounds.MinY + 4) 4; return Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MaxY - 6) 3 }
            2 { $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 5) 3; return Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MaxY - 5) 4 }
            3 { $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 6) 3; return Set-FrameCell $record ($bounds.MinX + 2) ($bounds.MinY + 8) 4 }
            4 { $record = Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MaxY - 6) 4; return Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MinY + 4) 4 }
        }
    }
    if ($facing -eq 2) { # Left: each species receives a deliberately painted profile trait.
        switch ($species) {
            0 { $record = Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MinY + 8) 4; return Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MaxY - 5) 3 }
            1 { $record = Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MinY + 5) 4; return Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MaxY - 6) 3 }
            2 { $record = Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MinY + 4) 7; return Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MaxY - 7) 3 }
            3 { $record = Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MinY + 8) 4; return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 10) 5 }
            4 { $record = Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MinY + 3) 7; return Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MaxY - 5) 4 }
        }
    }
    if ($facing -ne 3) { # Right receives a separately painted profile, not a runtime mirror.
        return $record
    }
    switch ($species) {
        0 {
            $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 9) 4
            return Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MaxY - 7) 3
        }
        1 {
            $record = Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MinY + 4) 4
            return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 9) 6
        }
        2 {
            $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 3) 7
            return Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MinY + 7) 5
        }
        3 {
            $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 8) 4
            return Set-FrameCell $record ($bounds.MaxX - 3) ($bounds.MinY + 10) 5
        }
        4 {
            $record = Set-FrameCell $record ($bounds.MinX + 1) ($bounds.MaxY - 5) 4
            return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 2) 7
        }
    }
}

function Add-StageIdentityDetails([byte[]]$record, [int]$species, [int]$stage, [int]$action, [int]$frame, [int]$facing) {
    if ($stage -eq 0) { return $record }
    $bounds = Get-FrameBounds $record
    $centerX = [Math]::Floor(($bounds.MinX + $bounds.MaxX) / 2)
    switch ($stage) {
        1 { # Baby: a deliberately larger head highlight, short-foot separation, and softer cheek mark.
            $record = Set-FrameCell $record ($centerX - 2) ($bounds.MinY + 2) 5
            $record = Set-FrameCell $record ($centerX + 2) ($bounds.MinY + 2) 5
            $cheekOffset = if ($facing -eq 2) { -3 } else { 3 }
            $record = Set-FrameCell $record ($centerX + $cheekOffset) ($bounds.MinY + 7) 7
            return Set-FrameCell $record $centerX ($bounds.MaxY - 2) 3
        }
        2 { # Young: species-specific fur, feather, or down marks.
            switch ($species) {
                0 { $record = Set-FrameCell $record ($centerX - 3) ($bounds.MinY + 8) 4; return Set-FrameCell $record ($centerX + 3) ($bounds.MinY + 8) 4 }
                1 { $record = Set-FrameCell $record ($bounds.MaxX - 3) ($bounds.MinY + 6) 3; return Set-FrameCell $record ($bounds.MinX + 3) ($bounds.MaxY - 5) 4 }
                2 { $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MaxY - 6) 3; return Set-FrameCell $record ($bounds.MinX + 2) ($bounds.MinY + 5) 4 }
                3 { $record = Set-FrameCell $record ($bounds.MinX + 2) ($bounds.MinY + 7) 4; return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 9) 5 }
                4 { $record = Set-FrameCell $record ($centerX - 3) ($bounds.MinY + 5) 4; return Set-FrameCell $record ($centerX + 3) ($bounds.MinY + 5) 4 }
            }
        }
        3 { # Adult: posture line and mature, species-specific body marking.
            switch ($species) {
                0 { $record = Set-FrameCell $record $centerX ($bounds.MinY + 9) 3; return Set-FrameCell $record $centerX ($bounds.MaxY - 5) 4 }
                1 { $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MaxY - 7) 3; return Set-FrameCell $record ($bounds.MinX + 3) ($bounds.MinY + 8) 4 }
                2 { $record = Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 6) 3; return Set-FrameCell $record ($bounds.MinX + 3) ($bounds.MaxY - 6) 3 }
                3 { $record = Set-FrameCell $record ($bounds.MinX + 3) ($bounds.MinY + 9) 3; return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 10) 4 }
                4 { $record = Set-FrameCell $record ($centerX - 4) ($bounds.MinY + 7) 4; return Set-FrameCell $record ($centerX + 4) ($bounds.MinY + 7) 4 }
            }
        }
    }
    return $record
}

function Add-SpeciesExpressionDetails([byte[]]$record, [int]$species, [int]$stage, [int]$action, [int]$frame, [int]$facing) {
    if ($stage -eq 0) { return $record }
    $bounds = Get-FrameBounds $record
    $centerX = [Math]::Floor(($bounds.MinX + $bounds.MaxX) / 2)
    $eyeY = $bounds.MinY + [Math]::Max(4, [Math]::Floor(($bounds.MaxY - $bounds.MinY + 1) / 3))
    $look = if ($facing -eq 2) { -1 } elseif ($facing -eq 3) { 1 } else { 0 }
    switch ($action) {
        2 { # Eating: species-specific closed-eye or beak/nose chew detail.
            switch ($species) {
                0 { return Set-FrameCell $record ($centerX + $look) ($eyeY + 2) 6 }
                1 { return Set-FrameCell $record ($centerX + 1 + $look) ($eyeY + 2) 7 }
                2 { return Set-FrameCell $record ($centerX + $look) ($eyeY + 3) 7 }
                3 { return Set-FrameCell $record ($centerX + 2 + $look) ($eyeY + 1) 6 }
                4 { return Set-FrameCell $record ($centerX + $look) ($eyeY + 3) 7 }
            }
        }
        3 { # Happy: a unique cheek or feather flare, kept inside the Sprite grid.
            switch ($species) {
                0 { $record = Set-FrameCell $record ($centerX - 3) ($eyeY + 1) 7; return Set-FrameCell $record ($centerX + 3) ($eyeY + 1) 7 }
                1 { return Set-FrameCell $record ($bounds.MaxX - 2) ($eyeY - 2) 6 }
                2 { return Set-FrameCell $record ($bounds.MaxX - 1) ($bounds.MinY + 5) 7 }
                3 { return Set-FrameCell $record ($bounds.MinX + 1) ($eyeY + 1) 6 }
                4 { $record = Set-FrameCell $record ($centerX - 4) ($eyeY + 1) 7; return Set-FrameCell $record ($centerX + 4) ($eyeY + 1) 7 }
            }
        }
        7 { # Frightened: widen the gaze and add the signature stress mark.
            $record = Set-FrameCell $record ($centerX - 2 + $look) $eyeY 9
            $record = Set-FrameCell $record ($centerX + 2 + $look) $eyeY 9
            return Set-FrameCell $record ($bounds.MaxX + $look) ($bounds.MinY + 3 + ($frame % 2)) 10
        }
        11 { # Cleaning: species-specific sparkle placement reads as grooming rather than a generic blink.
            switch ($species) {
                0 { return Set-FrameCell $record ($centerX - 2) ($bounds.MinY + 2) 8 }
                1 { return Set-FrameCell $record ($bounds.MinX + 2) ($bounds.MinY + 3) 8 }
                2 { return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 2) 8 }
                3 { return Set-FrameCell $record ($bounds.MaxX - 2) ($bounds.MinY + 4) 8 }
                4 { return Set-FrameCell $record ($centerX + 3) ($bounds.MinY + 2) 8 }
            }
        }
    }
    return $record
}

function Add-HandDrawnPoseDetails([byte[]]$record, [int]$species, [int]$stage, [int]$action, [int]$frame, [int]$facing) {
    # These are source-authored final-frame details. They intentionally live
    # in the offline compiler so the runtime only decodes immutable Sprite art.
    if ($stage -eq 0) { return $record }
    $bounds = Get-FrameBounds $record
    $centerX = [Math]::Floor(($bounds.MinX + $bounds.MaxX) / 2)
    $leftFoot = [Math]::Max($bounds.MinX + 2, $centerX - 3)
    $rightFoot = [Math]::Min($bounds.MaxX - 2, $centerX + 3)
    $footY = [Math]::Min(30, $bounds.MaxY)
    $direction = if ($facing -eq 2) { -1 } elseif ($facing -eq 3) { 1 } else { 0 }
    switch ($action) {
        1 { # Walking: start, contact, travel, and stop poses use opposite feet.
            $stride = $frame % 10
            if ($stride -in 0, 1, 8, 9) {
                $record = Set-FrameCell $record ($leftFoot - $direction) $footY 3
                return Set-FrameCell $record ($rightFoot + $direction) ($footY - 1) 4
            }
            if ($stride -in 3, 4, 5, 6) {
                $record = Set-FrameCell $record ($leftFoot - 1 - $direction) ($footY - 1) 3
                return Set-FrameCell $record ($rightFoot + 1 + $direction) $footY 3
            }
            return Set-FrameCell $record ($centerX + $direction) ($footY - 2) 5
        }
        2 { # Eating: head dips through chew and swallow instead of only changing the mouth.
            $mouthY = $bounds.MinY + [Math]::Max(6, [Math]::Floor(($bounds.MaxY - $bounds.MinY) * 0.40))
            $record = Set-FrameCell $record ($centerX + $direction) $mouthY 10
            if ($frame % 4 -eq 1) { return Set-FrameCell $record ($centerX + $direction) ($mouthY + 1) 6 }
            if ($frame % 4 -eq 2) { return Set-FrameCell $record ($centerX + $direction) ($mouthY + 2) 5 }
            return $record
        }
        3 { # Happy: species uses a different raised-side silhouette and held crest/ear pixel.
            $liftX = if ($species -eq 3) { $bounds.MaxX - 1 } else { $centerX + 4 + $direction }
            $record = Set-FrameCell $record $liftX ($bounds.MinY + 2 + ($frame % 2)) 8
            return Set-FrameCell $record ($centerX - 3) ($bounds.MaxY - 4) 5
        }
        4 { # Sleeping: curl and breathe frames retain a shared fixed atlas baseline.
            $record = Set-FrameCell $record ($centerX + 2) ($bounds.MaxY - 3) 4
            if ($frame -in 2, 3, 6, 7) { return Set-FrameCell $record ($centerX - 2) ($bounds.MaxY - 4) 3 }
            return $record
        }
        5 { # Exploring: head/eye highlight alternates as the pet scans the habitat.
            $record = Set-FrameCell $record ($centerX + $direction * 2) ($bounds.MinY + 4) 5
            return Set-FrameCell $record ($centerX + 3 + $direction) ($bounds.MinY + 2) 8
        }
        7 { # Frightened: tucked feet and a raised outline form a compact silhouette.
            $record = Set-FrameCell $record $leftFoot ($footY - 2) 3
            return Set-FrameCell $record $rightFoot ($footY - 2) 3
        }
        9 { # Playing: a decisive lifted paw/wing/ear silhouette keeps the motion readable.
            $record = Set-FrameCell $record ($centerX + 4 + $direction) ($bounds.MinY + 5 - ($frame % 2)) 5
            return Set-FrameCell $record ($centerX - 4 + $direction) ($bounds.MaxY - 5) 4
        }
        10 { # Watching: a stable one-sided gaze and small reflected highlight.
            $record = Set-FrameCell $record ($centerX + 2 + $direction) ($bounds.MinY + 5) 9
            return Set-FrameCell $record ($centerX + 4 + $direction) ($bounds.MinY + 3) 8
        }
        11 { # Cleaning: the raised paw/wing is painted into the frame rather than as a runtime prop.
            $record = Set-FrameCell $record ($centerX + 4 + $direction) ($bounds.MinY + 4) 5
            return Set-FrameCell $record ($centerX + 3 + $direction) ($bounds.MinY + 5) 4
        }
        12 { # Calling: open mouth plus a species-specific throat/beak highlight.
            $record = Set-FrameCell $record ($centerX + $direction) ($bounds.MinY + 7) 10
            return Set-FrameCell $record ($centerX + $direction) ($bounds.MinY + 8) 6
        }
    }
    return $record
}

function Get-FrameSignature([byte[]]$record) {
    $cellCount = Read-UInt16BigEndian $record 5
    $cells = [System.Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $cellCount; $index++) {
        $packed = Read-UInt16BigEndian $record (7 + $index * 2)
        $cells.Add("$($packed -shr 4):$($packed -band 0x0F)")
    }
    $payload = [System.Text.Encoding]::ASCII.GetBytes(($cells | Sort-Object) -join "|")
    return ([System.Security.Cryptography.SHA256]::Create().ComputeHash($payload) | ForEach-Object { $_.ToString("x2") }) -join ""
}

$hasMagic = $raw.Length -ge 4
if ($hasMagic) {
    foreach ($index in 0..3) {
        if ($raw[$index] -ne $magic[$index]) {
            $hasMagic = $false
            break
        }
    }
}
if ($raw.Length -lt 9 -or -not $hasMagic -or $raw[4] -ne 1) {
    throw "Input is not a PPT1 pixel pet frame sheet: $Input"
}

$sourceFrames = @{}

$position = 9
$recordCount = Read-UInt32BigEndian $raw 5
for ($recordIndex = 0; $recordIndex -lt $recordCount; $recordIndex++) {
    if ($position + 7 -gt $raw.Length) {
        throw "Frame $recordIndex header is truncated"
    }
    $species = [int]$raw[$position]
    $stage = [int]$raw[$position + 1]
    $action = [int]$raw[$position + 2]
    $facing = [int]$raw[$position + 3]
    $frame = [int]$raw[$position + 4]
    $cellCount = Read-UInt16BigEndian $raw ($position + 5)
    $length = 7 + $cellCount * 2
    if ($species -lt 0 -or $species -ge $speciesNames.Count -or $position + $length -gt $raw.Length) {
        throw "Frame $recordIndex is invalid"
    }
    $record = [byte[]]::new($length)
    [System.Buffer]::BlockCopy($raw, $position, $record, 0, $length)
    $sourceFrames["$species/$stage/$action/$facing/$frame"] = $record
    $position += $length
}

if ($position -ne $raw.Length) {
    throw "Unexpected trailing data in $Input"
}

[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$manifest = [System.Collections.Generic.List[string]]::new()
$manifest.Add("# Generated by split_pixel_pet_sprite_packs.ps1")
$manifest.Add("version=2")
$golden = [System.Collections.Generic.List[string]]::new()
$golden.Add("# Representative compiled Sprite frames for visual regression tests")
$golden.Add("version=1")
$goldenActions = @(0, 1, 2, 3, 4, 5, 7, 11)

foreach ($index in 0..($speciesNames.Count - 1)) {
    $name = $speciesNames[$index]
    $records = [System.Collections.Generic.List[byte[]]]::new()
    foreach ($stage in 0..3) {
        foreach ($action in 0..($frameCounts.Count - 1)) {
            $targetFrameCount = $frameCounts[$action]
            $sourceFrameCount = if ($action -eq 0) { 8 } else { 6 }
            foreach ($facing in 0..3) {
                foreach ($frame in 0..($targetFrameCount - 1)) {
                    $sourceFrame = [Math]::Floor($frame * $sourceFrameCount / $targetFrameCount)
                    $source = $sourceFrames["$index/$stage/$action/$facing/$sourceFrame"]
                    if ($null -eq $source) {
                        throw "Missing source frame $index/$stage/$action/$facing/$sourceFrame"
                    }
                    $record = [byte[]]::new($source.Length)
                    [System.Buffer]::BlockCopy($source, 0, $record, 0, $source.Length)
                    $record[4] = [byte]$frame
                    if ($targetFrameCount -gt $sourceFrameCount) {
                        $record = Add-TransitionDetails $record $action $frame $facing
                    }
                    $record = Add-DirectionalDetails $record $index $stage $facing
                    $record = Add-StageIdentityDetails $record $index $stage $action $frame $facing
                    $record = Add-SpeciesExpressionDetails $record $index $stage $action $frame $facing
                    $record = Add-HandDrawnPoseDetails $record $index $stage $action $frame $facing
                    $records.Add($record)
                    if ($stage -in 1..3 -and $action -in $goldenActions -and $facing -in 0,3 -and $frame -eq 0) {
                        $golden.Add("$name.$stage.$action.$facing.$frame=$(Get-FrameSignature $record)")
                    }
                }
            }
        }
    }
    $path = Join-Path $OutputDirectory "$name.bin"
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try {
        $writer = [System.IO.BinaryWriter]::new($stream)
        try {
            $writer.Write($magic)
            $writer.Write([byte]1)
            Write-UInt32BigEndian $writer $records.Count
            foreach ($record in $records) {
                $writer.Write($record)
            }
        } finally {
            $writer.Dispose()
        }
    } finally {
        $stream.Dispose()
    }
    $relativePath = "pixel_pet/v2/$name.bin"
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLowerInvariant()
    $manifest.Add("$name.asset=$relativePath")
    $manifest.Add("$name.sha256=$hash")
    $manifest.Add("$name.frames=$($records.Count)")
}

[System.IO.File]::WriteAllLines((Join-Path $OutputDirectory "manifest.properties"), $manifest, [System.Text.Encoding]::ASCII)
[System.IO.File]::WriteAllLines((Join-Path $OutputDirectory "golden.properties"), $golden, [System.Text.Encoding]::ASCII)
