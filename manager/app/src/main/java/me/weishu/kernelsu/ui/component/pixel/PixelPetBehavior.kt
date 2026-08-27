package me.weishu.kernelsu.ui.component.pixel

import androidx.compose.ui.geometry.Offset
import java.util.Calendar
import kotlin.math.abs

internal enum class PixelPetCareNeed {
    Incubating,
    Hungry,
    Tired,
    Dirty,
    Lonely,
    Restless,
    Shelter,
    Stargazing,
    Content,
}

internal data class PixelPetBehaviorPlan(
    val need: PixelPetCareNeed,
    val furnitureId: String,
    val furnitureKind: PixelPetFurnitureKind,
    val action: PixelPetAction,
    val targetX: Float,
    val targetY: Float,
)

internal enum class PixelPetDropAction {
    None,
    Feed,
    Explore,
}

internal fun pixelPetDropAction(x: Float, y: Float): PixelPetDropAction = when {
    y < 0.62f -> PixelPetDropAction.None
    x <= 0.28f -> PixelPetDropAction.Feed
    x >= 0.72f -> PixelPetDropAction.Explore
    else -> PixelPetDropAction.None
}

/**
 * Chooses a deterministic activity from the pet's current needs and the
 * furniture that is actually available in its habitat.
 */
internal object PixelPetBehaviorEngine {
    fun primaryNeed(state: PixelPetState): PixelPetCareNeed {
        if (!state.hatched) return PixelPetCareNeed.Incubating

        val needs = buildList {
            add(PixelPetCareNeed.Hungry to state.hunger)
            add(PixelPetCareNeed.Tired to minOf(state.energy, state.sleepQuality))
            add(PixelPetCareNeed.Dirty to state.cleanliness)
            add(PixelPetCareNeed.Lonely to state.moodValue)
            add(PixelPetCareNeed.Restless to state.exploration)
        }
        val lowest = needs.minBy { (_, value) -> value }
        return if (lowest.second >= 72) PixelPetCareNeed.Content else lowest.first
    }

    fun plan(
        state: PixelPetState,
        now: Long = System.currentTimeMillis(),
    ): PixelPetBehaviorPlan? {
        if (!state.enabled || !state.hatched) return null
        // Autonomous plans never preempt a visible manual action. The reducer
        // will revisit the same need after the current action settles.
        if (state.hasRecentAction(now)) return null
        val available = state.furniture.filter { it.durability > 0 }
        if (available.isEmpty()) return null

        val rankedNeeds = buildList {
            add(PixelPetCareNeed.Hungry to needScore(state.hunger, urgentAt = 35))
            add(PixelPetCareNeed.Tired to needScore(minOf(state.energy, state.sleepQuality), urgentAt = 38))
            add(PixelPetCareNeed.Dirty to needScore(state.cleanliness, urgentAt = 42))
            add(PixelPetCareNeed.Lonely to needScore(state.moodValue, urgentAt = 42))
            add(PixelPetCareNeed.Restless to needScore(state.exploration, urgentAt = 38))
        }.sortedByDescending { it.second }

        rankedNeeds.forEach { (need, score) ->
            if (score <= 28) return@forEach
            choose(available, kindsFor(need), state)?.let { return planFor(need, it) }
        }

        val weather = state.currentWeather(now)
        if (weather == PixelPetWeather.Drizzle || weather == PixelPetWeather.Mist) {
            choose(
                available,
                listOf(PixelPetFurnitureKind.Bed, PixelPetFurnitureKind.Lamp),
                state,
            )?.let { return planFor(PixelPetCareNeed.Shelter, it) }
        }
        if (weather == PixelPetWeather.Starlit || weather == PixelPetWeather.Meteor) {
            choose(
                available,
                listOf(PixelPetFurnitureKind.Aquarium, PixelPetFurnitureKind.Lamp),
                state,
            )?.let { return planFor(PixelPetCareNeed.Stargazing, it) }
        }
        if (isNight(now)) {
            choose(available, listOf(PixelPetFurnitureKind.Bed), state)
                ?.let { return planFor(PixelPetCareNeed.Tired, it) }
        }

        val personalityKinds = when (state.personality) {
            PixelPetPersonality.Curious -> listOf(PixelPetFurnitureKind.Aquarium, PixelPetFurnitureKind.Toy)
            PixelPetPersonality.Gentle -> listOf(PixelPetFurnitureKind.Plant, PixelPetFurnitureKind.Lamp)
            PixelPetPersonality.Playful -> listOf(PixelPetFurnitureKind.Toy, PixelPetFurnitureKind.Aquarium)
            PixelPetPersonality.Calm -> listOf(PixelPetFurnitureKind.Lamp, PixelPetFurnitureKind.Bed)
            PixelPetPersonality.Cheerful -> listOf(PixelPetFurnitureKind.Toy, PixelPetFurnitureKind.Plant)
        }
        // Species preference is evaluated before personality for ambient
        // behaviour. A penguin should naturally watch an aquarium, while a
        // dog gravitates toward a toy even if both have healthy statistics.
        val preferred = choose(
            available,
            (speciesFurnitureKinds(state.species) + personalityKinds).distinct(),
            state,
        )
        val fallback = preferred ?: available
            .sortedWith(compareBy<PixelPetFurniture> { it.interactions }.thenBy { it.id })
            .let { items -> items[((now / 45_000L) % items.size).toInt()] }
        return planFor(PixelPetCareNeed.Content, fallback)
    }

    fun targetPosition(item: PixelPetFurniture): Pair<Float, Float> {
        val sideOffset = if (item.x <= 0.5f) 0.07f else -0.07f
        val verticalOffset = when (item.kind) {
            PixelPetFurnitureKind.Bed -> -0.035f
            PixelPetFurnitureKind.FoodBowl -> -0.075f
            PixelPetFurnitureKind.Toy -> -0.055f
            PixelPetFurnitureKind.Lamp,
            PixelPetFurnitureKind.Plant,
            PixelPetFurnitureKind.Aquarium,
            -> -0.085f
        }
        return (item.x + sideOffset).coerceIn(0.04f, 0.96f) to
            (item.y + verticalOffset).coerceIn(0.08f, 0.88f)
    }

    fun movementPath(
        start: Offset,
        target: Offset,
        furniture: List<PixelPetFurniture>,
        targetFurnitureId: String? = null,
    ): List<Offset> {
        val boundedStart = start.coerceToHabitat()
        val boundedTarget = target.coerceToHabitat()
        val obstacles = furniture.filterNot { it.id == targetFurnitureId }.filter { item ->
            segmentPassesNear(boundedStart, boundedTarget, Offset(item.x, item.y))
        }
        if (obstacles.isEmpty()) return listOf(boundedTarget)

        val upperAisle = (obstacles.minOf(PixelPetFurniture::y) - 0.14f).coerceIn(0.12f, 0.78f)
        val lowerAisle = (obstacles.maxOf(PixelPetFurniture::y) + 0.12f).coerceIn(0.20f, 0.90f)
        val aisleY = listOf(upperAisle, lowerAisle)
            .minBy { candidate -> abs(candidate - boundedStart.y) + abs(candidate - boundedTarget.y) }
        return listOf(
            Offset(boundedStart.x, aisleY),
            Offset(boundedTarget.x, aisleY),
            boundedTarget,
        ).fold(emptyList()) { path, point ->
            if (path.lastOrNull()?.let { (it - point).getDistance() < 0.015f } == true) path else path + point
        }
    }

    fun resolveFreePosition(
        position: Offset,
        furniture: List<PixelPetFurniture>,
        ignoredFurnitureId: String? = null,
    ): Offset {
        var resolved = position.coerceToHabitat()
        furniture.filterNot { it.id == ignoredFurnitureId }.forEach { item ->
            val obstacle = Offset(item.x, item.y)
            if (abs(resolved.x - obstacle.x) < 0.085f && abs(resolved.y - obstacle.y) < 0.11f) {
                val above = (obstacle.y - 0.13f).coerceAtLeast(0.06f)
                val below = (obstacle.y + 0.13f).coerceAtMost(0.92f)
                resolved = resolved.copy(y = if (abs(resolved.y - above) <= abs(resolved.y - below)) above else below)
            }
        }
        return resolved
    }

    private fun planFor(need: PixelPetCareNeed, item: PixelPetFurniture): PixelPetBehaviorPlan {
        val (targetX, targetY) = targetPosition(item)
        return PixelPetBehaviorPlan(
            need = need,
            furnitureId = item.id,
            furnitureKind = item.kind,
            action = item.kind.interactionAction,
            targetX = targetX,
            targetY = targetY,
        )
    }

    private fun choose(
        available: List<PixelPetFurniture>,
        preferredKinds: List<PixelPetFurnitureKind>,
        state: PixelPetState,
    ): PixelPetFurniture? {
        preferredKinds.forEach { kind ->
            available
                .filter { it.kind == kind }
                .minWithOrNull(
                    compareBy<PixelPetFurniture> { it.interactions }
                        .thenBy { abs(it.x - state.lkmPositionX) + abs(it.y - state.lkmPositionY) }
                        .thenBy { it.id },
                )
                ?.let { return it }
        }
        return null
    }

    private fun kindsFor(need: PixelPetCareNeed): List<PixelPetFurnitureKind> = when (need) {
        PixelPetCareNeed.Hungry -> listOf(PixelPetFurnitureKind.FoodBowl)
        PixelPetCareNeed.Tired -> listOf(PixelPetFurnitureKind.Bed, PixelPetFurnitureKind.Lamp)
        PixelPetCareNeed.Dirty -> listOf(PixelPetFurnitureKind.Plant)
        PixelPetCareNeed.Lonely -> listOf(PixelPetFurnitureKind.Toy, PixelPetFurnitureKind.Plant)
        PixelPetCareNeed.Restless -> listOf(PixelPetFurnitureKind.Aquarium, PixelPetFurnitureKind.Toy)
        PixelPetCareNeed.Shelter -> listOf(PixelPetFurnitureKind.Bed, PixelPetFurnitureKind.Lamp)
        PixelPetCareNeed.Stargazing -> listOf(PixelPetFurnitureKind.Aquarium, PixelPetFurnitureKind.Lamp)
        PixelPetCareNeed.Incubating,
        PixelPetCareNeed.Content,
        -> emptyList()
    }

    private fun speciesFurnitureKinds(species: PixelPetSpecies?): List<PixelPetFurnitureKind> = when (species) {
        PixelPetSpecies.Cat -> listOf(PixelPetFurnitureKind.Aquarium, PixelPetFurnitureKind.Toy)
        PixelPetSpecies.Dog -> listOf(PixelPetFurnitureKind.Toy, PixelPetFurnitureKind.FoodBowl)
        PixelPetSpecies.Bird -> listOf(PixelPetFurnitureKind.Plant, PixelPetFurnitureKind.Aquarium)
        PixelPetSpecies.Rabbit -> listOf(PixelPetFurnitureKind.Plant, PixelPetFurnitureKind.FoodBowl)
        PixelPetSpecies.Penguin -> listOf(PixelPetFurnitureKind.Aquarium, PixelPetFurnitureKind.Lamp)
        PixelPetSpecies.Hamster -> listOf(PixelPetFurnitureKind.Toy, PixelPetFurnitureKind.FoodBowl)
        null -> emptyList()
    }

    private fun needScore(value: Int, urgentAt: Int): Int =
        (100 - value.coerceIn(0, 100)) + if (value <= urgentAt) 32 else 0

    private fun isNight(now: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 6
    }

    private fun segmentPassesNear(start: Offset, end: Offset, point: Offset): Boolean {
        val segment = end - start
        val lengthSquared = segment.x * segment.x + segment.y * segment.y
        if (lengthSquared <= 0.0001f) return false
        val relative = point - start
        val t = ((relative.x * segment.x + relative.y * segment.y) / lengthSquared).coerceIn(0f, 1f)
        val nearest = start + segment * t
        return (nearest - point).getDistance() < 0.11f
    }

    private fun Offset.coerceToHabitat(): Offset = Offset(
        x.coerceIn(0.02f, 0.98f),
        y.coerceIn(0.04f, 0.94f),
    )
}
