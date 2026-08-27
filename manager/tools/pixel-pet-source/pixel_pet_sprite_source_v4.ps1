param(
    [string]$SourceDirectory = (Join-Path $PSScriptRoot "v4"),
    [string]$OutputDirectory = (Join-Path $PSScriptRoot "..\..\app\src\main\assets\pixel_pet\v4")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
. (Join-Path $PSScriptRoot "pixel_pet_reference_masters_v1.ps1")

# v4 is the editable, data-only art source for the pet renderer. Every base
# model below is authored directly on a 32x32 logical pixel grid. Runtime reads
# only the generated immutable cels and never crops, down-samples, or filters
# the supplied design boards.
$speciesNames = @("penguin", "dog", "cat", "bird", "rabbit", "hamster")
$frameCounts = @(8, 10, 10, 10, 10, 10, 6, 6, 6, 6, 6, 6, 6)
$grid = 32
$columns = 40
$sheetSize = $grid * $columns
$magic = [byte[]](0x50, 0x50, 0x54, 0x31)
$codeBySymbol = @{ o = 1; b = 2; s = 3; c = 4; h = 5; a = 6; m = 7; r = 8; e = 9; x = 10 }
$colorBySymbol = @{
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

function Get-Descriptors {
    $items = [System.Collections.Generic.List[object]]::new()
    for ($stage = 0; $stage -lt 4; $stage++) {
        for ($action = 0; $action -lt $frameCounts.Count; $action++) {
            for ($facing = 0; $facing -lt 4; $facing++) {
                for ($frame = 0; $frame -lt $frameCounts[$action]; $frame++) {
                    $items.Add([PSCustomObject]@{ Stage = $stage; Action = $action; Facing = $facing; Frame = $frame })
                }
            }
        }
    }
    if ($items.Count -ne 1600) { throw "Unexpected v4 frame layout" }
    return $items
}

function Get-StageName([int]$stage) { return @("egg", "baby", "young", "adult")[$stage] }
function Cell-Key([int]$x, [int]$y) { return "$x,$y" }
function Clamp-Grid([int]$value) { return [Math]::Max(0, [Math]::Min($grid - 1, $value)) }

function New-FrameMap { return [System.Collections.Generic.Dictionary[string, char]]::new() }

function Copy-FrameMap($source) {
    $copy = New-FrameMap
    foreach ($entry in $source.GetEnumerator()) { $copy[$entry.Key] = $entry.Value }
    return $copy
}

function Put-Cell($map, [int]$x, [int]$y, [char]$symbol) {
    if ($x -ge 0 -and $x -lt $grid -and $y -ge 0 -and $y -lt $grid) {
        $map[(Cell-Key $x $y)] = $symbol
    }
}

function Remove-Cell($map, [int]$x, [int]$y) { [void]$map.Remove((Cell-Key $x $y)) }

function Fill-Rect($map, [int]$x, [int]$y, [int]$width, [int]$height, [char]$symbol) {
    for ($py = $y; $py -lt ($y + $height); $py++) {
        for ($px = $x; $px -lt ($x + $width); $px++) { Put-Cell $map $px $py $symbol }
    }
}

function Draw-Line($map, [int]$x0, [int]$y0, [int]$x1, [int]$y1, [char]$symbol) {
    $steps = [Math]::Max([Math]::Abs($x1 - $x0), [Math]::Abs($y1 - $y0))
    if ($steps -eq 0) { Put-Cell $map $x0 $y0 $symbol; return }
    for ($index = 0; $index -le $steps; $index++) {
        Put-Cell $map ([Math]::Round($x0 + ($x1 - $x0) * $index / $steps)) ([Math]::Round($y0 + ($y1 - $y0) * $index / $steps)) $symbol
    }
}

function Get-Bounds($map) {
    if ($map.Count -eq 0) { return $null }
    $minX = $grid; $maxX = -1; $minY = $grid; $maxY = -1
    foreach ($key in $map.Keys) {
        $parts = $key.Split(',')
        $x = [int]$parts[0]; $y = [int]$parts[1]
        if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
        if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
    }
    return [PSCustomObject]@{ MinX = [int]$minX; MaxX = [int]$maxX; MinY = [int]$minY; MaxY = [int]$maxY; CenterX = [int][Math]::Round(($minX + $maxX) / 2.0); CenterY = [int][Math]::Round(($minY + $maxY) / 2.0) }
}

function Translate-Frame($map, [int]$dx, [int]$dy) {
    if ($dx -eq 0 -and $dy -eq 0) { return }
    $moved = New-FrameMap
    foreach ($entry in $map.GetEnumerator()) {
        $parts = $entry.Key.Split(','); $x = [int]$parts[0] + $dx; $y = [int]$parts[1] + $dy
        if ($x -ge 0 -and $x -lt $grid -and $y -ge 0 -and $y -lt $grid) { $moved[(Cell-Key $x $y)] = $entry.Value }
    }
    $map.Clear()
    foreach ($entry in $moved.GetEnumerator()) { $map[$entry.Key] = $entry.Value }
}

function Translate-FrameRegion($map, [int]$maxY, [int]$dx, [int]$dy) {
    if ($dx -eq 0 -and $dy -eq 0) { return }
    $moving = [System.Collections.Generic.List[object]]::new()
    foreach ($entry in @($map.GetEnumerator())) {
        $parts = $entry.Key.Split(',')
        $x = [int]$parts[0]
        $y = [int]$parts[1]
        if ($y -le $maxY) {
            $moving.Add([PSCustomObject]@{ Key = $entry.Key; X = $x; Y = $y; Value = $entry.Value })
        }
    }
    foreach ($cell in $moving) { [void]$map.Remove($cell.Key) }
    foreach ($cell in $moving) { Put-Cell $map ($cell.X + $dx) ($cell.Y + $dy) $cell.Value }
}

function Normalize-Baseline($map) {
    $bounds = Get-Bounds $map
    if ($null -ne $bounds) { Translate-Frame $map 0 (29 - $bounds.MaxY) }
}

function Normalize-SafeArtboard($map) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $dx = if ($bounds.MinX -lt 1) {
        1 - $bounds.MinX
    } elseif ($bounds.MaxX -gt ($grid - 2)) {
        ($grid - 2) - $bounds.MaxX
    } else {
        0
    }
    if ($dx -ne 0) { Translate-Frame $map $dx 0 }
}

function New-AuthoredFrameFromRows([string[]]$rows) {
    # Keep the complete 32-cell artboard available to wide-winged birds and
    # curled tails. Rows still carry their own transparent margin, so the
    # authored silhouette remains inside the safe 1..30 runtime area.
    $authoredWidth = 32
    $authoredHeight = 26
    if ($rows.Count -ne $authoredHeight) { throw "An authored pet master must have $authoredHeight rows" }
    $map = New-FrameMap
    $leftX = 0
    $topY = 29 - ($authoredHeight - 1)
    for ($rowIndex = 0; $rowIndex -lt $rows.Count; $rowIndex++) {
        $row = $rows[$rowIndex].Replace(' ', '')
        if ($row.Length -gt $authoredWidth) { throw "Authored pet row is wider than $authoredWidth pixels: $row" }
        $row = $row.PadRight($authoredWidth, '.')
        for ($column = 0; $column -lt $authoredWidth; $column++) {
            $symbol = [char]$row[$column]
            if ($symbol -eq '.') { continue }
            if (-not $codeBySymbol.ContainsKey([string]$symbol)) { throw "Unsupported authored pixel symbol: $symbol" }
            Put-Cell $map ($leftX + $column) ($topY + $rowIndex) $symbol
        }
    }
    Normalize-Baseline $map
    return $map
}

function New-AuthoredFrameFromPattern([string[]]$rows) {
    if ($rows.Count -lt 1 -or $rows.Count -gt 30) {
        throw "An authored pet pattern must contain 1..30 rows"
    }
    $normalized = @($rows | ForEach-Object { $_.Replace(' ', '') })
    $patternWidth = ($normalized | ForEach-Object { $_.Length } | Measure-Object -Maximum).Maximum
    if ($patternWidth -lt 1 -or $patternWidth -gt 30) {
        throw "An authored pet pattern must be 1..30 pixels wide"
    }
    $map = New-FrameMap
    $leftX = [int][Math]::Floor(($grid - $patternWidth) / 2.0)
    $topY = 29 - ($rows.Count - 1)
    for ($rowIndex = 0; $rowIndex -lt $normalized.Count; $rowIndex++) {
        $row = $normalized[$rowIndex].PadRight($patternWidth, '.')
        for ($column = 0; $column -lt $patternWidth; $column++) {
            $symbol = [char]$row[$column]
            if ($symbol -eq '.') { continue }
            if (-not $codeBySymbol.ContainsKey([string]$symbol)) {
                throw "Unsupported authored pixel symbol: $symbol"
            }
            Put-Cell $map ($leftX + $column) ($topY + $rowIndex) $symbol
        }
    }
    Normalize-Baseline $map
    return $map
}

function Get-AuthoredAdultRows([string]$species) {
    # These are hand-authored front cels. The palettes are semantic so the
    # habitat can tint them without destroying the dark outline or face.
    switch ($species) {
        'penguin' { return @(
            '........oooo', '.......oobboo', '......oobbbboo', '......obbbbbbo',
            '.....obbbccbbbbo', '....obbbccccbbbbo', '....obbcchehccbbbo', '....obbccxeexccbbbo',
            '....obbcccaacccbbbo', '....obbcccccccccbbbo', '....obbbcccccccbbbbo', '...obbbbscccccsbbbbbo',
            '...obbbbscccccsbbbbbo', '...obbbbbssssbbbbbbbo', '...obbbbbbbbbbbbbbbbo', '..obbbbbbbbbbbbbbbbbbo',
            '..obbbbbbbbbbbbbbbbbbo', '..obbbbbbbbbbbbbbbbbbo', '...obbbbo....obbbbbo', '...obbbbo....obbbbbo',
            '....obbb......obbbo', '....obbb......obbbo', '.....aaa......aaa', '.....aaaa....aaaa',
            '......aa......aa', '......aa......aa'
        ) }
        'cat' { return @(
            '......oo......oo', '.....obbo....obbo', '.....obbbo..obbbo', '....obbbbbbbbbbo',
            '....obbbccccbbbbo', '...obbbcchehccbbbo', '...obbccxeexccbbbbo', '...obbcccmcccbbbbo',
            '....obbbccccbbbbo', '....obbbbbbbbbbo', '.....obbbbbbbbo', '.....obbbbbbbbbo',
            '....obbbbbbbbbbo', '....obbbbbbbbbbo', '...obbbccccccbbbbo', '...obbbccccccbbbbo',
            '...obbbbbbbbbbbbbo', '...obbbbbbbbbbbbbo', '..ooobbbbbbbbbbo', '..obboobbbbbbbbbo',
            '.obbbo.obbbbbbbbbo', '.obbo...obbbbbo', '..oo....obbbbbo', '.......obb..bbo',
            '......aa....aa', '......aa....aa'
        ) }
        'dog' { return @(
            '.....oooo....oooo', '....obbbbo..obbbbo', '....obbbbboobbbbbo', '.....obbbbbbbbbbo',
            '....obbbccccbbbbo', '...obbbcchehccbbbo', '...obbccxeexccbbbbo', '...obbcccaacccbbbbo',
            '....obbbccccbbbbo', '....obbbbbbbbbbo', '.....obbbbbbbbo', '....obbbbbbbbbbbo',
            '...obbbbbbbbbbbbbo', '...obbbccccccbbbbo', '...obbbccccccbbbbo', '...obbbbbbbbbbbbbo',
            '...obbbbbbbbbbbbbo', '....obbbbbbbbbbo', '....obbbbo..obbbo', '....obbbbo..obbbo',
            '.....obbb....bbbo', '.....obbb....bbbo', '......aa......aa', '......aa......aa',
            '.....aaa......aaa', '.....aaa......aaa'
        ) }
        'bird' { return @(
            '........oooo', '.......oobboo', '......oobbbboo', '.....obbbccbbbbo',
            '....obbbccheccbbbo', '....obbccxeexccbbbo', '....obbcccaacccbbbo', '...obbbcccccccbbbbo',
            '..obbbbscccccsbbbbbo', '.obbbbbssccccssbbbbbbo', '.obbbbssccccccssbbbbbo', '..obbbbssccccssbbbbbo',
            '...obbbbssssbbbbbo', '...obbbbbbbbbbbbbo', '....obbbbbbbbbbo', '.....obbbbbbbbo',
            '......obbbbbbo', '......obbbbbbo', '.....obbbbobbbo', '.....obbbbobbbo',
            '......aaa.aaa', '......aaa.aaa', '.......aa.aa', '.......aa.aa',
            '......aaaaaaa', '.......aaaaa'
        ) }
        'rabbit' { return @(
            '......oo......oo', '......obbo....obbo', '......obbo....obbo', '......obbo....obbo',
            '......obbbo..obbbo', '.....obbbbbbbbbbo', '....obbbccccbbbbo', '...obbbcchehccbbbo',
            '...obbccxeexccbbbbo', '...obbcccmcccbbbbo', '....obbbccccbbbbo', '....obbbbbbbbbbo',
            '.....obbbbbbbbo', '....obbbbbbbbbbo', '...obbbccccccbbbbo', '...obbbccccccbbbbo',
            '...obbbbbbbbbbbbbo', '...obbbbbbbbbbbbbo', '....obbbbbbbbbbbo', '....obbbbo..obbbo',
            '....obbbbo..obbbo', '.....obbb....bbbo', '.....obbb....bbbo', '......aa......aa',
            '......aa......aa', '......aa......aa'
        ) }
        'hamster' { return @(
            '......oooo....oooo', '.....obbbbo..obbbbo', '.....obbbbboobbbbbo', '....obbbbbbbbbbbbo',
            '...obbbccccccbbbbo', '...obbcchehccbbbbo', '...obbccxeexccbbbbo', '...obbcccmcccbbbbo',
            '..obbbcccaacccbbbbo', '..obbbcccccccccbbbbo', '..obbbbbccccccbbbbbo', '...obbbbssssbbbbbo',
            '...obbbbbbbbbbbbbo', '...obbbbbbbbbbbbbo', '...obbbccccccbbbbo', '...obbbccccccbbbbo',
            '...obbbbbbbbbbbbbo', '....obbbbbbbbbbo', '....obbbbo..obbbo', '....obbbbo..obbbo',
            '.....obbb....bbbo', '.....obbb....bbbo', '......aa......aa', '......aa......aa',
            '.......aaaaaaaa', '........aaaaaa'
        ) }
        default { throw "Missing authored model for $species" }
    }
}

function Get-AuthoredAdultRowsV2([string]$species) {
    # Each row below is a hand-authored 32x32 logical pixel cel. The leading
    # dots are intentional transparent cells; no reference bitmap is read,
    # cropped, quantized, or sampled by the compiler.
    switch ($species) {
        'penguin' { return @(
            '...........oooooo...........', '.........oobbbbboo.........', '........obbbbbbbbo........',
            '.......obbbbbbbbbbo.......', '......obbbccccccbbbo......', '.....obbbccccccccbbbo.....',
            '.....obbccceccecbbbbo.....', '.....obbccceccecbbbbo.....', '.....obbccccaccccbbbo.....',
            '....obbbccccccccccbbbbo....', '....obbbbbccccccbbbbbo....', '...obbbbsccccccsbbbbo...',
            '..obbbbbssssssssbbbbbo..', '..obbbbbbbbbbbbbbbbbbo..', '...obbbbbbbbbbbbbbbbo...',
            '...obbbbbbbbbbbbbbbbo...', '....obbbbbbbbbbbbbbo....', '....obbbbbbbbbbbbbbo....',
            '....obbbbbbbbbbbbbbo....', '....obbbbo....obbbbo....', '....obbbbo....obbbbo....', '.....obbb......obbbo....',
            '.....obbb......obbbo....', '......aaa......aaa.....', '......aaaa....aaaa.....',
            '.......aa......aa......'
        ) }
        'cat' { return @(
            '........oo........oo........', '.......obbo......obbo.......', '......obbbbo....obbbbo......',
            '.....obbbbbbo..obbbbbbo.....', '....obbbbbbbbbbbbbbbbbo....', '...obbbccccccccccccbbbbo...',
            '...obbccceccecceccecbbbo...', '...obbccceccecceccecbbbo...', '...obbccccmaacmccccbbbo...',
            '....obbbccccccccccccbbbo....', '.....obbbbbbbbbbbbbbbbo.....', '.....obbbbbbbbbbbbbbbbo.....',
            '..ooobbbbbbbbbbbbbbbbbbo...', '.obboobbbbbbbbbbbbbbbbbbo...', 'obbo..obbbccccccccccbbbbo...',
            'obbo..obbbccccccccccbbbbo...', '.obboobbbbbbbbbbbbbbbbbbo...', '..ooobbbbbbbbbbbbbbbbbbo...',
            '....obbbbbbbbbbbbbbbbbo....', '....obbbbbo....obbbbbo....', '....obbbbbo....obbbbbo....',
            '.....obbbb......obbbbo....', '.....obbbb......obbbbo....', '......aaa......aaa.......',
            '......aa........aa.......', '....obb..........obb......'
        ) }
        'dog' { return @(
            '....ooobbbb....bbbbooo...', '...oobbbbbbo..obbbbbboo..', '..obbbbbbbboobbbbbbbbbo..',
            '..obbbbbbbbbbbbbbbbbbbbo..', '..obbbccccccccccccccbbbbo..', '.obbcccecceccecceccecbbbo.',
            '.obbcccecceccecceccecbbbo.', '.obbcccccccaaccccccbbbo...', '..obbbccccccccccccccbbbo..',
            '...obbbbbbbbbbbbbbbbbbo...', '..obbbbbbbbbbbbbbbbbbbbo..', '..obbbbbbbbbbbbbbbbbbbbo..',
            '.obbbbbbbbbbbbbbbbbbbbbo..', '.obbbccccccccccccccbbbbo..', '.obbbccccccccccccccbbbbo..',
            '.obbbbbbbbbbbbbbbbbbbbbo..', '..obbbbbbbbbbbbbbbbbbbbo..', '..obbbbbbbbbbbbbbbbbbbbo..',
            '...obbbbbbbbbbbbbbbbbo...', '...obbbbo....obbbbbo.....', '...obbbbo....obbbbbo.....',
            '....obbb......obbbbo.....', '....obbb......obbbbo.....', '.....aaa......aaa.......',
            '.....aaaa....aaaa.......', '..oobbo........obboo....'
        ) }
        'bird' { return @(
            '...........oooo.............', '..........oobbbbo............', '.........obbbbbbo....oo.....',
            '........obbbbbbbbo..obbo....', '.......obbbccccbbbo.obbbbo...', '......obbbcccec cbbboobbbbo'.Replace(' ',''),
            '.....obbbccceccecbbbbo.....', '....obbbbccccccccbbbbo.....', '...obbbbssccccccssbbbbo....',
            '..obbbbsssccccccsssbbbbo...', '.obbbbbssssccccssssbbbbbo..', 'obbbb bssssccccssssbbbbbo...'.Replace(' ',''),
            '..obbbbbssssssssbbbbbo....', '...obbbbbbbbbbbbbbbbo.....', '....obbbbbbbbbbbbbbo......',
            '.....obbbbbbbbbbbbo.......', '.....obbbbssbbbbbo........', '....obbbbssssbbbbo........',
            '...obbbbssssssbbbbo.......', '...obbbb..ss..bbbbo.......', '....obbbbo..obbbbo.......',
            '.....aaa....aaa..........', '.....aaaa..aaaa..........', '......aa....aa...........',
            '.....aaaa..aaaa..........', '......aaa..aaa...........'
        ) }
        'rabbit' { return @(
            '......oo......oo...........', '......obbo....obbo..........', '......obbo....obbo..........',
            '......obbo....obbo..........', '......obbo....obbo..........', '......obbo....obbo..........',
            '.....obbbbbbbbbbbbo.........', '....obbbccccccccbbbbo.......', '...obbccceccecceccecbbbo...',
            '...obbccceccecceccecbbbo...', '...obbccccm aacmccccbbbo...'.Replace(' ',''), '....obbbccccccccccbbbo....',
            '....obbbbbbbbbbbbbbbbo....', '.....obbbbbbbbbbbbbbbbo....', '....obbbccccccccccccbbbo...',
            '...obbbccccccccccccccbbbo..', '...obbbbbbbbbbbbbbbbbbbo...', '...obbbbbbbbbbbbbbbbbbbo...',
            '....obbbbbbbbbbbbbbbbbo....', '....obbbbbbbbbbbbbbbbbo....', '.....obbbbbo..obbbbbo.....',
            '.....obbbbbo..obbbbbo.....', '......obbb......obbb......', '......aaa......aaa.......',
            '......aa........aa.......', '.........obbo..obbo.......'
        ) }
        'hamster' { return @(
            '........oobbo..obbo........', '.......obbbboobbbbo.........', '......obbbbbbbbbbbbo........',
            '.....obbbbbbbbbbbbbbo.......', '....obbbcccccccccccbbbo.....', '...obbbccceccecceccecbbbo...',
            '...obbbccceccecceccecbbbo...', '..obbbccmmccccccmmccbbbo....', '..obbbccccccccccccccbbbbo...',
            '..obbbbbccccccccccbbbbbo....', '..obbbbbbbbbbbbbbbbbbbbo....', '..obbbbssssssssssssbbbbo....',
            '..obbbbssssssssssssbbbbo....', '...obbbbbbbbbbbbbbbbbo.....', '...obbbbbbbbbbbbbbbbbo.....',
            '...obbbccccccccccccbbbo....', '...obbbccccccccccccbbbo....', '....obbbbbbbbbbbbbbbo......',
            '....obbbbbbbbbbbbbbbo......', '....obbbbo....obbbbo.......', '....obbbbo....obbbbo.......',
            '.....obbb......obbbo......', '.....obbb......obbbo......', '......aaa......aaa.......',
            '......aa........aa.......', '.....obbo....obbo........'
        ) }
        default { throw "Missing authored v2 model for $species" }
    }
}

function Scale-AuthoredFrame($source, [double]$scale) {
    $scaled = New-FrameMap
    foreach ($entry in $source.GetEnumerator() | Sort-Object { $parts = $_.Key.Split(','); [int]$parts[1] * $grid + [int]$parts[0] }) {
        $parts = $entry.Key.Split(',')
        $x = [int]$parts[0]
        $y = [int]$parts[1]
        $targetX = Clamp-Grid ([int][Math]::Round(16 + ($x - 16) * $scale))
        $targetY = Clamp-Grid ([int][Math]::Round(29 + ($y - 29) * $scale))
        $key = Cell-Key $targetX $targetY
        if (-not $scaled.ContainsKey($key)) { $scaled[$key] = $entry.Value }
    }
    Normalize-Baseline $scaled
    return $scaled
}

function Refine-AuthoredFrontFace($map, [string]$species) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $center = $bounds.CenterX
    $eyeY = [Math]::Max(8, $bounds.MinY + 6)
    $muzzleTop = $eyeY - 1
    # Clear only the inner face plane; the one-cell outline remains authored
    # by the surrounding silhouette and cannot be overwritten by the face.
    Fill-Rect $map ($center - 4) $muzzleTop 9 5 'c'
    switch ($species) {
        'penguin' {
            Put-Cell $map ($center - 3) $eyeY 'e'
            Put-Cell $map ($center + 2) $eyeY 'e'
            Put-Cell $map ($center - 2) ($eyeY - 1) 'h'
            Put-Cell $map ($center + 3) ($eyeY - 1) 'h'
            Fill-Rect $map ($center - 1) ($eyeY + 2) 3 1 'a'
            Put-Cell $map $center ($eyeY + 3) 'o'
        }
        'bird' {
            Put-Cell $map ($center - 2) $eyeY 'e'
            Put-Cell $map ($center + 2) $eyeY 'e'
            Put-Cell $map ($center - 2) ($eyeY - 1) 'h'
            Put-Cell $map ($center + 2) ($eyeY - 1) 'h'
            Put-Cell $map ($center + 4) ($eyeY + 2) 'a'
            Put-Cell $map ($center + 3) ($eyeY + 3) 'a'
        }
        default {
            Put-Cell $map ($center - 3) $eyeY 'e'
            Put-Cell $map ($center + 3) $eyeY 'e'
            Put-Cell $map ($center - 3) ($eyeY - 1) 'h'
            Put-Cell $map ($center + 3) ($eyeY - 1) 'h'
            Put-Cell $map $center ($eyeY + 2) 'a'
            Put-Cell $map $center ($eyeY + 3) 'o'
            Put-Cell $map ($center - 4) ($eyeY + 2) 'm'
            Put-Cell $map ($center + 4) ($eyeY + 2) 'm'
        }
    }
    switch ($species) {
        'cat' {
            Draw-Line $map ($bounds.MaxX - 2) ($bounds.CenterY + 1) ($bounds.MaxX + 2) ($bounds.CenterY - 2) 'o'
            Draw-Line $map ($bounds.MaxX - 1) ($bounds.CenterY + 1) ($bounds.MaxX + 2) ($bounds.CenterY - 1) 'b'
        }
        'dog' {
            Draw-Line $map ($bounds.MinX + 1) ($bounds.CenterY + 1) ($bounds.MinX - 2) ($bounds.CenterY - 2) 'o'
            Draw-Line $map $bounds.MinX ($bounds.CenterY + 1) ($bounds.MinX - 2) ($bounds.CenterY - 1) 'b'
        }
        'rabbit' { Fill-Rect $map ($bounds.MaxX - 1) ($bounds.CenterY + 2) 3 3 'c' }
        'hamster' { Fill-Rect $map ($center - 6) ($eyeY + 3) 3 2 'm'; Fill-Rect $map ($center + 4) ($eyeY + 3) 3 2 'm' }
    }
}

function Get-AuthoredStagePattern([string]$species, [int]$stage) {
    $key = "$species/$stage"
    switch ($key) {
        'cat/0' { return @(
            '.......oo.......', '.....oocccoo.....', '....occccccco....', '...occcbbccccco...',
            '..occccccccccco..', '..occbccccbccco..', '.occcccccccccccco.', '.occcbbbccccbbbco.',
            '.occcccccccccccco.', '.occbccccccccbcco.', '.occcccbbccccccco.', '.occcccccccccccco.',
            '..occccccccccco..', '..occcbbbccccco..', '...occcccccccco...', '...occcccccccco...',
            '....occccccco....', '.....occccco.....', '......occo.......', '.......oo........'
        ) }
        'dog/0' { return @(
            '.......oo.......', '.....oobbbboo....', '....obbbbbbbbo...', '...obbbbbbbbbbo..',
            '..obbbbccccbbbbo.', '..obbbccccccbbbo.', '.obbbcccececccbbbo', '.obbbccccaccccbbbo',
            '.obbbbbccccbbbbbo.', '.obbbbbbbbbbbbbbo.', '.obbbssbbbbssbbbo.', '.obbbbbbbbbbbbbbo.',
            '..obbbbbbbbbbbbo..', '..obbbbbbbbbbbbo..', '...obbbbbbbbbbo...', '...obbbbbbbbbbo...',
            '....obbbbbbbbo....', '.....obbbbbo.....', '......obbo.......', '.......oo........'
        ) }
        'bird/0' { return @(
            '.......oo.......', '.....oobbbboo....', '....obbbbbbbbo...', '...obbbbbbbbbbo..',
            '..obbbbhhbbbbbo..', '..obbbbbbbbbbbbo.', '.obbbbbcbbbbbbbbo.', '.obbbbbbbbbbbbbbo.',
            '.obbbhbbbbbhbbbbo.', '.obbbbbbbbbbbbbbo.', '.obbbbbbcbbbbbbbo.', '.obbbbbbbbbbbbbbo.',
            '..obbbhbbbbbbbbo..', '..obbbbbbbbbbbbo..', '...obbbbbbbbbbo...', '...obbbbbbbbbbo...',
            '....obbbbbbbbo....', '.....obbbbbo.....', '......obbo.......', '.......oo........'
        ) }
        'penguin/0' { return @(
            '.......oo.......', '.....oossssoo....', '....osssssssso...', '...osssssssssso..',
            '..osssccccsssso..', '..osscccecccssso.', '.osscccececccssso.', '.ossscccccccsssso.',
            '.osssssaaaassssso.', '.osssrrrrrrrrsssso.', '.osssrhrhrhrhsssso.', '.osssssssssssssso.',
            '..ossssccccsssso..', '..ossssccccsssso..', '...osssssssssso...', '...osssssssssso...',
            '....osssssssso....', '.....ossssso.....', '......osso.......', '.......oo........'
        ) }
        'rabbit/0' { return @(
            '.......oo.......', '.....ooccccoo....', '....occcccccco...', '...occcmmccccco..',
            '..occccccccccco..', '..occmccccmccco..', '.occcccccccccccco.', '.occcccmmmmccccco.',
            '.occcccccccccccco.', '.occmccccccccmcco.', '.occcccccccccccco.', '.occcccmmmcccccco.',
            '..occccccccccco..', '..occcmccccccco..', '...occcccccccco...', '...occcccccccco...',
            '....occccccco....', '.....occccco.....', '......occo.......', '.......oo........'
        ) }
        'hamster/0' { return @(
            '.......oo.......', '.....oobbbboo....', '....obbbbbbbbo...', '...obbbbbbbbbbo..',
            '..obbbbsbbbbbbbo.', '..obbbbbbbbbbbbo.', '.obbbbabbbbbabbbo.', '.obbbbbbbbbbbbbbo.',
            '.obbbbbbssbbbbbbbo.', '.obbbbbbbbbbbbbbo.', '.obbbabbbbbabbbbo.', '.obbbbbbbbbbbbbbo.',
            '..obbbbbbbbbbbbo..', '..obbbbsbbbbbbbo..', '...obbbbbbbbbbo...', '...obbbbbbbbbbo...',
            '....obbbbbbbbo....', '.....obbbbbo.....', '......obbo.......', '.......oo........'
        ) }

        'cat/1' { return @(
            '.........oo..oo.....', '........obboobbo....', '.......obbbbbbbbo...',
            '......obbccceccbbo..', '......obbcccacbbo...', '..ooo.obbbbbbbbbbo..',
            '.obboobbbbbbbbbbbbo.', 'obbbbbbbccccccbbbbo.', '.obbbbbbbbbbbbbbbbo.',
            '..obbbbbbbbbbbbbbo..', '...obbbbo..obbbbbo..', '...obbbo....obbbo...',
            '....aa......aa......', '....oo......oo......'
        ) }
        'dog/1' { return @(
            '.......oo....oo.....', '......obbo..obbo....', '.....obbbbbbbbbbo...',
            '....obbbcccecccbbbo.', '....obbccccacccbbbo.', '..ooobbbbbbbbbbbbbo.',
            '.obboobbbbbbbbbbbbo.', 'obbo.obbbccccbbbbo..', '.oo..obbbbbbbbbbo...',
            '.....obbbbbbbbbbo...', '.....obbbbo.obbbbo..', '......obbbo..obbbo..',
            '.......aa....aa.....', '.......oo....oo.....'
        ) }
        'bird/1' { return @(
            '.......oooo.........', '.....oobbbbboo......', '....obbbbbbbbbo.....',
            '...obbbccccebbbo.aa.', '..obbbcccccccbbboaa.', '..obbbccccccccbbbo..',
            '..obbbbccccccbbbbo..', '...obbbbssssbbbbo...', '....obbbbbbbbbbo....',
            '.....obbbbbbbbo.....', '......obbobbbo......', '.......aa.aa........',
            '......ooooooo.......'
        ) }
        'penguin/1' { return @(
            '.......oooo.........', '.....oosssssoo......', '....ossssssssso.....',
            '...ossccccccssso....', '..osscccececcssso...', '..ossccccacccssso...',
            '..osssccccccsssso...', '.osssssrrrrssssssso..', '.ossssrhrhrssssssso..',
            '..ossssssssssssso...', '...osssccccsssso....', '....osssccsssso.....',
            '.....aaa..aaa.......', '......oo..oo........'
        ) }
        'rabbit/1' { return @(
            '.........oo..oo.....', '........ocmoocmo....', '........ocmoocmo....',
            '.......occcccccco...', '......occcceccccco..', '.....occcccacccccco.',
            '....occccccccccccbo.', '..oocccccccccccccbo.', '.occccccccccccccco..',
            'occccccccccccccco...', '.occccccccccccbo....', '..occcco..occcco....',
            '...oooo....oooo.....'
        ) }
        'hamster/1' { return @(
            '......oo......oo....', '.....obbo....obbo...', '....obbbbbbbbbbbbo..',
            '...obbbceccccebbbbo.', '..obbbccccaccccbbbbo', '.obbbbbccccccbbbbbo.',
            'obbbbbbbbbbbbbbbbbo.', 'obbbccccccccccbbbo..', '.obbbbbbbbbbbbbbo...',
            '..obbbbbbbbbbbbo....', '...obbbo..obbbo.....', '....ooo....ooo......'
        ) }

        'cat/2' { return @(
            '.......oo......oo.......', '......obbo....obbo......', '.....obbbbo..obbbbo.....',
            '....obbbbbbbbbbbbbbo....', '...obbbccccccccbbbbo....', '..obbbcccececcccbbbbo...',
            '..obbcccccaacccccbbbo...', '..obbbccccccccccbbbbo...', '...obbbbbbbbbbbbbbo....',
            '..ooobbbbbbbbbbbbbbo...', '.obboobbbbbbbbbbbbbbo...', 'obbo.obbbcccccbbbbbbbo..',
            'obbo.obbbcccccbbbbbbbo..', '.oo..obbbbbbbbbbbbbbo...', '.....obbbbbbbbbbbbbbo...',
            '.....obbbbbo.obbbbbo....', '.....obbbbbo..obbbbbo...', '......obbb....obbbo.....',
            '.......aa....aa.........', '.......oo....oo.........'
        ) }
        'dog/2' { return @(
            '....ooo......ooo.........', '...obbbbo..obbbbo........', '..obbbbbboobbbbbbo.......',
            '..obbbbbbbbbbbbbbbbo.....', '.obbbccccccccccbbbbo.....', '.obbcccececcccccbbbo.....',
            '.obbccccaaaccccccbbbo....', '..obbbccccccccccbbbbo....', '...obbbbbbbbbbbbbbo......',
            '..oobbbbbbbbbbbbbbbbo....', '.obboobbbbbbbbbbbbbbo....', 'obbo.obbbbccccbbbbbbbo...',
            '.oo..obbbbccccbbbbbbbbo..', '.....obbbbbbbbbbbbbbbbo..', '.....obbbbbbbbbbbbbbbbo..',
            '.....obbbbo..obbbbbo.....', '......obbbo..obbbo.......', '......aaa....aaa.........',
            '.......oo....oo..........'
        ) }
        'bird/2' { return @(
            '..........oooo............', '........oobbbbboo..........', '.......obbbbbbbbbo..aa.....',
            '......obbbccccebbboaaa.....', '.....obbbcccccccbbbo.......', '...oobbbbccccccccbbbbo.....',
            '..obbbssssccccccssbbbbo....', '.obbbbssssccccssssbbbbbo...', 'obbbbbssssssssssssbbbbbo...',
            '.obbbbbssssssssssbbbbbo....', '..obbbbbbbbbbbbbbbbbo......', '...obbbbbbbbbbbbbbo........',
            '....obbbbssbbbbbo..........', '.....obbbssbbbbo...........', '......aaa..aaa.............',
            '.....oooooooooo............'
        ) }
        'penguin/2' { return @(
            '.......oooooo........', '.....oossssssoo......', '....osssssssssso.....',
            '...osssccccccssso....', '..ossscccececcssso...', '..osssccccacccssso...',
            '.ossssccccccccsssso..', '.osssssrrrrrrssssso..', '.ossssrhrhrhrssssso..',
            'ossssssssssssssssso..', 'osssssccccccssssssso.', 'ossssccccccccssssso..',
            '.ossssccccccssssso...', '..ossssssssssssso....', '...osssssssssso......',
            '....osss..sssso......', '.....aaa..aaa........', '......oo..oo.........'
        ) }
        'rabbit/2' { return @(
            '......oo......oo.......', '.....ocmo....ocmo......', '.....ocmo....ocmo......',
            '.....ocmo....ocmo......', '.....occo....occo......', '....occcccccccccco.....',
            '...occccececcccccco....', '..occccccaacccccccco...', '..occccccccccccccccbo...',
            '.occcccccccccccccccbo...', '.occcccccccccccccccbo...', '.occcsssccccccssscco....',
            '..occccccccccccccco.....', '...occccccccccccco......', '....occcco..occcco......',
            '.....occo....occo.......', '......aa......aa........'
        ) }
        'hamster/2' { return @(
            '......ooo....ooo.......', '.....obbbboobbbbo......', '....obbbbbbbbbbbbbo....',
            '...obbbccccccccbbbbo...', '..obbbcccececcccbbbbo..', '..obbccccaaacccccbbbo..',
            '.obbbbccccccccccbbbbo..', '.obbbbbbbbbbbbbbbbbbo..', 'obbbbssssssssssssbbbbo.',
            'obbbbssssssssssssbbbbo.', '.obbbbbbbbbbbbbbbbbbo..', '.obbbccccccccccccbbbo..',
            '..obbbccccccccccbbbo...', '...obbbbbbbbbbbbbbo....', '....obbbbo..obbbbbo....',
            '.....obbbo..obbbo......', '......aa....aa.........'
        ) }

        'cat/3' { return @(
            '.......oo........oo.......', '......obbo......obbo......', '.....obbbbo....obbbbo.....',
            '....obbbbbbo..obbbbbbo....', '...obbbbbbbbbbbbbbbbbbo...', '..obbbccccccccccccbbbbo...',
            '..obbcccheccccehccccbbbo..', '..obbcccecccccecccccbbo...', '..obbcccccaaccccccccbbbo..',
            '...obbbccccccccccccbbbo...', '....obbbbbbbbbbbbbbbbo....', '...ooobbbbbbbbbbbbbbbbo...',
            '..obboobbbsssbbbsssbbbbo...', '.obbboobbbssbbbbbssbbbbo..', '.obbo.obbbssbbbbbssbbbbo..',
            'obbo..obbbbbbbbbbbbbbbbo..', 'obbo..obbbccccccccccbbbo..', 'obbo..obbbccccccccccbbbo..',
            '.obbo..obbbbbbbbbbbbbo....', '..oo....obbbbbbbbbbbbo....', '........obbbb..obbbbbo....',
            '........obbbb..obbbbbo....', '.........obb....obbbo.....', '.........aaaa..aaaa.......',
            '..........oo....oo........'
        ) }
        'dog/3' { return @(
            '....oooo........oooo......', '...obbbbo......obbbbo.....', '..obbbbbbo....obbbbbbo....',
            '.obbbbbbbbo..obbbbbbbbo...', '.obbbbbbbbbbbbbbbbbbbbbo..', 'obbbbccccccccccccccccbbbo.',
            'obbbcccccecccccccceccccbbbo', 'obbbccccccccxxccccccccbbbo', 'obbbccccccaaacccccccccbbbo',
            '.obbbccccccccccccccccbbbo.', '..obbbbbbbbbbbbbbbbbbbbo..', '...obbbbbbbbbbbbbbbbbbo...',
            '..ooobbbbbrrrrrrbbbbbbbbo..', '.obboobbbbrrhrrrbbbbbbbbo..', 'obbo.obbbbbbbbbbbbbbbbbbo..',
            'obbo..obbbbbbbbbbbbbbbbbo..', '.oo...obbbbbbbbbbbbbbbbo...', '......obbbbbo..obbbbbo.....',
            '......obbbbbo..obbbbbo.....', '.......obbbbo..obbbbo......', '.......obbbbo..obbbbo......',
            '........aaa....aaa.........', '........aaaa..aaaa.........', '.........oo....oo..........'
        ) }
        'bird/3' { return @(
            'oo..........................oo', 'obbo......................obbo', 'obbbo....................obbbo',
            'obbbo........oooo........obbbo', 'obbbbbo...oobbbbboo...obbbbbo', '..obbbbbbo.obbbbbbbbo.obbbbbbo',
            '..obbbsssbbbbbbbbbbbbsssbbbo..', '...obbbsssbbbccccbbbsssbbbo...', '....obbbssbbccccccbbssbbbo....',
            '.....obbbbbccccccccbbbbbo.....', '......obbbcccceccccebbbo......', '.......obbccccccccccbbbo......',
            '........obcccccaacccbo...aa...', '........obbbbbccccbbbo..aaaa..', '.........obbbbbbbbbbo....aa...',
            '.........obbbbssbbbbo.........', '........obbbbssssbbbbo........', '.......obbbbssssssbbbbo.......',
            '........obbbbo..obbbbo........', '.........aaa......aaa.........', '........aaaa......aaaa........',
            '.......oooooooooooooooo.......'
        ) }
        'penguin/3' { return @(
            '.........a.a.........', '........aaaaa........', '.........aaa.........',
            '.......ooooooo.......', '.....oossssssssoo.....', '....osssssssssssso....',
            '...ossssccccccsssso...', '..osssscccececcssssso..', '..ossssccccacccssssso..',
            '.osssssccccccccssssso.', '.osssssrrrrrrrrssssso.', '.ossssrhrhrhrhrssssso.',
            'osssssssssssssssssssso', 'osssssccccccccssssssso', 'ossssccccccccccssssso.',
            'ossssccccccccccssssso.', '.ossssccccccccssssso..', '.ossssssccccssssssso..',
            '..osssssssssssssso...', '...ossssssssssssso....', '....ossss....sssso....',
            '....osss......ssso....', '.....aaaa....aaaa.....', '......aaa....aaa......',
            '.......oo....oo.......'
        ) }
        'rabbit/3' { return @(
            '......oo......oo......', '.....ocmo....ocmo.....', '.....ocmo....ocmo.....',
            '.....ocmo....ocmo.....', '.....ocmo....ocmo.....', '.....ocmo....ocmo.....',
            '....occcco..occcco....', '...occccccccccccccco...', '..occcccceccccceccccco..',
            '..occcccccccccccccccco.', '..occccccaacccccccccco.', '...occcccccccccccccco..',
            '....occcccccccccccco...', '....occcmmmccmmmccco...', '...occccmmmccmmmccccco..',
            '..occcccccccccccccccco..', '..occcccccccccccccccco..', '...occcccccccccccccco...',
            '....occcccccccccccco....', '.....occcccccccccco.....', '......occco..occco......',
            '......occco..occco......', '.......aaa....aaa.......', '........oo....oo........',
            '...................occo.'
        ) }
        'hamster/3' { return @(
            '.....ooo......ooo.....', '....obbbbo....obbbbo....', '...obbbbbbo..obbbbbbo...',
            '..obbbbbbbbbbbbbbbbbo..', '.obbbccccccccccccbbbo.', '.obbccccececccecccbbbo',
            '.obbccccccccccccccbbbo', '.obbccmmccaaccmmccbbbo', '.obbbccccccccccccbbbbo',
            '..obbbbbccccccbbbbbo..', '.obbbbbbbbbbbbbbbbbbo.', 'obbbbssssssssssssbbbbo',
            'obbbbssssssssssssbbbbo', 'obbbbbbbbbbbbbbbbbbbbo', '.obbbccccccccccccbbbo.',
            '.obbbccccccccccccbbbo.', '..obbbbbbbbbbbbbbbbo..', '...obbbbbbbbbbbbbbo...',
            '....obbbbbo..obbbbbo..', '....obbbbbo..obbbbbo..', '.....obbb....obbb.....',
            '......aaa....aaa......', '.......oo....oo.......'
        ) }
        default { throw "Missing authored stage pattern for $key" }
    }
}

function Get-HandDrawnStagePattern([string]$species, [int]$stage) {
    return Get-PixelPetReferenceMaster $species $stage
<#+
    # These cels are authored directly as semantic pixels. They deliberately
    # keep the source-board proportions: compact eggs and babies, readable
    # young pets, and detailed adult silhouettes. No bitmap is sampled here.
    $key = "$species/$stage"
    switch ($key) {
        # Eggs are six independent semantic masters. Their shell silhouette,
        # markings, highlight and visual weight are authored per species rather
        # than generated from one shared oval.
        'cat/0' { return @(
            '........oooo........', '......ooccccoo......', '.....occcccccco.....',
            '....occcbbbccccco....', '...occcbbbccccccco...', '..occccbbccccccccco..',
            '..ocbccccccccccbccco..', '.ocbbccccccccccbbccco.', '.ocbbbccccccccbbbccco.',
            '.occcccccccccccccccco.', '.occcccbbbcccccccccco.', '.occccbbccccbbbccccco.',
            '.occcccccccccccccccco.', '.occbccccccccccccbcco.', '..occccccccccccccco..',
            '..occcccbbbccccccco..', '...occcccccccccccco...', '....occccccccccco....',
            '.....occcccccco.....', '......occcccco......', '........oooo........'
        ) }
        'dog/0' { return @(
            '.......oooooo.......', '.....ooccccccoo.....', '....occcccccccco....',
            '...occcbbbbbbccco...', '..occcbbssssbbccco..', '.occcbbssssssbbccco.',
            '.occbbbccccccbbbcco.', 'ocbbbccccececccbbbco', 'ocbbbccccxxccccbbbco',
            'ocbbbccccaaaccccbbbo', 'occcccbbbbbbbbccccco', 'occcccccbbbbccccccco',
            'occcccccccccccccccco', '.occcccbbbbbbccccco.', '.occcccccccccccccco.',
            '..occcccccccccccco..', '...occcccccccccco...', '....occcccccccco....',
            '......occcccco......', '........oooo........'
        ) }
        'bird/0' { return @(
            '.........oooo.........', '.......oobbbboo.......', '......obbbbbbbbo......',
            '.....obbbbhbbbbbo.....', '....obbbbbbbbbbbbo....', '...obbbbrbbbbbbbbbo...',
            '...obbbbbbbbbbsbbbo...', '..obbbhbbbbbbbbbbbbo..', '..obbbbbbrbbbbbbbbbo..',
            '..obbbbbbbbbbbbbbbbo..', '..obbbhbbbbbhbbbbbo...', '..obbbbbbbbbbbbbbbbo..',
            '..obbbbbbrbbbbbbbbbo..', '...obbbbbbbbbbbbbbo...', '...obbbhbbbbbbbbbo....',
            '....obbbbbbbbbbbbo....', '.....obbbbbbbbbbo.....', '......obbbbbbbbo......',
            '.......obbbbbbo.......', '........obbbbo........', '.........oooo.........'
        ) }
        'penguin/0' { return @(
            '........oooo........', '......oosssssoo......', '.....ossssssssso.....',
            '....osssssssssssso....', '...osssccccccsssso...', '..osssccchhcccsssso..',
            '..ossccccececcccssso..', '.ossscccccccccccsssso.', '.ossssccccaccccssssso.',
            '.osssssaaaacccssssso.', '.osssssrrrrrrssssssso.', '.ossssrhrhrhrhrssssso.',
            '.osssssrrrrrrssssssso.', '.ossssssssssssssssso.', '..ossssccccccssssso..',
            '..ossssccccccssssso..', '...ossssssssssssso...', '....osssssssssso....',
            '.....osssssssso.....', '......ossssso......', '........oooo........'
        ) }
        'rabbit/0' { return @(
            '.........oo.........', '.......oocccoo.......', '......occcccco......',
            '.....occcmmmccco.....', '....occcmmmmmccco....', '...occcccmccccccco...',
            '..occmcccccccccmcco..', '..occcccmmmmcccccco..', '.occcccmmmmmmcccccco.',
            '.occcccmmmmmmcccccco.', '.occcccccmmccccccccco.', '.occmccccccccccccmcco.',
            '.occcccccccccccccccco.', '.occcccmmmcccccccccco.', '..occcccmccccccccco..',
            '..occccccccccccccco..', '...occcccccccccccco...', '....occccccccccco....',
            '.....occcccccco.....', '......occcccco......', '........oooo........'
        ) }
        'hamster/0' { return @(
            '.......oooooo.......', '.....oobbbbbboo.....', '....obbbbbbbbbbo....',
            '...obbbbsssbbbbbo...', '..obbbbssssbbbbbbbo..', '.obbbbbbbbbbbbbbbbbo.',
            'obbbbabbbbbbbbabbbbbo', 'obbbbbbbbbbbbbbbbbbbo', 'obbbbbaaabbbbaaabbbbo',
            'obbbbbbbbbbbbbbbbbbbo', 'obbbbbbbssssbbbbbbbbo', 'obbbbbbbbbbbbbbbbbbbo',
            '.obbbabbbbbbbbbabbbo.', '.obbbbbbbssbbbbbbbbo.', '.obbbbbbbbbbbbbbbbbo.',
            '..obbbbbbbbbbbbbbo..', '...obbbbbbbbbbbbo...', '....obbbbbbbbbbo....',
            '......obbbbbbo......', '........oooo........'
        ) }

        # Babies are complete species-specific bodies, not scaled adult cels.
        # Large heads, short limbs and juvenile markings remain explicit in
        # the source rows so their silhouette stays readable on the home card.
        'cat/1' { return @(
            '.......oo......oo.......', '......obbo....obbo......', '.....obbbo....obbbo.....',
            '....obbbbbbbbbbbbbo.....', '...obbbccccccccbbbbo....', '..obbbccceeecccccbbbo...',
            '..obbccccececcccccbbbo..', '..obbccccaaaccccccbbbo..', '..obbbccccccccccccbbbo..',
            '...obbbbccccccccbbbbo...', '....obbbbbbbbbbbbbbo....', '....obbbbbbbbbbbbbbo.oo.',
            '...obbbbccccccbbbbboobbo', '..obbbbbccccccbbbbbbbbbo.', '.obbbbbbbbbbbbbbbbbbbbo..',
            'obboobbbbbbbbbbbbbbbbo...', 'obbo.obbbbbbbbbbbbbbo....', '.oo..obbbbbo..obbbbbo....',
            '.....obbbbo....obbbbo....', '......obbo......obbo.....', '......aaa........aaa.....',
            '.......oo........oo......'
        ) }
        'dog/1' { return @(
            '.....oooo........oooo.....', '....obbbbo......obbbbo....', '...obbbbbo......obbbbbo...',
            '....obbbbbbbbbbbbbbbbo....', '...obbbccccccccccbbbbo...', '..obbbccceeecccccccbbbo..',
            '..obbccccececcccccccbbbo..', '..obbccccaaaccccccccbbbo..', '...obbbccccccccccccbbbo...',
            '....obbbbbbbbbbbbbbbbo....', '....obbbbbbbbbbbbbbbbo....', '...obbbbccccccccbbbbbo.oo.',
            '..obbbbbccccccccbbbbboobbo', '.obbbbbbbbbbbbbbbbbbbbbbo.', 'obbbbbbbbbbbbbbbbbbbbbbbo.',
            'obboobbbbbbbbbbbbbbbbbbo..', '.oo..obbbbbbbbbbbbbbbbo...', '.....obbbbbo...obbbbbo....',
            '......obbbo.....obbbo.....', '......obbo.......obbo.....', '.......aaa.......aaa......',
            '........oo.......oo.......'
        ) }
        'bird/1' { return @(
            '..........oooo..........', '........oobbbbboo........', '.......obbbbbbbbbo.......',
            '......obbbbeccbbbbo......', '.....obbbceeecccbbbbo....', '....obbbccccacccccbbbo...',
            '...obbbccccccccccccbbbo..', '..obbbbcccccccccccbbbbbo..', '.obbbbbssscccccccssbbbbbo.',
            'obbbbbssssccccccssssbbbbbo', '.obbbbbssscccccccssbbbbbo.', '..obbbbbssssssssbbbbbbbo..',
            '...obbbbbbbbbbbbbbbbbo....', '....obbbbbbbbbbbbbbo......', '.....obbbbbbbbbbbbo.......',
            '......obbbbobbbbbo........', '.......obbo.obbbo.........', '.......aaa...aaa..........',
            '........oo...oo...........'
        ) }
        'penguin/1' { return @(
            '........oooooo........', '......oossssssoo......', '.....osssssssssso.....',
            '....osssccccccssso....', '...osssccceeeccsssso...', '..osssccccececccsssso..',
            '..osssccccaaaccccsssso..', '.osssssccccccccccssssso.', 'osssssssccccccccssssssso',
            'osssssssccccccccssssssso', '.osssssssccccccssssssso.', '..osssssrrrrrrrrssssso..',
            '...ossssrhrhrhrssssso...', '...osssssssssssssssso...', '....osssssccccssssso....',
            '.....ossssccccsssso.....', '......osss....ssso......', '......osso....osso......',
            '.......aaa....aaa.......', '........oo....oo........'
        ) }
        'rabbit/1' { return @(
            '........oo....oo........', '.......ocmo..ocmo.......', '.......ocmo..ocmo.......',
            '.......ocmo..ocmo.......', '.......ocmo..ocmo.......', '......occcooocccco......',
            '.....occcccccccccco.....', '....occcceeeccccccco....', '...occcccececcccccccco...',
            '..occcccccacccccccccco..', '.occcccccccccccccccccco.', 'occcccccccccccccccccccco',
            'occcccccccccccccccccccco', '.occccccccccccccccccccbo', '..occcccccccccccccccbbo.',
            '...occcccccccccccccbo...', '....occcccccccccccbo....', '.....occcco..occcco.....',
            '......occo....occo......', '......aaa......aaa......', '.......oo......oo.......'
        ) }
        'hamster/1' { return @(
            '.......ooo......ooo.......', '......obbo......obbo......', '.....obbbbo....obbbbo.....',
            '....obbbbbbbbbbbbbbbo.....', '...obbbccccccccccbbbbo....', '..obbbccceeecccccccbbbo...',
            '.obbbbcccececcccccccbbbbo.', 'obbbbbccccaaacccccccbbbbbo', 'obbbbbcccmmmmmmccccbbbbbo.',
            'obbbbbccccmmmmccccccbbbbo.', 'obbbbbbcccccccccccbbbbbo..', '.obbbbbbbbbbbbbbbbbbbbbo..',
            '..obbbbccccccccccbbbbbo...', '...obbbccccccccccbbbbo....', '....obbbbbbbbbbbbbbo......',
            '.....obbbbbbbbbbbbo.......', '......obbbo..obbbo........', '......obbo....obbo........',
            '.......aaa....aaa.........', '........oo....oo..........'
        ) }

        # Checked-in semantic pixel masters. These rows are already fitted to
        # the 30x26 safe artboard, so generation never reads or scales a bitmap.
        'cat/2' { return @(
            '.......oooo......oooooo',
            '.......obaaoooooossaaao',
            '.......obaaseeeessasaao',
            '.......oaaaaasasaaaaaao',
            '.......oaaaaaaaaaaaaaao',
            '......oeaaaaaaaaaaaaaao',
            '......ossaaaaaaaaaaaaao',
            '......ossaassaaaaaasaao',
            '......osssaebcaaaaesaco',
            '.....osessaeeaccacsesao',
            '.....oeeeaacccbbsaccaao',
            '.oo...oesacccccccccccco',
            'oee....ossccccccccccao.',
            'occ.....oesaccccccaseo.',
            'obco...osssaaaaaaaseo..',
            'osao...oeaaaaaaaaaaeo..',
            'ossbooeassaaacccccbo...',
            'osshcssaaaaaaccccaso...',
            'oeacbsasaaaaaccccaso...',
            '.oaessaaaaaaaaacsaso...',
            '.ossesaaaasaaabcaao....',
            '..obesaaaasaaaseaso....',
            '...oeeacaseaabeecao....',
            '..ooabesaaescbsesseaaao',
            '...oobbeeeseeesbeebooo.',
            '.....oooooooooooooo....'
        ) }
        'dog/2' { return @(
            '.........o...oooooooo..o..',
            '.........o..osaaaaaaao.o..',
            '.........o.oaaaaaaaaaaoo..',
            '.........oaaaaaaaaaaaaao..',
            '.........oaaaaacaaacaaaso.',
            '........oaaaaaaaaaaaasaseo',
            '.......oeaaaaebsaaaaabsaso',
            '...oo..osaaabeescccbseaaso',
            '...bboobsaacaababssbaaaaso',
            '..ocaabeeacccccccsecccccbo',
            'obcseacsbechchhbsseebccbo.',
            'obcsebsbbeccccccsssscccbo.',
            'obaseeeesseaccccbeehccbo..',
            '.osasesaaasesaacbcccase...',
            '..oaaaaaaaaassaaaaaasse...',
            '...bsaaaaaaaaacaaacccae...',
            '...osaaaaaaaaachhhhcabo...',
            '...osaaaaaaaaaachhcaao....',
            '...osaaaaaaaaaaahccaao....',
            '....oaaasaasaaasccasao....',
            '....oaasesasaasesssaao....',
            '....oaaesbseaasessaaso....',
            '...oebasbbseaasessaaso....',
            '...bssssbbbbsccbbssbssbo..',
            '...oobbbbbbbssssbbsesoo...',
            '.....oooooooooooooooo.....'
        ) }
        'bird/2' { return @(
            '..........oooooo......',
            '.........osrrrrboo....',
            '........osbbbbbbro....',
            '........obbbbbbbbbo...',
            '.......osbbbbbbbbebo..',
            '......oesbbbsbbbbebo..',
            '......oebbbbsbsaabbso.',
            '......oebbbbbbsasbbso.',
            '.....obeesbbbbbsbbbso.',
            '.....ossbbsbbbbbbbso..',
            '...osbsbbbecccaccceeo.',
            '..oessbbsbeccccccceeo.',
            '.o.ssbssbbacccccccseo.',
            'oeoesebbsscccccccaeo..',
            'osseeeseeacccccccashoo',
            'oooeeeeeeaccccccaehbbo',
            '........oeaaaasebbsseo',
            '........oeeeeesbeeeeso',
            '.....oooeeseeeeseesseo',
            '...ooessseeeeeeeobeeso',
            '...eeseooooooooo.ooooo',
            '...seoo...............',
            '...eo.ooooooooooo.....',
            '.obaaaaaaaaaaaaaaaaao.',
            '..oooaaaaaaaaaaaaoooo.',
            '.....oooooooooooo.....'
        ) }
        'penguin/2' { return @(
            '....oooooooooooooo.....',
            '...oeeeeeeeeeeeeeo.....',
            '...oeeeeeeeeeeeeeeo....',
            '..oerccbeeebcccceeeo...',
            '..obccccbebcccccbeeo...',
            '..occbccbebccbbcbeeo...',
            '..ocseecbebcbeecceeo...',
            '..ocbcbbaaaccccbceeeo..',
            '..obbbcbaaacccbbceeo...',
            '..obbbccaeccccbbceeeo..',
            '..oeebccccccseesesreo..',
            '..orrsesseseersersreo..',
            '.oerrssrrsrssrrrrsseo..',
            'oeeesesrssrssrseseseeo.',
            'oeerseessssseeeseeeeeeo',
            'oeebcrrsssssesrrseeeeee',
            'oeerccccccccrssrbeeeeee',
            'oesbccccccccrsrrbeeeeoo',
            '.ocsccccccccreeebeeso..',
            '..oebcccccccrrssseeso..',
            '...eecccccccccccseeo...',
            '...oeesccccccccbeeso...',
            '...oseesssssssseeoo....',
            '...ssssseboosbbbsoo....',
            '...ooooooo..obsssso....',
            '.............ooooo.....'
        ) }
        'rabbit/2' { return @(
            'ooo........ooooo.......',
            'oss.......osssss.......',
            'ossoo.....essbso.......',
            'osssso..oosaaaso.......',
            '.ossso..oeaaaao........',
            '.ossseoossaaaso........',
            '.osssebhessbso.........',
            '..oesssssssaeo.........',
            '...eshhbssseo..........',
            '..obhhcsssso...........',
            '.oschhssesssesoo.......',
            'oeehhhsssessebssoo.....',
            'oeshhhbseessehhsso.....',
            'obhcchcbssssshhbsso....',
            'ochachhhbbbbbhhcssso...',
            'obhbhhhhhhhbhhhcssso...',
            '.oebbbbbbbchhhhhsssseo.',
            '.oebbbbbcbhhhhhbsssseso',
            '.oehhhhhhhhhhhcssssseso',
            '..ochhhhhhhhhhbssssseso',
            '...bchhhhhhchhbssssseo.',
            '...obchhcchbccbssssseo.',
            '...sbebbsccbbbsessseo..',
            'oaasssssccssasbbssso...',
            'ooosessseesaaseeoooo...',
            '...ooooooooooooo.......'
        ) }
        'hamster/2' { return @(
            '....oo........ooo......',
            '....oeo.......oeo......',
            '...obasooooooosbbo.....',
            '...oaaaassaaaassbso....',
            '...osaaaaaaaaaasbo.....',
            '...osaaaaaaaaaaso......',
            '...obaaaaaaaaaaaso.....',
            '..oseeaaaasesaaaso.....',
            '..osesbaaaseeaaaaeo....',
            '..obeeaaaaeeeaaaaeo....',
            '..occccbccccccccaso....',
            '..occbcbcchcccccaaso...',
            '..occbbbbbhcccccaaso...',
            '...bachhhhcccccaaaaso..',
            '...osbbccccccccbaaaso..',
            '...obbbbbbaccccbaaaso..',
            '...sbcbcccbaccaaaaasso.',
            '...ebcasccbccbaaaaaaseo',
            '..oesaasccsabsaaaaaaseo',
            '..obbsscccsesccbaaaaseo',
            '...baachcccccccbaaaaseo',
            '...baachcccccccbaaaasso',
            '..ooaaaccchccabbaaasebo',
            'obbsbbaseeeeesccssessbo',
            'ooosssessssssessesoooo.',
            '...ooooooooooooooo.....'
        ) }

        'cat/3' { return @(
            '..........ooo.....oooo.',
            '..........obs.....bbeo.',
            '..........oaaoooooaaso.',
            '.........osaaaasssasso.',
            '.........osaaaasassaso.',
            '.........osaacaaasaaabo',
            '........osaaabcaaaaaaso',
            '.oo.....oaaascebccbbaeo',
            'ochao...baaasccccccaase',
            'obcsso..ssssacccsabccbs',
            '.osseo..sbsaccccccccbso',
            '.osaso.occcbsccccccaso.',
            '.obaso..bccccaaaaasaao.',
            'ossso.ossssaccccccccbeo',
            '.osso.osasasaacccccbbo.',
            'obashoescasaasaccccbso.',
            'ocahcsaaaaaccaaccccaso.',
            'osshbeaasaaaacacccasso.',
            'oaaseaaaaaasaaasaasso..',
            'oasssaaaaaaaaassaaso...',
            '.ossessaaaaasassseso...',
            '..osesaaaasassseeaeo...',
            '..osesaasaessaassbbso..',
            '..obbbssssssbssssbsbo..',
            '...oooobbbbbbbbbbbooo..',
            '.......ooooooooooo.....'
        ) }
        'dog/3' { return @(
            '....oooo........oooo......', '...obbbbo......obbbbo.....', '..obbbbbbo....obbbbbbo....',
            '.obbbbbbbbo..obbbbbbbbo...', '.obbbbbbbbbbbbbbbbbbbbbo..', 'obbbbccccccccccccccccbbbo.',
            'obbbcccccecccccccceccccbbbo', 'obbbccccccccxxccccccccbbbo', 'obbbccccccaaacccccccccbbbo',
            '.obbbccccccccccccccccbbbo.', '..obbbbbbbbbbbbbbbbbbbbo..', '...obbbbbbbbbbbbbbbbbbo...',
            '..ooobbbbbrrrrrrbbbbbbbbo..', '.obboobbbbrrhrrrbbbbbbbbo..', 'obbo.obbbbbbbbbbbbbbbbbbo..',
            'obbo..obbbbbbbbbbbbbbbbbo..', '.oo...obbbbbbbbbbbbbbbbo...', '......obbbbbo..obbbbbo.....',
            '......obbbbbo..obbbbbo.....', '.......obbbbo..obbbbo......', '.......obbbbo..obbbbo......',
            '........aaa....aaa.........', '........aaaa..aaaa.........', '.........oo....oo..........'
        ) }
        'bird/3' { return @(
            '...o........................',
            '..oo.....................oo.',
            '..obo...................obo.',
            '.obsbbboo......o.o.....obbo.',
            '.obbbbbbbo....oooso....sbso.',
            '.osbbssssboo.obbbbo...obso.o',
            'obbbbbsssbbo..osssbooosbbboo',
            '.osssbbbbbbboossbbbrbsbsbbo.',
            '.obbbbssrbbbo.osbrrrresssbo.',
            '..osbbbbbbbbeo.obbsrbessbbo.',
            '...bbbbbsbsbbbosrrrbassssbo.',
            '...bssbbsssbbbbsrrrbbesssso.',
            '...obbbsssssbbbbbbrrrsssbo..',
            '....oossbbsssbsbbbbrbssbo...',
            '......obsssbsssbrbbbbebo....',
            '......oosbbsbssbrrrbbeo.....',
            '..........hhesbbbbbso.......',
            '..........osesbbbssoo.......',
            '.........osssessseo.........',
            '......ooobbbssseeso.........',
            '....oobbbbsssssbsso.........',
            '.....ooesbbsssssbso.........',
            '.......obbsbssssbsboo.......',
            '....oobbbbsbbbbbbbbbbbo.....',
            '......ooobbbbbbbbbbooo......',
            '.........oooooooooo.........'
        ) }
        'penguin/3' { return @(
            '.........a.a.........', '........aaaaa........', '.........aaa.........',
            '.......ooooooo.......', '.....oossssssssoo.....', '....osssssssssssso....',
            '...ossssccccccsssso...', '..osssscccececcssssso..', '..ossssccccacccssssso..',
            '.osssssccccccccssssso.', '.osssssrrrrrrrrssssso.', '.ossssrhrhrhrhrssssso.',
            'osssssssssssssssssssso', 'osssssccccccccssssssso', 'ossssccccccccccssssso.',
            'ossssccccccccccssssso.', '.ossssccccccccssssso..', '.ossssssccccssssssso..',
            '..osssssssssssssso...', '...ossssssssssssso....', '....ossss....sssso....',
            '....osss......ssso....', '.....aaaa....aaaa.....', '......aaa....aaa......',
            '.......oo....oo.......'
        ) }
        'rabbit/3' { return @(
            '......oo......oo......', '.....ocmo....ocmo.....', '.....ocmo....ocmo.....',
            '.....ocmo....ocmo.....', '.....ocmo....ocmo.....', '.....ocmo....ocmo.....',
            '....occcco..occcco....', '...occccccccccccccco...', '..occcccceccccceccccco..',
            '..occcccccccccccccccco.', '..occccccaacccccccccco.', '...occcccccccccccccco..',
            '....occcccccccccccco...', '....occcmmmccmmmccco...', '...occccmmmccmmmccccco..',
            '..occcccccccccccccccco..', '..occcccccccccccccccco..', '...occcccccccccccccco...',
            '....occcccccccccccco....', '.....occcccccccccco.....', '......occco..occco......',
            '......occco..occco......', '.......aaa....aaa.......', '........oo....oo........',
            '...................occo.'
        ) }
        'hamster/3' { return @(
            '....oo.......oooo.....',
            '...ossooo...oesseo....',
            '...eaassbooobsbabs....',
            '...obaaassaaaasaas....',
            '...csaaaaaaaaassso....',
            '...osaaaaaaaaaaseo....',
            '...essaaaabbaaaase....',
            '..osesaaaabesaaase....',
            '.obaesaaaasesaaaas....',
            '.oeaacaacaaaaccaaso...',
            '.oecccbaccccccccaso...',
            '..occbcscchcccccaaeo..',
            '..occbbbbchccccaaaeo..',
            '...esabcccccacaaaaso..',
            '...saaaaaaccccaaaaso..',
            '...saaaccccaccaaaasso.',
            '...sccaccccacaaaaasso.',
            '...sbcascbbccaaaaaaseo',
            '..oaasscccseecaaaaaseo',
            '...aaachccccccaaaaaseo',
            '...aaachccccccaaaaaseo',
            '..osaaaccccccaaaaaseso',
            'ooassaaaaaaaaaaaaaseeo',
            'ooaesssssssseccbsebaoo',
            '..oooosoaaaooooooooo..',
            '......o.ooo...........'
        ) }
        default { throw "Missing hand-drawn stage pattern for $key" }
    }
#>
}

function Read-StageMaster([string]$species, [int]$stage) {
    try {
        return New-AuthoredFrameFromPattern (Get-HandDrawnStagePattern $species $stage)
    } catch {
        throw "Invalid authored pattern for $species/$stage`: $($_.Exception.Message)"
    }
}

function Draw-AuthoredEggHatching($map, [int]$frame) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $centerX = $bounds.CenterX
    $topY = $bounds.MinY
    # Six authored crack cels are placed relative to each species shell, so a
    # tall bird egg and a broad hamster egg keep their own silhouette.
    switch ($frame) {
        0 {
            Put-Cell $map ($centerX + 3) ([Math]::Max(1, $topY - 1)) 'h'
        }
        1 {
            Draw-Line $map $centerX ($topY + 2) ($centerX - 1) ($topY + 6) 'o'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 8) 'h'
        }
        2 {
            Draw-Line $map $centerX ($topY + 2) ($centerX - 1) ($topY + 7) 'o'
            Draw-Line $map ($centerX - 1) ($topY + 7) ($centerX + 2) ($topY + 9) 'o'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 7) 'c'
            Put-Cell $map ($bounds.MaxX + 2) ($topY + 10) 's'
        }
        3 {
            Draw-Line $map $centerX ($topY + 1) ($centerX - 2) ($topY + 7) 'o'
            Draw-Line $map ($centerX - 2) ($topY + 7) ($centerX + 2) ($topY + 11) 'o'
            Draw-Line $map ($centerX + 2) ($topY + 11) ($centerX + 1) ($topY + 14) 'o'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 6) 'c'
            Put-Cell $map ($bounds.MaxX + 2) ($topY + 9) 's'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 13) 'h'
        }
        4 {
            for ($x = $centerX - 1; $x -le $centerX + 1; $x++) { Remove-Cell $map $x $topY }
            Draw-Line $map $centerX ($topY + 1) ($centerX - 3) ($topY + 8) 'o'
            Draw-Line $map ($centerX - 3) ($topY + 8) ($centerX + 2) ($topY + 12) 'o'
            Fill-Rect $map ($centerX - 2) ([Math]::Max(1, $topY - 2)) 2 2 'c'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 5) 'c'
            Put-Cell $map ($bounds.MaxX + 2) ($topY + 9) 's'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 14) 'h'
        }
        default {
            for ($y = $topY; $y -le $topY + 1; $y++) {
                for ($x = $centerX - 2; $x -le $centerX + 2; $x++) { Remove-Cell $map $x $y }
            }
            Draw-Line $map $centerX ($topY + 1) ($centerX - 4) ($topY + 9) 'o'
            Draw-Line $map ($centerX - 4) ($topY + 9) ($centerX + 2) ($topY + 14) 'o'
            Fill-Rect $map ($centerX - 2) ([Math]::Max(1, $topY - 3)) 3 2 'h'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 4) 'c'
            Put-Cell $map ($bounds.MaxX + 2) ($topY + 8) 's'
            Put-Cell $map ($bounds.MaxX + 1) ($topY + 13) 'h'
            Put-Cell $map $bounds.MaxX ($topY + 16) 'c'
        }
    }
}

function Draw-EggActionOriginal($map, [string]$species, [int]$action, [int]$frame) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $count = $frameCounts[$action]
    $phase = $frame % $count
    $centerX = $bounds.CenterX
    $topY = $bounds.MinY
    switch ($action) {
        0 { # A calm eight-cel sway with a moving shell glint.
            Translate-Frame $map @(-1, 0, 1, 1, 0, -1, -1, 0)[$phase] 0
            Put-Cell $map ($bounds.MinX + 2 + $phase) ([Math]::Max(1, $topY - 1)) 'h'
        }
        1 { # Eggs travel with short rolls rather than borrowed walking feet.
            $roll = @(-2, -1, 0, 1, 2, 2, 1, 0, -1, -2)[$phase]
            Translate-Frame $map $roll 0
            Put-Cell $map (Clamp-Grid ($centerX - 5 + $phase)) (27 + ($phase % 2)) 's'
        }
        2 { # Feeding before hatch is represented as warmth entering the shell.
            $pulseX = Clamp-Grid ($bounds.MinX - 1 + $phase)
            $pulseY = [Math]::Max(1, $topY + 2 + ($phase % 4))
            Put-Cell $map $pulseX $pulseY 'a'
            Put-Cell $map (Clamp-Grid ($pulseX + 1)) ([Math]::Max(1, $pulseY - 1)) 'h'
        }
        3 { # Happy bounce keeps a ground contact marker under the shell.
            $jump = @(0, -1, -2, -3, -2, -1, 0, -1, -2, 0)[$phase]
            Translate-Frame $map 0 $jump
            Fill-Rect $map ($centerX - 2 - ($phase % 2)) 29 (5 + ($phase % 2) * 2) 1 's'
            Put-Cell $map (Clamp-Grid ($bounds.MaxX + 1)) ([Math]::Max(1, $topY - 1 + ($phase % 3))) 'r'
        }
        4 { # A sleeping egg settles to one side with a moving Z trail.
            Translate-Frame $map @(-1, -1, 0, 0, 1, 1, 0, -1, 0, 1)[$phase] 0
            Put-Cell $map (Clamp-Grid ($bounds.MaxX + 1 + ($phase % 3))) ([Math]::Max(1, $topY - 1 - [Math]::Floor($phase / 3))) 'h'
        }
        5 { # Curious wobble and a question-like two-pixel signal.
            Translate-Frame $map @(-1, 0, 1, 2, 1, 0, -1, -2, -1, 0)[$phase] 0
            $signalX = Clamp-Grid ($bounds.MaxX + 1 + ($phase % 2))
            $signalY = [Math]::Max(1, $topY + ($phase % 5))
            Put-Cell $map $signalX $signalY 'r'
            Put-Cell $map $signalX (Clamp-Grid ($signalY + 2)) 'h'
        }
        6 { Draw-AuthoredEggHatching $map $frame }
        7 {
            Translate-Frame $map @(0, -2, 2, -1, 1, 0)[$phase] 0
            Draw-Line $map $centerX ($topY + 3) ($centerX + $(if ($phase % 2 -eq 0) { -1 } else { 1 })) ($topY + 7) 'o'
            Put-Cell $map (Clamp-Grid ($bounds.MinX - 1)) ($topY + $phase) 'r'
        }
        8 {
            Translate-Frame $map @(-1, -1, 0, 1, 1, 0)[$phase] 0
            Put-Cell $map (Clamp-Grid ($bounds.MinX - 1 + $phase)) ([Math]::Max(1, $topY - 1)) 'm'
            Put-Cell $map (Clamp-Grid ($bounds.MinX + $phase)) ([Math]::Max(1, $topY - 2)) 'm'
        }
        9 {
            $hop = @(0, -1, -3, -2, -1, 0)[$phase]
            Translate-Frame $map 0 $hop
            Fill-Rect $map ($centerX - 2) 29 5 1 's'
            Put-Cell $map (Clamp-Grid ($bounds.MaxX + 1 - $phase)) (27 - ($phase % 2)) 'a'
        }
        10 {
            Translate-Frame $map @(0, 1, 1, 0, -1, 0)[$phase] 0
            Put-Cell $map (Clamp-Grid ($bounds.MaxX + 1)) ([Math]::Max(1, $topY - 1 + $phase)) 'r'
        }
        11 {
            $sparkX = Clamp-Grid ($bounds.MinX - 1 + $phase * 2)
            $sparkY = [Math]::Max(1, $topY + 1 + ($phase % 3) * 3)
            Put-Cell $map $sparkX $sparkY 'h'
            Put-Cell $map (Clamp-Grid ($sparkX + 1)) $sparkY 'r'
        }
        12 {
            $waveX = Clamp-Grid ($bounds.MaxX + 1 + ($phase % 3))
            $waveY = [Math]::Max(1, $topY + 2 + $phase)
            Put-Cell $map $waveX $waveY 'a'
            Put-Cell $map (Clamp-Grid ($waveX + 1)) ([Math]::Max(1, $waveY - 1)) 'a'
        }
    }
}

function Convert-ToBabySleepPose($map) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $resting = New-FrameMap
    foreach ($entry in $map.GetEnumerator()) {
        $parts = $entry.Key.Split(',')
        $x = [int]$parts[0]
        $y = [int]$parts[1]
        $distance = [Math]::Max(0, $bounds.CenterY - $y)
        $newY = $y + [int][Math]::Floor($distance * 0.34)
        Put-Cell $resting $x $newY $entry.Value
    }
    $map.Clear()
    foreach ($entry in $resting.GetEnumerator()) { $map[$entry.Key] = $entry.Value }
}

function Draw-BabySpeciesMotion($map, [string]$species, [int]$action, [int]$facing, [int]$phase) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $direction = if ($facing -eq 2) { -1 } else { 1 }
    $motion = if ($phase % 2 -eq 0) { -1 } else { 1 }
    switch ($species) {
        'cat' {
            Draw-Line $map ($bounds.CenterX + 6 * $direction) ($bounds.CenterY + 4) ($bounds.CenterX + (8 + $motion) * $direction) ($bounds.CenterY + 1 - ($phase % 2)) 'b'
            if ($action -in 0, 3, 8 -and $facing -ne 1) { Put-Cell $map ($bounds.CenterX - 3) ($bounds.MinY + 2 + ($phase % 2)) 'h' }
        }
        'dog' {
            Fill-Rect $map ($bounds.CenterX - 7) ($bounds.MinY + 3 + ($phase % 2)) 2 3 's'
            Fill-Rect $map ($bounds.CenterX + 6) ($bounds.MinY + 3 + (($phase + 1) % 2)) 2 3 's'
            Draw-Line $map ($bounds.CenterX + 6 * $direction) ($bounds.CenterY + 5) ($bounds.CenterX + (8 + $motion) * $direction) ($bounds.CenterY + 3) 'b'
        }
        'bird' {
            $wingLift = if ($action -in 1, 3, 9) { 3 + ($phase % 3) } else { 2 }
            Fill-Rect $map ($bounds.CenterX - 7) ($bounds.CenterY - $wingLift) 3 (4 + $phase % 2) 'r'
            Fill-Rect $map ($bounds.CenterX + 5) ($bounds.CenterY - $wingLift) 3 (4 + ($phase + 1) % 2) 'r'
        }
        'penguin' {
            $flipperY = $bounds.CenterY - 1 - $(if ($action -in 1, 3, 9) { $phase % 3 } else { 0 })
            Draw-Line $map ($bounds.CenterX - 6) $flipperY ($bounds.CenterX - 8) ($flipperY + 4) 's'
            Draw-Line $map ($bounds.CenterX + 6) $flipperY ($bounds.CenterX + 8) ($flipperY + 4) 's'
        }
        'rabbit' {
            $earLean = if ($action -in 4, 7) { 2 } else { $motion }
            Draw-Line $map ($bounds.CenterX - 4) ($bounds.MinY + 5) ($bounds.CenterX - 5 + $earLean) ([Math]::Max(1, $bounds.MinY - 1)) 'c'
            Draw-Line $map ($bounds.CenterX + 3) ($bounds.MinY + 5) ($bounds.CenterX + 4 + $earLean) ([Math]::Max(1, $bounds.MinY - 1)) 'c'
            Fill-Rect $map ($bounds.CenterX - 5 + $earLean) ([Math]::Max(1, $bounds.MinY)) 1 4 'm'
            Fill-Rect $map ($bounds.CenterX + 4 + $earLean) ([Math]::Max(1, $bounds.MinY)) 1 4 'm'
        }
        'hamster' {
            if ($facing -ne 1) {
                Put-Cell $map ($bounds.CenterX - 5) ($bounds.MinY + 7 + ($phase % 2)) 'm'
                Put-Cell $map ($bounds.CenterX + 5) ($bounds.MinY + 7 + (($phase + 1) % 2)) 'm'
            }
            Fill-Rect $map ($bounds.CenterX - 2 + $motion) ($bounds.MaxY - 2) 2 2 'c'
        }
    }
}

function Draw-BabyActionOriginal($map, [string]$species, [int]$action, [int]$facing, [int]$frame) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $count = $frameCounts[$action]
    $phase = $frame % $count
    $side = if ($facing -eq 2) { -1 } elseif ($facing -eq 3) { 1 } else { 0 }
    $headX = $bounds.CenterX + $side * 2
    $headY = $bounds.MinY + 5
    switch ($action) {
        0 {
            if ($phase -in 5, 6 -and $facing -ne 1) { Fill-Rect $map ($headX - 2) $headY 5 1 'o' }
            Put-Cell $map (Clamp-Grid ($bounds.MaxX + 1)) ($bounds.CenterY - 2 + $phase % 4) 'h'
        }
        1 {
            $bob = @(0, -1, -2, -1, 0, -1, -2, -1, 0, -1)[$phase]
            $step = @(-3, -2, -1, 0, 1, 3, 2, 1, 0, -1)[$phase]
            Translate-Frame $map $side $bob
            Fill-Rect $map ($bounds.CenterX - 5 + $step) 28 3 2 'o'
            Fill-Rect $map ($bounds.CenterX + 3 - $step) 28 3 2 'o'
            Put-Cell $map (Clamp-Grid ($bounds.MinX - 1 + $phase)) (26 + $phase % 3) 's'
        }
        2 {
            $reach = @(5, 4, 3, 2, 1, 0, 1, 2, 3, 4)[$phase]
            $foodX = Clamp-Grid ($headX + $(if ($side -lt 0) { -4 } else { 4 }) + $(if ($side -lt 0) { $reach } else { -$reach }))
            $foodY = Clamp-Grid ($headY + 4 + [Math]::Abs(2 - ($phase % 5)))
            Draw-Line $map ($bounds.CenterX + $side * 3) ($bounds.CenterY + 2) $foodX ($foodY + 1) 'c'
            Fill-Rect $map $foodX $foodY 2 2 'a'
            Put-Cell $map (Clamp-Grid ($foodX - 2 + $phase % 5)) (Clamp-Grid ($foodY - 2 - [Math]::Floor($phase / 5))) 'h'
            Put-Cell $map (Clamp-Grid ($bounds.MinX + $phase)) ([Math]::Max(1, $bounds.MinY - 1)) 'h'
            if ($facing -ne 1) { Fill-Rect $map ($headX - 1) ($headY + 2) 3 1 'x' }
        }
        3 {
            $jump = @(0, -1, -3, -5, -4, -2, 0, -2, -4, -1)[$phase]
            Translate-Frame $map 0 $jump
            Fill-Rect $map ($bounds.CenterX - 3 - $phase % 2) 29 (7 + ($phase % 2) * 2) 1 's'
            Draw-Line $map ($bounds.CenterX - 4) ($bounds.CenterY + 2 + $jump) ($bounds.CenterX - 7) ($bounds.CenterY - 2 + $jump) 'b'
            Draw-Line $map ($bounds.CenterX + 4) ($bounds.CenterY + 2 + $jump) ($bounds.CenterX + 7) ($bounds.CenterY - 2 + $jump) 'b'
            Put-Cell $map (Clamp-Grid ($bounds.MinX + $phase)) ([Math]::Max(1, $bounds.MinY - 1 + $phase % 3)) 'r'
        }
        4 {
            Convert-ToBabySleepPose $map
            $rest = Get-Bounds $map
            Translate-Frame $map @(-1, -1, 0, 0, 1, 1, 0, -1, 0, 1)[$phase] 0
            if ($facing -ne 1) { Fill-Rect $map ($rest.CenterX - 3) ($rest.MinY + 5) 6 1 'o' }
            Put-Cell $map (Clamp-Grid ($rest.MaxX + 1 + $phase % 4)) ([Math]::Max(1, $rest.MinY - 1 - [Math]::Floor($phase / 4))) 'h'
        }
        5 {
            $stride = @(-2, -1, 0, 1, 2, 1, 0, -1, -2, 0)[$phase]
            Translate-Frame $map ($side + $stride) @(0, -1, 0, 1, 0, -1, 0, 1, 0, -1)[$phase]
            $lookDirection = if ($side -lt 0) { -1 } else { 1 }
            Put-Cell $map (Clamp-Grid ($bounds.CenterX + $lookDirection * (5 + $phase % 3))) ([Math]::Max(1, $bounds.MinY - 1 + $phase % 5)) 'h'
            Put-Cell $map (Clamp-Grid ($bounds.CenterX + $lookDirection * (7 + $phase % 2))) ([Math]::Max(1, $bounds.MinY + 1 + [Math]::Floor($phase / 2))) 'r'
        }
        6 {
            Translate-Frame $map 0 @(0, -1, -2, -1, 0, -1)[$phase]
            Put-Cell $map (Clamp-Grid ($bounds.MinX - 1 + $phase)) ($bounds.MaxY - 1) 'c'
        }
        7 {
            Translate-Frame $map @(0, -2, 2, -1, 1, 0)[$phase] 0
            if ($facing -ne 1) { Fill-Rect $map ($headX - 3) ($headY - 1) 2 3 'h'; Fill-Rect $map ($headX + 2) ($headY - 1) 2 3 'h' }
            Put-Cell $map (Clamp-Grid ($bounds.MinX - 1)) ([Math]::Max(1, $bounds.MinY + $phase)) 'r'
        }
        8 {
            Translate-Frame $map @(-1, -2, -1, 0, 1, 0)[$phase] -1
            if ($facing -ne 1) { Fill-Rect $map ($headX - 2) $headY 5 1 'o' }
            Put-Cell $map (Clamp-Grid ($bounds.MinX + $phase)) ([Math]::Max(1, $bounds.MinY - 1)) 'm'
            Put-Cell $map (Clamp-Grid ($bounds.MinX + 1 + $phase)) ([Math]::Max(1, $bounds.MinY - 2)) 'm'
        }
        9 {
            $hop = @(0, -2, -4, -3, -1, 0)[$phase]
            Translate-Frame $map $side $hop
            Fill-Rect $map ($bounds.CenterX - 3) 29 7 1 's'
            Fill-Rect $map (Clamp-Grid ($bounds.CenterX + 6 - $phase)) (25 + $phase % 3) 2 2 'a'
        }
        10 {
            Translate-Frame $map 0 -1
            if ($facing -ne 1) { Put-Cell $map ($headX + $side) ($headY - 1) 'h' }
            Put-Cell $map (Clamp-Grid ($bounds.MaxX + 1 + $phase % 2)) ([Math]::Max(1, $bounds.MinY - 1 + $phase)) 'r'
        }
        11 {
            Draw-Line $map ($bounds.CenterX + $side * 3) ($bounds.CenterY + 3) ($headX + $(if ($side -lt 0) { -2 } else { 2 })) ($headY - 1 + $phase % 3) 'c'
            Put-Cell $map (Clamp-Grid ($bounds.MinX - 1 + $phase * 2)) ([Math]::Max(1, $bounds.MinY + $phase % 3)) 'h'
        }
        12 {
            if ($facing -ne 1) { Fill-Rect $map ($headX - 1) ($headY + 2) 3 2 'x' }
            $waveX = Clamp-Grid ($bounds.MaxX + 1 + $phase % 3)
            $waveY = [Math]::Max(1, $bounds.MinY + 1 + $phase)
            Put-Cell $map $waveX $waveY 'a'
            Put-Cell $map (Clamp-Grid ($waveX + 1)) ([Math]::Max(1, $waveY - 1)) 'a'
        }
    }
    Draw-BabySpeciesMotion $map $species $action $facing $phase
}

function Compress-ForDirection($source, [int]$facing) {
    if ($facing -eq 0) { return Copy-FrameMap $source }
    $result = New-FrameMap
    foreach ($entry in $source.GetEnumerator()) {
        $parts = $entry.Key.Split(','); $x = [int]$parts[0]; $y = [int]$parts[1]
        $directionScale = if ($facing -eq 1) { 0.90 } else { 0.78 }
        $newX = [int][Math]::Round(16 + ($x - 16) * $directionScale)
        Put-Cell $result $newX $y $entry.Value
    }
    return $result
}

function Draw-DirectionalFeatures($map, [string]$species, [int]$stage, [int]$facing, [int]$frame) {
    if ($stage -eq 0) {
        # An egg has no animal limbs or face direction. Keeping the shell
        # direction-neutral avoids the old animal-like artifacts before hatch.
        return
    }
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $left = $facing -eq 2; $right = $facing -eq 3; $back = $facing -eq 1
    $headY = [Math]::Max(2, $bounds.MinY + [Math]::Max(1, [int](($bounds.MaxY - $bounds.MinY) * 0.12)))
    $bodyY = [Math]::Min(27, $bounds.MinY + [int](($bounds.MaxY - $bounds.MinY) * 0.60))
    if ($back) {
        Fill-Rect $map ($bounds.CenterX - 2) ($headY + 3) 5 2 's'
        switch ($species) {
            "cat" { Draw-Line $map ($bounds.CenterX + 4) ($bodyY + 3) ($bounds.CenterX + 7) ($bodyY - 1) 'o'; Draw-Line $map ($bounds.CenterX + 5) ($bodyY + 3) ($bounds.CenterX + 7) ($bodyY - 1) 'b' }
            "dog" { Fill-Rect $map ($bounds.CenterX - 1) ($headY + 1) 3 3 'c'; Draw-Line $map ($bounds.CenterX + 4) ($bodyY + 2) ($bounds.CenterX + 7) ($bodyY - 2) 'o' }
            "bird" { Fill-Rect $map ($bounds.CenterX - 6) ($bodyY - 2) 3 7 's'; Fill-Rect $map ($bounds.CenterX + 4) ($bodyY - 2) 3 7 's' }
            "penguin" { Fill-Rect $map ($bounds.CenterX - 7) ($bodyY - 1) 3 6 's'; Fill-Rect $map ($bounds.CenterX + 5) ($bodyY - 1) 3 6 's' }
            "rabbit" { Fill-Rect $map ($bounds.CenterX - 4) ($headY - 5) 2 8 's'; Fill-Rect $map ($bounds.CenterX + 3) ($headY - 5) 2 8 's'; Fill-Rect $map ($bounds.CenterX + 5) ($bodyY + 2) 3 3 'c' }
            "hamster" { Fill-Rect $map ($bounds.CenterX - 2) ($headY + 2) 5 1 's'; Fill-Rect $map ($bounds.CenterX - 1) ($bodyY + 3) 3 2 'c' }
        }
        return
    }
    if ($left -or $right) {
        $sign = if ($left) { -1 } else { 1 }
        $frontX = if ($left) { [Math]::Max(1, $bounds.MinX - 1) } else { [Math]::Min(30, $bounds.MaxX + 1) }
        $rearX = if ($left) { $bounds.MaxX - 1 } else { $bounds.MinX + 1 }
        Put-Cell $map $frontX ($headY + 3) 'o'
        Put-Cell $map $frontX ($headY + 4) 'o'
        switch ($species) {
            "cat" {
                Fill-Rect $map ($frontX - [Math]::Max(0, $sign)) ($headY + 3) 2 2 'c'
                Draw-Line $map $rearX ($bodyY + 3) ($rearX - $sign * 4) ($bodyY - 1) 'o'
                Draw-Line $map ($rearX - $sign) ($bodyY + 3) ($rearX - $sign * 4) ($bodyY - 1) 'b'
            }
            "dog" { Fill-Rect $map ($frontX - 1) ($headY + 3) 3 2 'c'; Draw-Line $map $rearX ($bodyY + 2) ($rearX - $sign * 4) ($bodyY - 1) 'o' }
            "bird" { Fill-Rect $map ($frontX - [Math]::Max(0, $sign)) ($headY + 4) 3 2 'a'; Fill-Rect $map ($bounds.CenterX - $sign * 2) ($bodyY - 2) 4 6 's' }
            "penguin" { Fill-Rect $map ($frontX - [Math]::Max(0, $sign)) ($headY + 5) 3 1 'a'; Fill-Rect $map ($bounds.CenterX - $sign * 2) ($bodyY - 1) 3 6 's' }
            "rabbit" { Fill-Rect $map ($frontX - [Math]::Max(0, $sign)) ($headY + 4) 2 2 'c'; Fill-Rect $map ($rearX - [Math]::Max(0, $sign)) ($bodyY + 2) 3 3 'c' }
            "hamster" {
                $frontEdge = if ($sign -lt 0) { $bounds.MinX } else { $bounds.MaxX }
                $rearEdge = if ($sign -lt 0) { $bounds.MaxX } else { $bounds.MinX }
                Fill-Rect $map ($frontEdge + $sign) ($headY + 4) 2 1 'm'
                Put-Cell $map ($frontEdge + $sign) ($headY + 3) 'c'
                Fill-Rect $map ($rearEdge - $sign) ($bodyY + 1) 2 2 'c'
                Put-Cell $map ($rearEdge - $sign) ($bodyY + 3) 's'
            }
        }
        Put-Cell $map ($frontX - [Math]::Max(0, $sign)) ($headY + 2 + ($frame % 2)) 'x'
    }
}

function Draw-ActionOriginal($map, [string]$species, [int]$stage, [int]$action, [int]$facing, [int]$frame) {
    if ($stage -eq 0) {
        Draw-EggActionOriginal $map $species $action $frame
        return
    }
    if ($stage -eq 1) {
        Draw-BabyActionOriginal $map $species $action $facing $frame
        return
    }
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { return }
    $count = $frameCounts[$action]; $phase = $frame % $count
    $side = if ($facing -eq 2) { -1 } elseif ($facing -eq 3) { 1 } else { 0 }
    $headX = $bounds.CenterX + $side * 3; $headY = $bounds.MinY + 3; $handX = $bounds.CenterX + $side * 4
    switch ($action) {
        0 { # idle: breath, blink, species-specific small movement
            if ($phase -in 5, 6 -and $facing -ne 1) { Fill-Rect $map ($headX - 1) ($headY + 3) 3 1 'o' }
            if ($species -eq "cat" -and $phase % 3 -eq 1) { Draw-Line $map ($bounds.MaxX - 1) ($bounds.CenterY + 2) ($bounds.MaxX + 1) ($bounds.CenterY - 1) 'b' }
            if ($species -eq "bird" -and $phase % 3 -eq 1) { Fill-Rect $map ($bounds.CenterX - 5) ($bounds.CenterY) 2 2 'h' }
            if ($species -eq "penguin" -and $phase % 2 -eq 1) { Fill-Rect $map ($bounds.CenterX - 7) ($bounds.CenterY + 1) 1 3 's' }
        }
        1 { # walking: authored foot cycle and body bob
            $lift = @(0, -1, -1, 0, 0, 0, -1, -1, 0, 0)[$phase]
            Translate-Frame $map 0 $lift
            $footShift = if ($phase % 4 -in 1, 2) { 2 } else { -2 }
            Fill-Rect $map ($bounds.CenterX - 4 + $footShift / 2) 28 3 2 'o'
            Fill-Rect $map ($bounds.CenterX + 2 - $footShift / 2) 28 3 2 'o'
            if ($species -eq "rabbit" -and $phase -in 2, 3, 4) { Translate-Frame $map $side -1 }
        }
        2 { # eating: held food and deliberate chewing pose
            $dip = @(0, 1, 1, 2, 1, 0, 0, 1, 1, 0)[$phase]
            $poseLean = if ($side -eq 0) { 0 } else { $side }
            Translate-FrameRegion $map ($headY + 6) $poseLean (1 + ($phase % 2))
            $bounds = Get-Bounds $map
            $headX = $bounds.CenterX + $side * 3
            $headY = $bounds.MinY + 3
            $handX = $bounds.CenterX + $side * 4
            $foodX = if ($side -lt 0) {
                [Math]::Max(1, $bounds.MinX - 2)
            } else {
                [Math]::Min(29, $bounds.MaxX + 1)
            }
            $foodY = [Math]::Max(2, $headY + 5 - $dip)
            Draw-Line $map $handX ($headY + 7) $foodX ($foodY + 1) 'c'
            Fill-Rect $map $foodX $foodY 2 2 'a'
            Put-Cell $map ($foodX + $(if ($side -lt 0) { 0 } else { 1 })) ($foodY - 1) 'h'
            Fill-Rect $map ($headX - 1) ($headY + 5) 2 1 'x'
            if ($phase % 2 -eq 0) { Put-Cell $map ($headX + 2) ($headY + 5) 'c' }
        }
        3 { # happy: raised limbs and distinct celebration cels
            Translate-Frame $map 0 (@(0, -1, -2, -2, -1, 0, 0, -1, -2, -1)[$phase])
            Draw-Line $map ($handX - 3) ($headY + 7) ($handX - 5) ($headY + 3) 'b'
            Draw-Line $map ($handX + 3) ($headY + 7) ($handX + 5) ($headY + 3) 'b'
            Put-Cell $map ($bounds.MaxX + 1) ($bounds.MinY + 1) 'r'
        }
        4 { # sleeping: low resting silhouette, breathe and Z cels
            if ($phase -in 2, 3, 4, 5) { Translate-Frame $map 1 0 }
            Fill-Rect $map ($bounds.CenterX - 3) ($bounds.MinY + 4) 5 1 'o'
            Put-Cell $map ($bounds.MaxX - 1) ($bounds.MinY - 1 - ($phase % 3)) 'h'
            Put-Cell $map ($bounds.MaxX + 1) ($bounds.MinY - 2 - ($phase % 2)) 'h'
        }
        5 { # exploring: stride with look-ahead sparkle
            Translate-Frame $map $side (@(0, -1, 0, 1, 1, 0, 0, -1, 0, 1)[$phase])
            Put-Cell $map ($bounds.MaxX + 1) ($bounds.MinY + 1 + $phase % 3) 'h'
        }
        6 { # hatching: only used by the egg state, keep a calm fallback body pose
            Put-Cell $map ($bounds.CenterX) ($bounds.MinY + 2 + $phase % 2) 'h'
        }
        7 { # frightened: tucked limbs and visible shake
            Translate-Frame $map (@(0, -1, 1, -1, 1, 0)[$phase]) 0
            Put-Cell $map ($bounds.MinX - 1) ($bounds.MinY + 2) 'a'
            Put-Cell $map ($bounds.MinX - 2) ($bounds.MinY + 1 + ($phase % 2)) 'r'
        }
        8 { # petted: head lean plus heart
            Translate-Frame $map (@(0, -1, -1, 0, 1, 0)[$phase]) -1
            Fill-Rect $map ($headX - 1) ($headY + 3) 3 1 'o'
            Put-Cell $map ($bounds.MinX + $phase) ($bounds.MinY - 1) 'r'
        }
        9 { # playing: hop and toy silhouette
            Translate-Frame $map $side (@(0, -1, -2, -3, -2, -1)[$phase])
            Fill-Rect $map ($bounds.CenterX + 5) ($bounds.MaxY - 2) 3 2 'a'
        }
        10 { # watching: raised head and attentive eye
            Translate-Frame $map 0 -1
            if ($facing -ne 1) { Put-Cell $map ($headX + $side) ($headY + 2) 'h' }
            Put-Cell $map ($bounds.MaxX + 1) ($bounds.MinY + 1 + $phase % 2) 'r'
        }
        11 { # cleaning: paw/wing reaches the face
            Fill-Rect $map ($handX - 1) ($headY + 1 + $phase % 2) 3 4 'c'
            Put-Cell $map ($bounds.MinX + 1 + $phase) ($bounds.MinY - 1) 'h'
        }
        12 { # calling: open mouth plus sound wave
            if ($facing -ne 1) { Fill-Rect $map ($headX - 1) ($headY + 5) 3 2 'x' }
            $waveY = [Math]::Max(1, $bounds.MinY - 1 - ($phase % 2))
            $waveX = Clamp-Grid ($bounds.CenterX + 2 + ($phase % 3))
            Put-Cell $map $waveX $waveY 'a'
            Put-Cell $map (Clamp-Grid ($waveX + 1)) ([Math]::Max(1, $waveY - 1)) 'a'
        }
    }
}

function Get-FrameAnchors($map, [string]$species, [int]$stage, [int]$action, [int]$facing, [int]$frame) {
    $bounds = Get-Bounds $map
    if ($null -eq $bounds) { throw "Empty authored frame" }
    $side = if ($facing -eq 2) { -1 } elseif ($facing -eq 3) { 1 } else { 0 }
    $phase = $frame % $frameCounts[$action]
    $headY = (Clamp-Grid ($bounds.MinY + 3 + $(if ($action -eq 2) { 1 } else { 0 })))
    $headX = Clamp-Grid ($bounds.CenterX + $side * 2)
    $handLift = switch ($action) { 2 { -3 + $phase % 2 } 11 { -4 + $phase % 2 } 5 { -3 } 1 { if ($phase % 2 -eq 0) { -1 } else { 1 } } default { 0 } }
    $handX = Clamp-Grid ($bounds.CenterX + $side * 4 + $(if ($action -eq 1 -and $phase % 2 -eq 0) { -1 } else { 1 }))
    $tailX = Clamp-Grid ($bounds.CenterX - $side * 6 + $(if ($species -eq "bird") { -$side } else { 0 }))
    $frontLayer = if ($facing -eq 1) { 0 } else { 2 }
    return [PSCustomObject]@{ anchors = [ordered]@{
        head = @($headX, $headY, $frontLayer)
        back = @((Clamp-Grid ($bounds.CenterX - $side * 3)), (Clamp-Grid ($bounds.CenterY + 2)), 0)
        hand = @($handX, (Clamp-Grid ($bounds.MaxY - 5 + $handLift)), $(if ($facing -eq 1) { 0 } else { 2 }))
        neck = @($headX, (Clamp-Grid ($headY + 4)), $(if ($facing -eq 1) { 0 } else { 1 }))
        tail = @($tailX, (Clamp-Grid ($bounds.MaxY - 6 - $phase % 2)), 0)
        trail = @((Clamp-Grid ($bounds.CenterX + $(if ($phase % 2 -eq 0) { -2 } else { 2 }))), (Clamp-Grid ($bounds.MaxY - 1)), 0)
    } }
}

function Convert-Cells($map) {
    $cells = [System.Collections.Generic.List[uint16]]::new()
    foreach ($entry in $map.GetEnumerator()) {
        $parts = $entry.Key.Split(','); $x = [int]$parts[0]; $y = [int]$parts[1]
        $symbol = [string]$entry.Value
        $code = $codeBySymbol[$symbol]
        if ($null -eq $code) { throw "Unsupported source symbol: $symbol" }
        $cells.Add([uint16](($y * $grid + $x) * 16 + $code))
    }
    return @($cells | Sort-Object { [int]($_ -shr 4) })
}

function Write-SourceSheet([string]$path, $frames) {
    $bitmap = [System.Drawing.Bitmap]::new($sheetSize, $sheetSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($index = 0; $index -lt $frames.Count; $index++) {
            $baseX = ($index % $columns) * $grid; $baseY = [int][Math]::Floor($index / $columns) * $grid
            foreach ($entry in $frames[$index].Map.GetEnumerator()) {
                $parts = $entry.Key.Split(','); $color = $colorBySymbol[[string]$entry.Value]
                $bitmap.SetPixel($baseX + [int]$parts[0], $baseY + [int]$parts[1], $color)
            }
        }
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $bitmap.Dispose() }
}

function Write-Pack([string]$path, [int]$speciesIndex, $frames) {
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
    try {
        $writer = [System.IO.BinaryWriter]::new($stream)
        try {
            $writer.Write($magic); $writer.Write([byte]2); Write-UInt32BigEndian $writer $frames.Count
            foreach ($item in $frames) {
                $d = $item.Descriptor; $cells = Convert-Cells $item.Map
                $writer.Write([byte]$speciesIndex); $writer.Write([byte]$d.Stage); $writer.Write([byte]$d.Action); $writer.Write([byte]$d.Facing); $writer.Write([byte]$d.Frame); Write-UInt16BigEndian $writer $cells.Count
                foreach ($slot in @("head", "back", "hand", "neck", "tail", "trail")) { $anchor = @($item.Anchors.anchors.$slot); Write-UInt16BigEndian $writer ((Clamp-Grid ([int]$anchor[1])) * $grid + (Clamp-Grid ([int]$anchor[0]))) }
                foreach ($slot in @("head", "back", "hand", "neck", "tail", "trail")) { $writer.Write([byte][int]@($item.Anchors.anchors.$slot)[2]) }
                foreach ($cell in $cells) { Write-UInt16BigEndian $writer ([int]$cell) }
            }
        } finally { $writer.Dispose() }
    } finally { $stream.Dispose() }
}

function Write-SourceManifest([string]$directory) {
    $sources = [ordered]@{}
    foreach ($species in $speciesNames) {
        $image = Join-Path $directory "$species.png"; $anchors = Join-Path $directory "$species.anchors.json"
        $sources[$species] = [ordered]@{
            image = "$species.png"; imageSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $image).Hash.ToLowerInvariant()
            anchors = "$species.anchors.json"; anchorsSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $anchors).Hash.ToLowerInvariant()
            provenance = "authored-v4-native-pixel-grid"
        }
    }
    $manifest = [ordered]@{ version = 4; grid = 32; columns = 40; rows = 40; framesPerSpecies = 1600; actions = @("idle", "walking", "eating", "happy", "sleeping", "exploring", "hatching", "frightened", "petted", "playing", "watching", "cleaning", "calling"); directions = @("front", "back", "left", "right"); sources = $sources }
    [System.IO.File]::WriteAllText((Join-Path $directory "source-manifest.json"), ($manifest | ConvertTo-Json -Depth 7), [System.Text.Encoding]::UTF8)
}

[System.IO.Directory]::CreateDirectory($SourceDirectory) | Out-Null
[System.IO.Directory]::CreateDirectory($OutputDirectory) | Out-Null
$descriptors = Get-Descriptors
$runtimeManifest = [System.Collections.Generic.List[string]]::new()
$runtimeManifest.Add("# Generated from v4 per-frame pixel-pet original sheets")
$runtimeManifest.Add("version=4")

for ($speciesIndex = 0; $speciesIndex -lt $speciesNames.Count; $speciesIndex++) {
    $species = $speciesNames[$speciesIndex]
    $masters = @{}
    for ($stage = 0; $stage -lt 4; $stage++) { $masters[$stage] = Read-StageMaster $species $stage }
    $frames = [System.Collections.Generic.List[object]]::new()
    foreach ($descriptor in $descriptors) {
        $map = if ($descriptor.Stage -eq 0) {
            Copy-FrameMap $masters[$descriptor.Stage]
        } else {
            Compress-ForDirection $masters[$descriptor.Stage] $descriptor.Facing
        }
        Draw-DirectionalFeatures $map $species $descriptor.Stage $descriptor.Facing $descriptor.Frame
        Draw-ActionOriginal $map $species $descriptor.Stage $descriptor.Action $descriptor.Facing $descriptor.Frame
        Normalize-Baseline $map
        Normalize-SafeArtboard $map
        $anchors = Get-FrameAnchors $map $species $descriptor.Stage $descriptor.Action $descriptor.Facing $descriptor.Frame
        $frames.Add([PSCustomObject]@{ Descriptor = $descriptor; Map = $map; Anchors = $anchors })
    }
    if ($frames.Count -ne 1600) { throw "Missing v4 frames for $species" }
    $sheet = Join-Path $SourceDirectory "$species.png"; $anchorPath = Join-Path $SourceDirectory "$species.anchors.json"; $pack = Join-Path $OutputDirectory "$species.bin"
    Write-SourceSheet $sheet $frames
    [System.IO.File]::WriteAllText($anchorPath, (($frames | ForEach-Object { $_.Anchors }) | ConvertTo-Json -Depth 5), [System.Text.Encoding]::UTF8)
    Write-Pack $pack $speciesIndex $frames
    $runtimeManifest.Add("$species.asset=pixel_pet/v4/$species.bin")
    $runtimeManifest.Add("$species.sha256=$((Get-FileHash -Algorithm SHA256 -LiteralPath $pack).Hash.ToLowerInvariant())")
    $runtimeManifest.Add("$species.frames=1600")
    $runtimeManifest.Add("$species.format=2")
}

Write-SourceManifest $SourceDirectory
[System.IO.File]::WriteAllLines((Join-Path $OutputDirectory "manifest.properties"), $runtimeManifest, [System.Text.Encoding]::ASCII)
Write-Output "Compiled six-species v4 authored pixel-pet frame sheets to $OutputDirectory"
