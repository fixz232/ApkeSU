param(
    [string]$ReferenceDirectory = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ReferenceDirectory)) {
    $ReferenceDirectory = Join-Path $PSScriptRoot "..\app\src\main\assets\pixel_pet\reference"
}

$sourcePath = Join-Path $ReferenceDirectory "hamster_young.png"
$outputPath = Join-Path $ReferenceDirectory "hamster_adult_body.png"
$source = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $sourcePath).Path)
try {
    $target = [System.Drawing.Bitmap]::new(48, 48, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($target)
        try {
            $graphics.Clear([System.Drawing.Color]::Transparent)
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
            $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, 48, 48))
        } finally {
            $graphics.Dispose()
        }
        $temporaryPath = "$outputPath.tmp.png"
        $target.Save($temporaryPath, [System.Drawing.Imaging.ImageFormat]::Png)
        Move-Item -LiteralPath $temporaryPath -Destination $outputPath -Force
    } finally {
        $target.Dispose()
    }
} finally {
    $source.Dispose()
}

Write-Output "Generated clean 48px hamster body reference at $outputPath"
