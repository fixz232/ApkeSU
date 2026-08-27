# Pixel Pet Adult Master V2

V2 replaces the first adult master pass with a larger, manually maintained
pixel silhouette system. It is deliberately built from fixed source stencils,
not geometric drawing primitives or runtime scale changes.

The source writer expands the authored contour from the fixed 32x32 pivot
after drawing. This gives the full LKM card a larger character while keeping
each pose on the same foot baseline.

The five adult species each have a dedicated front, side, back, and sleeping
pose. The source stencils use the shared indexed material palette:

- `o`: outline
- `b`, `s`: coat or feather base and shadow
- `c`, `h`: chest and highlight
- `a`: accent
- `e`, `x`: face details

`Idle`, `Walking`, `Eating`, and `Sleeping` preserve the v3 frame identifiers
and attachments. Frame variations move physical body parts: tails, ears,
wings, feet, beaks, paws, and food. They do not rely on decorative floating
pixels to simulate animation.

Run this before compiling the Android runtime packs:

```powershell
pwsh -NoProfile -File ./redraw_adult_master_v2.ps1 -PreviewPath ../../build/pixel-pet-adult-master-v2.png
pwsh -NoProfile -File ./pixel_pet_sprite_source_v3.ps1 -Mode Compile
```
