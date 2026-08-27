# Pixel Pet Editable Sprite Source

`v3` stores one 1280x1280 PNG sheet and one frame-anchor JSON file for every
species. Each sheet contains 1,600 32x32 frames in fixed row-major order. The
PNG palette is indexed by the compiler; use a nearest-neighbor pixel editor and
do not introduce anti-aliasing or new colors.

```powershell
# Export the checked-in v2 runtime packs once into editable sources.
pwsh ./pixel_pet_sprite_source_v3.ps1 -Mode Export

# Rebuild the runtime v3 packs after editing a PNG or its anchor metadata.
pwsh ./pixel_pet_sprite_source_v3.ps1 -Mode Compile
```

Each `*.anchors.json` record contains the six anchor positions and render layer
for a specific frame. Layer `0` is behind the model, `1` is body overlay, and
`2` is foreground. The Android runtime validates the v3 manifest checksum and
falls back to v2 packs when v3 is unavailable.
