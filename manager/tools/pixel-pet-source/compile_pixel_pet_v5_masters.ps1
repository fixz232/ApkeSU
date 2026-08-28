param(
    [string]$SourceDirectory = (Join-Path $PSScriptRoot "v5-masters-src"),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "v5-masters")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$speciesNames = @("penguin", "dog", "cat", "bird", "rabbit", "hamster")
$stageNames = @("egg", "baby", "young", "adult")
$stageSizes = @(16, 16, 32, 48)
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

function Read-PixelRows([string]$path, [int]$size) {
    $rows = @(Get-Content -LiteralPath $path | Where-Object { -not $_.StartsWith("#") })
    if ($rows.Count -ne $size) {
        throw "$path must contain exactly $size pixel rows; found $($rows.Count)"
    }
    for ($y = 0; $y -lt $rows.Count; $y++) {
        if ($rows[$y].Length -ne $size) {
            throw "$path row $y must be exactly $size pixels"
        }
        foreach ($symbol in $rows[$y].ToCharArray()) {
            if ($symbol -ne '.' -and -not $colors.Contains([string]$symbol)) {
                throw "$path row $y contains unsupported pixel '$symbol'"
            }
        }
    }
    return $rows
}

function Assert-MasterGeometry([string]$name, [string[]]$rows, [int]$size) {
    $painted = [System.Collections.Generic.List[object]]::new()
    for ($y = 0; $y -lt $size; $y++) {
        for ($x = 0; $x -lt $size; $x++) {
            if ($rows[$y][$x] -ne '.') {
                $painted.Add(@($x, $y))
            }
        }
    }
    if ($painted.Count -eq 0) { throw "$name has no painted pixels" }
    $minX = ($painted | ForEach-Object { $_[0] } | Measure-Object -Minimum).Minimum
    $maxX = ($painted | ForEach-Object { $_[0] } | Measure-Object -Maximum).Maximum
    if ($minX -lt 1 -or $maxX -gt ($size - 2)) {
        throw "$name must keep a one-pixel horizontal action margin; bounds=$minX..$maxX"
    }
}

function Write-Master([string]$species, [string]$stage, [int]$size) {
    $name = "${species}_${stage}"
    $sourcePath = Join-Path $SourceDirectory "$name.px"
    $outputPath = Join-Path $OutputDirectory "$name.png"
    $rows = Read-PixelRows $sourcePath $size
    Assert-MasterGeometry $name $rows $size
    $bitmap = [System.Drawing.Bitmap]::new($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt $size; $y++) {
            for ($x = 0; $x -lt $size; $x++) {
                $symbol = [string]$rows[$y][$x]
                if ($symbol -ne '.') {
                    $bitmap.SetPixel($x, $y, $colors[$symbol])
                }
            }
        }
        $bitmap.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
foreach ($species in $speciesNames) {
    for ($stageIndex = 0; $stageIndex -lt $stageNames.Count; $stageIndex++) {
        Write-Master $species $stageNames[$stageIndex] $stageSizes[$stageIndex]
    }
}

$masterEntries = [ordered]@{}
foreach ($species in $speciesNames) {
    $masterEntries[$species] = [ordered]@{}
    for ($stageIndex = 0; $stageIndex -lt $stageNames.Count; $stageIndex++) {
        $stage = $stageNames[$stageIndex]
        $file = Join-Path $OutputDirectory "${species}_${stage}.png"
        $source = Join-Path $SourceDirectory "${species}_${stage}.px"
        $masterEntries[$species][$stage] = [ordered]@{
            source = [System.IO.Path]::GetFileName($source)
            sourceSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $source).Hash.ToLowerInvariant()
            image = [System.IO.Path]::GetFileName($file)
            canvasSize = $stageSizes[$stageIndex]
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $file).Hash.ToLowerInvariant()
        }
    }
}
$manifest = [ordered]@{
    schemaVersion = 2
    sourceVersion = 5
    provenance = "editable-semantic-pixel-rows"
    palette = @($colors.Keys)
    species = $masterEntries
}
[System.IO.File]::WriteAllText(
    (Join-Path $OutputDirectory "manifest.json"),
    ($manifest | ConvertTo-Json -Depth 7),
    [System.Text.Encoding]::UTF8
)

Write-Output "Compiled 24 editable semantic pixel masters"
