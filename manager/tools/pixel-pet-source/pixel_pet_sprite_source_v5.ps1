param(
    [string]$SourceDirectory = (Join-Path $PSScriptRoot "v5"),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\..\app\src\main\assets\pixel_pet\v5")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$speciesNames = @("penguin", "dog", "cat", "bird", "rabbit", "hamster")
$stageNames = @("egg", "baby", "young", "adult")
$stageSizes = @(16, 16, 32, 48)
$slots = @("head", "back", "hand", "neck", "tail", "trail")
$magic = [byte[]](0x50, 0x50, 0x54, 0x31)
$formatVersion = 3
$framesPerStage = 400
$framesPerSpecies = $framesPerStage * $stageNames.Count
$codeByArgb = @{}
$palette = [ordered]@{
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
$code = 1
foreach ($entry in $palette.GetEnumerator()) {
    $codeByArgb[[int]$entry.Value.ToArgb()] = $code
    $code++
}

function Write-UInt16BigEndian([System.IO.BinaryWriter]$writer, [int]$value) {
    $writer.Write([byte](($value -shr 8) -band 0xFF))
    $writer.Write([byte]($value -band 0xFF))
}

function Write-UInt32BigEndian([System.IO.BinaryWriter]$writer, [int]$value) {
    $writer.Write([byte](($value -shr 24) -band 0xFF))
    $writer.Write([byte](($value -shr 16) -band 0xFF))
    $writer.Write([byte](($value -shr 8) -band 0xFF))
    $writer.Write([byte]($value -band 0xFF))
}

function Get-Sha256([string]$path) {
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $path).Hash.ToLowerInvariant()
}

function Read-StageSource([string]$species, [int]$stageIndex) {
    $stage = $stageNames[$stageIndex]
    $imagePath = Join-Path $SourceDirectory "${species}_${stage}.png"
    $metadataPath = Join-Path $SourceDirectory "${species}_${stage}.json"
    if (-not (Test-Path -LiteralPath $imagePath -PathType Leaf)) {
        throw "Missing native Sprite sheet: $imagePath"
    }
    if (-not (Test-Path -LiteralPath $metadataPath -PathType Leaf)) {
        throw "Missing native Sprite metadata: $metadataPath"
    }
    $metadata = Get-Content -Raw -LiteralPath $metadataPath | ConvertFrom-Json
    if ([int]$metadata.version -ne 5 -or [int]$metadata.format -ne $formatVersion) {
        throw "Unsupported native Sprite metadata: $species/$stage"
    }
    if ([string]$metadata.species -ne $species -or [string]$metadata.stage -ne $stage) {
        throw "Mismatched native Sprite metadata: $species/$stage"
    }
    $size = $stageSizes[$stageIndex]
    if ([int]$metadata.canvasSize -ne $size -or [int]$metadata.columns -ne 20 -or [int]$metadata.rows -ne 20) {
        throw "Invalid native Sprite sheet geometry: $species/$stage"
    }
    $frames = @($metadata.frames)
    if ($frames.Count -ne $framesPerStage) {
        throw "Invalid native Sprite frame count: $species/$stage"
    }
    $bitmap = [System.Drawing.Bitmap]::new($imagePath)
    if ($bitmap.Width -ne $size * 20 -or $bitmap.Height -ne $size * 20) {
        $bitmap.Dispose()
        throw "Invalid native Sprite sheet dimensions: $species/$stage"
    }
    return [PSCustomObject]@{
        Bitmap = $bitmap
        Frames = $frames
        ImagePath = $imagePath
        MetadataPath = $metadataPath
        Size = $size
    }
}

function Read-PackedCells($source, $record) {
    $size = [int]$record.width
    if ($size -ne $source.Size -or [int]$record.height -ne $source.Size) {
        throw "Frame canvas does not match its stage Sprite sheet"
    }
    $index = [int]$record.index
    if ($index -lt 0 -or $index -ge $framesPerStage) {
        throw "Invalid native Sprite frame index"
    }
    $baseX = ($index % 20) * $size
    $baseY = [int][Math]::Floor($index / 20) * $size
    $cells = [System.Collections.Generic.List[uint16]]::new()
    for ($y = 0; $y -lt $size; $y++) {
        for ($x = 0; $x -lt $size; $x++) {
            $color = $source.Bitmap.GetPixel($baseX + $x, $baseY + $y)
            if ($color.A -eq 0) { continue }
            if ($color.A -ne 0xFF) {
                throw "Semi-transparent pixel in native Sprite frame $index at $x,$y"
            }
            $cellCode = $codeByArgb[[int]$color.ToArgb()]
            if ($null -eq $cellCode) {
                throw "Non-palette pixel in native Sprite frame $index at $x,$y"
            }
            $cells.Add([uint16]((($y * $size + $x) * 16) + $cellCode))
        }
    }
    if ($cells.Count -eq 0) { throw "Empty native Sprite frame $index" }
    return $cells
}

function Assert-FrameMetadata($record, [int]$stageIndex, [int]$size) {
    if ([int]$record.stage -ne $stageIndex) { throw "Invalid frame growth stage" }
    if ([int]$record.action -lt 0 -or [int]$record.action -ge 13) { throw "Invalid frame action" }
    if ([int]$record.facing -lt 0 -or [int]$record.facing -ge 4) { throw "Invalid frame facing" }
    if ([int]$record.frame -lt 0 -or [int]$record.frame -ge 10) { throw "Invalid timing frame" }
    if ([int]$record.pivotX -lt 0 -or [int]$record.pivotX -ge $size) { throw "Invalid frame pivot" }
    if ([int]$record.baselineY -lt 0 -or [int]$record.baselineY -ge $size) { throw "Invalid frame baseline" }
    if (-not ([string]$record.provenance).StartsWith("authored-v5-")) {
        throw "Frame lacks authored v5 provenance"
    }
    foreach ($slot in $slots) {
        $anchor = @($record.anchors.$slot)
        if ($anchor.Count -ne 3) { throw "Missing $slot attachment" }
        if ([int]$anchor[0] -lt 0 -or [int]$anchor[0] -ge $size) { throw "Invalid $slot x attachment" }
        if ([int]$anchor[1] -lt 0 -or [int]$anchor[1] -ge $size) { throw "Invalid $slot y attachment" }
        if ([int]$anchor[2] -lt 0 -or [int]$anchor[2] -gt 2) { throw "Invalid $slot attachment layer" }
    }
}

function Write-SpeciesPack([string]$species, [int]$speciesIndex, [string]$path) {
    $sources = [System.Collections.Generic.List[object]]::new()
    try {
        for ($stage = 0; $stage -lt $stageNames.Count; $stage++) {
            $sources.Add((Read-StageSource $species $stage))
        }
        $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
        try {
            $writer = [System.IO.BinaryWriter]::new($stream)
            try {
                $writer.Write($magic)
                $writer.Write([byte]$formatVersion)
                Write-UInt32BigEndian $writer $framesPerSpecies
                for ($stage = 0; $stage -lt $sources.Count; $stage++) {
                    $source = $sources[$stage]
                    foreach ($record in $source.Frames) {
                        Assert-FrameMetadata $record $stage $source.Size
                        $cells = Read-PackedCells $source $record
                        $writer.Write([byte]$speciesIndex)
                        $writer.Write([byte][int]$record.stage)
                        $writer.Write([byte][int]$record.action)
                        $writer.Write([byte][int]$record.facing)
                        $writer.Write([byte][int]$record.frame)
                        $writer.Write([byte][int]$record.width)
                        $writer.Write([byte][int]$record.height)
                        $writer.Write([byte][int]$record.pivotX)
                        $writer.Write([byte][int]$record.baselineY)
                        Write-UInt16BigEndian $writer $cells.Count
                        foreach ($slot in $slots) {
                            $anchor = @($record.anchors.$slot)
                            Write-UInt16BigEndian $writer (([int]$anchor[1] * $source.Size) + [int]$anchor[0])
                        }
                        foreach ($slot in $slots) {
                            $writer.Write([byte][int]@($record.anchors.$slot)[2])
                        }
                        foreach ($cell in $cells) { Write-UInt16BigEndian $writer ([int]$cell) }
                    }
                }
            } finally {
                $writer.Dispose()
            }
        } finally {
            $stream.Dispose()
        }
    } finally {
        foreach ($source in $sources) { $source.Bitmap.Dispose() }
    }
}

[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$runtimeManifest = [System.Collections.Generic.List[string]]::new()
$runtimeManifest.Add("# Generated from v5 native per-frame pixel originals")
$runtimeManifest.Add("version=5")
$sourceEntries = [ordered]@{}

for ($speciesIndex = 0; $speciesIndex -lt $speciesNames.Count; $speciesIndex++) {
    $species = $speciesNames[$speciesIndex]
    $pack = Join-Path $OutputDirectory "$species.bin"
    Write-SpeciesPack $species $speciesIndex $pack
    $runtimeManifest.Add("$species.asset=pixel_pet/v5/$species.bin")
    $runtimeManifest.Add("$species.sha256=$(Get-Sha256 $pack)")
    $runtimeManifest.Add("$species.frames=$framesPerSpecies")
    $runtimeManifest.Add("$species.format=$formatVersion")
    $sourceEntries[$species] = [ordered]@{}
    foreach ($stage in $stageNames) {
        $image = Join-Path $SourceDirectory "${species}_${stage}.png"
        $metadata = Join-Path $SourceDirectory "${species}_${stage}.json"
        $sourceEntries[$species][$stage] = [ordered]@{
            image = [System.IO.Path]::GetFileName($image)
            imageSha256 = Get-Sha256 $image
            metadata = [System.IO.Path]::GetFileName($metadata)
            metadataSha256 = Get-Sha256 $metadata
        }
    }
}

[System.IO.File]::WriteAllLines(
    (Join-Path $OutputDirectory "manifest.properties"),
    $runtimeManifest,
    [System.Text.Encoding]::ASCII
)
$sourceManifest = [ordered]@{
    version = 5
    format = $formatVersion
    stageCanvasSizes = $stageSizes
    framesPerSpecies = $framesPerSpecies
    provenance = "authored-v5-native-semantic-pixel-sheets"
    sources = $sourceEntries
}
[System.IO.File]::WriteAllText(
    (Join-Path $SourceDirectory "source-manifest.json"),
    ($sourceManifest | ConvertTo-Json -Depth 7),
    [System.Text.Encoding]::UTF8
)

Write-Output "Compiled six-species v5 native Sprite sheets to $OutputDirectory"
