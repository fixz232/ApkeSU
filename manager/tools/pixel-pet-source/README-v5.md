# Pixel Pet V5 Native Sprite Sources

V5 is the runtime source of truth for all six pixel pets.

- `v5-masters/` contains the 24 editable, transparent semantic-pixel masters.
- `redraw_pixel_pet_v5_sources.ps1` expands those native masters into the
  checked-in per-frame Sprite sheets and authored attachment metadata.
- Egg and baby frames use native `16x16` canvases.
- Young frames use native `32x32` canvases.
- Advanced frames use native `48x48` canvases.
- Each species and stage has one editable PNG Sprite sheet containing 400
  independently stored frames: thirteen actions, four facings, and every
  timing cel.
- The matching JSON file stores the pivot, foot baseline, six equipment
  anchors, and render layers for each frame.
- The compiler only validates and packs existing PNG pixels. It does not
  mirror, resize, compress, deform, or generate action frames.
- Runtime rendering uses integer scaling and `FilterQuality.None`.

The authoring step never reads a design board, clipboard image, temporary QA
file, or cropped bitmap. Reference boards are used only for visual review;
every shipped input is a native-resolution semantic pixel master.

Only the ten colors declared in `pixel_pet_sprite_source_v5.ps1` are valid.
Transparent pixels must have alpha `0`; painted pixels must have alpha `255`.
This keeps every source pixel deterministic and prevents anti-aliasing from
entering the Android pack.

Redraw the checked-in frame sources, then compile the runtime packs from
`manager/`:

```powershell
pwsh ./tools/pixel-pet-source/redraw_pixel_pet_v5_sources.ps1
pwsh ./tools/pixel-pet-source/pixel_pet_sprite_source_v5.ps1
```

The command validates all 9,600 frames, rewrites six format-v3 packs under
`app/src/main/assets/pixel_pet/v5/`, and refreshes both SHA-256 manifests.
