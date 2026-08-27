param(
    [string]$ReferenceDirectory = ""
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($ReferenceDirectory)) {
    $ReferenceDirectory = Join-Path $PSScriptRoot "..\app\src\main\assets\pixel_pet\reference"
}

$species = @("penguin", "dog", "cat", "bird", "rabbit", "hamster")
$stageSizes = [ordered]@{
    egg = 16
    baby = 16
    young = 32
    adult = 48
}

function Save-NativeCanvas([string]$Path, [int]$Size) {
    $source = [System.Drawing.Bitmap]::new((Resolve-Path -LiteralPath $Path).Path)
    try {
        if ($source.Width -eq $Size -and $source.Height -eq $Size) {
            return
        }
        $target = [System.Drawing.Bitmap]::new($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $target.SetResolution(96, 96)
            $graphics = [System.Drawing.Graphics]::FromImage($target)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.DrawImage($source, [System.Drawing.Rectangle]::new(0, 0, $Size, $Size))
            } finally {
                $graphics.Dispose()
            }
            $temporaryPath = "$Path.tmp.png"
            $target.Save($temporaryPath, [System.Drawing.Imaging.ImageFormat]::Png)
            $target.Dispose()
            $target = $null
            $source.Dispose()
            $source = $null
            Move-Item -LiteralPath $temporaryPath -Destination $Path -Force
        } finally {
            if ($null -ne $target) { $target.Dispose() }
        }
    } finally {
        if ($null -ne $source) { $source.Dispose() }
    }
}

foreach ($name in $species) {
    foreach ($stage in $stageSizes.Keys) {
        $path = Join-Path $ReferenceDirectory "$name`_$stage.png"
        if (-not (Test-Path -LiteralPath $path)) {
            throw "Missing reference sprite: $path"
        }
        Save-NativeCanvas $path $stageSizes[$stage]
    }
}

Write-Output "Normalized pixel pet reference artboards to egg/baby 16px, young 32px, adult 48px."
