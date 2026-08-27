# Pixel Pet Design Draft V1

These transparent PNGs are generated from the user-provided editable boards:

- `../design-boards/core-stages.png`
- `../design-boards/hamster-stages.png`

The source crop coordinates, matte removal, egg recovery, palette cleanup, and
baseline normalization are defined in `../import_user_design_board_v1.ps1`.
Regenerate this directory from `manager/` with:

```powershell
./tools/pixel-pet-source/import_user_design_board_v1.ps1 -OutputDirectory ./tools/pixel-pet-source/design-draft-v1
```

Runtime copies are stored in `app/src/main/assets/pixel_pet/reference/`.
The intended canvas sizes are 32px for eggs, 48px for babies, 64px for young
pets, and 96px for adults. `hamster_adult_body.png` deliberately uses the
standalone hamster pose because the supplied high-form board embeds the hamster
inside its wheel; the wheel remains a habitat asset.
