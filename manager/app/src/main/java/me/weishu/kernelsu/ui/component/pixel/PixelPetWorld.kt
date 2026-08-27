package me.weishu.kernelsu.ui.component.pixel

import androidx.annotation.StringRes
import me.weishu.kernelsu.R
import org.json.JSONObject
import java.util.UUID

enum class PixelPetFurnitureKind(
    @StringRes val labelRes: Int,
    val cost: Int,
    val interactionAction: PixelPetAction,
) {
    FoodBowl(R.string.pixel_pet_furniture_food_bowl, 28, PixelPetAction.Eating),
    Bed(R.string.pixel_pet_furniture_bed, 42, PixelPetAction.Sleeping),
    Toy(R.string.pixel_pet_furniture_toy, 36, PixelPetAction.Playing),
    Lamp(R.string.pixel_pet_furniture_lamp, 48, PixelPetAction.Watching),
    Plant(R.string.pixel_pet_furniture_plant, 24, PixelPetAction.Cleaning),
    Aquarium(R.string.pixel_pet_furniture_aquarium, 64, PixelPetAction.Watching),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetFurnitureKind? =
            entries.firstOrNull { it.name == value }
    }
}

enum class PixelPetFoodKind(
    @StringRes val labelRes: Int,
    val cost: Int,
    val hunger: Int,
    val energy: Int,
    val cleanliness: Int,
    val mood: Int,
    val growth: Int,
    val preferredSpecies: Set<PixelPetSpecies>,
) {
    FoodPellet(
        R.string.pixel_pet_food_pellet,
        6,
        hunger = 14,
        energy = 4,
        cleanliness = -1,
        mood = 2,
        growth = 1,
        preferredSpecies = emptySet(),
    ),
    Berry(
        R.string.pixel_pet_food_berry,
        10,
        hunger = 20,
        energy = 3,
        cleanliness = 1,
        mood = 7,
        growth = 2,
        preferredSpecies = setOf(
            PixelPetSpecies.Cat,
            PixelPetSpecies.Rabbit,
            PixelPetSpecies.Bird,
            PixelPetSpecies.Hamster,
        ),
    ),
    OceanFish(
        R.string.pixel_pet_food_ocean_fish,
        14,
        hunger = 26,
        energy = 6,
        cleanliness = -2,
        mood = 5,
        growth = 3,
        preferredSpecies = setOf(PixelPetSpecies.Penguin, PixelPetSpecies.Cat),
    ),
    MoonMilk(
        R.string.pixel_pet_food_moon_milk,
        18,
        hunger = 18,
        energy = 10,
        cleanliness = 2,
        mood = 8,
        growth = 2,
        preferredSpecies = setOf(PixelPetSpecies.Dog, PixelPetSpecies.Rabbit),
    ),
    CloudCandy(
        R.string.pixel_pet_food_cloud_candy,
        22,
        hunger = 12,
        energy = 14,
        cleanliness = -1,
        mood = 12,
        growth = 4,
        preferredSpecies = setOf(PixelPetSpecies.Bird),
    ),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetFoodKind? = entries.firstOrNull { it.name == value }
    }
}

data class PixelPetFurniture(
    val id: String = UUID.randomUUID().toString(),
    val kind: PixelPetFurnitureKind,
    val x: Float,
    val y: Float,
    val interactions: Int = 0,
    val durability: Int = 100,
    val rotationQuarterTurns: Int = 0,
    val layer: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("kind", kind.name)
        .put("x", x.toDouble())
        .put("y", y.toDouble())
        .put("interactions", interactions)
        .put("durability", durability)
        .put("rotationQuarterTurns", rotationQuarterTurns)
        .put("layer", layer)

    companion object {
        fun fromJson(json: JSONObject): PixelPetFurniture? {
            val kind = PixelPetFurnitureKind.fromStored(json.optString("kind")) ?: return null
            return PixelPetFurniture(
                id = json.optString("id").takeIf(String::isNotBlank) ?: UUID.randomUUID().toString(),
                kind = kind,
                x = json.optDouble("x", 0.5).toFloat(),
                y = json.optDouble("y", 0.65).toFloat(),
                interactions = json.optInt("interactions", 0),
                durability = json.optInt("durability", 100),
                rotationQuarterTurns = json.optInt("rotationQuarterTurns", 0),
                layer = json.optInt("layer", 0),
            )
        }
    }
}

internal const val PIXEL_PET_SAVE_SCHEMA_VERSION = 5
internal const val MAX_PIXEL_PET_FURNITURE = 18
internal const val PIXEL_PET_FURNITURE_REPAIR_COST = 8
