param(
    [ValidateSet("Export", "Compile")]
    [string]$Mode = "Export",
    [string]$SourceDirectory = "",
    [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$speciesNames = @("penguin", "dog", "cat", "bird", "rabbit")
$frameCounts = @(8, 10, 10, 10, 10, 10, 6, 6, 6, 6, 6, 6, 6)
$grid = 32
$columns = 40
$rows = 40
$sheetSize = $grid * $columns
$magic = [byte[]](0x50, 0x50, 0x54, 0x31)
$stageCanvasSizes = [ordered]@{
    egg = 16
    baby = 16
    young = 32
    adult = 48
}

# Indexed colors make the PNG source safe for pixel editing. The compiler
# rejects anti-aliased or unknown pixels instead of silently changing art.
$palette = @(
    "00000000", "FF24212B", "FFC8A27C", "FF766A82", "FFF6EAD7", "FFFFFFFF",
    "FFFFAB76", "FFD27C9E", "FF9EDEFA", "FF302A36", "FF17131D"
) | ForEach-Object { [Convert]::ToUInt32($_, 16) }

if ([string]::IsNullOrWhiteSpace($SourceDirectory)) {
    $SourceDirectory = if ($Mode -eq "Export") {
        Join-Path $PSScriptRoot "..\..\app\src\main\assets\pixel_pet\v2"
    } else {
        Join-Path $PSScriptRoot "v3"
    }
}
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
    $OutputDirectory = if ($Mode -eq "Export") {
        Join-Path $PSScriptRoot "v3"
    } else {
        Join-Path $PSScriptRoot "..\..\app\src\main\assets\pixel_pet\v3"
    }
}

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

function Write-UInt16BigEndian([System.IO.BinaryWriter]$writer, [int]$value) {
    $writer.Write([byte](($value -shr 8) -band 0xFF))
    $writer.Write([byte]($value -band 0xFF))
}

function Get-Descriptors {
    $items = [System.Collections.Generic.List[object]]::new()
    for ($stage = 0; $stage -lt 4; $stage++) {
        for ($action = 0; $action -lt $frameCounts.Count; $action++) {
            for ($facing = 0; $facing -lt 4; $facing++) {
                for ($frame = 0; $frame -lt $frameCounts[$action]; $frame++) {
                    $items.Add([PSCustomObject]@{
                        Stage = $stage
                        Action = $action
                        Facing = $facing
                        Frame = $frame
                    })
                }
            }
        }
    }
    if ($items.Count -ne 1600) { throw "Unexpected v3 frame layout" }
    return $items
}

function Read-Ppt1Frames([string]$path, [int]$expectedSpecies) {
    $raw = [System.IO.File]::ReadAllBytes((Resolve-Path -LiteralPath $path))
    if ($raw.Length -lt 9 -or $raw[0] -ne $magic[0] -or $raw[1] -ne $magic[1] -or $raw[2] -ne $magic[2] -or $raw[3] -ne $magic[3]) {
        throw "Input is not a PPT sprite sheet: $path"
    }
    if ($raw[4] -ne 1) { throw "Editable export expects a v2/PPT1 sprite pack" }
    $count = Read-UInt32BigEndian $raw 5
    $offset = 9
    $frames = [System.Collections.Generic.List[object]]::new()
    for ($index = 0; $index -lt $count; $index++) {
        if ($offset + 7 -gt $raw.Length) { throw "Truncated frame header $index" }
        $species = [int]$raw[$offset]
        $stage = [int]$raw[$offset + 1]
        $action = [int]$raw[$offset + 2]
        $facing = [int]$raw[$offset + 3]
        $frame = [int]$raw[$offset + 4]
        $cellCount = Read-UInt16BigEndian $raw ($offset + 5)
        $recordLength = 7 + $cellCount * 2
        if ($offset + $recordLength -gt $raw.Length) { throw "Truncated frame cells $index" }
        if ($species -ne $expectedSpecies) { throw "Unexpected species record in $path" }
        $cells = [uint16[]]::new($cellCount)
        for ($cell = 0; $cell -lt $cellCount; $cell++) {
            $cells[$cell] = [uint16](Read-UInt16BigEndian $raw ($offset + 7 + $cell * 2))
        }
        $frames.Add([PSCustomObject]@{
            Stage = $stage
            Action = $action
            Facing = $facing
            Frame = $frame
            Cells = $cells
        })
        $offset += $recordLength
    }
    if ($offset -ne $raw.Length -or $frames.Count -ne 1600) { throw "Invalid frame count in $path" }
    return $frames
}

function Get-Bounds([uint16[]]$cells) {
    $minX = 31; $maxX = 0; $minY = 31; $maxY = 0
    foreach ($packed in $cells) {
        $position = $packed -shr 4
        $x = $position % $grid
        $y = [Math]::Floor($position / $grid)
        $minX = [Math]::Min($minX, $x); $maxX = [Math]::Max($maxX, $x)
        $minY = [Math]::Min($minY, $y); $maxY = [Math]::Max($maxY, $y)
    }
    return @{ MinX = $minX; MaxX = $maxX; MinY = $minY; MaxY = $maxY; CenterX = [Math]::Floor(($minX + $maxX) / 2) }
}

function Clamp-Grid([int]$value) { return [Math]::Max(0, [Math]::Min($grid - 1, $value)) }

function Get-FrameAnchors([uint16[]]$cells, [object]$descriptor, [int]$species) {
    $bounds = Get-Bounds $cells
    $direction = if ($descriptor.Facing -eq 2) { -1 } elseif ($descriptor.Facing -eq 3) { 1 } else { 0 }
    $walkLift = if ($descriptor.Action -eq 1 -and $descriptor.Frame % 4 -in 1, 2) { -1 } else { 0 }
    $head = @((Clamp-Grid $bounds.CenterX), (Clamp-Grid ($bounds.MinY + 2 + $walkLift)))
    $neck = @((Clamp-Grid $bounds.CenterX), (Clamp-Grid ($bounds.MinY + [Math]::Max(5, [Math]::Floor(($bounds.MaxY - $bounds.MinY) * 0.43)) + $walkLift)))
    $backX = if ($direction -lt 0) { $bounds.MinX + 2 } elseif ($direction -gt 0) { $bounds.MaxX - 2 } else { $bounds.MaxX - 2 }
    $tailX = if ($species -eq 0) { $bounds.CenterX } elseif ($direction -lt 0) { $bounds.MaxX - 1 } elseif ($direction -gt 0) { $bounds.MinX + 1 } else { $bounds.MaxX - 1 }
    $handX = if ($direction -lt 0) { $bounds.MinX + 2 } elseif ($direction -gt 0) { $bounds.MaxX - 2 } else { $bounds.CenterX + 3 }
    $handLift = switch ($descriptor.Action) {
        1 { if ($descriptor.Frame % 2 -eq 0) { -1 } else { 1 } } # walking counter-swing
        2 { -2 - ($descriptor.Frame % 2) } # eating lift and chew
        4 { 1 + ($descriptor.Frame % 3) } # sleeping curl and breathing shift
        9 { -2 - ($descriptor.Frame % 2) } # play reach
        11 { -2 - ($descriptor.Frame % 2) } # cleaning paw/wing
        default { 0 }
    }
    $back = @((Clamp-Grid $backX), (Clamp-Grid ($bounds.MinY + [Math]::Floor(($bounds.MaxY - $bounds.MinY) * 0.58))))
    $hand = @((Clamp-Grid $handX), (Clamp-Grid ($bounds.MaxY - 5 + $handLift)))
    $tailBob = if ($descriptor.Frame % 2 -eq 0) { 0 } else { -1 }
    $tail = @((Clamp-Grid $tailX), (Clamp-Grid ($bounds.MaxY - 6 + $tailBob)))
    $trailOffset = if ($descriptor.Frame % 2 -eq 0) { -2 } else { 2 }
    $trail = @((Clamp-Grid ($bounds.CenterX + $trailOffset)), (Clamp-Grid ($bounds.MaxY - 1)))
    $headLayer = if ($descriptor.Facing -eq 1) { 0 } else { 2 }
    $neckLayer = if ($descriptor.Facing -eq 1) { 0 } else { 1 }
    $handLayer = if ($descriptor.Facing -eq 1 -or ($descriptor.Action -eq 4 -and $descriptor.Facing -ne 0)) { 0 } else { 2 }
    return [PSCustomObject]@{
        head = @($head[0], $head[1], $headLayer)
        back = @($back[0], $back[1], 0)
        hand = @($hand[0], $hand[1], $handLayer)
        neck = @($neck[0], $neck[1], $neckLayer)
        tail = @($tail[0], $tail[1], 0)
        trail = @($trail[0], $trail[1], 0)
    }
}

function Write-Bitmap([string]$path, [object[]]$frames) {
    $bytes = [byte[]]::new($sheetSize * $sheetSize * 4)
    for ($index = 0; $index -lt $frames.Count; $index++) {
        $baseX = ($index % $columns) * $grid
        $baseY = [Math]::Floor($index / $columns) * $grid
        foreach ($packed in $frames[$index].Cells) {
            $position = $packed -shr 4
            $code = $packed -band 0x0F
            $color = $palette[$code]
            $x = $baseX + ($position % $grid)
            $y = $baseY + [Math]::Floor($position / $grid)
            $offset = ($y * $sheetSize + $x) * 4
            $bytes[$offset] = [byte]($color -band 0xFF)
            $bytes[$offset + 1] = [byte](($color -shr 8) -band 0xFF)
            $bytes[$offset + 2] = [byte](($color -shr 16) -band 0xFF)
            $bytes[$offset + 3] = [byte](($color -shr 24) -band 0xFF)
        }
    }
    $bitmap = [System.Drawing.Bitmap]::new($sheetSize, $sheetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $rect = [System.Drawing.Rectangle]::new(0, 0, $sheetSize, $sheetSize)
        $data = $bitmap.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::WriteOnly, $bitmap.PixelFormat)
        try { [System.Runtime.InteropServices.Marshal]::Copy($bytes, 0, $data.Scan0, $bytes.Length) } finally { $bitmap.UnlockBits($data) }
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $bitmap.Dispose() }
}

function Read-BitmapFrames([string]$path) {
    $bitmap = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $path).Path)
    try {
        if ($bitmap.Width -ne $sheetSize -or $bitmap.Height -ne $sheetSize) { throw "Invalid source sheet size: $path" }
        $reversePalette = @{}
        for ($index = 0; $index -lt $palette.Count; $index++) { $reversePalette[[int64]$palette[$index]] = $index }
        $rect = [System.Drawing.Rectangle]::new(0, 0, $sheetSize, $sheetSize)
        $data = $bitmap.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $bytes = [byte[]]::new($sheetSize * $sheetSize * 4)
            [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
        } finally { $bitmap.UnlockBits($data) }
        $frames = [System.Collections.Generic.List[uint16[]]]::new()
        for ($index = 0; $index -lt 1600; $index++) {
            $baseX = ($index % $columns) * $grid
            $baseY = [Math]::Floor($index / $columns) * $grid
            $cells = [System.Collections.Generic.List[uint16]]::new()
            for ($y = 0; $y -lt $grid; $y++) {
                for ($x = 0; $x -lt $grid; $x++) {
                    $offset = (($baseY + $y) * $sheetSize + $baseX + $x) * 4
                    $argb = [uint32]$bytes[$offset + 3] -shl 24 -bor [uint32]$bytes[$offset + 2] -shl 16 -bor [uint32]$bytes[$offset + 1] -shl 8 -bor [uint32]$bytes[$offset]
                    if (-not $reversePalette.ContainsKey([int64]$argb)) { throw "Unknown source color at frame $index ($x,$y) in $path" }
                    $code = [int]$reversePalette[[int64]$argb]
                    if ($code -ne 0) { $cells.Add([uint16](($y * $grid + $x) * 16 + $code)) }
                }
            }
            $frames.Add($cells.ToArray())
        }
        return $frames
    } finally { $bitmap.Dispose() }
}

function Write-V3Pack([string]$path, [int]$species, [uint16[][]]$frames, [object[]]$anchors) {
    $descriptors = Get-Descriptors
    if ($frames.Count -ne $descriptors.Count -or $anchors.Count -ne $descriptors.Count) { throw "v3 source frame count mismatch" }
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try {
        $writer = [System.IO.BinaryWriter]::new($stream)
        try {
            $writer.Write($magic); $writer.Write([byte]2); Write-UInt32BigEndian $writer $descriptors.Count
            for ($index = 0; $index -lt $descriptors.Count; $index++) {
                $descriptor = $descriptors[$index]
                $cells = $frames[$index]
                $meta = $anchors[$index]
                $writer.Write([byte]$species); $writer.Write([byte]$descriptor.Stage); $writer.Write([byte]$descriptor.Action)
                $writer.Write([byte]$descriptor.Facing); $writer.Write([byte]$descriptor.Frame); Write-UInt16BigEndian $writer $cells.Count
                foreach ($slot in @("head", "back", "hand", "neck", "tail", "trail")) {
                    $anchor = @($meta.anchors.$slot)
                    if ($anchor.Count -ne 3) { throw "Invalid $slot anchor for frame $index" }
                    $x = Clamp-Grid ([int]$anchor[0]); $y = Clamp-Grid ([int]$anchor[1])
                    Write-UInt16BigEndian $writer ($y * $grid + $x)
                }
                foreach ($slot in @("head", "back", "hand", "neck", "tail", "trail")) {
                    $layer = [int]@($meta.anchors.$slot)[2]
                    if ($layer -lt 0 -or $layer -gt 2) { throw "Invalid $slot layer for frame $index" }
                    $writer.Write([byte]$layer)
                }
                foreach ($cell in $cells) { Write-UInt16BigEndian $writer $cell }
            }
        } finally { $writer.Dispose() }
    } finally { $stream.Dispose() }
}

function Write-SourceManifest([string]$directory) {
    $manifest = [ordered]@{
        version = 3
        grid = $grid
        columns = $columns
        rows = $rows
        framesPerSpecies = 1600
        stageCanvasSizes = $stageCanvasSizes
        palette = $palette
        sources = [ordered]@{}
    }
    foreach ($name in $speciesNames) {
        $image = Join-Path $directory "$name.png"
        $anchors = Join-Path $directory "$name.anchors.json"
        $manifest.sources[$name] = [ordered]@{
            image = "$name.png"
            imageSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $image).Hash.ToLowerInvariant()
            anchors = "$name.anchors.json"
            anchorsSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $anchors).Hash.ToLowerInvariant()
        }
    }
    [System.IO.File]::WriteAllText(
        (Join-Path $directory "source-manifest.json"),
        ($manifest | ConvertTo-Json -Depth 4),
        [System.Text.Encoding]::UTF8
    )
}

if ($Mode -eq "Export") {
    [System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
    $descriptors = Get-Descriptors
    for ($species = 0; $species -lt $speciesNames.Count; $species++) {
        $name = $speciesNames[$species]
        $frames = Read-Ppt1Frames (Join-Path $SourceDirectory "$name.bin") $species
        Write-Bitmap (Join-Path $OutputDirectory "$name.png") $frames
        $metadata = [System.Collections.Generic.List[object]]::new()
        for ($index = 0; $index -lt $frames.Count; $index++) {
            $metadata.Add([PSCustomObject]@{ index = $index; anchors = Get-FrameAnchors $frames[$index].Cells $descriptors[$index] $species })
        }
        [System.IO.File]::WriteAllText((Join-Path $OutputDirectory "$name.anchors.json"), ($metadata | ConvertTo-Json -Depth 5), [System.Text.Encoding]::UTF8)
    }
    Write-SourceManifest $OutputDirectory
    Write-Output "Exported editable v3 pixel pet sources to $OutputDirectory"
    exit 0
}

[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$manifestLines = [System.Collections.Generic.List[string]]::new()
$manifestLines.Add("# Generated from editable PNG Sprite sheets by pixel_pet_sprite_source_v3.ps1")
$manifestLines.Add("version=3")
for ($species = 0; $species -lt $speciesNames.Count; $species++) {
    $name = $speciesNames[$species]
    $frames = Read-BitmapFrames (Join-Path $SourceDirectory "$name.png")
    $anchors = @(Get-Content -Raw (Join-Path $SourceDirectory "$name.anchors.json") | ConvertFrom-Json)
    $path = Join-Path $OutputDirectory "$name.bin"
    Write-V3Pack $path $species $frames $anchors
    $manifestLines.Add("$name.asset=pixel_pet/v3/$name.bin")
    $manifestLines.Add("$name.sha256=$((Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLowerInvariant())")
    $manifestLines.Add("$name.frames=1600")
    $manifestLines.Add("$name.format=2")
}
[System.IO.File]::WriteAllLines((Join-Path $OutputDirectory "manifest.properties"), $manifestLines, [System.Text.Encoding]::ASCII)
Write-SourceManifest $SourceDirectory
Write-Output "Compiled editable v3 pixel pet sources to $OutputDirectory"
