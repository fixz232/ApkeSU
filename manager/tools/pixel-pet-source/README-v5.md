# Pixel Pet V5 Native Sprite Sources

V5 is the runtime source of truth for all six pixel pets.

- `v5-masters-src/` contains the 24 editable semantic-pixel row masters.
- `compile_pixel_pet_v5_masters.ps1` validates those rows and writes the 24
  transparent PNG masters under `v5-masters/`.
- `redraw_pixel_pet_v5_sources.ps1` expands the masters into checked-in
  direction/action Sprite sheets and per-frame attachment metadata.
- Egg and baby frames use native `16x16` canvases.
- Young frames use native `32x32` canvases.
- Advanced frames use native `48x48` canvases.
- Each species and stage has one generated PNG Sprite sheet containing 400
  independently stored frames: thirteen actions, four facings, and every
  timing cel. Front masters stay pixel-authored; the authoring pass builds
  bounded side/back poses and action keyframes before the compiler packs them.
- The matching JSON file stores the pivot, foot baseline, six equipment
  anchors, and render layers for each frame.
- The compiler only validates and packs existing PNG pixels. It does not
  mirror, resize, compress, deform, or generate action frames.
- Runtime rendering uses integer scaling and `FilterQuality.None`.

The normal authoring path never reads a design board, clipboard image,
temporary QA file, or cropped bitmap. Reference boards are used only for
visual review; every shipped input is an editable native-resolution `.px`
master.

Only the ten colors declared in `pixel_pet_sprite_source_v5.ps1` are valid.
Transparent pixels must have alpha `0`; painted pixels must have alpha `255`.
This keeps every source pixel deterministic and prevents anti-aliasing from
entering the Android pack.

Redraw the checked-in frame sources, then compile the runtime packs from
`manager/`:

```powershell
pwsh ./tools/pixel-pet-source/compile_pixel_pet_v5_masters.ps1
pwsh ./tools/pixel-pet-source/redraw_pixel_pet_v5_sources.ps1
pwsh ./tools/pixel-pet-source/pixel_pet_sprite_source_v5.ps1
```

The command validates all 9,600 frames, rewrites six format-v3 packs under
`app/src/main/assets/pixel_pet/v5/`, and refreshes both SHA-256 manifests.
