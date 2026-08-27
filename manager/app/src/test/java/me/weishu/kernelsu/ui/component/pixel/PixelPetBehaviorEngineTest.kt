package me.weishu.kernelsu.ui.component.pixel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelPetBehaviorEngineTest {
    @Test
    fun hungryPetChoosesFoodBowl() {
        val bowl = furniture(PixelPetFurnitureKind.FoodBowl, "bowl")
        val bed = furniture(PixelPetFurnitureKind.Bed, "bed")

        val plan = PixelPetBehaviorEngine.plan(pet(hunger = 8, furniture = listOf(bed, bowl)), now = 60_000L)

        assertEquals(PixelPetCareNeed.Hungry, plan?.need)
        assertEquals(bowl.id, plan?.furnitureId)
        assertEquals(PixelPetAction.Eating, plan?.action)
    }

    @Test
    fun tiredPetChoosesBed() {
        val bed = furniture(PixelPetFurnitureKind.Bed, "bed")

        val plan = PixelPetBehaviorEngine.plan(
            pet(energy = 12, sleepQuality = 18, furniture = listOf(bed)),
            now = 60_000L,
        )

        assertEquals(PixelPetCareNeed.Tired, plan?.need)
        assertEquals(PixelPetAction.Sleeping, plan?.action)
    }

    @Test
    fun lowCleanlinessChoosesPlant() {
        val plant = furniture(PixelPetFurnitureKind.Plant, "plant")

        val plan = PixelPetBehaviorEngine.plan(pet(cleanliness = 9, furniture = listOf(plant)), now = 60_000L)

        assertEquals(PixelPetCareNeed.Dirty, plan?.need)
        assertEquals(plant.id, plan?.furnitureId)
    }

    @Test
    fun lowMoodChoosesToy() {
        val toy = furniture(PixelPetFurnitureKind.Toy, "toy")

        val plan = PixelPetBehaviorEngine.plan(pet(moodValue = 11, furniture = listOf(toy)), now = 60_000L)

        assertEquals(PixelPetCareNeed.Lonely, plan?.need)
        assertEquals(toy.id, plan?.furnitureId)
    }

    @Test
    fun lowExplorationChoosesAquarium() {
        val aquarium = furniture(PixelPetFurnitureKind.Aquarium, "aquarium")

        val plan = PixelPetBehaviorEngine.plan(pet(exploration = 10, furniture = listOf(aquarium)), now = 60_000L)

        assertEquals(PixelPetCareNeed.Restless, plan?.need)
        assertEquals(aquarium.id, plan?.furnitureId)
    }

    @Test
    fun missingPreferredFurnitureFallsBackToAvailableItem() {
        val lamp = furniture(PixelPetFurnitureKind.Lamp, "lamp")

        val plan = PixelPetBehaviorEngine.plan(
            pet(hunger = 0, personality = PixelPetPersonality.Playful, furniture = listOf(lamp)),
            now = 12 * 60 * 60 * 1_000L,
        )

        assertEquals(lamp.id, plan?.furnitureId)
    }

    @Test
    fun speciesUsesItsPreferredFurnitureForAmbientActivity() {
        val toy = furniture(PixelPetFurnitureKind.Toy, "toy")
        val aquarium = furniture(PixelPetFurnitureKind.Aquarium, "aquarium")

        val plan = PixelPetBehaviorEngine.plan(
            pet(
                species = PixelPetSpecies.Penguin,
                personality = PixelPetPersonality.Playful,
                furniture = listOf(toy, aquarium),
            ),
            now = 12 * 60 * 60 * 1_000L,
        )

        assertEquals(aquarium.id, plan?.furnitureId)
        assertEquals(PixelPetAction.Watching, plan?.action)
    }

    @Test
    fun unavailableFurnitureProducesNoBehavior() {
        assertNull(PixelPetBehaviorEngine.plan(pet(furniture = emptyList()), now = 60_000L))
    }

    @Test
    fun drizzleSeeksShelterWhenCoreNeedsAreStable() {
        val lamp = furniture(PixelPetFurnitureKind.Lamp, "lamp")
        val state = pet(furniture = listOf(lamp)).copy(weatherOverride = PixelPetWeather.Drizzle)

        val plan = PixelPetBehaviorEngine.plan(state, now = 60_000L)

        assertEquals(PixelPetCareNeed.Shelter, plan?.need)
        assertEquals(lamp.id, plan?.furnitureId)
        assertEquals(PixelPetAction.Watching, plan?.action)
    }

    @Test
    fun autonomousPlanDoesNotInterruptCurrentAction() {
        val bowl = furniture(PixelPetFurnitureKind.FoodBowl, "bowl")
        val state = pet(
            hunger = 4,
            furniture = listOf(bowl),
            lastAction = PixelPetAction.Eating,
            lastActionAt = 60_000L,
        )

        assertNull(PixelPetBehaviorEngine.plan(state, now = 60_200L))
        val updated = PixelPetReducer.autoInteractFurniture(state, now = 60_200L)
        assertEquals(PixelPetAction.Eating, updated.lastAction)
        assertNull(updated.activeFurnitureId)
    }

    @Test
    fun automaticInteractionPersistsTargetAndAction() {
        val bowl = furniture(PixelPetFurnitureKind.FoodBowl, "bowl", x = 0.25f, y = 0.72f)
        val initial = pet(hunger = 8, furniture = listOf(bowl), lastFurnitureInteractionAt = 0L)

        val walking = PixelPetReducer.autoInteractFurniture(initial, now = 90_000L)
        val (targetX, targetY) = PixelPetBehaviorEngine.targetPosition(bowl)

        assertEquals(bowl.id, walking.activeFurnitureId)
        assertEquals(PixelPetAction.Walking, walking.lastAction)
        assertEquals(PixelPetAction.Eating, walking.queuedAction)
        assertEquals(targetX, walking.lkmPositionX, 0.0001f)
        assertEquals(targetY, walking.lkmPositionY, 0.0001f)
        assertEquals(initial.hunger, walking.hunger)

        val eating = PixelPetReducer.refresh(
            walking,
            now = 90_000L + PixelPetAction.Walking.durationMillis + 1L,
        )
        assertEquals(PixelPetAction.Eating, eating.lastAction)
        assertEquals(null, eating.queuedAction)
        assertTrue(eating.hunger > initial.hunger)
    }

    @Test
    fun refreshClearsFinishedFurnitureActivity() {
        val toy = furniture(PixelPetFurnitureKind.Toy, "toy")
        val state = pet(
            furniture = listOf(toy),
            activeFurnitureId = toy.id,
            lastAction = PixelPetAction.Playing,
            lastActionAt = 1_000L,
        )

        val refreshed = PixelPetReducer.refresh(state, now = 10_000L)

        assertNull(refreshed.activeFurnitureId)
    }

    @Test
    fun furnitureInteractionCannotBeQueuedTwiceWhileWalking() {
        val toy = furniture(PixelPetFurnitureKind.Toy, "toy")
        val walking = PixelPetReducer.interactWithFurniture(
            pet(furniture = listOf(toy)),
            toy.id,
            now = 20_000L,
        )
        val duplicate = PixelPetReducer.interactWithFurniture(walking, toy.id, now = 20_100L)

        assertEquals(PixelPetAction.Walking, duplicate.lastAction)
        assertEquals(PixelPetAction.Playing, duplicate.queuedAction)
        assertEquals(0, duplicate.furniture.single().interactions)
    }

    @Test
    fun furnitureDoesNotOverwriteAnActiveHigherPriorityAction() {
        val bed = furniture(PixelPetFurnitureKind.Bed, "bed")
        val eating = pet(
            furniture = listOf(bed),
            lastAction = PixelPetAction.Eating,
            lastActionAt = 20_000L,
        )

        val updated = PixelPetReducer.interactWithFurniture(eating, bed.id, now = 20_300L)

        assertEquals(eating, updated)
    }

    @Test
    fun dropTargetsOnlyActivateInsideBottomCorners() {
        assertEquals(PixelPetDropAction.Feed, pixelPetDropAction(0.20f, 0.80f))
        assertEquals(PixelPetDropAction.Explore, pixelPetDropAction(0.82f, 0.80f))
        assertEquals(PixelPetDropAction.None, pixelPetDropAction(0.50f, 0.80f))
        assertEquals(PixelPetDropAction.None, pixelPetDropAction(0.12f, 0.40f))
    }

    private fun pet(
        hunger: Int = 82,
        energy: Int = 82,
        cleanliness: Int = 82,
        moodValue: Int = 82,
        sleepQuality: Int = 82,
        exploration: Int = 82,
        species: PixelPetSpecies = PixelPetSpecies.Cat,
        personality: PixelPetPersonality = PixelPetPersonality.Curious,
        furniture: List<PixelPetFurniture>,
        activeFurnitureId: String? = null,
        lastFurnitureInteractionAt: Long = 0L,
        lastAction: PixelPetAction = PixelPetAction.Idle,
        lastActionAt: Long = 0L,
    ) = PixelPetState(
        enabled = true,
        hatched = true,
        species = species,
        hunger = hunger,
        energy = energy,
        cleanliness = cleanliness,
        moodValue = moodValue,
        sleepQuality = sleepQuality,
        exploration = exploration,
        personality = personality,
        furniture = furniture,
        activeFurnitureId = activeFurnitureId,
        lastFurnitureInteractionAt = lastFurnitureInteractionAt,
        lastAction = lastAction,
        lastActionAt = lastActionAt,
        lastWellbeingAt = 10_000L,
        lastNeedsAt = 10_000L,
    )

    private fun furniture(
        kind: PixelPetFurnitureKind,
        id: String,
        x: Float = 0.5f,
        y: Float = 0.7f,
    ) = PixelPetFurniture(id = id, kind = kind, x = x, y = y)
}
