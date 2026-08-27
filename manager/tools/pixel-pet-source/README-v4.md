# Pixel Pet V4 Frame Originals

`v4` is the runtime source of truth for the pixel-pet body renderer.

- Each of the six species has one editable `1280x1280` PNG Sprite sheet.
- A sheet contains `1600` independently stored `32x32` cels: four growth
  stages, thirteen actions, four facings, and every timing frame.
- `*.anchors.json` contains six authored attachment points and depth layers for
  every cel. The renderer reads them directly and never infers them from a
  Sprite's visible bounds.
- All six species and all four growth-stage masters are maintained as semantic
  pixel cells in `pixel_pet_sprite_source_v4.ps1`. The supplied design boards
  are visual references only: the compiler never reads, crops, quantizes, or
  downsamples them.
- The side, back, and action cels are compiled into editable output and stored
  before Android runtime rendering. Runtime drawing uses integer scaling and
  `FilterQuality.None`.

Regenerate the checked-in source sheets and runtime packs from `manager/`:

```powershell
pwsh ./tools/pixel-pet-source/pixel_pet_sprite_source_v4.ps1
```

The script validates the fixed `1600`-frame layout by construction, writes
checksums into both manifests, and emits all runtime packs under
`app/src/main/assets/pixel_pet/v4/`.
