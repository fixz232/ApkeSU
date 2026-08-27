# Pixel Pet Adult Master V1

`redraw_adult_master_v1.ps1` owns the first art-direction pass for the five
adult pets. It redraws only four foundational actions and preserves the fixed
32x32 normalized frame layout, action ordinals, pivot baseline, four facings,
and v3 anchor contract. Reference stage artboards are exported separately as
egg/baby `16x16`, growing `32x32`, and adult/high-form `48x48`; the runtime
normalizes them to this frame layout before drawing.

| Action | Frames | Direction |
| --- | ---: | --- |
| Idle | 8 | breathing, blink, species-tail/wing variation |
| Walking | 10 | distinct stride, foot placement, locomotion cue |
| Eating | 10 | food approach, nibble rhythm, hand/beak movement |
| Sleeping | 10 | curled/resting silhouette and drifting dream pixels |

The script writes into `v3/<species>.png` and `<species>.anchors.json`. Those
files are the editable source assets. After reviewing or manually editing the
pixels, compile them into Android resources:

```powershell
pwsh -NoProfile -File ./pixel_pet_sprite_source_v3.ps1 -Mode Compile
```

Generate a contact sheet while redrawing to review the five species against the
same visual baseline:

```powershell
pwsh -NoProfile -File ./redraw_adult_master_v1.ps1 -PreviewPath ../../build/pixel-pet-adult-master-v1.png
```
