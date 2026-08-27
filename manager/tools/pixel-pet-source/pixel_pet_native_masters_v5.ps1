# Native pixel masters for the v5 pet packs.
#
# Every compact pet below is described as explicit semantic pixel runs. The
# supplied design boards are visual references only; this source never opens,
# samples, crops, or rescales them. Young and advanced forms reuse the larger
# explicit run tables in pixel_pet_reference_masters_v1.ps1.

. (Join-Path $PSScriptRoot "pixel_pet_reference_masters_v1.ps1")

function New-PixelPetNativeMaster(
    [int]$canvasSize,
    [int]$baseline,
    [string[]]$rows,
    [string]$provenance
) {
    if ($canvasSize -notin @(16, 32, 48)) { throw "Unsupported native canvas: $canvasSize" }
    if ($baseline -lt 0 -or $baseline -ge $canvasSize) { throw "Invalid native baseline" }
    if ($rows.Count -lt 1 -or $rows.Count -gt ($baseline + 1)) { throw "Invalid native row count" }
    foreach ($row in $rows) {
        if ($row.Length -ne $canvasSize) {
            throw "Native row must be exactly $canvasSize pixels: '$row'"
        }
        foreach ($symbol in $row.ToCharArray()) {
            if ($symbol -notin @('.', 'o', 'b', 's', 'c', 'h', 'a', 'm', 'r', 'e', 'x')) {
                throw "Unsupported native pixel symbol: $symbol"
            }
        }
    }
    return [PSCustomObject]@{
        CanvasSize = $canvasSize
        Baseline = $baseline
        PivotX = [int][Math]::Floor($canvasSize / 2.0)
        Rows = $rows
        Provenance = $provenance
    }
}

function New-CompactRows([object[]]$runs) {
    return New-ReferenceRows 16 $runs
}

function Convert-ReferenceRowsToCanvas([string[]]$rows, [int]$canvasSize) {
    if ($canvasSize -lt 30) { throw "Reference rows require at least a 30-pixel canvas" }
    $left = [int][Math]::Floor(($canvasSize - 30) / 2.0)
    return @($rows | ForEach-Object { ('.' * $left) + $_ + ('.' * ($canvasSize - 30 - $left)) })
}

function Get-PixelPetNativeMaster([string]$species, [int]$stage) {
    $key = "$species/$stage"
    switch ($key) {
        'cat/0' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(6, 'oooo'), @(4, 'ooccccoo'), @(3, 'occhhccco'), @(2, 'occaaacccco'),
            @(2, 'occaaccccco'), @(1, 'occccccccccco'), @(1, 'occaaccccccco'),
            @(1, 'ocaaaccccccco'), @(1, 'occcccaaaccco'), @(1, 'occccccccccco'),
            @(2, 'occhcccccco'), @(3, 'occcccccco'), @(4, 'ooccccco'), @(5, 'oooooo')
        )) 'authored-v5-cat-egg-16'
        }
        'dog/0' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(6, 'oooo'), @(4, 'ooccccoo'), @(3, 'occcccccco'), @(2, 'occbbbbcccco'),
            @(2, 'ocbbssbbccco'), @(1, 'ocbccccccbcco'), @(1, 'obcccececccbo'),
            @(1, 'obccccaccccbo'), @(1, 'occcbbbccccco'), @(1, 'occcccccccco'),
            @(2, 'occhcccccco'), @(3, 'occcccccco'), @(4, 'ooccccco'), @(5, 'oooooo')
        )) 'authored-v5-dog-egg-16'
        }
        'bird/0' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(6, 'oooo'), @(4, 'oobbbboo'), @(3, 'obbbbbbbbo'), @(2, 'obbbhbbbbbo'),
            @(2, 'obbbbbbbbbbo'), @(1, 'obbrbbbbbbbbo'), @(1, 'obbbbbbbbbbo'),
            @(1, 'obhbbbbbbbbbo'), @(1, 'obbbbbrbbbbbo'), @(1, 'obbbbbbbbbbo'),
            @(2, 'obbhbbbbbo'), @(3, 'obbbbbbbbo'), @(4, 'oobbbboo'), @(6, 'oooo')
        )) 'authored-v5-bird-egg-16'
        }
        'penguin/0' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(6, 'oooo'), @(4, 'oossssoo'), @(3, 'osssssssso'), @(2, 'ossccccssso'),
            @(1, 'ossceeccecsso'), @(1, 'ossscccccssso'), @(1, 'ossssaaacssso'),
            @(1, 'ossssccccsssso'), @(1, 'ossssrrrrsssso'), @(1, 'osssrhrhrssso'),
            @(2, 'ossssssssso'), @(3, 'osssccccso'), @(4, 'oossssoo'), @(5, 'oooooo')
        )) 'authored-v5-penguin-egg-16'
        }
        'rabbit/0' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(6, 'oooo'), @(4, 'ooccccoo'), @(3, 'occmccccco'), @(2, 'occmmmccccco'),
            @(2, 'occcmmccccco'), @(1, 'occccccccccco'), @(1, 'occcccmmmccco'),
            @(1, 'occccmmmmccco'), @(1, 'occcccmmccccco'), @(1, 'occmcccccmcco'),
            @(2, 'occcccccccco'), @(3, 'occcmmmcco'), @(4, 'ooccccco'), @(5, 'oooooo')
        )) 'authored-v5-rabbit-egg-16'
        }
        'hamster/0' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(5, 'oooooo'), @(3, 'oobbbbbboo'), @(2, 'obbbbbbbbbbo'), @(1, 'obbbbssbbbbbo'),
            @(1, 'obbbssssbbbbo'), @(0, 'obbbbbbbbbbbbbo'), @(0, 'obbbabbbbbabbbo'),
            @(0, 'obbbbbbbbbbbbbo'), @(0, 'obbbbaaabbaaabbo'), @(0, 'obbbbbbbbbbbbbo'),
            @(1, 'obbbbbssbbbbo'), @(2, 'obbbbbbbbbbo'), @(3, 'oobbbbbbboo'), @(5, 'oooooo')
        )) 'authored-v5-hamster-egg-16'
        }

        'cat/1' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(9, 'oo.oo'), @(8, 'obobbo'), @(7, 'obbbbbo'), @(6, 'obcccbbo'),
            @(1, 'oo...obcecebbo'), @(0, 'obbo..obcacbbo'), @(0, 'ob.oo.obbbbbbo'),
            @(0, 'ob..ooobbbbbbo'), @(1, 'oo..obbbbbbo'), @(5, 'obbbbbbo'),
            @(5, 'obbobbo'), @(5, 'obbo.obbo'), @(6, 'oo...oo')
        )) 'authored-v5-cat-baby-16'
        }
        'dog/1' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(8, 'oooo.ooo'), @(7, 'obbbobbo'), @(6, 'obbbbbbbo'), @(5, 'obccccbbbo'),
            @(1, 'oo..obcecebbo'), @(0, 'obbo.obcacccbbo'), @(0, 'ob.ooobbbbbbbbo'),
            @(0, 'ob..obbbbbbbbbo'), @(1, 'oo..obbbbbbo'), @(5, 'obbbbbbo'),
            @(5, 'obbobbo'), @(5, 'obbo.obbo'), @(6, 'oo...oo')
        )) 'authored-v5-dog-baby-16'
        }
        'bird/1' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(7, 'oooo'), @(5, 'oocccboo'), @(4, 'occcccbbo'), @(3, 'occecccbbbo'),
            @(2, 'occccccaabbbo'), @(1, 'occcccccabbbbo'), @(0, 'obccccccbbbbbbo'),
            @(1, 'obbbccccbbbbo'), @(2, 'obbbbbbbbbo'), @(3, 'obbbbbbbo'),
            @(4, 'obbbbbo'), @(4, 'obboobbo'), @(5, 'oao.oao')
        )) 'authored-v5-bird-baby-16'
        }
        'penguin/1' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(6, 'oooooo'), @(4, 'oossssoo'), @(3, 'osssssssso'), @(3, 'ossccccsso'),
            @(2, 'ossceeccecsso'), @(2, 'osssccacccsso'), @(1, 'ossssccccsssso'),
            @(0, 'osssssccccssssso'), @(0, 'osssssrrrrssssso'), @(1, 'osssrhrhrsssso'),
            @(2, 'ossssssssso'), @(3, 'ossscccsso'), @(4, 'osso.osso'), @(5, 'oao..oao')
        )) 'authored-v5-penguin-baby-16'
        }
        'rabbit/1' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(5, 'oo..oo'), @(4, 'ocm.ocmo'), @(4, 'ocm.ocmo'), @(3, 'occcccbo'),
            @(2, 'occececcbbo'), @(2, 'occcaccccbbo'), @(3, 'occcccccccbo'),
            @(4, 'occcccccccbo'), @(5, 'occccccccbo'), @(5, 'occcccccbo'),
            @(5, 'occo.occco'), @(6, 'oco..oco'), @(6, 'oao..oao')
        )) 'authored-v5-rabbit-baby-16'
        }
        'hamster/1' { return New-PixelPetNativeMaster 16 14 (New-CompactRows @(
            @(5, 'ooo.ooo'), @(4, 'obbo.obbo'), @(3, 'obbbbbbbo'), @(2, 'obcececbbo'),
            @(1, 'obcccacccbbo'), @(0, 'obccmmmmccbbbo'), @(0, 'obbbbccccbbbbbo'),
            @(0, 'obbbbbbbbbbbbo'), @(1, 'obbbccccbbbbo'), @(2, 'obbbbbbbbbo'),
            @(3, 'obbbbbbo'), @(4, 'obbobbo'), @(5, 'obo.obo'), @(5, 'oao.oao')
        )) 'authored-v5-hamster-baby-16'
        }
        default {
            if ($stage -notin @(2, 3)) { throw "Missing native pixel master for $key" }
            $canvas = if ($stage -eq 2) { 32 } else { 48 }
            $baseline = if ($stage -eq 2) { 29 } else { 44 }
            $rows = Convert-ReferenceRowsToCanvas (Get-PixelPetReferenceMaster $species $stage) $canvas
            return New-PixelPetNativeMaster $canvas $baseline $rows "authored-v5-$species-stage-$stage-native"
        }
    }
}
