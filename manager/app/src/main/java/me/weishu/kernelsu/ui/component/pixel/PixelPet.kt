package me.weishu.kernelsu.ui.component.pixel

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.PowerManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

const val PIXEL_PET_ENABLED_KEY = "pixel_pet_enabled"

private const val PIXEL_PET_PREFS = "pixel_pet"
private const val KEY_HATCHED = "hatched"
private const val KEY_NAME = "name"
private const val KEY_APPEARANCE = "appearance"
private const val KEY_HUNGER = "hunger"
private const val KEY_AFFECTION = "affection"
private const val KEY_GROWTH = "growth"
private const val KEY_COINS = "coins"
private const val KEY_LAST_CHECK_IN_DAY = "last_check_in_day"
private const val KEY_LAST_OBSERVED_DAY = "last_observed_day"
private const val KEY_CHECK_IN_STREAK = "check_in_streak"
private const val KEY_LAST_INTERACTION_AT = "last_interaction_at"
private const val KEY_LAST_ACTION = "last_action"
private const val KEY_LAST_ACTION_AT = "last_action_at"
private const val KEY_LAST_HABITAT_INTERACTION_AT = "last_habitat_interaction_at"
private const val KEY_TOTAL_HABITAT_INTERACTIONS = "total_habitat_interactions"
private const val KEY_LAST_WELLBEING_AT = "last_wellbeing_at"
private const val KEY_HABITAT = "habitat"
private const val KEY_ACCESSORY = "accessory"
private const val KEY_OWNED_ACCESSORIES = "owned_accessories"
private const val KEY_EQUIPPED_ACCESSORIES = "equipped_accessories"
private const val KEY_UNLOCKED_HABITATS = "unlocked_habitats"
private const val KEY_TOTAL_INTERACTIONS = "total_interactions"
private const val KEY_TOTAL_FEEDS = "total_feeds"
private const val KEY_HIGHEST_STREAK = "highest_streak"
private const val KEY_DAILY_TASK_DAY = "daily_task_day"
private const val KEY_DAILY_INTERACTIONS = "daily_interactions"
private const val KEY_DAILY_FEEDS = "daily_feeds"
private const val KEY_DAILY_HABITAT_INTERACTIONS = "daily_habitat_interactions"
private const val KEY_DAILY_HABITAT_CHANGED = "daily_habitat_changed"
private const val KEY_CLAIMED_DAILY_TASKS = "claimed_daily_tasks"
private const val KEY_CLAIMED_ACHIEVEMENTS = "claimed_achievements"
private const val KEY_SAVED_LOOKS = "saved_looks"
private const val KEY_REMINDER_ENABLED = "reminder_enabled"
private const val KEY_SPECIES = "species"
private const val KEY_INCUBATION_STARTED_AT = "incubation_started_at"
private const val KEY_TEACHING_ENERGY = "teaching_energy"
private const val KEY_TEACHINGS = "teachings"
private const val KEY_PERSONALITY = "personality"
private const val KEY_MEMORIES = "memories"
private const val KEY_WEATHER_OVERRIDE = "weather_override"
private const val KEY_QUEUED_ACTION = "queued_action"
private const val KEY_CHAT_MESSAGES = "chat_messages"
private const val KEY_LKM_POSITION_X = "lkm_position_x"
private const val KEY_LKM_POSITION_Y = "lkm_position_y"
private const val KEY_LKM_LANDSCAPE_POSITION_X = "lkm_landscape_position_x"
private const val KEY_LKM_LANDSCAPE_POSITION_Y = "lkm_landscape_position_y"
private const val KEY_LKM_POSITION_LOCKED = "lkm_position_locked"
private const val KEY_ENERGY = "energy"
private const val KEY_CLEANLINESS = "cleanliness"
private const val KEY_MOOD_VALUE = "mood_value"
private const val KEY_SLEEP_QUALITY = "sleep_quality"
private const val KEY_EXPLORATION = "exploration"
private const val KEY_LAST_NEEDS_AT = "last_needs_at"
private const val KEY_FURNITURE = "furniture"
private const val KEY_OWNED_FURNITURE = "owned_furniture"
private const val KEY_FOOD_INVENTORY = "food_inventory"
private const val KEY_ACTIVE_FURNITURE_ID = "active_furniture_id"
private const val KEY_LAST_FURNITURE_INTERACTION_AT = "last_furniture_interaction_at"
private const val MIN_INTERACTION_INTERVAL_MILLIS = 750L
internal const val MIN_HABITAT_INTERACTION_INTERVAL_MILLIS = 1_200L
private const val WELLBEING_INTERVAL_MILLIS = 4 * 60 * 60 * 1000L
private const val NEEDS_INTERVAL_MILLIS = 60 * 60 * 1000L
private const val MAX_OFFLINE_WELLBEING_STEPS = 42
private const val MAX_OFFLINE_NEEDS_STEPS = 36
private const val MAX_DAILY_INTERACTION_REWARDS = 30
private const val MAX_DAILY_HABITAT_REWARDS = 12
private const val MIN_FURNITURE_INTERACTION_INTERVAL_MILLIS = 18_000L
private const val MIN_FURNITURE_AUTOPLAY_INTERVAL_MILLIS = 45_000L
internal const val PIXEL_PET_INCUBATION_MILLIS = 5 * 60 * 1000L
private const val MAX_TEACHING_ENERGY = 12
private const val MAX_PET_TEACHINGS = 24
private const val MAX_PET_MEMORIES = 32
private const val MAX_PET_CHAT_MESSAGES = 40
internal const val DEFAULT_PIXEL_PET_POSITION_X = 0.72f
internal const val DEFAULT_PIXEL_PET_POSITION_Y = 0.46f

enum class PixelPetSpecies(
    @StringRes val labelRes: Int,
    val defaultName: String,
    val defaultPersonality: PixelPetPersonality,
) {
    Penguin(R.string.pixel_pet_species_penguin, "Pingo", PixelPetPersonality.Calm),
    Dog(R.string.pixel_pet_species_dog, "Momo", PixelPetPersonality.Playful),
    Cat(R.string.pixel_pet_species_cat, "Neko", PixelPetPersonality.Curious),
    Bird(R.string.pixel_pet_species_bird, "Tori", PixelPetPersonality.Cheerful),
    Rabbit(R.string.pixel_pet_species_rabbit, "Mimi", PixelPetPersonality.Gentle),
    Hamster(R.string.pixel_pet_species_hamster, "Hamu", PixelPetPersonality.Playful),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetSpecies? = entries.firstOrNull { it.name == value }
    }
}

/** A saved coat/feather palette; it changes material colors without changing the species silhouette. */
enum class PixelPetAppearance(@StringRes val labelRes: Int) {
    Natural(R.string.pixel_pet_appearance_natural),
    Dawn(R.string.pixel_pet_appearance_dawn),
    Frost(R.string.pixel_pet_appearance_frost),
    Dusk(R.string.pixel_pet_appearance_dusk),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetAppearance =
            entries.firstOrNull { it.name == value } ?: Natural
    }
}

enum class PixelPetPersonality(@StringRes val labelRes: Int, val promptTrait: String) {
    Curious(R.string.pixel_pet_personality_curious, "curious, observant, and eager to explore"),
    Gentle(R.string.pixel_pet_personality_gentle, "gentle, thoughtful, and reassuring"),
    Playful(R.string.pixel_pet_personality_playful, "playful, energetic, and affectionate"),
    Calm(R.string.pixel_pet_personality_calm, "calm, patient, and quietly kind"),
    Cheerful(R.string.pixel_pet_personality_cheerful, "bright, encouraging, and expressive"),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetPersonality =
            entries.firstOrNull { it.name == value } ?: Gentle
    }
}

enum class PixelPetWeather(@StringRes val labelRes: Int) {
    Clear(R.string.pixel_pet_weather_clear),
    Breezy(R.string.pixel_pet_weather_breezy),
    Drizzle(R.string.pixel_pet_weather_drizzle),
    Starlit(R.string.pixel_pet_weather_starlit),
    Meteor(R.string.pixel_pet_weather_meteor),
    Mist(R.string.pixel_pet_weather_mist),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetWeather? = entries.firstOrNull { it.name == value }
    }
}

enum class PixelPetMemoryKind {
    Milestone,
    Care,
    Habitat,
    Lesson,
}

data class PixelPetMemory(
    val kind: PixelPetMemoryKind,
    val text: String,
    val recordedAt: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("kind", kind.name)
        .put("text", text)
        .put("recordedAt", recordedAt)

    companion object {
        fun fromJson(json: JSONObject): PixelPetMemory? {
            val text = json.optString("text").trim().take(180)
            if (text.isBlank()) return null
            return PixelPetMemory(
                kind = PixelPetMemoryKind.entries.firstOrNull { it.name == json.optString("kind") }
                    ?: PixelPetMemoryKind.Care,
                text = text,
                recordedAt = json.optLong("recordedAt", System.currentTimeMillis()).coerceAtLeast(0L),
            )
        }
    }
}

enum class PixelPetChatRole {
    User,
    Pet,
}

enum class PixelPetChatMessageStatus {
    Ready,
    Generating,
    Error,
    Stopped,
}

enum class PixelPetAction(
    val durationMillis: Long,
    @StringRes val labelRes: Int,
) {
    Idle(0L, R.string.pixel_pet_action_idle),
    Walking(1_800L, R.string.pixel_pet_action_walking),
    Eating(2_300L, R.string.pixel_pet_action_eating),
    Happy(2_400L, R.string.pixel_pet_action_happy),
    Sleeping(6_500L, R.string.pixel_pet_action_sleeping),
    Exploring(3_200L, R.string.pixel_pet_action_exploring),
    Hatching(1_800L, R.string.pixel_pet_action_hatching),
    Frightened(1_700L, R.string.pixel_pet_action_frightened),
    Petted(2_000L, R.string.pixel_pet_action_petted),
    Playing(2_800L, R.string.pixel_pet_action_playing),
    Watching(2_100L, R.string.pixel_pet_action_watching),
    Cleaning(2_400L, R.string.pixel_pet_action_cleaning),
    Calling(1_800L, R.string.pixel_pet_action_calling),
    ;

    val priority: Int
        get() = when (this) {
            Hatching -> 100
            Eating -> 90
            Frightened -> 85
            Petted -> 74
            Playing -> 68
            Exploring -> 62
            Calling -> 56
            Cleaning -> 52
            Watching -> 46
            Walking -> 42
            Happy -> 36
            Sleeping -> 20
            Idle -> 0
        }
}

enum class PixelPetGrowthStage(
    @StringRes val labelRes: Int,
    /**
     * Native artboard used by the editable PNG source. The renderer
     * normalizes every artboard to the shared card baseline, so this is an
     * art-detail contract rather than a second display scale.
     */
    val sourceCanvasSize: Int,
    /** Native v5 semantic Sprite artboard. */
    val spriteCanvasSize: Int,
) {
    // Reference PNGs retain their audited recovery sizes. Runtime v5 Sprites
    // use the native pixel-game sizes in the final column.
    Egg(R.string.pixel_pet_stage_egg, sourceCanvasSize = 32, spriteCanvasSize = 16),
    Baby(R.string.pixel_pet_stage_baby, sourceCanvasSize = 48, spriteCanvasSize = 16),
    Young(R.string.pixel_pet_stage_young, sourceCanvasSize = 64, spriteCanvasSize = 32),
    Adult(R.string.pixel_pet_stage_adult, sourceCanvasSize = 96, spriteCanvasSize = 48),
}

/** The LKM card is a compact stage; habitat mode opens the same world wider. */
enum class PixelPetStageMode {
    Card,
    Immersive,
}

data class PixelPetChatMessage(
    val role: PixelPetChatRole,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: PixelPetChatMessageStatus = PixelPetChatMessageStatus.Ready,
    val id: String = UUID.randomUUID().toString(),
) {
    fun toJson(): JSONObject = JSONObject()
        .put("role", role.name)
        .put("text", text)
        .put("createdAt", createdAt)
        .put("status", status.name)
        .put("id", id)

    companion object {
        fun fromJson(json: JSONObject): PixelPetChatMessage? {
            val text = json.optString("text").trim().take(600)
            if (text.isBlank()) return null
            val role = PixelPetChatRole.entries.firstOrNull { it.name == json.optString("role") }
                ?: PixelPetChatRole.Pet
            return PixelPetChatMessage(
                role = role,
                text = text,
                createdAt = json.optLong("createdAt", System.currentTimeMillis()).coerceAtLeast(0L),
                status = PixelPetChatMessageStatus.entries.firstOrNull {
                    it.name == json.optString("status")
                } ?: PixelPetChatMessageStatus.Ready,
                id = json.optString("id").takeIf(String::isNotBlank)
                    ?: UUID.randomUUID().toString(),
            )
        }
    }
}

enum class PixelPetHabitat(
    @StringRes val labelRes: Int,
    val unlockLevel: Int,
) {
    Garden(R.string.pixel_pet_habitat_garden, 1),
    Cloud(R.string.pixel_pet_habitat_cloud, 2),
    Moon(R.string.pixel_pet_habitat_moon, 4),
    Lagoon(R.string.pixel_pet_habitat_lagoon, 6),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetHabitat = entries.firstOrNull { it.name == value } ?: Garden
    }
}

enum class PixelPetAccessorySlot(@StringRes val labelRes: Int) {
    Head(R.string.pixel_pet_accessory_slot_head),
    Back(R.string.pixel_pet_accessory_slot_back),
    Hand(R.string.pixel_pet_accessory_slot_hand),
    Neck(R.string.pixel_pet_accessory_slot_neck),
    Tail(R.string.pixel_pet_accessory_slot_tail),
    Trail(R.string.pixel_pet_accessory_slot_trail),
}

enum class PixelPetAccessory(
    @StringRes val labelRes: Int,
    val cost: Int,
    val slot: PixelPetAccessorySlot,
) {
    LeafCrown(R.string.pixel_pet_item_leaf_crown, 24, PixelPetAccessorySlot.Head),
    StarPin(R.string.pixel_pet_item_star_pin, 38, PixelPetAccessorySlot.Head),
    ShellBag(R.string.pixel_pet_item_shell_bag, 52, PixelPetAccessorySlot.Back),
    WateringCan(R.string.pixel_pet_item_watering_can, 44, PixelPetAccessorySlot.Hand),
    CloudKite(R.string.pixel_pet_item_cloud_kite, 58, PixelPetAccessorySlot.Hand),
    MoonLantern(R.string.pixel_pet_item_moon_lantern, 72, PixelPetAccessorySlot.Hand),
    LagoonRod(R.string.pixel_pet_item_lagoon_rod, 86, PixelPetAccessorySlot.Hand),
    AuroraScarf(R.string.pixel_pet_item_aurora_scarf, 64, PixelPetAccessorySlot.Neck),
    CometBell(R.string.pixel_pet_item_comet_bell, 46, PixelPetAccessorySlot.Neck),
    TailRibbon(R.string.pixel_pet_item_tail_ribbon, 58, PixelPetAccessorySlot.Tail),
    PawSpark(R.string.pixel_pet_item_paw_spark, 74, PixelPetAccessorySlot.Trail),
    ;

    companion object {
        fun fromStored(value: String?): PixelPetAccessory? = entries.firstOrNull { it.name == value }
    }
}

enum class PixelPetDailyTask(
    @StringRes val labelRes: Int,
    val coinsReward: Int,
    val growthReward: Int,
) {
    CheckIn(R.string.pixel_pet_task_check_in, 8, 2),
    Interact(R.string.pixel_pet_task_interact, 6, 2),
    Feed(R.string.pixel_pet_task_feed, 7, 2),
    Explore(R.string.pixel_pet_task_explore, 8, 3),
}

enum class PixelPetAchievement(
    @StringRes val labelRes: Int,
    val coinsReward: Int,
    val growthReward: Int,
) {
    FirstHatch(R.string.pixel_pet_achievement_first_hatch, 12, 3),
    Friendly(R.string.pixel_pet_achievement_friendly, 26, 5),
    Dedicated(R.string.pixel_pet_achievement_dedicated, 42, 8),
    Collector(R.string.pixel_pet_achievement_collector, 64, 10),
    Explorer(R.string.pixel_pet_achievement_explorer, 72, 12),
    Veteran(R.string.pixel_pet_achievement_veteran, 96, 16),
}

data class PixelPetLook(
    val habitat: PixelPetHabitat,
    val accessories: Set<PixelPetAccessory>,
) {
    fun encode(): String = buildString {
        append(habitat.name)
        append(':')
        append(accessories.sortedBy(PixelPetAccessory::ordinal).joinToString(",") { it.name })
    }

    companion object {
        fun decode(value: String): PixelPetLook? {
            val parts = value.split(':', limit = 2)
            val habitat = PixelPetHabitat.fromStored(parts.firstOrNull())
            val accessories = parts.getOrNull(1).orEmpty()
                .split(',')
                .mapNotNull(PixelPetAccessory::fromStored)
                .toSet()
            return PixelPetLook(habitat, accessories)
        }
    }
}

data class PixelPetState(
    val enabled: Boolean = false,
    val hatched: Boolean = false,
    val species: PixelPetSpecies? = null,
    val appearance: PixelPetAppearance = PixelPetAppearance.Natural,
    val incubationStartedAt: Long = 0L,
    val name: String = "Piko",
    val hunger: Int = 64,
    val affection: Int = 0,
    val growth: Int = 0,
    val coins: Int = 20,
    val lastCheckInDay: Long = Long.MIN_VALUE,
    val lastObservedDay: Long = Long.MIN_VALUE,
    val checkInStreak: Int = 0,
    val lastInteractionAt: Long = 0L,
    val lastAction: PixelPetAction = PixelPetAction.Idle,
    val lastActionAt: Long = 0L,
    val queuedAction: PixelPetAction? = null,
    val lastHabitatInteractionAt: Long = 0L,
    val totalHabitatInteractions: Int = 0,
    val lastWellbeingAt: Long = 0L,
    val habitat: PixelPetHabitat = PixelPetHabitat.Garden,
    val accessory: PixelPetAccessory? = null,
    val ownedAccessories: Set<PixelPetAccessory> = emptySet(),
    val equippedAccessories: Set<PixelPetAccessory> = emptySet(),
    val unlockedHabitats: Set<PixelPetHabitat> = setOf(PixelPetHabitat.Garden),
    val totalInteractions: Int = 0,
    val totalFeeds: Int = 0,
    val highestCheckInStreak: Int = 0,
    val dailyTaskDay: Long = Long.MIN_VALUE,
    val dailyInteractions: Int = 0,
    val dailyFeeds: Int = 0,
    val dailyHabitatInteractions: Int = 0,
    val dailyHabitatChanged: Boolean = false,
    val claimedDailyTasks: Set<PixelPetDailyTask> = emptySet(),
    val claimedAchievements: Set<PixelPetAchievement> = emptySet(),
    val savedLooks: List<PixelPetLook> = emptyList(),
    val reminderEnabled: Boolean = false,
    val teachingEnergy: Int = 0,
    val teachings: List<String> = emptyList(),
    val personality: PixelPetPersonality = PixelPetPersonality.Gentle,
    val memories: List<PixelPetMemory> = emptyList(),
    val weatherOverride: PixelPetWeather? = null,
    val chatMessages: List<PixelPetChatMessage> = emptyList(),
    val lkmPositionX: Float = 0.72f,
    val lkmPositionY: Float = 0.46f,
    val lkmLandscapePositionX: Float = 0.72f,
    val lkmLandscapePositionY: Float = 0.46f,
    val lkmPositionLocked: Boolean = false,
    val energy: Int = 72,
    val cleanliness: Int = 78,
    val moodValue: Int = 74,
    val sleepQuality: Int = 72,
    val exploration: Int = 48,
    val lastNeedsAt: Long = 0L,
    val furniture: List<PixelPetFurniture> = emptyList(),
    val ownedFurniture: Set<PixelPetFurnitureKind> = emptySet(),
    val foodInventory: Map<PixelPetFoodKind, Int> = emptyMap(),
    val activeFurnitureId: String? = null,
    val lastFurnitureInteractionAt: Long = 0L,
) {
    val level: Int get() = (growth / 25) + 1
    val growthInLevel: Int get() = growth % 25
    val growthStage: PixelPetGrowthStage
        get() = when {
            !hatched -> PixelPetGrowthStage.Egg
            growth < 25 -> PixelPetGrowthStage.Baby
            growth < 100 -> PixelPetGrowthStage.Young
            else -> PixelPetGrowthStage.Adult
        }
    val affectionLevel: Int get() = (affection / 100) + 1
    val affectionInLevel: Int get() = affection % 100
    val canCheckIn: Boolean
        get() {
            val currentDay = currentPixelPetDay()
            return currentDay >= lastObservedDay && lastCheckInDay < currentDay
        }
    val isHungry: Boolean get() = hunger < 35
    val isTired: Boolean get() = energy < 30
    val needsCare: Boolean get() = cleanliness < 30 || moodValue < 30
    val isIncubating: Boolean get() = !hatched && species != null && incubationStartedAt > 0L
    val currentAction: PixelPetAction get() = displayAction()
    fun incubationRemainingMillis(now: Long = System.currentTimeMillis()): Long =
        (PIXEL_PET_INCUBATION_MILLIS - (now - incubationStartedAt)).coerceAtLeast(0L)

    fun incubationProgress(now: Long = System.currentTimeMillis()): Float = when {
        !isIncubating -> if (hatched) 1f else 0f
        else -> ((now - incubationStartedAt).toFloat() / PIXEL_PET_INCUBATION_MILLIS)
            .coerceIn(0f, 1f)
    }

    fun isIncubationReady(now: Long = System.currentTimeMillis()): Boolean =
        isIncubating && incubationRemainingMillis(now) == 0L

    val equippedAccessoriesOrLegacy: Set<PixelPetAccessory>
        get() = equippedAccessories.ifEmpty { setOfNotNull(accessory) }
    val mood: PixelPetMood
        get() = when {
            !hatched -> PixelPetMood.Egg
            hunger <= 12 -> PixelPetMood.Frightened
            isHungry -> PixelPetMood.Hungry
            isPixelPetNight() -> PixelPetMood.Sleeping
            isTired -> PixelPetMood.Sleeping
            cleanliness < 30 -> PixelPetMood.Frightened
            moodValue >= 75 -> PixelPetMood.Content
            lastAction == PixelPetAction.Exploring -> PixelPetMood.Exploring
            canCheckIn -> PixelPetMood.Exploring
            else -> PixelPetMood.Content
        }

    val isSleeping: Boolean get() = hatched && isPixelPetNight()

    fun currentWeather(now: Long = System.currentTimeMillis()): PixelPetWeather =
        weatherOverride ?: automaticPixelPetWeather(habitat, now)

    fun hasRecentAction(now: Long = System.currentTimeMillis()): Boolean =
        lastAction != PixelPetAction.Idle &&
            now >= lastActionAt &&
            now - lastActionAt <= lastAction.durationMillis

    fun isTaskComplete(task: PixelPetDailyTask): Boolean = when (task) {
        PixelPetDailyTask.CheckIn -> !canCheckIn
        PixelPetDailyTask.Interact -> dailyInteractions >= 3
        PixelPetDailyTask.Feed -> dailyFeeds >= 1
        PixelPetDailyTask.Explore -> dailyHabitatChanged
    }

    fun isAchievementComplete(achievement: PixelPetAchievement): Boolean = when (achievement) {
        PixelPetAchievement.FirstHatch -> hatched
        PixelPetAchievement.Friendly -> totalInteractions >= 25
        PixelPetAchievement.Dedicated -> highestCheckInStreak >= 7
        PixelPetAchievement.Collector -> ownedAccessories.containsAll(PixelPetAccessory.entries)
        PixelPetAchievement.Explorer -> unlockedHabitats.containsAll(PixelPetHabitat.entries)
        PixelPetAchievement.Veteran -> level >= 6
    }
}

enum class PixelPetMood {
    Egg,
    Hungry,
    Frightened,
    Sleeping,
    Exploring,
    Content,
}

enum class PixelPetRestoreMode {
    Merge,
    Replace,
}

data class PixelPetBackupPreview(
    val schemaVersion: Int,
    val state: PixelPetState,
)

val LocalPixelPetState = staticCompositionLocalOf { PixelPetState() }

internal object PixelPetReducer {
    fun refresh(state: PixelPetState, now: Long = System.currentTimeMillis()): PixelPetState {
        val advanced = advanceAction(finishIncubation(state, now), now)
        val observedDay = maxOf(advanced.lastObservedDay, currentPixelPetDay(now))
        val observed = advanced.copy(lastObservedDay = observedDay)
        val incubated = if (observed.activeFurnitureId != null && !observed.hasRecentAction(now)) {
            observed.copy(activeFurnitureId = null)
        } else {
            observed
        }
        val daily = if (incubated.dailyTaskDay >= observedDay) {
            incubated
        } else {
            incubated.copy(
                dailyTaskDay = observedDay,
                dailyInteractions = 0,
                dailyFeeds = 0,
                dailyHabitatInteractions = 0,
                dailyHabitatChanged = false,
                claimedDailyTasks = emptySet(),
            )
        }
        if (!daily.hatched) return unlockEligible(daily)

        var updated = daily.copy(
            lastWellbeingAt = daily.lastWellbeingAt.takeIf { it > 0L } ?: now,
            lastNeedsAt = daily.lastNeedsAt.takeIf { it > 0L } ?: now,
        )
        if (daily.lastWellbeingAt > 0L && now > daily.lastWellbeingAt) {
            val elapsedSteps = ((now - daily.lastWellbeingAt) / WELLBEING_INTERVAL_MILLIS)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            if (elapsedSteps > 0) {
                val appliedSteps = elapsedSteps.coerceAtMost(MAX_OFFLINE_WELLBEING_STEPS)
                updated = updated.copy(
                    hunger = (updated.hunger - appliedSteps * 3).coerceAtLeast(0),
                    affection = (updated.affection - appliedSteps / 2).coerceAtLeast(0),
                    lastWellbeingAt = if (elapsedSteps > MAX_OFFLINE_WELLBEING_STEPS) {
                        now
                    } else {
                        daily.lastWellbeingAt + appliedSteps * WELLBEING_INTERVAL_MILLIS
                    },
                )
            }
        }
        if (daily.lastNeedsAt > 0L && now > daily.lastNeedsAt) {
            val elapsedSteps = ((now - daily.lastNeedsAt) / NEEDS_INTERVAL_MILLIS)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
            if (elapsedSteps > 0) {
                val appliedSteps = elapsedSteps.coerceAtMost(MAX_OFFLINE_NEEDS_STEPS)
                val night = isPixelPetNight(now)
                updated = updated.copy(
                    energy = if (night) {
                        (updated.energy + appliedSteps * 3).coerceAtMost(100)
                    } else {
                        (updated.energy - appliedSteps * 2).coerceAtLeast(0)
                    },
                    cleanliness = (updated.cleanliness - appliedSteps * 2).coerceAtLeast(0),
                    moodValue = (updated.moodValue - appliedSteps).coerceAtLeast(0),
                    sleepQuality = if (night) {
                        (updated.sleepQuality + appliedSteps * 2).coerceAtMost(100)
                    } else {
                        (updated.sleepQuality - appliedSteps).coerceAtLeast(0)
                    },
                    exploration = (updated.exploration - appliedSteps).coerceAtLeast(0),
                    lastNeedsAt = if (elapsedSteps > MAX_OFFLINE_NEEDS_STEPS) {
                        now
                    } else {
                        daily.lastNeedsAt + appliedSteps * NEEDS_INTERVAL_MILLIS
                    },
                )
            }
        }
        return unlockEligible(updated)
    }

    fun setEnabled(state: PixelPetState, enabled: Boolean): PixelPetState = state.copy(enabled = enabled)

    fun setAppearance(
        state: PixelPetState,
        appearance: PixelPetAppearance,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || !current.hatched || current.appearance == appearance) return current
        return remember(
            scheduleAction(current.copy(appearance = appearance), PixelPetAction.Happy, now),
            PixelPetMemoryKind.Milestone,
            "Changed to the ${appearance.name.lowercase()} appearance",
            now,
        )
    }

    fun chooseSpecies(
        state: PixelPetState,
        species: PixelPetSpecies,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || current.hatched || current.isIncubating) return current
        return current.copy(
            species = species,
            appearance = PixelPetAppearance.Natural,
            incubationStartedAt = now,
            lastAction = PixelPetAction.Hatching,
            lastActionAt = now,
            queuedAction = null,
            name = species.defaultName,
            personality = species.defaultPersonality,
            chatMessages = emptyList(),
            teachings = emptyList(),
            memories = emptyList(),
            teachingEnergy = 0,
        )
    }

    fun hatch(state: PixelPetState, now: Long = System.currentTimeMillis()): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || current.hatched || current.species == null || !current.isIncubationReady(now)) {
            return current
        }
        return finishIncubation(current, now)
    }

    private fun finishIncubation(state: PixelPetState, now: Long): PixelPetState {
        if (
            state.hatched ||
            state.species == null ||
            state.incubationStartedAt <= 0L ||
            now - state.incubationStartedAt < PIXEL_PET_INCUBATION_MILLIS
        ) {
            return state
        }
        return unlockEligible(
            remember(
                scheduleAction(
                    state.copy(
                hatched = true,
                incubationStartedAt = 0L,
                hunger = state.hunger.coerceAtLeast(72),
                affection = (state.affection + 4).coerceAtMost(999),
                growth = (state.growth + 3).coerceAtMost(99_999),
                coins = (state.coins + 10).coerceAtMost(99_999),
                lastWellbeingAt = now,
                    ),
                    PixelPetAction.Happy,
                    now,
                ),
                PixelPetMemoryKind.Milestone,
                "First hatch completed",
                now,
            ),
        )
    }

    fun interact(state: PixelPetState, now: Long): PixelPetState {
        val current = refresh(state, now)
        if (
            !current.enabled ||
            !current.hatched ||
            now < current.lastInteractionAt ||
            now - current.lastInteractionAt < MIN_INTERACTION_INTERVAL_MILLIS
        ) {
            return current
        }
        val rewardActive = current.dailyInteractions < MAX_DAILY_INTERACTION_REWARDS
        return unlockEligible(
            remember(
                scheduleAction(
                    current.copy(
                hunger = (current.hunger - 1).coerceAtLeast(0),
                energy = (current.energy + 2).coerceAtMost(100),
                moodValue = (current.moodValue + 4).coerceAtMost(100),
                exploration = (current.exploration + 3).coerceAtMost(100),
                affection = (current.affection + 2).coerceAtMost(999),
                growth = (current.growth + if (rewardActive) 1 else 0).coerceAtMost(99_999),
                coins = (current.coins + if (rewardActive) 1 else 0).coerceAtMost(99_999),
                lastInteractionAt = now,
                totalInteractions = (current.totalInteractions + 1).coerceAtMost(99_999),
                dailyInteractions = (current.dailyInteractions + 1).coerceAtMost(999),
                    ),
                    PixelPetAction.Playing,
                    now,
                ),
                PixelPetMemoryKind.Care,
                "Enjoys playful time with their owner",
                now,
            ),
        )
    }

    fun pet(state: PixelPetState, now: Long = System.currentTimeMillis()): PixelPetState {
        val current = refresh(state, now)
        if (
            !current.enabled ||
            !current.hatched ||
            now < current.lastInteractionAt ||
            now - current.lastInteractionAt < MIN_INTERACTION_INTERVAL_MILLIS
        ) {
            return current
        }
        val rewardActive = current.dailyInteractions < MAX_DAILY_INTERACTION_REWARDS
        return unlockEligible(
            remember(
                scheduleAction(
                    current.copy(
                        energy = (current.energy + 1).coerceAtMost(100),
                        moodValue = (current.moodValue + 6).coerceAtMost(100),
                        affection = (current.affection + 3).coerceAtMost(999),
                        growth = (current.growth + if (rewardActive) 1 else 0).coerceAtMost(99_999),
                        coins = (current.coins + if (rewardActive) 1 else 0).coerceAtMost(99_999),
                        lastInteractionAt = now,
                        totalInteractions = (current.totalInteractions + 1).coerceAtMost(99_999),
                        dailyInteractions = (current.dailyInteractions + 1).coerceAtMost(999),
                    ),
                    PixelPetAction.Petted,
                    now,
                ),
                PixelPetMemoryKind.Care,
                "Feels safe when gently petted",
                now,
            ),
        )
    }

    fun feed(state: PixelPetState, now: Long = System.currentTimeMillis()): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || !current.hatched || current.coins < 5 || current.hunger >= 100) return current
        return unlockEligible(
            remember(
                scheduleAction(
                    current.copy(
                hunger = (current.hunger + 18).coerceAtMost(100),
                energy = (current.energy + 4).coerceAtMost(100),
                cleanliness = (current.cleanliness - 1).coerceAtLeast(0),
                moodValue = (current.moodValue + 5).coerceAtMost(100),
                growth = (current.growth + 2).coerceAtMost(99_999),
                coins = current.coins - 5,
                teachingEnergy = (current.teachingEnergy + 1).coerceAtMost(MAX_TEACHING_ENERGY),
                totalFeeds = (current.totalFeeds + 1).coerceAtMost(99_999),
                dailyFeeds = (current.dailyFeeds + 1).coerceAtMost(999),
                    ),
                    PixelPetAction.Eating,
                    now,
                ),
                PixelPetMemoryKind.Care,
                "Looks forward to meal time",
                now,
            ),
        )
    }

    fun checkIn(state: PixelPetState, now: Long): PixelPetState {
        val current = refresh(state, now)
        val today = currentPixelPetDay(now)
        if (
            !current.enabled ||
                !current.hatched ||
                today < current.lastObservedDay ||
                current.lastCheckInDay >= today
        ) return current
        val streak = if (current.lastCheckInDay == previousPixelPetDay(now)) {
            (current.checkInStreak + 1).coerceAtMost(999)
        } else {
            1
        }
        val streakBonus = (streak - 1).coerceIn(0, 7)
        return unlockEligible(
            scheduleAction(
                current.copy(
                growth = (current.growth + 5 + streakBonus / 2).coerceAtMost(99_999),
                coins = (current.coins + 18 + streakBonus).coerceAtMost(99_999),
                affection = (current.affection + 3).coerceAtMost(999),
                lastCheckInDay = today,
                checkInStreak = streak,
                highestCheckInStreak = maxOf(current.highestCheckInStreak, streak),
                ),
                PixelPetAction.Happy,
                now,
            ),
        )
    }

    fun exploreHabitat(state: PixelPetState, now: Long): PixelPetState {
        val current = refresh(state, now)
        if (
            !current.enabled ||
            !current.hatched ||
            current.habitat !in current.unlockedHabitats ||
            now < current.lastHabitatInteractionAt ||
            now - current.lastHabitatInteractionAt < MIN_HABITAT_INTERACTION_INTERVAL_MILLIS
        ) {
            return current
        }
        val rewardActive = current.dailyHabitatInteractions < MAX_DAILY_HABITAT_REWARDS
        return unlockEligible(
            remember(
                scheduleAction(
                current.copy(
                energy = (current.energy + 5).coerceAtMost(100),
                moodValue = (current.moodValue + 5).coerceAtMost(100),
                exploration = (current.exploration + 8).coerceAtMost(100),
                affection = (current.affection + 2).coerceAtMost(999),
                growth = (current.growth + if (rewardActive) 2 else 0).coerceAtMost(99_999),
                coins = (current.coins + if (rewardActive) 2 else 0).coerceAtMost(99_999),
                lastHabitatInteractionAt = now,
                totalHabitatInteractions = (current.totalHabitatInteractions + 1).coerceAtMost(99_999),
                dailyHabitatInteractions = (current.dailyHabitatInteractions + 1).coerceAtMost(999),
                    ),
                    PixelPetAction.Exploring,
                    now,
                ),
                PixelPetMemoryKind.Habitat,
                "Explored the ${current.habitat.name.lowercase()} habitat",
                now,
            ),
        )
    }

    fun buyFurniture(state: PixelPetState, kind: PixelPetFurnitureKind): PixelPetState {
        val current = refresh(state)
        if (!current.enabled || !current.hatched || current.coins < kind.cost) return current
        return current.copy(
            coins = current.coins - kind.cost,
            ownedFurniture = current.ownedFurniture + kind,
        )
    }

    fun placeFurniture(
        state: PixelPetState,
        kind: PixelPetFurnitureKind,
        x: Float,
        y: Float,
    ): PixelPetState {
        val current = refresh(state)
        if (
            !current.enabled ||
            !current.hatched ||
            kind !in current.ownedFurniture ||
            current.furniture.size >= MAX_PIXEL_PET_FURNITURE
        ) return current
        val placed = current.furniture + PixelPetFurniture(
            kind = kind,
            x = x.coerceIn(0.06f, 0.94f),
            y = y.coerceIn(0.18f, 0.86f),
        )
        return current.copy(furniture = placed)
    }

    fun moveFurniture(
        state: PixelPetState,
        id: String,
        x: Float,
        y: Float,
    ): PixelPetState = state.copy(
        furniture = state.furniture.map { item ->
            if (item.id == id) {
                item.copy(
                    x = x.coerceIn(0.06f, 0.94f),
                    y = y.coerceIn(0.18f, 0.86f),
                )
            } else {
                item
            }
        },
    )

    fun removeFurniture(state: PixelPetState, id: String): PixelPetState = state.copy(
        furniture = state.furniture.filterNot { it.id == id },
        activeFurnitureId = state.activeFurnitureId.takeUnless { it == id },
    )

    fun repairFurniture(
        state: PixelPetState,
        id: String,
    ): PixelPetState {
        val current = refresh(state)
        val item = current.furniture.firstOrNull { it.id == id } ?: return current
        if (
            !current.enabled ||
            !current.hatched ||
            item.durability >= 100 ||
            current.coins < PIXEL_PET_FURNITURE_REPAIR_COST
        ) return current
        return current.copy(
            coins = current.coins - PIXEL_PET_FURNITURE_REPAIR_COST,
            furniture = current.furniture.map { furniture ->
                if (furniture.id == id) furniture.copy(durability = 100) else furniture
            },
        )
    }

    fun buyFood(state: PixelPetState, food: PixelPetFoodKind): PixelPetState {
        val current = refresh(state)
        if (!current.enabled || !current.hatched || current.coins < food.cost) return current
        return current.copy(
            coins = current.coins - food.cost,
            foodInventory = current.foodInventory + (food to ((current.foodInventory[food] ?: 0) + 1)),
        )
    }

    fun useFood(state: PixelPetState, food: PixelPetFoodKind, now: Long = System.currentTimeMillis()): PixelPetState {
        val current = refresh(state, now)
        val amount = current.foodInventory[food] ?: 0
        if (!current.enabled || !current.hatched || amount <= 0 || current.hunger >= 100) return current
        val preferred = food.preferredSpecies.contains(current.species)
        return unlockEligible(
            remember(
                scheduleAction(
                    current.copy(
                        hunger = (current.hunger + food.hunger + if (preferred) 4 else 0).coerceAtMost(100),
                        energy = (current.energy + food.energy).coerceAtMost(100),
                        cleanliness = (current.cleanliness + food.cleanliness).coerceIn(0, 100),
                        moodValue = (current.moodValue + food.mood + if (preferred) 4 else 0).coerceAtMost(100),
                        growth = (current.growth + food.growth + if (preferred) 1 else 0).coerceAtMost(99_999),
                        teachingEnergy = (current.teachingEnergy + 1).coerceAtMost(MAX_TEACHING_ENERGY),
                        totalFeeds = (current.totalFeeds + 1).coerceAtMost(99_999),
                        dailyFeeds = (current.dailyFeeds + 1).coerceAtMost(999),
                        foodInventory = current.foodInventory + (food to (amount - 1)),
                    ),
                    PixelPetAction.Eating,
                    now,
                ),
                PixelPetMemoryKind.Care,
                if (preferred) "Enjoyed a favorite ${food.name.lowercase()} meal" else "Tried a ${food.name.lowercase()} meal",
                now,
            ),
        )
    }

    fun interactWithFurniture(
        state: PixelPetState,
        id: String,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        // A rejected furniture tap must be a no-op while a higher-priority
        // visible action is still playing. In particular, do not refresh
        // daily bookkeeping as a side effect of a rejected interaction.
        if (
            state.queuedAction != null ||
            (state.activeFurnitureId != null && state.hasRecentAction(now)) ||
            (state.hasRecentAction(now) && state.lastAction.priority >= PixelPetAction.Walking.priority)
        ) return state
        val current = refresh(state, now)
        val item = current.furniture.firstOrNull { it.id == id }
        if (
            !current.enabled ||
            !current.hatched ||
            item == null ||
            item.durability <= 0 ||
            current.queuedAction != null ||
            (current.activeFurnitureId != null && current.hasRecentAction(now)) ||
            (current.hasRecentAction(now) && current.lastAction.priority >= PixelPetAction.Walking.priority) ||
            now - current.lastFurnitureInteractionAt < MIN_FURNITURE_INTERACTION_INTERVAL_MILLIS
        ) return current
        val (targetX, targetY) = PixelPetBehaviorEngine.targetPosition(item)
        return current.copy(
                    activeFurnitureId = id,
                    lkmPositionX = targetX,
                    lkmPositionY = targetY,
                    lkmLandscapePositionX = targetX,
                    lkmLandscapePositionY = targetY,
                    lastAction = PixelPetAction.Walking,
                    lastActionAt = now,
                    queuedAction = item.kind.interactionAction,
        )
    }

    fun autoInteractFurniture(state: PixelPetState, now: Long = System.currentTimeMillis()): PixelPetState {
        val current = refresh(state, now)
        if (
            !current.enabled ||
            !current.hatched ||
            current.furniture.isEmpty() ||
            now - current.lastFurnitureInteractionAt < MIN_FURNITURE_AUTOPLAY_INTERVAL_MILLIS
        ) return current
        val plan = PixelPetBehaviorEngine.plan(current, now) ?: return current
        return interactWithFurniture(current, plan.furnitureId, now)
    }

    fun buyOrEquip(
        state: PixelPetState,
        accessory: PixelPetAccessory,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || !current.hatched) return current
        val purchased = if (accessory in current.ownedAccessories) {
            current
        } else if (current.coins >= accessory.cost) {
            current.copy(
                coins = current.coins - accessory.cost,
                ownedAccessories = current.ownedAccessories + accessory,
            )
        } else {
            return current
        }
        return equip(purchased, accessory)
    }

    fun unequip(state: PixelPetState): PixelPetState = state.copy(
        accessory = null,
        equippedAccessories = emptySet(),
    )

    fun unequip(state: PixelPetState, slot: PixelPetAccessorySlot): PixelPetState {
        val equipped = state.equippedAccessoriesOrLegacy.filterNot { it.slot == slot }.toSet()
        return state.copy(
            accessory = equipped.firstOrNull(),
            equippedAccessories = equipped,
        )
    }

    fun setHabitat(
        state: PixelPetState,
        habitat: PixelPetHabitat,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || habitat !in current.unlockedHabitats) return current
        val changed = habitat != current.habitat
        val updated = scheduleAction(
            current.copy(
                habitat = habitat,
                dailyHabitatChanged = current.dailyHabitatChanged || changed,
            ),
            PixelPetAction.Exploring,
            now,
        )
        return if (changed) {
            remember(
                updated,
                PixelPetMemoryKind.Habitat,
                "Moved into the ${habitat.name.lowercase()} habitat",
                now,
            )
        } else {
            updated
        }
    }

    fun setPersonality(
        state: PixelPetState,
        personality: PixelPetPersonality,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || !current.hatched) return current
        if (current.personality == personality) return current
        return remember(
            scheduleAction(current.copy(personality = personality), PixelPetAction.Watching, now),
            PixelPetMemoryKind.Milestone,
            "Grew into a ${personality.name.lowercase()} personality",
            now,
        )
    }

    fun setWeatherOverride(
        state: PixelPetState,
        weather: PixelPetWeather?,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || !current.hatched) return current
        if (current.weatherOverride == weather) return current
        val updated = scheduleAction(
            current.copy(weatherOverride = weather),
            PixelPetAction.Watching,
            now,
        )
        return weather?.let {
            remember(
                updated,
                PixelPetMemoryKind.Habitat,
                "Watched a ${it.name.lowercase()} sky in ${current.habitat.name.lowercase()}",
                now,
            )
        } ?: updated
    }

    fun saveLook(state: PixelPetState): PixelPetState {
        val look = PixelPetLook(state.habitat, state.equippedAccessoriesOrLegacy)
        val savedLooks = (state.savedLooks - look + look).takeLast(6)
        return state.copy(savedLooks = savedLooks)
    }

    fun applyLook(state: PixelPetState, look: PixelPetLook): PixelPetState {
        if (look.habitat !in state.unlockedHabitats || !state.ownedAccessories.containsAll(look.accessories)) {
            return state
        }
        val equipped = look.accessories.groupBy(PixelPetAccessory::slot).values.map { it.first() }.toSet()
        return state.copy(
            habitat = look.habitat,
            accessory = equipped.firstOrNull(),
            equippedAccessories = equipped,
        )
    }

    fun claimDailyTask(
        state: PixelPetState,
        task: PixelPetDailyTask,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || !current.hatched || !current.isTaskComplete(task) || task in current.claimedDailyTasks) {
            return current
        }
        return unlockEligible(
            current.copy(
                coins = (current.coins + task.coinsReward).coerceAtMost(99_999),
                growth = (current.growth + task.growthReward).coerceAtMost(99_999),
                claimedDailyTasks = current.claimedDailyTasks + task,
            ),
        )
    }

    fun claimAchievement(
        state: PixelPetState,
        achievement: PixelPetAchievement,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState {
        val current = refresh(state, now)
        if (!current.enabled || !current.hatched || !current.isAchievementComplete(achievement) || achievement in current.claimedAchievements) {
            return current
        }
        return unlockEligible(
            current.copy(
                coins = (current.coins + achievement.coinsReward).coerceAtMost(99_999),
                growth = (current.growth + achievement.growthReward).coerceAtMost(99_999),
                claimedAchievements = current.claimedAchievements + achievement,
            ),
        )
    }

    fun setReminder(state: PixelPetState, enabled: Boolean): PixelPetState = state.copy(reminderEnabled = enabled)

    fun teach(state: PixelPetState, lesson: String): PixelPetState {
        val text = lesson.trim().replace(Regex("\\s+"), " ").take(180)
        if (!state.enabled || !state.hatched || state.teachingEnergy <= 0 || text.isBlank()) return state
        val teachings = (state.teachings.filterNot { it.equals(text, ignoreCase = true) } + text)
            .takeLast(MAX_PET_TEACHINGS)
        val now = System.currentTimeMillis()
        return unlockEligible(
            remember(
                scheduleAction(
                    state.copy(
                teachingEnergy = state.teachingEnergy - 1,
                teachings = teachings,
                affection = (state.affection + 3).coerceAtMost(999),
                growth = (state.growth + 2).coerceAtMost(99_999),
                    ),
                    PixelPetAction.Calling,
                    now,
                ),
                PixelPetMemoryKind.Lesson,
                "Owner taught: $text",
                now,
            ),
        )
    }

    fun appendChatMessage(state: PixelPetState, message: PixelPetChatMessage): PixelPetState {
        val text = message.text.trim().take(600)
        if (text.isBlank()) return state
        return state.copy(
            chatMessages = (state.chatMessages + message.copy(text = text)).takeLast(MAX_PET_CHAT_MESSAGES),
        )
    }

    fun replaceChatMessage(state: PixelPetState, message: PixelPetChatMessage): PixelPetState {
        val index = state.chatMessages.indexOfFirst { it.id == message.id }
        if (index < 0) return state
        return state.copy(
            chatMessages = state.chatMessages.map { current ->
                if (current.id == message.id) message.copy(text = message.text.trim().take(600)) else current
            },
        )
    }

    fun clearChatMessages(state: PixelPetState): PixelPetState = state.copy(chatMessages = emptyList())

    fun mergeBackup(current: PixelPetState, incoming: PixelPetState): PixelPetState {
        val preferredName = current.name.takeUnless { it == "Piko" }.orEmpty()
        val foodInventory = PixelPetFoodKind.entries.associateWith { food ->
            maxOf(current.foodInventory[food] ?: 0, incoming.foodInventory[food] ?: 0)
        }.filterValues { it > 0 }
        return sanitizePixelPetState(
            current.copy(
                enabled = current.enabled,
                hatched = current.hatched || incoming.hatched,
                species = current.species ?: incoming.species,
                incubationStartedAt = if (current.species != null) current.incubationStartedAt else incoming.incubationStartedAt,
                name = preferredName.ifBlank { incoming.name },
                hunger = maxOf(current.hunger, incoming.hunger),
                affection = maxOf(current.affection, incoming.affection),
                growth = maxOf(current.growth, incoming.growth),
                coins = maxOf(current.coins, incoming.coins),
                checkInStreak = maxOf(current.checkInStreak, incoming.checkInStreak),
                highestCheckInStreak = maxOf(current.highestCheckInStreak, incoming.highestCheckInStreak),
                totalInteractions = maxOf(current.totalInteractions, incoming.totalInteractions),
                totalFeeds = maxOf(current.totalFeeds, incoming.totalFeeds),
                energy = maxOf(current.energy, incoming.energy),
                cleanliness = maxOf(current.cleanliness, incoming.cleanliness),
                moodValue = maxOf(current.moodValue, incoming.moodValue),
                sleepQuality = maxOf(current.sleepQuality, incoming.sleepQuality),
                exploration = maxOf(current.exploration, incoming.exploration),
                habitat = if (current.habitat != PixelPetHabitat.Garden) current.habitat else incoming.habitat,
                ownedAccessories = current.ownedAccessories + incoming.ownedAccessories,
                equippedAccessories = current.equippedAccessories + incoming.equippedAccessories,
                unlockedHabitats = current.unlockedHabitats + incoming.unlockedHabitats,
                ownedFurniture = current.ownedFurniture + incoming.ownedFurniture,
                furniture = (current.furniture + incoming.furniture).distinctBy(PixelPetFurniture::id),
                foodInventory = foodInventory,
                teachings = (current.teachings + incoming.teachings).distinct().takeLast(MAX_PET_TEACHINGS),
                memories = (current.memories + incoming.memories).distinctBy { it.kind to it.text }
                    .takeLast(MAX_PET_MEMORIES),
                savedLooks = (current.savedLooks + incoming.savedLooks).distinct().takeLast(6),
                chatMessages = (current.chatMessages + incoming.chatMessages).takeLast(MAX_PET_CHAT_MESSAGES),
                lkmPositionX = current.lkmPositionX,
                lkmPositionY = current.lkmPositionY,
                lkmLandscapePositionX = current.lkmLandscapePositionX,
                lkmLandscapePositionY = current.lkmLandscapePositionY,
                lkmPositionLocked = current.lkmPositionLocked,
            ),
        )
    }

    fun setLkmPosition(
        state: PixelPetState,
        x: Float,
        y: Float,
        now: Long = System.currentTimeMillis(),
        snap: Boolean = true,
        landscape: Boolean = false,
    ): PixelPetState = scheduleAction(
        if (landscape) {
            state.copy(
                lkmLandscapePositionX = if (snap) snapPixelPetPosition(x) else x.coerceIn(0f, 1f),
                lkmLandscapePositionY = if (snap) snapPixelPetPosition(y) else y.coerceIn(0f, 1f),
            )
        } else {
            state.copy(
                lkmPositionX = if (snap) snapPixelPetPosition(x) else x.coerceIn(0f, 1f),
                lkmPositionY = if (snap) snapPixelPetPosition(y) else y.coerceIn(0f, 1f),
            )
        },
        PixelPetAction.Walking,
        now,
    )

    fun resetLkmPosition(state: PixelPetState): PixelPetState = state.copy(
        lkmPositionX = DEFAULT_PIXEL_PET_POSITION_X,
        lkmPositionY = DEFAULT_PIXEL_PET_POSITION_Y,
        lkmLandscapePositionX = DEFAULT_PIXEL_PET_POSITION_X,
        lkmLandscapePositionY = DEFAULT_PIXEL_PET_POSITION_Y,
    )

    fun setLkmPositionLocked(state: PixelPetState, locked: Boolean): PixelPetState =
        state.copy(lkmPositionLocked = locked)

    fun replaceFurnitureLayout(
        state: PixelPetState,
        furniture: List<PixelPetFurniture>,
    ): PixelPetState = sanitizePixelPetState(state.copy(furniture = furniture))

    fun rotateFurniture(state: PixelPetState, id: String): PixelPetState = state.copy(
        furniture = state.furniture.map { item ->
            if (item.id == id) item.copy(rotationQuarterTurns = (item.rotationQuarterTurns + 1).mod(4)) else item
        },
    )

    fun moveFurnitureLayer(state: PixelPetState, id: String, delta: Int): PixelPetState = state.copy(
        furniture = state.furniture.map { item ->
            if (item.id == id) item.copy(layer = (item.layer + delta).coerceIn(-4, 4)) else item
        },
    )

    fun resetProgress(state: PixelPetState): PixelPetState = PixelPetState(enabled = state.enabled)

    fun rename(state: PixelPetState, name: String): PixelPetState = state.copy(
        name = name.trim().take(20).ifBlank { "Piko" },
    )

    private fun equip(state: PixelPetState, accessory: PixelPetAccessory): PixelPetState {
        val equipped = (state.equippedAccessoriesOrLegacy - state.equippedAccessoriesOrLegacy.filter {
            it.slot == accessory.slot
        }.toSet()) + accessory
        return state.copy(
            accessory = equipped.firstOrNull(),
            equippedAccessories = equipped,
        )
    }

    private fun unlockEligible(state: PixelPetState): PixelPetState {
        val equipped = state.equippedAccessoriesOrLegacy.filter { it in state.ownedAccessories }.toSet()
        return state.copy(
            accessory = equipped.firstOrNull(),
            equippedAccessories = equipped,
            unlockedHabitats = state.unlockedHabitats + PixelPetHabitat.entries.filter { state.level >= it.unlockLevel },
        )
    }

    private fun scheduleAction(
        state: PixelPetState,
        action: PixelPetAction,
        now: Long,
    ): PixelPetState {
        val active = state.lastAction.takeIf { state.hasRecentAction(now) }
        return if (active != null && active.priority >= action.priority) {
            state.copy(queuedAction = state.queuedAction ?: action)
        } else {
            state.copy(
                lastAction = action,
                lastActionAt = now,
                queuedAction = null,
            )
        }
    }

    private fun advanceAction(state: PixelPetState, now: Long): PixelPetState {
        val queued = state.queuedAction ?: return state
        return if (state.hasRecentAction(now)) {
            state
        } else {
            val arrived = state.copy(
                lastAction = queued,
                lastActionAt = now,
                queuedAction = null,
            )
            val furniture = arrived.activeFurnitureId
                ?.let { id -> arrived.furniture.firstOrNull { it.id == id } }
            if (furniture != null && furniture.kind.interactionAction == queued) {
                completeFurnitureInteraction(arrived, furniture, now)
            } else {
                arrived
            }
        }
    }

    private fun completeFurnitureInteraction(
        state: PixelPetState,
        item: PixelPetFurniture,
        now: Long,
    ): PixelPetState {
        val updatedItem = item.copy(
            interactions = (item.interactions + 1).coerceAtMost(99_999),
            durability = (item.durability - 1).coerceAtLeast(0),
        )
        val adjusted = when (item.kind) {
            PixelPetFurnitureKind.FoodBowl -> state.copy(hunger = (state.hunger + 4).coerceAtMost(100))
            PixelPetFurnitureKind.Bed -> state.copy(
                energy = (state.energy + 10).coerceAtMost(100),
                sleepQuality = (state.sleepQuality + 7).coerceAtMost(100),
            )
            PixelPetFurnitureKind.Toy -> state.copy(
                moodValue = (state.moodValue + 7).coerceAtMost(100),
                exploration = (state.exploration + 4).coerceAtMost(100),
            )
            PixelPetFurnitureKind.Lamp -> state.copy(sleepQuality = (state.sleepQuality + 4).coerceAtMost(100))
            PixelPetFurnitureKind.Plant -> state.copy(
                cleanliness = (state.cleanliness + 5).coerceAtMost(100),
                moodValue = (state.moodValue + 3).coerceAtMost(100),
            )
            PixelPetFurnitureKind.Aquarium -> state.copy(
                moodValue = (state.moodValue + 4).coerceAtMost(100),
                exploration = (state.exploration + 5).coerceAtMost(100),
            )
        }
        return unlockEligible(
            remember(
                adjusted.copy(
                    furniture = adjusted.furniture.map { if (it.id == item.id) updatedItem else it },
                    lastFurnitureInteractionAt = now,
                    totalHabitatInteractions = (adjusted.totalHabitatInteractions + 1).coerceAtMost(99_999),
                    dailyHabitatInteractions = (adjusted.dailyHabitatInteractions + 1).coerceAtMost(999),
                ),
                PixelPetMemoryKind.Habitat,
                "Spent time with the ${item.kind.name.lowercase()}",
                now,
            ),
        )
    }

    private fun remember(
        state: PixelPetState,
        kind: PixelPetMemoryKind,
        text: String,
        now: Long,
    ): PixelPetState {
        val clean = text.trim().take(180)
        if (clean.isBlank()) return state
        val memories = state.memories.filterNot { it.kind == kind && it.text == clean } +
            PixelPetMemory(kind = kind, text = clean, recordedAt = now)
        return state.copy(memories = memories.takeLast(MAX_PET_MEMORIES))
    }
}

private fun snapPixelPetPosition(value: Float): Float {
    val bounded = value.coerceIn(0f, 1f)
    val anchor = listOf(0.08f, 0.28f, 0.50f, 0.72f, 0.92f)
        .minByOrNull { kotlin.math.abs(it - bounded) }
    return if (anchor != null && kotlin.math.abs(anchor - bounded) <= 0.035f) anchor else bounded
}

private fun isPixelPetNight(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    return hour >= 22 || hour < 6
}

private fun automaticPixelPetWeather(
    habitat: PixelPetHabitat,
    nowMillis: Long,
): PixelPetWeather {
    if (isPixelPetNight(nowMillis)) {
        return if (habitat == PixelPetHabitat.Moon) PixelPetWeather.Meteor else PixelPetWeather.Starlit
    }
    val day = currentPixelPetDay(nowMillis)
    return when (((day + habitat.ordinal) % 3L).toInt()) {
        0 -> PixelPetWeather.Clear
        1 -> when (habitat) {
            PixelPetHabitat.Garden,
            PixelPetHabitat.Lagoon,
            -> PixelPetWeather.Drizzle
            else -> PixelPetWeather.Breezy
        }
        else -> when (habitat) {
            PixelPetHabitat.Lagoon -> PixelPetWeather.Mist
            PixelPetHabitat.Moon -> PixelPetWeather.Meteor
            else -> PixelPetWeather.Breezy
        }
    }
}

internal data class PixelPetSnapshot(
    val enabled: Boolean,
    val hatched: Boolean,
    val species: String? = null,
    val appearance: String = PixelPetAppearance.Natural.name,
    val incubationStartedAt: Long = 0L,
    val name: String,
    val hunger: Int,
    val affection: Int,
    val growth: Int,
    val coins: Int,
    val lastCheckInDay: Long,
    val lastObservedDay: Long = Long.MIN_VALUE,
    val checkInStreak: Int,
    val lastInteractionAt: Long,
    val lastAction: String = PixelPetAction.Idle.name,
    val lastActionAt: Long = 0L,
    val queuedAction: String? = null,
    val lastHabitatInteractionAt: Long = 0L,
    val totalHabitatInteractions: Int = 0,
    val lastWellbeingAt: Long,
    val habitat: String,
    val accessory: String?,
    val ownedAccessories: Set<String>,
    val equippedAccessories: Set<String>,
    val unlockedHabitats: Set<String>,
    val totalInteractions: Int,
    val totalFeeds: Int,
    val highestCheckInStreak: Int,
    val dailyTaskDay: Long,
    val dailyInteractions: Int,
    val dailyFeeds: Int,
    val dailyHabitatInteractions: Int = 0,
    val dailyHabitatChanged: Boolean,
    val claimedDailyTasks: Set<String>,
    val claimedAchievements: Set<String>,
    val savedLooks: List<String>,
    val reminderEnabled: Boolean,
    val teachingEnergy: Int = 0,
    val teachings: List<String> = emptyList(),
    val personality: String = PixelPetPersonality.Gentle.name,
    val memories: List<PixelPetMemory> = emptyList(),
    val weatherOverride: String? = null,
    val chatMessages: List<PixelPetChatMessage> = emptyList(),
    val lkmPositionX: Float = 0.72f,
    val lkmPositionY: Float = 0.46f,
    val lkmLandscapePositionX: Float = 0.72f,
    val lkmLandscapePositionY: Float = 0.46f,
    val lkmPositionLocked: Boolean = false,
    val energy: Int = 72,
    val cleanliness: Int = 78,
    val moodValue: Int = 74,
    val sleepQuality: Int = 72,
    val exploration: Int = 48,
    val lastNeedsAt: Long = 0L,
    val furniture: List<PixelPetFurniture> = emptyList(),
    val ownedFurniture: Set<String> = emptySet(),
    val foodInventory: Map<String, Int> = emptyMap(),
    val activeFurnitureId: String? = null,
    val lastFurnitureInteractionAt: Long = 0L,
    val schemaVersion: Int = PIXEL_PET_SAVE_SCHEMA_VERSION,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", schemaVersion)
        .put("enabled", enabled)
        .put("hatched", hatched)
        .put("species", species)
        .put("appearance", appearance)
        .put("incubationStartedAt", incubationStartedAt)
        .put("name", name)
        .put("hunger", hunger)
        .put("affection", affection)
        .put("growth", growth)
        .put("coins", coins)
        .put("lastCheckInDay", lastCheckInDay)
        .put("lastObservedDay", lastObservedDay)
        .put("checkInStreak", checkInStreak)
        .put("lastInteractionAt", lastInteractionAt)
        .put("lastAction", lastAction)
        .put("lastActionAt", lastActionAt)
        .put("queuedAction", queuedAction)
        .put("lastHabitatInteractionAt", lastHabitatInteractionAt)
        .put("totalHabitatInteractions", totalHabitatInteractions)
        .put("lastWellbeingAt", lastWellbeingAt)
        .put("habitat", habitat)
        .put("accessory", accessory)
        .put("ownedAccessories", JSONArray(ownedAccessories.toList()))
        .put("equippedAccessories", JSONArray(equippedAccessories.toList()))
        .put("unlockedHabitats", JSONArray(unlockedHabitats.toList()))
        .put("totalInteractions", totalInteractions)
        .put("totalFeeds", totalFeeds)
        .put("highestCheckInStreak", highestCheckInStreak)
        .put("dailyTaskDay", dailyTaskDay)
        .put("dailyInteractions", dailyInteractions)
        .put("dailyFeeds", dailyFeeds)
        .put("dailyHabitatInteractions", dailyHabitatInteractions)
        .put("dailyHabitatChanged", dailyHabitatChanged)
        .put("claimedDailyTasks", JSONArray(claimedDailyTasks.toList()))
        .put("claimedAchievements", JSONArray(claimedAchievements.toList()))
        .put("savedLooks", JSONArray(savedLooks))
        .put("reminderEnabled", reminderEnabled)
        .put("teachingEnergy", teachingEnergy)
        .put("teachings", JSONArray(teachings))
        .put("personality", personality)
        .put("memories", JSONArray().apply { memories.forEach { put(it.toJson()) } })
        .put("weatherOverride", weatherOverride)
        .put("chatMessages", JSONArray().apply { chatMessages.forEach { put(it.toJson()) } })
        .put("lkmPositionX", lkmPositionX.toDouble())
        .put("lkmPositionY", lkmPositionY.toDouble())
        .put("lkmLandscapePositionX", lkmLandscapePositionX.toDouble())
        .put("lkmLandscapePositionY", lkmLandscapePositionY.toDouble())
        .put("lkmPositionLocked", lkmPositionLocked)
        .put("energy", energy)
        .put("cleanliness", cleanliness)
        .put("moodValue", moodValue)
        .put("sleepQuality", sleepQuality)
        .put("exploration", exploration)
        .put("lastNeedsAt", lastNeedsAt)
        .put("furniture", JSONArray().apply { furniture.forEach { put(it.toJson()) } })
        .put("ownedFurniture", JSONArray(ownedFurniture.toList()))
        .put("foodInventory", JSONObject().apply {
            foodInventory.forEach { (food, count) -> put(food, count) }
        })
        .put("activeFurnitureId", activeFurnitureId)
        .put("lastFurnitureInteractionAt", lastFurnitureInteractionAt)

    fun toState(): PixelPetState = sanitizePixelPetState(
        PixelPetState(
            enabled = enabled,
            hatched = hatched,
            species = PixelPetSpecies.fromStored(species),
            appearance = PixelPetAppearance.fromStored(appearance),
            incubationStartedAt = incubationStartedAt,
            name = name,
            hunger = hunger,
            affection = affection,
            growth = growth,
            coins = coins,
            lastCheckInDay = lastCheckInDay,
            lastObservedDay = lastObservedDay,
            checkInStreak = checkInStreak,
            lastInteractionAt = lastInteractionAt,
            lastAction = PixelPetAction.entries.firstOrNull { it.name == lastAction }
                ?: PixelPetAction.Idle,
            lastActionAt = lastActionAt,
            queuedAction = PixelPetAction.entries.firstOrNull { it.name == queuedAction }
                ?.takeUnless { it == PixelPetAction.Idle || it == PixelPetAction.Sleeping },
            lastHabitatInteractionAt = lastHabitatInteractionAt,
            totalHabitatInteractions = totalHabitatInteractions,
            lastWellbeingAt = lastWellbeingAt,
            habitat = PixelPetHabitat.fromStored(habitat),
            accessory = PixelPetAccessory.fromStored(accessory),
            ownedAccessories = ownedAccessories.mapNotNull(PixelPetAccessory::fromStored).toSet(),
            equippedAccessories = equippedAccessories.mapNotNull(PixelPetAccessory::fromStored).toSet(),
            unlockedHabitats = unlockedHabitats.map(PixelPetHabitat::fromStored).toSet(),
            totalInteractions = totalInteractions,
            totalFeeds = totalFeeds,
            highestCheckInStreak = highestCheckInStreak,
            dailyTaskDay = dailyTaskDay,
            dailyInteractions = dailyInteractions,
            dailyFeeds = dailyFeeds,
            dailyHabitatInteractions = dailyHabitatInteractions,
            dailyHabitatChanged = dailyHabitatChanged,
            claimedDailyTasks = claimedDailyTasks.mapNotNull { value ->
                PixelPetDailyTask.entries.firstOrNull { it.name == value }
            }.toSet(),
            claimedAchievements = claimedAchievements.mapNotNull { value ->
                PixelPetAchievement.entries.firstOrNull { it.name == value }
            }.toSet(),
            savedLooks = savedLooks.mapNotNull(PixelPetLook::decode),
            reminderEnabled = reminderEnabled,
            teachingEnergy = teachingEnergy,
            teachings = teachings,
            personality = PixelPetPersonality.fromStored(personality),
            memories = memories,
            weatherOverride = PixelPetWeather.fromStored(weatherOverride),
            chatMessages = chatMessages,
            lkmPositionX = lkmPositionX,
            lkmPositionY = lkmPositionY,
            lkmLandscapePositionX = lkmLandscapePositionX,
            lkmLandscapePositionY = lkmLandscapePositionY,
            lkmPositionLocked = lkmPositionLocked,
            energy = energy,
            cleanliness = cleanliness,
            moodValue = moodValue,
            sleepQuality = sleepQuality,
            exploration = exploration,
            lastNeedsAt = lastNeedsAt,
            furniture = furniture,
            ownedFurniture = ownedFurniture.mapNotNull(PixelPetFurnitureKind::fromStored).toSet(),
            foodInventory = foodInventory.mapNotNull { (key, value) ->
                PixelPetFoodKind.fromStored(key)?.let { it to value }
            }.toMap(),
            activeFurnitureId = activeFurnitureId,
            lastFurnitureInteractionAt = lastFurnitureInteractionAt,
        ),
    )

    companion object {
        fun fromJson(json: JSONObject): PixelPetSnapshot = PixelPetSnapshot(
            schemaVersion = json.optInt("schemaVersion", 1),
            enabled = json.optBoolean("enabled", false),
            hatched = json.optBoolean("hatched", false),
            species = json.optString("species").takeIf(String::isNotBlank),
            appearance = json.optString("appearance", PixelPetAppearance.Natural.name),
            incubationStartedAt = json.optLong("incubationStartedAt", 0L),
            name = json.optString("name", "Piko"),
            hunger = json.optInt("hunger", 64),
            affection = json.optInt("affection", 0),
            growth = json.optInt("growth", 0),
            coins = json.optInt("coins", 20),
            lastCheckInDay = json.optLong("lastCheckInDay", Long.MIN_VALUE),
            lastObservedDay = json.optLong("lastObservedDay", Long.MIN_VALUE),
            checkInStreak = json.optInt("checkInStreak", 0),
            lastInteractionAt = json.optLong("lastInteractionAt", 0L),
            lastAction = json.optString("lastAction", PixelPetAction.Idle.name),
            lastActionAt = json.optLong("lastActionAt", 0L),
            queuedAction = json.optString("queuedAction").takeIf(String::isNotBlank),
            lastHabitatInteractionAt = json.optLong("lastHabitatInteractionAt", 0L),
            totalHabitatInteractions = json.optInt("totalHabitatInteractions", 0),
            lastWellbeingAt = json.optLong("lastWellbeingAt", 0L),
            habitat = json.optString("habitat", PixelPetHabitat.Garden.name),
            accessory = json.optString("accessory").takeIf(String::isNotBlank),
            ownedAccessories = json.stringSet("ownedAccessories"),
            equippedAccessories = json.stringSet("equippedAccessories"),
            unlockedHabitats = json.stringSet("unlockedHabitats").ifEmpty {
                setOf(PixelPetHabitat.Garden.name)
            },
            totalInteractions = json.optInt("totalInteractions", 0),
            totalFeeds = json.optInt("totalFeeds", 0),
            highestCheckInStreak = json.optInt("highestCheckInStreak", 0),
            dailyTaskDay = json.optLong("dailyTaskDay", Long.MIN_VALUE),
            dailyInteractions = json.optInt("dailyInteractions", 0),
            dailyFeeds = json.optInt("dailyFeeds", 0),
            dailyHabitatInteractions = json.optInt("dailyHabitatInteractions", 0),
            dailyHabitatChanged = json.optBoolean("dailyHabitatChanged", false),
            claimedDailyTasks = json.stringSet("claimedDailyTasks"),
            claimedAchievements = json.stringSet("claimedAchievements"),
            savedLooks = json.stringList("savedLooks"),
            reminderEnabled = json.optBoolean("reminderEnabled", false),
            teachingEnergy = json.optInt("teachingEnergy", 0),
            teachings = json.stringList("teachings"),
            personality = json.optString("personality", PixelPetPersonality.Gentle.name),
            memories = json.pixelPetMemories(),
            weatherOverride = json.optString("weatherOverride").takeIf(String::isNotBlank),
            chatMessages = json.pixelPetChatMessages(),
            lkmPositionX = json.optDouble("lkmPositionX", 0.72).toFloat(),
            lkmPositionY = json.optDouble("lkmPositionY", 0.46).toFloat(),
            lkmLandscapePositionX = json.optDouble(
                "lkmLandscapePositionX",
                json.optDouble("lkmPositionX", 0.72),
            ).toFloat(),
            lkmLandscapePositionY = json.optDouble(
                "lkmLandscapePositionY",
                json.optDouble("lkmPositionY", 0.46),
            ).toFloat(),
            lkmPositionLocked = json.optBoolean("lkmPositionLocked", false),
            energy = json.optInt("energy", 72),
            cleanliness = json.optInt("cleanliness", 78),
            moodValue = json.optInt("moodValue", 74),
            sleepQuality = json.optInt("sleepQuality", 72),
            exploration = json.optInt("exploration", 48),
            lastNeedsAt = json.optLong("lastNeedsAt", 0L),
            furniture = json.pixelPetFurniture(),
            ownedFurniture = json.stringSet("ownedFurniture"),
            foodInventory = json.pixelPetFoodInventory(),
            activeFurnitureId = json.optString("activeFurnitureId").takeIf(String::isNotBlank),
            lastFurnitureInteractionAt = json.optLong("lastFurnitureInteractionAt", 0L),
        )

        fun from(state: PixelPetState): PixelPetSnapshot = PixelPetSnapshot(
            enabled = state.enabled,
            hatched = state.hatched,
            species = state.species?.name,
            appearance = state.appearance.name,
            incubationStartedAt = state.incubationStartedAt,
            name = state.name,
            hunger = state.hunger,
            affection = state.affection,
            growth = state.growth,
            coins = state.coins,
            lastCheckInDay = state.lastCheckInDay,
            lastObservedDay = state.lastObservedDay,
            checkInStreak = state.checkInStreak,
            lastInteractionAt = state.lastInteractionAt,
            lastAction = state.lastAction.name,
            lastActionAt = state.lastActionAt,
            queuedAction = state.queuedAction?.name,
            lastHabitatInteractionAt = state.lastHabitatInteractionAt,
            totalHabitatInteractions = state.totalHabitatInteractions,
            lastWellbeingAt = state.lastWellbeingAt,
            habitat = state.habitat.name,
            accessory = state.accessory?.name,
            ownedAccessories = state.ownedAccessories.mapTo(linkedSetOf()) { it.name },
            equippedAccessories = state.equippedAccessoriesOrLegacy.mapTo(linkedSetOf()) { it.name },
            unlockedHabitats = state.unlockedHabitats.mapTo(linkedSetOf()) { it.name },
            totalInteractions = state.totalInteractions,
            totalFeeds = state.totalFeeds,
            highestCheckInStreak = state.highestCheckInStreak,
            dailyTaskDay = state.dailyTaskDay,
            dailyInteractions = state.dailyInteractions,
            dailyFeeds = state.dailyFeeds,
            dailyHabitatInteractions = state.dailyHabitatInteractions,
            dailyHabitatChanged = state.dailyHabitatChanged,
            claimedDailyTasks = state.claimedDailyTasks.mapTo(linkedSetOf()) { it.name },
            claimedAchievements = state.claimedAchievements.mapTo(linkedSetOf()) { it.name },
            savedLooks = state.savedLooks.map(PixelPetLook::encode),
            reminderEnabled = state.reminderEnabled,
            teachingEnergy = state.teachingEnergy,
            teachings = state.teachings,
            personality = state.personality.name,
            memories = state.memories,
            weatherOverride = state.weatherOverride?.name,
            chatMessages = state.chatMessages,
            lkmPositionX = state.lkmPositionX,
            lkmPositionY = state.lkmPositionY,
            lkmLandscapePositionX = state.lkmLandscapePositionX,
            lkmLandscapePositionY = state.lkmLandscapePositionY,
            lkmPositionLocked = state.lkmPositionLocked,
            energy = state.energy,
            cleanliness = state.cleanliness,
            moodValue = state.moodValue,
            sleepQuality = state.sleepQuality,
            exploration = state.exploration,
            lastNeedsAt = state.lastNeedsAt,
            furniture = state.furniture,
            ownedFurniture = state.ownedFurniture.mapTo(linkedSetOf()) { it.name },
            foodInventory = state.foodInventory.mapKeys { it.key.name },
            activeFurnitureId = state.activeFurnitureId,
            lastFurnitureInteractionAt = state.lastFurnitureInteractionAt,
            schemaVersion = PIXEL_PET_SAVE_SCHEMA_VERSION,
        )
    }
}

private fun JSONObject.stringSet(key: String): Set<String> = stringList(key).toSet()

private fun JSONObject.stringList(key: String): List<String> {
    val array = optJSONArray(key) ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optString(index).takeIf(String::isNotBlank)?.let(::add)
        }
    }
}

private fun JSONObject.pixelPetChatMessages(): List<PixelPetChatMessage> {
    val array = optJSONArray("chatMessages") ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let(PixelPetChatMessage::fromJson)?.let(::add)
        }
    }
}

private fun JSONObject.pixelPetMemories(): List<PixelPetMemory> {
    val array = optJSONArray("memories") ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let(PixelPetMemory::fromJson)?.let(::add)
        }
    }
}

private fun JSONObject.pixelPetFurniture(): List<PixelPetFurniture> {
    val array = optJSONArray("furniture") ?: return emptyList()
    return buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let(PixelPetFurniture::fromJson)?.let(::add)
        }
    }
}

private fun JSONObject.pixelPetFoodInventory(): Map<String, Int> {
    val inventory = optJSONObject("foodInventory") ?: return emptyMap()
    return buildMap {
        val keys = inventory.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            put(key, inventory.optInt(key, 0))
        }
    }
}

class PixelPetBackupIncompatibleException(val version: Int) :
    IllegalArgumentException("Unsupported pixel pet save schema: $version")

object PixelPetStore {
    private val lock = Any()

    fun read(context: Context): PixelPetState = synchronized(lock) {
        val prefs = context.pixelPetPreferences()
        val current = read(prefs)
        val refreshed = PixelPetReducer.refresh(current)
        if (refreshed != current) {
            persist(prefs, refreshed)
            if (
                !current.hatched &&
                refreshed.hatched &&
                refreshed.reminderEnabled &&
                refreshed.enabled
            ) {
                PixelPetReminder.schedule(context)
            }
        }
        refreshed
    }

    fun setEnabled(context: Context, enabled: Boolean): PixelPetState = update(context) {
        PixelPetReducer.setEnabled(it, enabled)
    }.also { state ->
        if (state.reminderEnabled && state.enabled && state.hatched) {
            PixelPetReminder.schedule(context)
        } else {
            PixelPetReminder.cancel(context)
        }
    }

    fun applyThemeEnabled(context: Context, enabled: Boolean): PixelPetState = setEnabled(context, enabled)

    fun chooseSpecies(
        context: Context,
        species: PixelPetSpecies,
        now: Long = System.currentTimeMillis(),
    ): PixelPetState = update(context) {
        PixelPetReducer.chooseSpecies(it, species, now)
    }

    fun setAppearance(context: Context, appearance: PixelPetAppearance): PixelPetState = update(context) {
        PixelPetReducer.setAppearance(it, appearance)
    }

    fun hatch(context: Context): PixelPetState = update(context, PixelPetReducer::hatch).also { state ->
        if (state.reminderEnabled && state.enabled && state.hatched) {
            PixelPetReminder.schedule(context)
        }
    }

    fun interact(context: Context, now: Long = System.currentTimeMillis()): PixelPetState = update(context) {
        PixelPetReducer.interact(it, now)
    }

    fun pet(context: Context, now: Long = System.currentTimeMillis()): PixelPetState = update(context) {
        PixelPetReducer.pet(it, now)
    }

    fun feed(context: Context): PixelPetState = update(context, PixelPetReducer::feed)

    fun checkIn(context: Context, now: Long = System.currentTimeMillis()): PixelPetState = update(context) {
        PixelPetReducer.checkIn(it, now)
    }

    fun exploreHabitat(context: Context, now: Long = System.currentTimeMillis()): PixelPetState = update(context) {
        PixelPetReducer.exploreHabitat(it, now)
    }

    fun buyFurniture(context: Context, kind: PixelPetFurnitureKind): PixelPetState = update(context) {
        PixelPetReducer.buyFurniture(it, kind)
    }

    fun placeFurniture(
        context: Context,
        kind: PixelPetFurnitureKind,
        x: Float,
        y: Float,
    ): PixelPetState = update(context) {
        PixelPetReducer.placeFurniture(it, kind, x, y)
    }

    fun moveFurniture(context: Context, id: String, x: Float, y: Float): PixelPetState = update(context) {
        PixelPetReducer.moveFurniture(it, id, x, y)
    }

    fun removeFurniture(context: Context, id: String): PixelPetState = update(context) {
        PixelPetReducer.removeFurniture(it, id)
    }

    fun buyFood(context: Context, food: PixelPetFoodKind): PixelPetState = update(context) {
        PixelPetReducer.buyFood(it, food)
    }

    fun useFood(context: Context, food: PixelPetFoodKind): PixelPetState = update(context) {
        PixelPetReducer.useFood(it, food)
    }

    fun interactWithFurniture(context: Context, id: String): PixelPetState = update(context) {
        PixelPetReducer.interactWithFurniture(it, id)
    }

    fun autoInteractFurniture(context: Context): PixelPetState = update(context) {
        PixelPetReducer.autoInteractFurniture(it)
    }

    fun buyOrEquip(context: Context, accessory: PixelPetAccessory): PixelPetState = update(context) {
        PixelPetReducer.buyOrEquip(it, accessory)
    }

    fun unequip(context: Context): PixelPetState = update(context, PixelPetReducer::unequip)

    fun unequip(context: Context, slot: PixelPetAccessorySlot): PixelPetState = update(context) {
        PixelPetReducer.unequip(it, slot)
    }

    fun setHabitat(context: Context, habitat: PixelPetHabitat): PixelPetState = update(context) {
        PixelPetReducer.setHabitat(it, habitat)
    }

    fun setPersonality(context: Context, personality: PixelPetPersonality): PixelPetState = update(context) {
        PixelPetReducer.setPersonality(it, personality)
    }

    fun setWeatherOverride(context: Context, weather: PixelPetWeather?): PixelPetState = update(context) {
        PixelPetReducer.setWeatherOverride(it, weather)
    }

    fun rename(context: Context, name: String): PixelPetState = update(context) {
        PixelPetReducer.rename(it, name)
    }

    fun saveLook(context: Context): PixelPetState = update(context, PixelPetReducer::saveLook)

    fun applyLook(context: Context, look: PixelPetLook): PixelPetState = update(context) {
        PixelPetReducer.applyLook(it, look)
    }

    fun claimDailyTask(context: Context, task: PixelPetDailyTask): PixelPetState = update(context) {
        PixelPetReducer.claimDailyTask(it, task)
    }

    fun claimAchievement(context: Context, achievement: PixelPetAchievement): PixelPetState = update(context) {
        PixelPetReducer.claimAchievement(it, achievement)
    }

    fun setReminder(context: Context, enabled: Boolean): PixelPetState = update(context) {
        PixelPetReducer.setReminder(it, enabled)
    }.also { state ->
        if (state.reminderEnabled && state.enabled && state.hatched) {
            PixelPetReminder.schedule(context)
        } else {
            PixelPetReminder.cancel(context)
        }
    }

    fun teach(context: Context, lesson: String): PixelPetState = update(context) {
        PixelPetReducer.teach(it, lesson)
    }

    fun appendChatMessage(context: Context, message: PixelPetChatMessage): PixelPetState = update(context) {
        PixelPetReducer.appendChatMessage(it, message)
    }

    fun replaceChatMessage(context: Context, message: PixelPetChatMessage): PixelPetState = update(context) {
        PixelPetReducer.replaceChatMessage(it, message)
    }

    fun clearChatMessages(context: Context): PixelPetState = update(context, PixelPetReducer::clearChatMessages)

    fun setLkmPosition(
        context: Context,
        x: Float,
        y: Float,
        snap: Boolean = true,
        landscape: Boolean = false,
    ): PixelPetState = update(context) {
        PixelPetReducer.setLkmPosition(it, x, y, snap = snap, landscape = landscape)
    }

    fun setLkmPositionLocked(context: Context, locked: Boolean): PixelPetState = update(context) {
        PixelPetReducer.setLkmPositionLocked(it, locked)
    }

    fun replaceFurnitureLayout(context: Context, furniture: List<PixelPetFurniture>): PixelPetState = update(context) {
        PixelPetReducer.replaceFurnitureLayout(it, furniture)
    }

    fun rotateFurniture(context: Context, id: String): PixelPetState = update(context) {
        PixelPetReducer.rotateFurniture(it, id)
    }

    fun moveFurnitureLayer(context: Context, id: String, delta: Int): PixelPetState = update(context) {
        PixelPetReducer.moveFurnitureLayer(it, id, delta)
    }

    fun repairFurniture(context: Context, id: String): PixelPetState = update(context) {
        PixelPetReducer.repairFurniture(it, id)
    }

    fun resetLkmPosition(context: Context): PixelPetState = update(context, PixelPetReducer::resetLkmPosition)

    fun resetProgress(context: Context): PixelPetState = update(context, PixelPetReducer::resetProgress).also {
        PixelPetReminder.cancel(context)
    }

    fun exportBackup(context: Context): String = PixelPetSnapshot.from(read(context)).toJson().toString()

    fun previewBackup(raw: String): PixelPetBackupPreview {
        val json = JSONObject(raw)
        val sourceVersion = json.optInt("schemaVersion", 1)
        if (sourceVersion > PIXEL_PET_SAVE_SCHEMA_VERSION) {
            throw PixelPetBackupIncompatibleException(sourceVersion)
        }
        val snapshot = PixelPetSnapshot.fromJson(json)
        return PixelPetBackupPreview(snapshot.schemaVersion, snapshot.toState())
    }

    fun restoreBackup(
        context: Context,
        raw: String,
        mode: PixelPetRestoreMode = PixelPetRestoreMode.Replace,
    ): PixelPetState = synchronized(lock) {
        val incoming = previewBackup(raw).state
        val restored = when (mode) {
            PixelPetRestoreMode.Merge -> PixelPetReducer.mergeBackup(read(context.pixelPetPreferences()), incoming)
            PixelPetRestoreMode.Replace -> incoming
        }
        persist(context.pixelPetPreferences(), restored)
        if (restored.reminderEnabled && restored.enabled && restored.hatched) {
            PixelPetReminder.schedule(context)
        } else {
            PixelPetReminder.cancel(context)
        }
        restored
    }

    private fun update(context: Context, transform: (PixelPetState) -> PixelPetState): PixelPetState = synchronized(lock) {
        val prefs = context.pixelPetPreferences()
        val raw = read(prefs)
        val current = PixelPetReducer.refresh(raw)
        val updated = sanitizePixelPetState(transform(current))
        if (updated != raw) persist(prefs, updated)
        updated
    }

    private fun persist(prefs: SharedPreferences, state: PixelPetState) {
        val snapshot = PixelPetSnapshot.from(state)
        prefs.edit()
            .putBoolean(PIXEL_PET_ENABLED_KEY, snapshot.enabled)
            .putBoolean(KEY_HATCHED, snapshot.hatched)
            .putString(KEY_SPECIES, snapshot.species)
            .putString(KEY_APPEARANCE, snapshot.appearance)
            .putLong(KEY_INCUBATION_STARTED_AT, snapshot.incubationStartedAt)
            .putString(KEY_NAME, snapshot.name)
            .putInt(KEY_HUNGER, snapshot.hunger)
            .putInt(KEY_AFFECTION, snapshot.affection)
            .putInt(KEY_GROWTH, snapshot.growth)
            .putInt(KEY_COINS, snapshot.coins)
            .putLong(KEY_LAST_CHECK_IN_DAY, snapshot.lastCheckInDay)
            .putLong(KEY_LAST_OBSERVED_DAY, snapshot.lastObservedDay)
            .putInt(KEY_CHECK_IN_STREAK, snapshot.checkInStreak)
            .putLong(KEY_LAST_INTERACTION_AT, snapshot.lastInteractionAt)
            .putString(KEY_LAST_ACTION, snapshot.lastAction)
            .putLong(KEY_LAST_ACTION_AT, snapshot.lastActionAt)
            .putLong(KEY_LAST_HABITAT_INTERACTION_AT, snapshot.lastHabitatInteractionAt)
            .putInt(KEY_TOTAL_HABITAT_INTERACTIONS, snapshot.totalHabitatInteractions)
            .putLong(KEY_LAST_WELLBEING_AT, snapshot.lastWellbeingAt)
            .putString(KEY_HABITAT, snapshot.habitat)
            .putString(KEY_ACCESSORY, snapshot.accessory)
            .putString(KEY_OWNED_ACCESSORIES, snapshot.ownedAccessories.joinToString(","))
            .putString(KEY_EQUIPPED_ACCESSORIES, snapshot.equippedAccessories.joinToString(","))
            .putString(KEY_UNLOCKED_HABITATS, snapshot.unlockedHabitats.joinToString(","))
            .putInt(KEY_TOTAL_INTERACTIONS, snapshot.totalInteractions)
            .putInt(KEY_TOTAL_FEEDS, snapshot.totalFeeds)
            .putInt(KEY_HIGHEST_STREAK, snapshot.highestCheckInStreak)
            .putLong(KEY_DAILY_TASK_DAY, snapshot.dailyTaskDay)
            .putInt(KEY_DAILY_INTERACTIONS, snapshot.dailyInteractions)
            .putInt(KEY_DAILY_FEEDS, snapshot.dailyFeeds)
            .putInt(KEY_DAILY_HABITAT_INTERACTIONS, snapshot.dailyHabitatInteractions)
            .putBoolean(KEY_DAILY_HABITAT_CHANGED, snapshot.dailyHabitatChanged)
            .putString(KEY_CLAIMED_DAILY_TASKS, snapshot.claimedDailyTasks.joinToString(","))
            .putString(KEY_CLAIMED_ACHIEVEMENTS, snapshot.claimedAchievements.joinToString(","))
            .putString(KEY_SAVED_LOOKS, snapshot.savedLooks.joinToString(";"))
            .putBoolean(KEY_REMINDER_ENABLED, snapshot.reminderEnabled)
            .putInt(KEY_TEACHING_ENERGY, snapshot.teachingEnergy)
            .putString(KEY_TEACHINGS, snapshot.teachings.joinToString("\u001f"))
            .putString(KEY_PERSONALITY, snapshot.personality)
            .putString(KEY_MEMORIES, snapshot.memoriesJson())
            .putString(KEY_WEATHER_OVERRIDE, snapshot.weatherOverride)
            .putString(KEY_QUEUED_ACTION, snapshot.queuedAction)
            .putString(KEY_CHAT_MESSAGES, snapshot.chatMessagesJson())
            .putFloat(KEY_LKM_POSITION_X, snapshot.lkmPositionX)
            .putFloat(KEY_LKM_POSITION_Y, snapshot.lkmPositionY)
            .putFloat(KEY_LKM_LANDSCAPE_POSITION_X, snapshot.lkmLandscapePositionX)
            .putFloat(KEY_LKM_LANDSCAPE_POSITION_Y, snapshot.lkmLandscapePositionY)
            .putBoolean(KEY_LKM_POSITION_LOCKED, snapshot.lkmPositionLocked)
            .putInt(KEY_ENERGY, snapshot.energy)
            .putInt(KEY_CLEANLINESS, snapshot.cleanliness)
            .putInt(KEY_MOOD_VALUE, snapshot.moodValue)
            .putInt(KEY_SLEEP_QUALITY, snapshot.sleepQuality)
            .putInt(KEY_EXPLORATION, snapshot.exploration)
            .putLong(KEY_LAST_NEEDS_AT, snapshot.lastNeedsAt)
            .putString(KEY_FURNITURE, snapshot.furnitureJson())
            .putString(KEY_OWNED_FURNITURE, snapshot.ownedFurniture.joinToString(","))
            .putString(KEY_FOOD_INVENTORY, snapshot.foodInventory.entries.joinToString(",") { "${it.key}=${it.value}" })
            .putString(KEY_ACTIVE_FURNITURE_ID, snapshot.activeFurnitureId)
            .putLong(KEY_LAST_FURNITURE_INTERACTION_AT, snapshot.lastFurnitureInteractionAt)
            .apply()
    }

    private fun read(prefs: SharedPreferences): PixelPetState = PixelPetSnapshot(
            enabled = prefs.getBoolean(PIXEL_PET_ENABLED_KEY, false),
            hatched = prefs.getBoolean(KEY_HATCHED, false),
            species = prefs.getString(KEY_SPECIES, null),
            appearance = prefs.getString(KEY_APPEARANCE, PixelPetAppearance.Natural.name)
                ?: PixelPetAppearance.Natural.name,
            incubationStartedAt = prefs.getLong(KEY_INCUBATION_STARTED_AT, 0L),
            name = prefs.getString(KEY_NAME, "Piko").orEmpty(),
            hunger = prefs.getInt(KEY_HUNGER, 64),
            affection = prefs.getInt(KEY_AFFECTION, 0),
            growth = prefs.getInt(KEY_GROWTH, 0),
            coins = prefs.getInt(KEY_COINS, 20),
            lastCheckInDay = prefs.getLong(KEY_LAST_CHECK_IN_DAY, Long.MIN_VALUE),
            lastObservedDay = prefs.getLong(KEY_LAST_OBSERVED_DAY, Long.MIN_VALUE),
            checkInStreak = prefs.getInt(KEY_CHECK_IN_STREAK, 0),
             lastInteractionAt = prefs.getLong(KEY_LAST_INTERACTION_AT, 0L),
             lastAction = prefs.getString(KEY_LAST_ACTION, PixelPetAction.Idle.name).orEmpty(),
             lastActionAt = prefs.getLong(KEY_LAST_ACTION_AT, 0L),
             lastHabitatInteractionAt = prefs.getLong(KEY_LAST_HABITAT_INTERACTION_AT, 0L),
             totalHabitatInteractions = prefs.getInt(KEY_TOTAL_HABITAT_INTERACTIONS, 0),
             lastWellbeingAt = prefs.getLong(KEY_LAST_WELLBEING_AT, 0L),
            habitat = prefs.getString(KEY_HABITAT, null).orEmpty(),
            accessory = prefs.getString(KEY_ACCESSORY, null),
            ownedAccessories = prefs.getString(KEY_OWNED_ACCESSORIES, "").orEmpty()
                .split(',')
                .toSet(),
            equippedAccessories = prefs.getString(KEY_EQUIPPED_ACCESSORIES, "").orEmpty()
                .split(',')
                .toSet(),
            unlockedHabitats = when {
                prefs.contains(KEY_UNLOCKED_HABITATS) -> prefs.getString(KEY_UNLOCKED_HABITATS, "").orEmpty()
                    .split(',')
                    .toSet()
                prefs.contains(KEY_HATCHED) -> PixelPetHabitat.entries.mapTo(linkedSetOf()) { it.name }
                else -> setOf(PixelPetHabitat.Garden.name)
            },
            totalInteractions = prefs.getInt(KEY_TOTAL_INTERACTIONS, 0),
            totalFeeds = prefs.getInt(KEY_TOTAL_FEEDS, 0),
            highestCheckInStreak = prefs.getInt(KEY_HIGHEST_STREAK, 0),
            dailyTaskDay = prefs.getLong(KEY_DAILY_TASK_DAY, Long.MIN_VALUE),
             dailyInteractions = prefs.getInt(KEY_DAILY_INTERACTIONS, 0),
             dailyFeeds = prefs.getInt(KEY_DAILY_FEEDS, 0),
             dailyHabitatInteractions = prefs.getInt(KEY_DAILY_HABITAT_INTERACTIONS, 0),
             dailyHabitatChanged = prefs.getBoolean(KEY_DAILY_HABITAT_CHANGED, false),
            claimedDailyTasks = prefs.getString(KEY_CLAIMED_DAILY_TASKS, "").orEmpty().split(',').toSet(),
            claimedAchievements = prefs.getString(KEY_CLAIMED_ACHIEVEMENTS, "").orEmpty().split(',').toSet(),
            savedLooks = prefs.getString(KEY_SAVED_LOOKS, "").orEmpty().split(';').filter(String::isNotBlank),
            reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
            teachingEnergy = prefs.getInt(KEY_TEACHING_ENERGY, 0),
            teachings = prefs.getString(KEY_TEACHINGS, "").orEmpty()
                .split('\u001f')
                .filter(String::isNotBlank),
            personality = prefs.getString(KEY_PERSONALITY, PixelPetPersonality.Gentle.name)
                ?: PixelPetPersonality.Gentle.name,
            memories = prefs.getString(KEY_MEMORIES, "").orEmpty().toPixelPetMemories(),
            weatherOverride = prefs.getString(KEY_WEATHER_OVERRIDE, null),
            queuedAction = prefs.getString(KEY_QUEUED_ACTION, null),
            chatMessages = prefs.getString(KEY_CHAT_MESSAGES, "").orEmpty().toPixelPetChatMessages(),
            lkmPositionX = prefs.getFloat(KEY_LKM_POSITION_X, 0.72f),
            lkmPositionY = prefs.getFloat(KEY_LKM_POSITION_Y, 0.46f),
            lkmLandscapePositionX = prefs.getFloat(
                KEY_LKM_LANDSCAPE_POSITION_X,
                prefs.getFloat(KEY_LKM_POSITION_X, 0.72f),
            ),
            lkmLandscapePositionY = prefs.getFloat(
                KEY_LKM_LANDSCAPE_POSITION_Y,
                prefs.getFloat(KEY_LKM_POSITION_Y, 0.46f),
            ),
            lkmPositionLocked = prefs.getBoolean(KEY_LKM_POSITION_LOCKED, false),
            energy = prefs.getInt(KEY_ENERGY, 72),
            cleanliness = prefs.getInt(KEY_CLEANLINESS, 78),
            moodValue = prefs.getInt(KEY_MOOD_VALUE, 74),
            sleepQuality = prefs.getInt(KEY_SLEEP_QUALITY, 72),
            exploration = prefs.getInt(KEY_EXPLORATION, 48),
            lastNeedsAt = prefs.getLong(KEY_LAST_NEEDS_AT, 0L),
            furniture = prefs.getString(KEY_FURNITURE, "").orEmpty().toPixelPetFurniture(),
            ownedFurniture = prefs.getString(KEY_OWNED_FURNITURE, "").orEmpty().split(',').toSet(),
            foodInventory = prefs.getString(KEY_FOOD_INVENTORY, "").orEmpty().toPixelPetFoodInventory(),
            activeFurnitureId = prefs.getString(KEY_ACTIVE_FURNITURE_ID, null),
            lastFurnitureInteractionAt = prefs.getLong(KEY_LAST_FURNITURE_INTERACTION_AT, 0L),
        ).toState()
}

private fun PixelPetSnapshot.chatMessagesJson(): String = JSONArray().apply {
    chatMessages.forEach { put(it.toJson()) }
}.toString()

private fun PixelPetSnapshot.memoriesJson(): String = JSONArray().apply {
    memories.forEach { put(it.toJson()) }
}.toString()

private fun PixelPetSnapshot.furnitureJson(): String = JSONArray().apply {
    furniture.forEach { put(it.toJson()) }
}.toString()

private fun String.toPixelPetChatMessages(): List<PixelPetChatMessage> = runCatching {
    JSONObject().put("chatMessages", JSONArray(this)).pixelPetChatMessages()
}.getOrDefault(emptyList())

private fun String.toPixelPetMemories(): List<PixelPetMemory> = runCatching {
    JSONObject().put("memories", JSONArray(this)).pixelPetMemories()
}.getOrDefault(emptyList())

private fun String.toPixelPetFurniture(): List<PixelPetFurniture> = runCatching {
    val array = JSONArray(this)
    buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let(PixelPetFurniture::fromJson)?.let(::add)
        }
    }
}.getOrDefault(emptyList())

private fun String.toPixelPetFoodInventory(): Map<String, Int> = buildMap {
    split(',').forEach { entry ->
        val parts = entry.split('=', limit = 2)
        val count = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
        if (parts[0].isNotBlank() && count > 0) put(parts[0], count)
    }
}

internal fun sanitizePixelPetState(state: PixelPetState): PixelPetState {
    val owned = state.ownedAccessories
    val equipped = state.equippedAccessoriesOrLegacy.filter { it in owned }.toSet()
    val species = state.species ?: PixelPetSpecies.Cat.takeIf { state.hatched }
    val unlocked = (state.unlockedHabitats + PixelPetHabitat.Garden).toMutableSet().apply {
        addAll(PixelPetHabitat.entries.filter { state.level >= it.unlockLevel })
    }
    val habitat = state.habitat.takeIf { it in unlocked } ?: PixelPetHabitat.Garden
    val ownedFurniture = state.ownedFurniture
    val furniture = state.furniture
        .filter { it.kind in ownedFurniture }
        .distinctBy(PixelPetFurniture::id)
        // Keep the oldest placed items when importing an oversized legacy save.
        // Taking the tail silently discarded furniture the user had already placed.
        .take(MAX_PIXEL_PET_FURNITURE)
    return state.copy(
        species = species,
        incubationStartedAt = if (state.hatched) 0L else state.incubationStartedAt.coerceAtLeast(0L),
        name = state.name.trim().take(20).ifBlank { "Piko" },
        hunger = state.hunger.coerceIn(0, 100),
        energy = state.energy.coerceIn(0, 100),
        cleanliness = state.cleanliness.coerceIn(0, 100),
        moodValue = state.moodValue.coerceIn(0, 100),
        sleepQuality = state.sleepQuality.coerceIn(0, 100),
        exploration = state.exploration.coerceIn(0, 100),
        affection = state.affection.coerceIn(0, 999),
        growth = state.growth.coerceIn(0, 99_999),
        coins = state.coins.coerceIn(0, 99_999),
        checkInStreak = state.checkInStreak.coerceIn(0, 999),
        lastInteractionAt = state.lastInteractionAt.coerceAtLeast(0L),
        lastActionAt = state.lastActionAt.coerceAtLeast(0L),
        lastHabitatInteractionAt = state.lastHabitatInteractionAt.coerceAtLeast(0L),
        totalHabitatInteractions = state.totalHabitatInteractions.coerceIn(0, 99_999),
        lastWellbeingAt = state.lastWellbeingAt.coerceAtLeast(0L),
        lastNeedsAt = state.lastNeedsAt.coerceAtLeast(0L),
        habitat = habitat,
        accessory = equipped.firstOrNull(),
        equippedAccessories = equipped,
        unlockedHabitats = unlocked,
        furniture = furniture.map { item ->
            item.copy(
                x = item.x.coerceIn(0.06f, 0.94f),
                y = item.y.coerceIn(0.18f, 0.86f),
                interactions = item.interactions.coerceIn(0, 99_999),
                durability = item.durability.coerceIn(0, 100),
                rotationQuarterTurns = item.rotationQuarterTurns.mod(4),
                layer = item.layer.coerceIn(-4, 4),
            )
        },
        ownedFurniture = ownedFurniture,
        foodInventory = state.foodInventory
            .filterKeys { it in PixelPetFoodKind.entries }
            .mapValues { (_, count) -> count.coerceIn(0, 999) }
            .filterValues { it > 0 },
        activeFurnitureId = state.activeFurnitureId.takeIf { id -> furniture.any { it.id == id } },
        totalInteractions = state.totalInteractions.coerceIn(0, 99_999),
        totalFeeds = state.totalFeeds.coerceIn(0, 99_999),
        highestCheckInStreak = state.highestCheckInStreak.coerceIn(0, 999),
        dailyInteractions = state.dailyInteractions.coerceIn(0, 999),
        dailyFeeds = state.dailyFeeds.coerceIn(0, 999),
        dailyHabitatInteractions = state.dailyHabitatInteractions.coerceIn(0, 999),
        savedLooks = state.savedLooks
            .distinct()
            .filter { look -> look.habitat in unlocked && owned.containsAll(look.accessories) }
            .takeLast(6),
        teachingEnergy = state.teachingEnergy.coerceIn(0, MAX_TEACHING_ENERGY),
        teachings = state.teachings
            .map { it.trim().replace(Regex("\\s+"), " ").take(180) }
            .filter(String::isNotBlank)
            .distinct()
            .takeLast(MAX_PET_TEACHINGS),
        memories = state.memories
            .mapNotNull { memory ->
                memory.text.trim().replace(Regex("\\s+"), " ").take(180)
                    .takeIf(String::isNotBlank)
                    ?.let { memory.copy(text = it, recordedAt = memory.recordedAt.coerceAtLeast(0L)) }
            }
            .distinctBy { it.kind to it.text }
            .takeLast(MAX_PET_MEMORIES),
        queuedAction = state.queuedAction?.takeUnless {
            it == PixelPetAction.Idle || it == PixelPetAction.Sleeping || it == PixelPetAction.Hatching
        },
        chatMessages = state.chatMessages
            .mapNotNull { message ->
                message.text.trim().take(600).takeIf(String::isNotBlank)?.let {
                    message.copy(
                        text = it,
                        createdAt = message.createdAt.coerceAtLeast(0L),
                        status = if (message.status == PixelPetChatMessageStatus.Generating) {
                            PixelPetChatMessageStatus.Stopped
                        } else {
                            message.status
                        },
                    )
                }
            }
            .takeLast(MAX_PET_CHAT_MESSAGES),
        lkmPositionX = state.lkmPositionX.coerceIn(0f, 1f),
        lkmPositionY = state.lkmPositionY.coerceIn(0f, 1f),
        lkmLandscapePositionX = state.lkmLandscapePositionX.coerceIn(0f, 1f),
        lkmLandscapePositionY = state.lkmLandscapePositionY.coerceIn(0f, 1f),
    )
}

@Composable
fun rememberPixelPetState(): MutableState<PixelPetState> {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember(context) { mutableStateOf(PixelPetStore.read(context)) }
    DisposableEffect(context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (isPixelPetPreferenceKey(key)) {
                state.value = PixelPetStore.read(context)
            }
        }
        context.pixelPetPreferences().registerOnSharedPreferenceChangeListener(listener)
        onDispose { context.pixelPetPreferences().unregisterOnSharedPreferenceChangeListener(listener) }
    }
    LaunchedEffect(
        context,
        lifecycleOwner,
        state.value.isIncubating,
        state.value.furniture.isNotEmpty(),
        state.value.queuedAction,
    ) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(
                    when {
                        state.value.queuedAction != null -> 250L
                        state.value.isIncubating -> 1_000L
                        state.value.furniture.isNotEmpty() -> MIN_FURNITURE_AUTOPLAY_INTERVAL_MILLIS
                        else -> 60_000L
                    },
                )
                if (state.value.furniture.isNotEmpty()) {
                    PixelPetStore.autoInteractFurniture(context)
                }
                state.value = PixelPetStore.read(context)
            }
        }
    }
    return state
}

@Composable
internal fun PixelPetAvatar(
    state: PixelPetState,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    idlePhase: Float = 0f,
    showGround: Boolean = true,
    facing: PixelPetFacing = PixelPetFacing.Front,
) {
    val context = LocalContext.current
    val palette = pixelPalette(PixelStyle.PetCompanion, isInDarkTheme())
    val species = state.species ?: PixelPetSpecies.Cat
    val displayedAction = if (state.hatched) state.displayAction() else PixelPetAction.Hatching
    val renderMotion = rememberPixelPetRenderMotion(displayedAction, idlePhase)
    val spriteColors = pixelPetModelColors(
        species = species,
        appearance = state.appearance,
        habitat = state.habitat,
        weather = state.currentWeather(),
        night = isPixelPetNight(),
        warmLight = state.furniture.any { it.kind == PixelPetFurnitureKind.Lamp && it.durability > 0 },
    )
    DisposableEffect(context) {
        PixelPetSpriteAtlas.registerMemoryCallbacks(context)
        onDispose { }
    }
    var packState by remember(species) { mutableStateOf(PixelPetSpriteAtlas.packState(species)) }
    val spriteFrame = PixelPetSpriteAtlas.loadedFrame(
        species = species,
        stage = state.growthStage,
        action = renderMotion.action,
        frame = if (state.hatched) renderMotion.frame else pixelPetEggSpriteFrame(state.incubationProgress()),
        facing = facing,
    )
    LaunchedEffect(context, species, spriteFrame == null) {
        if (spriteFrame == null) {
            packState = withContext(Dispatchers.Default) {
                PixelPetSpriteAtlas.prepare(context, species)
            }
        }
    }
    LaunchedEffect(
        spriteFrame,
        packState,
        species,
        state.appearance,
        state.growthStage,
        state.habitat,
        state.weatherOverride,
        renderMotion.action,
        renderMotion.frame / 3,
        facing,
    ) {
        if (spriteFrame != null) {
            withContext(Dispatchers.Default) {
                PixelPetSpriteAtlas.prewarm(
                    species = species,
                    stage = state.growthStage,
                    action = renderMotion.action,
                    facing = facing,
                    colors = spriteColors,
                    startFrame = renderMotion.frame,
                )
            }
        }
    }
    Canvas(
        modifier = modifier.size(size),
    ) {
        val unit = (this.size.minDimension / 12f).coerceAtLeast(1f)
        if (showGround) drawPixelPetAvatarHabitat(state.habitat, palette, unit)
        drawPixelPetModel(
            state = state,
            themePalette = palette,
            unit = unit,
            renderMotion = renderMotion,
            facing = facing,
            suppliedFrame = spriteFrame,
        )
    }
}

internal data class PixelPetModelColors(
    val outline: Color,
    val base: Color,
    val shade: Color,
    val cream: Color,
    val highlight: Color,
    val accent: Color,
    val reflection: Color,
    val eye: Color,
)

internal fun pixelPetModelColors(
    species: PixelPetSpecies,
    appearance: PixelPetAppearance = PixelPetAppearance.Natural,
    habitat: PixelPetHabitat = PixelPetHabitat.Garden,
    weather: PixelPetWeather = PixelPetWeather.Clear,
    night: Boolean = false,
    warmLight: Boolean = false,
): PixelPetModelColors {
    val base = when (species) {
    PixelPetSpecies.Cat -> PixelPetModelColors(
        outline = Color(0xFF4F342C),
        base = Color(0xFFE77D38),
        shade = Color(0xFFB75332),
        cream = Color(0xFFFFDDA8),
        highlight = Color(0xFFFFF0CF),
        accent = Color(0xFFF1A04D),
        reflection = Color(0xFFFFC879),
        eye = Color(0xFF2F2220),
    )
    PixelPetSpecies.Dog -> PixelPetModelColors(
        outline = Color(0xFF4A352B),
        base = Color(0xFFD89358),
        shade = Color(0xFFA85E3F),
        cream = Color(0xFFFFE5BD),
        highlight = Color(0xFFFFD7A1),
        accent = Color(0xFFD96762),
        reflection = Color(0xFFFFDCAF),
        eye = Color(0xFF2E211D),
    )
    PixelPetSpecies.Bird -> PixelPetModelColors(
        outline = Color(0xFF35526A),
        base = Color(0xFF86C9E8),
        shade = Color(0xFF4A91BD),
        cream = Color(0xFFF2CD69),
        highlight = Color(0xFFD7F4FF),
        accent = Color(0xFFF29A4B),
        reflection = Color(0xFFC8EEFF),
        eye = Color(0xFF203343),
    )
    PixelPetSpecies.Rabbit -> PixelPetModelColors(
        outline = Color(0xFF584955),
        base = Color(0xFFE7D9E3),
        shade = Color(0xFFB99CAA),
        cream = Color(0xFFFFF8FD),
        highlight = Color(0xFFFFFFFF),
        accent = Color(0xFFF08BA6),
        reflection = Color(0xFFE6F6FF),
        eye = Color(0xFF372A35),
    )
    PixelPetSpecies.Penguin -> PixelPetModelColors(
        outline = Color(0xFF202B3D),
        base = Color(0xFF3D5E83),
        shade = Color(0xFF263A59),
        cream = Color(0xFFF2F6FF),
        highlight = Color(0xFFD8F0FF),
        accent = Color(0xFFF4A64E),
        reflection = Color(0xFFBFE9FF),
        eye = Color(0xFF172131),
    )
    PixelPetSpecies.Hamster -> PixelPetModelColors(
        outline = Color(0xFF53342A),
        base = Color(0xFFD79B68),
        shade = Color(0xFF985A3F),
        cream = Color(0xFFFFE3BD),
        highlight = Color(0xFFFFF3DC),
        accent = Color(0xFFEF8E9E),
        reflection = Color(0xFFFFDEAD),
        eye = Color(0xFF2A1A17),
    )
    }
    val habitatTint = when (habitat) {
        PixelPetHabitat.Garden -> Color(0xFF8DBE76)
        PixelPetHabitat.Cloud -> Color(0xFFB6D7F2)
        PixelPetHabitat.Moon -> Color(0xFF9C8DE0)
        PixelPetHabitat.Lagoon -> Color(0xFF63B8C8)
    }
    val weatherTint = when (weather) {
        PixelPetWeather.Clear -> habitatTint
        PixelPetWeather.Breezy -> Color(0xFFB7D9CF)
        PixelPetWeather.Drizzle -> Color(0xFF86B8CC)
        PixelPetWeather.Starlit -> Color(0xFFA48CE1)
        PixelPetWeather.Meteor -> Color(0xFFE6A5E8)
        PixelPetWeather.Mist -> Color(0xFFC3CDD2)
    }
    val coatTint = when (appearance) {
        PixelPetAppearance.Natural -> Color.Transparent
        PixelPetAppearance.Dawn -> Color(0xFFFFB07B)
        PixelPetAppearance.Frost -> Color(0xFFBEEAFF)
        PixelPetAppearance.Dusk -> Color(0xFFC5A2F0)
    }
    val coatAmount = when (appearance) {
        PixelPetAppearance.Natural -> 0f
        PixelPetAppearance.Dawn -> 0.18f
        PixelPetAppearance.Frost -> 0.16f
        PixelPetAppearance.Dusk -> 0.17f
    }
    val materialTint = when {
        warmLight -> Color(0xFFFFBD78)
        night -> Color(0xFF8D9FFF)
        else -> weatherTint
    }
    val tintAmount = when {
        warmLight -> 0.16f
        night -> 0.20f
        weather == PixelPetWeather.Drizzle -> 0.18f
        else -> 0.10f
    }
    fun tint(color: Color, amount: Float = tintAmount): Color = lerp(color, materialTint, amount)
    fun coat(color: Color, amount: Float = coatAmount): Color = lerp(color, coatTint, amount)
    return base.copy(
        outline = tint(coat(base.outline, coatAmount * 0.48f), if (night) 0.18f else 0.08f),
        base = tint(coat(base.base)),
        shade = tint(coat(base.shade, coatAmount * 0.62f), tintAmount * 0.8f),
        cream = tint(coat(base.cream, coatAmount * 0.28f), tintAmount * 0.58f),
        highlight = when {
            weather == PixelPetWeather.Meteor -> lerp(tint(coat(base.highlight), 0.16f), Color(0xFFFFC6F3), 0.34f)
            weather == PixelPetWeather.Drizzle -> lerp(tint(coat(base.highlight), 0.12f), Color(0xFFE4F8FF), 0.46f)
            night -> lerp(tint(coat(base.highlight), 0.12f), Color(0xFFC9D7FF), 0.28f)
            else -> tint(coat(base.highlight), tintAmount * 0.42f)
        },
        accent = if (weather == PixelPetWeather.Drizzle) {
            lerp(tint(coat(base.accent, coatAmount * 0.48f)), Color(0xFFD6F4FF), 0.18f)
        } else {
            tint(coat(base.accent, coatAmount * 0.48f), tintAmount * 0.72f)
        },
        reflection = when {
            weather == PixelPetWeather.Drizzle -> lerp(coat(base.reflection, coatAmount * 0.25f), Color(0xFFE7FAFF), 0.55f)
            night -> lerp(coat(base.reflection, coatAmount * 0.25f), Color(0xFFB9C9FF), 0.32f)
            warmLight -> lerp(coat(base.reflection, coatAmount * 0.25f), Color(0xFFFFD9AB), 0.35f)
            else -> tint(coat(base.reflection, coatAmount * 0.25f), tintAmount * 0.56f)
        },
        eye = tint(coat(base.eye, coatAmount * 0.18f), if (night) 0.10f else 0.04f),
    )
}

private fun DrawScope.pixel(
    color: Color,
    unit: Float,
    x: Float,
    y: Float,
    width: Float = 1f,
    height: Float = 1f,
) {
    val left = (unit * x).roundToInt().toFloat()
    val top = (unit * y).roundToInt().toFloat()
    val pixelWidth = (unit * width).roundToInt().coerceAtLeast(1).toFloat()
    val pixelHeight = (unit * height).roundToInt().coerceAtLeast(1).toFloat()
    drawRect(color, Offset(left, top), Size(pixelWidth, pixelHeight))
}

private fun DrawScope.pixelLine(
    color: Color,
    unit: Float,
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    width: Float = 0.28f,
) {
    drawLine(
        color,
        Offset((unit * startX).roundToInt().toFloat(), (unit * startY).roundToInt().toFloat()),
        Offset((unit * endX).roundToInt().toFloat(), (unit * endY).roundToInt().toFloat()),
        (unit * width).roundToInt().coerceAtLeast(1).toFloat(),
    )
}

private fun PixelPetState.displayAction(): PixelPetAction = when {
    !hatched -> PixelPetAction.Hatching
    hunger <= 8 || cleanliness <= 10 -> PixelPetAction.Frightened
    hasRecentAction() -> lastAction
    currentWeather() in setOf(PixelPetWeather.Drizzle, PixelPetWeather.Mist) -> PixelPetAction.Watching
    isSleeping -> PixelPetAction.Sleeping
    else -> ambientPixelPetAction()
}

private fun PixelPetState.ambientPixelPetAction(now: Long = System.currentTimeMillis()): PixelPetAction {
    when (currentWeather(now)) {
        PixelPetWeather.Drizzle,
        PixelPetWeather.Mist,
        PixelPetWeather.Starlit,
        PixelPetWeather.Meteor,
        -> return PixelPetAction.Watching
        else -> Unit
    }
    val slot = ((now / 15_000L) + habitat.ordinal + personality.ordinal) % 9L
    return when {
        currentWeather(now) == PixelPetWeather.Drizzle && slot % 3L == 0L -> PixelPetAction.Watching
        slot == 0L || slot == 5L -> PixelPetAction.Watching
        slot == 3L -> PixelPetAction.Cleaning
        slot == 7L && personality == PixelPetPersonality.Playful -> PixelPetAction.Playing
        else -> PixelPetAction.Idle
    }
}

private fun DrawScope.drawPixelPetModel(
    state: PixelPetState,
    themePalette: PixelPalette,
    unit: Float,
    renderMotion: PixelPetRenderMotion,
    facing: PixelPetFacing,
    suppliedFrame: PixelPetSpriteFrame?,
) {
    val species = state.species ?: PixelPetSpecies.Cat
    val colors = pixelPetModelColors(
        species = species,
        appearance = state.appearance,
        habitat = state.habitat,
        weather = state.currentWeather(),
        night = isPixelPetNight(),
        warmLight = state.furniture.any { it.kind == PixelPetFurnitureKind.Lamp && it.durability > 0 },
    )
    val isEgg = !state.hatched
    val action = renderMotion.action
    val frame = if (isEgg) {
        pixelPetEggSpriteFrame(state.incubationProgress())
    } else {
        renderMotion.frame
    }
    val stage = state.growthStage
    drawPixelPetGroundFeedback(
        state = state,
        palette = themePalette,
        unit = unit,
        action = action,
        frame = frame,
        facing = facing,
    )
    drawPixelPetMaterialBehindModel(state, colors, unit, action, frame)
    drawPixelPetShadow(themePalette, unit, stage, action, frame, facing)
    val spriteFrame = suppliedFrame
    if (spriteFrame == null) {
        drawPixelPetSpriteLoadPlaceholder(themePalette, unit)
        return
    }
    fun attachmentLayer(slot: PixelPetAccessorySlot): PixelPetAccessoryRenderLayer =
        PixelPetSpriteAtlas.accessoryAttachment(
            spriteFrame,
            species,
            stage,
            action,
            slot,
            facing,
        ).layer

    fun accessoryAnchor(slot: PixelPetAccessorySlot): PixelPetAccessoryAnchor =
        PixelPetSpriteAtlas.accessoryAnchor(
            spriteFrame,
            species,
            stage,
            action,
            slot,
            facing,
            unit,
        )
    if (isEgg) {
        drawPixelPetSpriteFrame(spriteFrame, colors, unit)
        return
    }
    PixelPetAccessorySlot.entries.forEach { slot ->
        if (attachmentLayer(slot) == PixelPetAccessoryRenderLayer.BehindModel) {
            drawPixelPetAccessories(
                state.equippedAccessoriesOrLegacy,
                colors,
                unit,
                species,
                stage,
                action,
                frame,
                slot,
                facing,
                accessoryAnchor(slot),
            )
        }
    }
    drawPixelPetSpriteFrame(spriteFrame, colors, unit)
    drawPixelPetExpressionLayer(
        state = state,
        colors = colors,
        unit = unit,
        species = species,
        stage = stage,
        action = action,
        frame = frame,
        spriteFrame = spriteFrame,
        facing = facing,
    )
    PixelPetAccessorySlot.entries.forEach { slot ->
        if (attachmentLayer(slot) == PixelPetAccessoryRenderLayer.BodyOverlay) {
            drawPixelPetAccessories(
                state.equippedAccessoriesOrLegacy,
                colors,
                unit,
                species,
                stage,
                action,
                frame,
                slot,
                facing,
                accessoryAnchor(slot),
            )
        }
    }
    PixelPetAccessorySlot.entries.forEach { slot ->
        if (attachmentLayer(slot) == PixelPetAccessoryRenderLayer.FrontModel) {
            drawPixelPetAccessories(
                state.equippedAccessoriesOrLegacy,
                colors,
                unit,
                species,
                stage,
                action,
                frame,
                slot,
                facing,
                accessoryAnchor(slot),
            )
        }
    }
    drawPixelPetMaterialFrontModel(state, colors, unit, action, frame)
}

private fun DrawScope.drawPixelPetSpriteLoadPlaceholder(
    palette: PixelPalette,
    unit: Float,
) {
    val color = palette.secondary.copy(alpha = 0.62f)
    listOf(0f, 1.4f, 2.8f).forEachIndexed { index, offset ->
        val lift = if (index == 1) -0.5f else 0f
        drawRect(
            color = color,
            topLeft = Offset(unit * (4.1f + offset), unit * (7.1f + lift)),
            size = Size(unit * 0.75f, unit * 0.75f),
        )
    }
}

/**
 * Depth is authored from slot, direction, and pose rather than visible Sprite
 * bounds, so temporary particles and limb motion cannot make gear flicker.
 */
internal fun pixelPetAccessoryLayer(
    slot: PixelPetAccessorySlot,
    facing: PixelPetFacing,
    action: PixelPetAction,
): PixelPetAccessoryRenderLayer = when (slot) {
    PixelPetAccessorySlot.Back,
    PixelPetAccessorySlot.Tail,
    PixelPetAccessorySlot.Trail,
    -> PixelPetAccessoryRenderLayer.BehindModel
    PixelPetAccessorySlot.Neck -> if (facing == PixelPetFacing.Back) {
        PixelPetAccessoryRenderLayer.BehindModel
    } else {
        PixelPetAccessoryRenderLayer.BodyOverlay
    }
    PixelPetAccessorySlot.Head -> if (facing == PixelPetFacing.Back) {
        PixelPetAccessoryRenderLayer.BehindModel
    } else {
        PixelPetAccessoryRenderLayer.FrontModel
    }
    PixelPetAccessorySlot.Hand -> when {
        action == PixelPetAction.Sleeping && facing != PixelPetFacing.Front -> PixelPetAccessoryRenderLayer.BehindModel
        action == PixelPetAction.Cleaning && facing == PixelPetFacing.Back -> PixelPetAccessoryRenderLayer.BehindModel
        else -> PixelPetAccessoryRenderLayer.FrontModel
    }
}

internal fun pixelPetEggSpriteFrame(progress: Float): Int =
    (progress.coerceIn(0f, 1f) * (PixelPetSpriteAtlas.frameCount(PixelPetAction.Hatching) - 1))
        .roundToInt()

internal enum class PixelPetExpression {
    Content,
    Sleepy,
    Hungry,
    Curious,
    Delighted,
    Startled,
}

internal fun PixelPetState.currentExpression(action: PixelPetAction): PixelPetExpression = when {
    !hatched || action == PixelPetAction.Hatching -> PixelPetExpression.Curious
    hunger <= 22 || energy <= 18 -> PixelPetExpression.Hungry
    action == PixelPetAction.Frightened || moodValue <= 16 -> PixelPetExpression.Startled
    action == PixelPetAction.Sleeping || sleepQuality <= 18 -> PixelPetExpression.Sleepy
    action in setOf(PixelPetAction.Happy, PixelPetAction.Petted, PixelPetAction.Playing) || moodValue >= 76 -> PixelPetExpression.Delighted
    action in setOf(PixelPetAction.Exploring, PixelPetAction.Watching, PixelPetAction.Calling) -> PixelPetExpression.Curious
    else -> PixelPetExpression.Content
}

private fun DrawScope.drawPixelPetExpressionLayer(
    state: PixelPetState,
    colors: PixelPetModelColors,
    unit: Float,
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    frame: Int,
    spriteFrame: PixelPetSpriteFrame?,
    facing: PixelPetFacing,
    headOverride: PixelPetAccessoryAnchor? = null,
) {
    if (!state.hatched || facing == PixelPetFacing.Back) return
    val expression = state.currentExpression(action)
    val head = headOverride ?: spriteFrame?.let {
        PixelPetSpriteAtlas.accessoryAnchor(
            spriteFrame = it,
            species = species,
            stage = stage,
            action = action,
            slot = PixelPetAccessorySlot.Head,
            facing = facing,
            unit = unit,
        )
    } ?: return
    val profile = when (facing) {
        PixelPetFacing.Front -> PixelPetExpressionProfile(
            leftEyeX = -0.92f,
            rightEyeX = 0.54f,
            eyeY = -0.72f,
            cheekY = 0.54f,
            mouthY = 0.76f,
        )
        PixelPetFacing.Left -> PixelPetExpressionProfile(
            leftEyeX = -0.44f,
            rightEyeX = -0.44f,
            eyeY = -0.60f,
            cheekY = 0.34f,
            mouthY = 0.56f,
        )
        PixelPetFacing.Right -> PixelPetExpressionProfile(
            leftEyeX = 0.44f,
            rightEyeX = 0.44f,
            eyeY = -0.60f,
            cheekY = 0.34f,
            mouthY = 0.56f,
        )
        PixelPetFacing.Back -> return
    }
    fun facePixel(color: Color, x: Float, y: Float, width: Float, height: Float) {
        pixel(color, unit, head.x + x, head.y + y, width, height)
    }
    fun bothEyes(color: Color, width: Float, height: Float) {
        facePixel(color, profile.leftEyeX, profile.eyeY, width, height)
        if (facing == PixelPetFacing.Front) {
            facePixel(color, profile.rightEyeX, profile.eyeY, width, height)
        }
    }
    val blink = action == PixelPetAction.Idle && frame in 5..6
    when (expression) {
        PixelPetExpression.Content -> if (blink) {
            bothEyes(colors.outline.copy(alpha = 0.78f), 0.62f, 0.24f)
        }
        PixelPetExpression.Sleepy -> {
            bothEyes(colors.shade.copy(alpha = 0.82f), 0.74f, 0.22f)
        }
        PixelPetExpression.Hungry -> {
            facePixel(colors.accent.copy(alpha = 0.76f), profile.leftEyeX - 0.54f, profile.cheekY, 0.40f, 0.34f)
            if (facing == PixelPetFacing.Front) {
                facePixel(colors.accent.copy(alpha = 0.76f), profile.rightEyeX + 0.54f, profile.cheekY, 0.40f, 0.34f)
            }
        }
        PixelPetExpression.Curious -> {
            facePixel(colors.reflection.copy(alpha = 0.88f), profile.leftEyeX + 0.14f, profile.eyeY - 0.30f, 0.30f, 0.30f)
            if (facing == PixelPetFacing.Front && frame % 3 == 0) {
                facePixel(colors.reflection.copy(alpha = 0.74f), profile.rightEyeX + 0.14f, profile.eyeY - 0.30f, 0.30f, 0.30f)
            }
        }
        PixelPetExpression.Delighted -> {
            facePixel(colors.accent.copy(alpha = 0.72f), profile.leftEyeX - 0.50f, profile.cheekY, 0.48f, 0.34f)
            if (facing == PixelPetFacing.Front) {
                facePixel(colors.accent.copy(alpha = 0.72f), profile.rightEyeX + 0.50f, profile.cheekY, 0.48f, 0.34f)
            }
            facePixel(colors.highlight.copy(alpha = 0.88f), -0.16f, profile.mouthY, 0.42f, 0.22f)
        }
        PixelPetExpression.Startled -> {
            bothEyes(colors.reflection.copy(alpha = 0.90f), 0.42f, 0.42f)
            facePixel(colors.accent.copy(alpha = 0.62f), -0.16f, profile.mouthY, 0.38f, 0.48f)
        }
    }
}

private data class PixelPetExpressionProfile(
    val leftEyeX: Float,
    val rightEyeX: Float,
    val eyeY: Float,
    val cheekY: Float,
    val mouthY: Float,
)

private fun DrawScope.drawPixelPetGroundFeedback(
    state: PixelPetState,
    palette: PixelPalette,
    unit: Float,
    action: PixelPetAction,
    frame: Int,
    facing: PixelPetFacing,
) {
    val stride = if (action in setOf(PixelPetAction.Walking, PixelPetAction.Exploring)) {
        if (frame % 2 == 0) -1.35f else 1.35f
    } else {
        0f
    }
    val footX = 6f + stride
    val footY = 10.22f
    when {
        state.currentWeather() == PixelPetWeather.Drizzle || state.habitat == PixelPetHabitat.Lagoon -> {
            pixel(palette.highlight.copy(alpha = 0.38f), unit, footX - 1.18f, footY + 0.20f, 2.36f, 0.20f)
            pixel(palette.secondary.copy(alpha = 0.25f), unit, footX - 0.70f, footY - 0.18f, 1.40f, 0.18f)
            if (action in setOf(PixelPetAction.Walking, PixelPetAction.Exploring)) {
                pixel(palette.highlight.copy(alpha = 0.24f), unit, footX - 1.70f, footY + 0.52f, 0.58f, 0.18f)
            }
        }
        state.habitat == PixelPetHabitat.Moon -> {
            pixel(palette.shadow.copy(alpha = 0.36f), unit, footX - 0.72f, footY, 1.44f, 0.30f)
            if (action in setOf(PixelPetAction.Walking, PixelPetAction.Exploring)) {
                pixel(palette.highlight.copy(alpha = 0.24f), unit, footX + stride * 0.55f, footY + 0.40f, 0.42f, 0.18f)
            }
        }
        state.habitat == PixelPetHabitat.Garden -> {
            pixel(palette.primary.copy(alpha = 0.42f), unit, footX - 0.74f, footY, 1.48f, 0.26f)
            if (action in setOf(PixelPetAction.Walking, PixelPetAction.Exploring)) {
                pixel(palette.highlight.copy(alpha = 0.48f), unit, footX - 1.46f, footY - 0.28f, 0.40f, 0.76f)
                pixel(palette.secondary.copy(alpha = 0.44f), unit, footX + 1.12f, footY - 0.22f, 0.34f, 0.66f)
            }
        }
        else -> {
            pixel(palette.highlight.copy(alpha = 0.28f), unit, footX - 0.92f, footY, 1.84f, 0.20f)
        }
    }
    if (facing == PixelPetFacing.Left || facing == PixelPetFacing.Right) {
        pixel(palette.shadow.copy(alpha = 0.14f), unit, footX + if (facing == PixelPetFacing.Left) 1.25f else -1.25f, footY + 0.40f, 0.38f, 0.18f)
    }
}

private fun DrawScope.drawPixelPetMaterialBehindModel(
    state: PixelPetState,
    colors: PixelPetModelColors,
    unit: Float,
    action: PixelPetAction,
    frame: Int,
) {
    val weather = state.currentWeather()
    if (weather == PixelPetWeather.Drizzle) {
        pixel(colors.reflection.copy(alpha = 0.42f), unit, 3.66f, 3.32f + (frame % 2) * 0.24f, 0.32f, 0.86f)
        pixel(colors.reflection.copy(alpha = 0.34f), unit, 8.18f, 4.06f + ((frame + 1) % 2) * 0.22f, 0.28f, 0.72f)
    }
    if (state.habitat == PixelPetHabitat.Lagoon) {
        pixel(colors.reflection.copy(alpha = 0.26f), unit, 3.62f, 9.28f, 4.86f, 0.20f)
        pixel(colors.accent.copy(alpha = 0.18f), unit, 4.48f, 9.70f, 3.12f, 0.16f)
    }
    if (action == PixelPetAction.Frightened) {
        pixel(colors.accent.copy(alpha = 0.42f), unit, 3.32f, 2.84f + (frame % 2) * 0.22f, 0.24f, 0.64f)
    }
}

private fun DrawScope.drawPixelPetMaterialFrontModel(
    state: PixelPetState,
    colors: PixelPetModelColors,
    unit: Float,
    action: PixelPetAction,
    frame: Int,
) {
    val weather = state.currentWeather()
    if (weather == PixelPetWeather.Drizzle) {
        val dropX = 7.84f + if (frame % 2 == 0) 0f else 0.30f
        pixel(colors.reflection.copy(alpha = 0.58f), unit, dropX, 4.18f, 0.28f, 0.52f)
        pixel(colors.highlight.copy(alpha = 0.32f), unit, 5.08f, 3.56f, 0.36f, 0.20f)
    }
    if (isPixelPetNight()) {
        pixel(colors.reflection.copy(alpha = 0.44f), unit, 3.72f, 4.40f, 0.24f, 2.18f)
        pixel(colors.reflection.copy(alpha = 0.30f), unit, 8.02f, 5.26f, 0.20f, 1.62f)
    }
    if (state.furniture.any { it.kind == PixelPetFurnitureKind.Lamp && it.durability > 0 }) {
        pixel(Color(0xFFFFC777).copy(alpha = 0.44f), unit, 7.78f, 4.28f, 0.28f, 2.52f)
        pixel(Color(0xFFFFE6A4).copy(alpha = 0.30f), unit, 6.98f, 3.92f, 0.68f, 0.22f)
    }
    if (state.habitat == PixelPetHabitat.Lagoon && action in setOf(PixelPetAction.Watching, PixelPetAction.Exploring)) {
        pixel(colors.reflection.copy(alpha = 0.42f), unit, 5.26f, 8.72f + (frame % 2) * 0.18f, 1.46f, 0.18f)
    }
}

private fun DrawScope.drawPixelPetShadow(
    palette: PixelPalette,
    unit: Float,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    frame: Int,
    facing: PixelPetFacing,
) {
    val actionScale = when (action) {
        PixelPetAction.Sleeping -> 1.12f
        PixelPetAction.Happy,
        PixelPetAction.Playing,
        PixelPetAction.Hatching,
        -> if (frame in 2..3) 0.72f else 0.88f
        PixelPetAction.Frightened -> 0.84f
        else -> 1f
    }
    val directionOffset = when (facing) {
        PixelPetFacing.Left -> -0.28f
        PixelPetFacing.Right -> 0.28f
        PixelPetFacing.Front,
        PixelPetFacing.Back,
        -> 0f
    }
    val pixel = pixelPetIntegerScale(unit).toFloat()
    val width = (unit * 8.2f * actionScale / pixel).roundToInt().coerceAtLeast(4) * pixel
    val height = pixel * if (action == PixelPetAction.Sleeping) 3f else 2f
    val centerX = (size.width / 2f + unit * directionOffset).roundToInt().toFloat()
    val top = (unit * 9.85f / pixel).roundToInt() * pixel
    val left = (centerX - width / 2f).roundToInt().toFloat()
    drawRect(
        color = palette.shadow.copy(alpha = 0.20f),
        topLeft = Offset(left, top),
        size = Size(width, height),
    )
    drawRect(
        color = palette.shadow.copy(alpha = 0.12f),
        topLeft = Offset(left + pixel, top - pixel),
        size = Size((width - pixel * 2f).coerceAtLeast(pixel * 2f), pixel),
    )
}

internal data class PixelPetAccessorySpriteProfile(
    val shiftX: Float,
    val shiftY: Float,
    val detailX: Float,
    val detailY: Float,
)

/**
 * Accessory micro-sprites are positioned from the authored body pose, then
 * receive a species/direction adjustment. This avoids a universal hat or bag
 * silhouette sitting in the same place on every pet.
 */
internal fun pixelPetAccessorySpriteProfile(
    species: PixelPetSpecies,
    facing: PixelPetFacing,
    action: PixelPetAction,
): PixelPetAccessorySpriteProfile {
    val speciesShift = when (species) {
        PixelPetSpecies.Cat -> -0.12f to -0.08f
        PixelPetSpecies.Dog -> 0.06f to 0.04f
        PixelPetSpecies.Bird -> 0f to 0.16f
        PixelPetSpecies.Rabbit -> 0.02f to -0.22f
        PixelPetSpecies.Penguin -> 0f to 0.10f
        PixelPetSpecies.Hamster -> 0f to -0.02f
    }
    val facingShift = when (facing) {
        PixelPetFacing.Front -> 0f to 0f
        PixelPetFacing.Back -> 0f to 0.16f
        PixelPetFacing.Left -> -0.18f to 0.04f
        PixelPetFacing.Right -> 0.18f to 0.04f
    }
    val actionShift = when (action) {
        PixelPetAction.Eating,
        PixelPetAction.Cleaning,
        -> 0f to -0.18f
        PixelPetAction.Sleeping -> 0.22f to 0.22f
        PixelPetAction.Happy,
        PixelPetAction.Playing,
        -> 0f to -0.12f
        else -> 0f to 0f
    }
    return PixelPetAccessorySpriteProfile(
        shiftX = speciesShift.first + facingShift.first + actionShift.first,
        shiftY = speciesShift.second + facingShift.second + actionShift.second,
        detailX = when (facing) {
            PixelPetFacing.Left -> -1f
            PixelPetFacing.Right -> 1f
            else -> 0f
        },
        detailY = if (species == PixelPetSpecies.Rabbit) -0.48f else 0f,
    )
}

private fun DrawScope.drawPixelPetAccessories(
    accessories: Set<PixelPetAccessory>,
    colors: PixelPetModelColors,
    unit: Float,
    species: PixelPetSpecies,
    stage: PixelPetGrowthStage,
    action: PixelPetAction,
    frame: Int,
    slot: PixelPetAccessorySlot,
    facing: PixelPetFacing,
    anchor: PixelPetAccessoryAnchor,
) {
    val profile = pixelPetAccessorySpriteProfile(species, facing, action)
    fun anchoredPixel(
        color: Color,
        dx: Float,
        dy: Float,
        width: Float,
        height: Float,
    ) {
        pixel(
            color,
            unit,
            anchor.x + (dx + profile.shiftX) * anchor.scale,
            anchor.y + (dy + profile.shiftY) * anchor.scale,
            width * anchor.scale,
            height * anchor.scale,
        )
    }
    fun anchoredLine(
        color: Color,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        width: Float,
    ) {
        pixelLine(
            color,
            unit,
            anchor.x + (startX + profile.shiftX) * anchor.scale,
            anchor.y + (startY + profile.shiftY) * anchor.scale,
            anchor.x + (endX + profile.shiftX) * anchor.scale,
            anchor.y + (endY + profile.shiftY) * anchor.scale,
            width * anchor.scale,
        )
    }
    accessories.filter { it.slot == slot }.forEach { accessory ->
        when (accessory) {
            PixelPetAccessory.LeafCrown -> {
                anchoredPixel(Color(0xFF69A868), -2.9f, 0f, 5.8f, 0.55f)
                anchoredPixel(Color(0xFF8DCA6E), -2.0f, -0.75f, 0.9f, 1.0f)
                anchoredPixel(Color(0xFF8DCA6E), 0.95f, -0.90f, 0.9f, 1.15f)
                anchoredPixel(colors.highlight, profile.detailX - 0.18f, profile.detailY - 0.62f, 0.46f, 0.46f)
            }
            PixelPetAccessory.StarPin -> {
                anchoredPixel(Color(0xFFFFD85D), 2.35f, 2.40f, 1.0f, 1.0f)
                anchoredPixel(Color(0xFFFFD85D), 1.95f, 2.75f, 1.85f, 0.28f)
                anchoredPixel(colors.reflection, 2.55f + profile.detailX * 0.30f, 2.55f, 0.28f, 0.28f)
            }
            PixelPetAccessory.ShellBag -> {
                anchoredPixel(Color(0xFF7FC7C8), 0f, 0.75f, 1.7f, 1.55f)
                anchoredLine(colors.outline, 0.05f, 0.75f, -1.35f, -0.50f, 0.34f)
                anchoredPixel(colors.highlight, 0.36f + profile.detailX * 0.22f, 1.06f, 0.38f, 0.38f)
            }
            PixelPetAccessory.WateringCan -> {
                anchoredPixel(Color(0xFF7FAFD0), -1.90f, 0.75f, 1.85f, 1.35f)
                anchoredLine(colors.outline, -0.30f, 0.75f, 0.40f, -0.55f, 0.34f)
                anchoredPixel(colors.reflection, -2.20f + profile.detailX * 0.20f, 1.12f, 0.45f, 0.28f)
            }
            PixelPetAccessory.CloudKite -> {
                anchoredPixel(Color(0xFFA9D9F1), -2.05f, -3.25f, 1.35f, 1.35f)
                anchoredLine(colors.outline, -0.70f, -1.90f, 0.40f, 1.55f, 0.20f)
                anchoredPixel(colors.highlight, -1.76f + profile.detailX * 0.18f, -3.02f, 0.34f, 0.34f)
            }
            PixelPetAccessory.MoonLantern -> {
                anchoredPixel(Color(0xFFFFD47A), -1.80f, -1.60f, 1.35f, 1.75f)
                anchoredLine(colors.outline, -1.13f, -1.60f, -1.13f, -2.90f, 0.26f)
                anchoredPixel(colors.highlight, -1.46f + profile.detailX * 0.14f, -1.18f, 0.38f, 0.44f)
            }
            PixelPetAccessory.LagoonRod -> {
                anchoredLine(Color(0xFF8C684E), -1.50f, -2.15f, 0.50f, 2.30f, 0.34f)
                anchoredLine(Color(0xFF9ED9F0), -1.50f, -2.15f, -2.20f, 0.65f, 0.16f)
                anchoredPixel(colors.reflection, -2.36f + profile.detailX * 0.22f, 0.58f, 0.28f, 0.28f)
            }
            PixelPetAccessory.AuroraScarf -> {
                anchoredPixel(Color(0xFF9E8DE7), -2.75f, -0.35f, 5.50f, 0.74f)
                anchoredPixel(Color(0xFF74CFE1), -2.18f, 0.26f, 4.35f, 0.30f)
                anchoredPixel(Color(0xFFE2C3FF), 1.95f + profile.detailX * 0.22f, 0.34f, 1.38f, 1.56f)
                anchoredPixel(colors.highlight, -1.44f, -0.18f, 0.54f, 0.26f)
            }
            PixelPetAccessory.CometBell -> {
                anchoredPixel(Color(0xFFFFCB65), -0.66f, 0.04f, 1.32f, 1.16f)
                anchoredPixel(Color(0xFFFFEC9D), -0.32f, 0.22f, 0.46f, 0.44f)
                anchoredPixel(colors.outline, -0.20f, 1.08f, 0.42f, 0.36f)
            }
            PixelPetAccessory.TailRibbon -> {
                anchoredPixel(Color(0xFFFF91B7), -0.70f + profile.detailX * 0.16f, -0.66f, 1.40f, 0.72f)
                anchoredPixel(Color(0xFFFFC7D8), -1.54f + profile.detailX * 0.18f, -1.08f, 0.86f, 1.48f)
                anchoredPixel(Color(0xFFFFC7D8), 0.72f + profile.detailX * 0.18f, -1.08f, 0.86f, 1.48f)
                anchoredPixel(colors.highlight, -0.18f, -0.36f, 0.42f, 0.42f)
            }
            PixelPetAccessory.PawSpark -> {
                val step = if (action == PixelPetAction.Walking || action == PixelPetAction.Exploring) {
                    if (frame % 2 == 0) -1.15f else 1.15f
                } else {
                    0f
                }
                anchoredPixel(Color(0xFFA8E8FF), step - 0.46f, 0.18f, 0.92f, 0.34f)
                anchoredPixel(Color(0xFFE8C9FF), -step * 0.55f, -0.42f, 0.34f, 0.34f)
                anchoredPixel(colors.highlight, step + profile.detailX * 0.18f, -0.74f, 0.24f, 0.24f)
            }
        }
    }
}

@Composable
fun PixelPetMiniCard(
    state: PixelPetState,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = pixelPalette(PixelStyle.PetCompanion, isInDarkTheme())
    val idlePhase = rememberPixelPetIdlePhase()
    val shape = RoundedCornerShape(6.dp)
    Surface(
        color = palette.surfaceAlt.copy(alpha = 0.78f),
        contentColor = palette.highlight,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.outline.copy(alpha = 0.82f), shape)
            .clickable(onClick = onAction),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PixelPetLkmHabitatBackdrop(
                state = state,
                modifier = Modifier.fillMaxSize(),
                idlePhase = idlePhase,
            )
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PixelPetAvatar(state = state, size = 42.dp, idlePhase = idlePhase, showGround = false)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = if (state.hatched) state.name else stringResource(R.string.pixel_pet_egg_name),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.highlight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (state.hatched) {
                            stringResource(R.string.pixel_pet_mini_status, state.level, state.coins)
                        } else {
                            stringResource(R.string.pixel_pet_hatch_hint)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.highlight.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (state.hatched) stringResource(R.string.pixel_pet_interact_short) else stringResource(R.string.pixel_pet_hatch_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun PixelPetLkmScene(
    state: PixelPetState,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = pixelPalette(PixelStyle.PetCompanion, isInDarkTheme())
    val idlePhase = rememberPixelPetIdlePhase()
    val width = if (compact) 42.dp else 96.dp
    val height = if (compact) 34.dp else 72.dp
    val avatarSize = if (compact) 30.dp else 54.dp
    val shape = RoundedCornerShape(if (compact) 5.dp else 8.dp)
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(palette.surfaceAlt.copy(alpha = 0.70f))
            .border(1.dp, palette.outline.copy(alpha = 0.72f), shape),
        contentAlignment = Alignment.BottomCenter,
    ) {
        PixelPetLkmHabitatBackdrop(
            state = state,
            modifier = Modifier.fillMaxSize(),
            idlePhase = idlePhase,
        )
        PixelPetAvatar(
            state = state,
            size = avatarSize,
            modifier = Modifier.padding(bottom = if (compact) 1.dp else 2.dp),
            idlePhase = idlePhase,
            showGround = false,
        )
    }
}

@Composable
fun PixelPetLkmInteractivePet(
    state: PixelPetState,
    compact: Boolean,
    onInteract: () -> Unit,
    onOpenChat: () -> Unit,
    onPet: () -> Unit,
    onFeed: () -> Unit,
    onExplore: () -> Unit,
    onPositionChanged: (Float, Float, Boolean) -> Unit,
    idlePhase: Float? = null,
    showFurniture: Boolean = true,
    stageMode: PixelPetStageMode = PixelPetStageMode.Card,
    avatarSize: Dp? = null,
    hitSize: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val resolvedAvatarSize = avatarSize ?: pixelPetLkmInteractiveAvatarSize(compact, stageMode)
    val animationPhase = idlePhase ?: rememberPixelPetIdlePhase()
    val petPalette = pixelPalette(PixelStyle.PetCompanion, isInDarkTheme())
    val petName = if (state.hatched) state.name else stringResource(R.string.pixel_pet_egg_name)
    val petDescription = stringResource(R.string.pixel_pet_card_description, petName)
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val currentOnInteract by rememberUpdatedState(onInteract)
    val currentOnOpenChat by rememberUpdatedState(onOpenChat)
    val currentOnPet by rememberUpdatedState(onPet)
    val currentOnFeed by rememberUpdatedState(onFeed)
    val currentOnExplore by rememberUpdatedState(onExplore)
    val currentOnPositionChanged by rememberUpdatedState(onPositionChanged)
    val scope = rememberCoroutineScope()
    var pendingTapJob by remember { mutableStateOf<Job?>(null) }
    var previousTapAt by remember { mutableStateOf(0L) }
    var previousTapPosition by remember { mutableStateOf(Offset.Unspecified) }
    val haptic = LocalHapticFeedback.current
    val dragColor = MaterialTheme.colorScheme.primary
    val exploreColor = MaterialTheme.colorScheme.tertiary
    val resolvedHitSize = hitSize ?: pixelPetLkmInteractiveHitSize(compact, stageMode)
    val cardStageLift = if (!compact && stageMode == PixelPetStageMode.Card) 7.dp else 0.dp
    val furnitureActivity = state.activeFurnitureActivity()
    val activeFurnitureId = furnitureActivity?.furnitureId
    val inactiveFurniture = remember(state.furniture, activeFurnitureId) {
        state.furniture.filterNot { it.id == activeFurnitureId }
    }
    val activeFurniture = remember(state.furniture, activeFurnitureId) {
        state.furniture.filter { it.id == activeFurnitureId }
    }
    val inactiveFurnitureDraw: DrawScope.() -> Unit = remember(inactiveFurniture, petPalette) {
        { drawPixelPetFurnitureItems(inactiveFurniture, petPalette, phase = 0f) }
    }
    BoxWithConstraints(modifier = modifier) {
        val maxX = with(density) { (maxWidth - resolvedHitSize).toPx().coerceAtLeast(0f) }
        val maxY = with(density) { (maxHeight - resolvedHitSize).toPx().coerceAtLeast(0f) }
        val targetX = (if (landscape) state.lkmLandscapePositionX else state.lkmPositionX) * maxX
        val targetY = (if (landscape) state.lkmLandscapePositionY else state.lkmPositionY) * maxY
        val animatedX = remember(maxX) { Animatable(targetX) }
        val animatedY = remember(maxY) { Animatable(targetY) }
        var dragOffsetX by remember(maxX) { mutableFloatStateOf(targetX) }
        var dragOffsetY by remember(maxY) { mutableFloatStateOf(targetY) }
        var dragging by remember { mutableStateOf(false) }
        var settlingDrag by remember { mutableStateOf(false) }
        var facing by remember { mutableStateOf(PixelPetFacing.Front) }
        val movingToTarget = !dragging && !settlingDrag &&
            (abs(animatedX.value - targetX) > 0.5f || abs(animatedY.value - targetY) > 0.5f)
        val renderedX = if (dragging || settlingDrag) dragOffsetX else animatedX.value
        val renderedY = if (dragging || settlingDrag) dragOffsetY else animatedY.value
        val movementStartedAt = remember(targetX, targetY) { System.currentTimeMillis() }
        val visualState = if (movingToTarget && state.hatched) {
            state.copy(lastAction = PixelPetAction.Walking, lastActionAt = movementStartedAt)
        } else {
            state
        }

        LaunchedEffect(
            targetX,
            targetY,
            state.activeFurnitureId,
            state.furniture,
            dragging,
            settlingDrag,
        ) {
            if (!dragging && !settlingDrag) {
                val start = normalizePixelPetDragPosition(Offset(animatedX.value, animatedY.value), maxX, maxY)
                val target = normalizePixelPetDragPosition(Offset(targetX, targetY), maxX, maxY)
                val path = PixelPetBehaviorEngine.movementPath(
                    start = start,
                    target = target,
                    furniture = state.furniture,
                    targetFurnitureId = state.activeFurnitureId,
                )
                path.forEach { waypoint ->
                    val waypointX = waypoint.x * maxX
                    val waypointY = waypoint.y * maxY
                    val delta = Offset(waypointX - animatedX.value, waypointY - animatedY.value)
                    facing = PixelPetSpriteAtlas.resolveFacing(delta.x, delta.y, facing)
                    val duration = (240 + delta.getDistance().coerceAtMost(420f) * 1.5f).toInt()
                    coroutineScope {
                        launch { animatedX.animateTo(waypointX, tween(duration, easing = FastOutSlowInEasing)) }
                        launch { animatedY.animateTo(waypointY, tween(duration, easing = FastOutSlowInEasing)) }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showFurniture && inactiveFurniture.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(),
                    onDraw = inactiveFurnitureDraw,
                )
            }
            if (showFurniture && activeFurniture.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPixelPetFurnitureItems(
                        activeFurniture,
                        petPalette,
                        animationPhase,
                        activeFurnitureId,
                        furnitureActivity?.action,
                        furnitureActivity?.progress ?: 0f,
                    )
                }
            }
            if (dragging) {
                PixelPetDropZone(
                    label = stringResource(R.string.pixel_pet_feed),
                    color = dragColor,
                    compact = compact,
                    icon = Icons.Rounded.Restaurant,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
                PixelPetDropZone(
                    label = stringResource(R.string.pixel_pet_explore_habitat),
                    color = exploreColor,
                    compact = compact,
                    icon = Icons.Rounded.Explore,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
            Box(
                modifier = Modifier
                    .size(resolvedHitSize)
                    .semantics {
                        contentDescription = petDescription
                        onClick {
                            currentOnInteract()
                            true
                        }
                        onLongClick {
                            currentOnPet()
                            true
                        }
                    }
                    .graphicsLayer {
                        translationX = renderedX
                        translationY = renderedY
                        scaleX = if (dragging) 1.06f else 1f
                        scaleY = if (dragging) 1.06f else 1f
                    }
                    .pointerInput(maxX, maxY, state.hatched, state.lkmPositionLocked, landscape) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Main,
                            )
                            var lastPosition = down.position
                            var releasedAt = down.uptimeMillis
                            var gestureCancelled = false
                            var dragStarted = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    gestureCancelled = true
                                    break
                                }
                                releasedAt = change.uptimeMillis
                                if (!change.pressed) {
                                    if (dragStarted || distanceBetween(change.position, down.position) <= viewConfiguration.touchSlop) {
                                        change.consume()
                                    }
                                    break
                                }
                                val dragAmount = change.position - lastPosition
                                lastPosition = change.position
                                val distanceFromDown = (change.position - down.position).getDistance()
                                var startedThisEvent = false
                                if (
                                    !state.lkmPositionLocked &&
                                    !dragStarted &&
                                    distanceFromDown > viewConfiguration.touchSlop
                                ) {
                                    dragOffsetX = animatedX.value
                                    dragOffsetY = animatedY.value
                                    dragStarted = true
                                    startedThisEvent = true
                                    dragging = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                if (dragStarted && dragAmount != Offset.Zero) {
                                    change.consume()
                                    val appliedDelta = if (startedThisEvent) {
                                        change.position - down.position
                                    } else {
                                        dragAmount
                                    }
                                    facing = PixelPetSpriteAtlas.resolveFacing(appliedDelta.x, appliedDelta.y, facing)
                                    val resolved = resolvePixelPetDragPositionWithResistance(
                                        current = Offset(dragOffsetX, dragOffsetY),
                                        delta = appliedDelta,
                                        maxX = maxX,
                                        maxY = maxY,
                                    )
                                    dragOffsetX = resolved.x
                                    dragOffsetY = resolved.y
                                }
                            }

                            if (gestureCancelled) {
                                dragging = false
                                return@awaitEachGesture
                            }
                            if (!dragStarted) {
                                val heldFor = releasedAt - down.uptimeMillis
                                val moved = distanceBetween(lastPosition, down.position)
                                if (moved > viewConfiguration.touchSlop) {
                                    return@awaitEachGesture
                                }
                                if (heldFor >= viewConfiguration.longPressTimeoutMillis) {
                                    pendingTapJob?.cancel()
                                    pendingTapJob = null
                                    currentOnPet()
                                } else {
                                    val doubleTap = previousTapPosition != Offset.Unspecified &&
                                        releasedAt - previousTapAt <= viewConfiguration.doubleTapTimeoutMillis &&
                                        distanceBetween(previousTapPosition, down.position) <= viewConfiguration.touchSlop * 2f
                                    if (doubleTap) {
                                        pendingTapJob?.cancel()
                                        pendingTapJob = null
                                        previousTapAt = 0L
                                        previousTapPosition = Offset.Unspecified
                                        currentOnOpenChat()
                                    } else {
                                        previousTapAt = releasedAt
                                        previousTapPosition = down.position
                                        pendingTapJob?.cancel()
                                        pendingTapJob = scope.launch {
                                            delay(viewConfiguration.doubleTapTimeoutMillis)
                                            currentOnInteract()
                                            pendingTapJob = null
                                        }
                                    }
                                }
                                return@awaitEachGesture
                            }

                            val clamped = settlePixelPetDragPosition(
                                position = Offset(dragOffsetX, dragOffsetY),
                                maxX = maxX,
                                maxY = maxY,
                            )
                            val normalized = PixelPetBehaviorEngine.resolveFreePosition(
                                normalizePixelPetDragPosition(clamped, maxX, maxY),
                                state.furniture,
                                state.activeFurnitureId,
                            )
                            val settled = Offset(normalized.x * maxX, normalized.y * maxY)
                            currentOnPositionChanged(normalized.x, normalized.y, landscape)
                            when (pixelPetDropAction(normalized.x, normalized.y)) {
                                PixelPetDropAction.Feed -> currentOnFeed()
                                PixelPetDropAction.Explore -> currentOnExplore()
                                PixelPetDropAction.None -> Unit
                            }
                            settlingDrag = true
                            dragging = false
                            scope.launch {
                                coroutineScope {
                                    launch {
                                        animatedX.snapTo(dragOffsetX)
                                        animatedX.animateTo(
                                            settled.x,
                                            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.82f),
                                        )
                                    }
                                    launch {
                                        animatedY.snapTo(dragOffsetY)
                                        animatedY.animateTo(
                                            settled.y,
                                            spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.82f),
                                        )
                                    }
                                }
                                dragOffsetX = settled.x
                                dragOffsetY = settled.y
                                settlingDrag = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                PixelPetAvatar(
                    state = visualState,
                    size = resolvedAvatarSize,
                    modifier = Modifier.offset(y = -cardStageLift),
                    idlePhase = animationPhase,
                    showGround = false,
                    facing = facing,
                )
            }
            activeFurniture.firstOrNull()?.let { item ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawPixelPetActiveFurnitureForeground(
                        item = item,
                        palette = petPalette,
                        phase = animationPhase,
                        action = furnitureActivity?.action,
                        progress = furnitureActivity?.progress ?: 0f,
                    )
                }
            }
        }
    }
}

/**
 * The full LKM habitat is the pet's primary home, so baby stages use their
 * silhouette rather than an icon-like scale reduction to communicate age.
 */
internal fun pixelPetLkmInteractiveAvatarSize(
    compact: Boolean,
    stageMode: PixelPetStageMode = PixelPetStageMode.Card,
): Dp = when {
    compact -> 34.dp
    stageMode == PixelPetStageMode.Immersive -> 164.dp
    else -> 96.dp
}

internal fun pixelPetLkmInteractiveHitSize(
    compact: Boolean,
    stageMode: PixelPetStageMode = PixelPetStageMode.Card,
): Dp = when {
    compact -> 52.dp
    stageMode == PixelPetStageMode.Immersive -> 192.dp
    else -> 112.dp
}

internal fun resolvePixelPetDragPosition(
    current: Offset,
    delta: Offset,
    maxX: Float,
    maxY: Float,
): Offset = Offset(
    x = (current.x + delta.x).coerceIn(0f, maxX.coerceAtLeast(0f)),
    y = (current.y + delta.y).coerceIn(0f, maxY.coerceAtLeast(0f)),
)

internal fun resolvePixelPetDragPositionWithResistance(
    current: Offset,
    delta: Offset,
    maxX: Float,
    maxY: Float,
): Offset = Offset(
    x = resistedPixelPetAxis(current.x, delta.x, maxX),
    y = resistedPixelPetAxis(current.y, delta.y, maxY),
)

private fun resistedPixelPetAxis(current: Float, delta: Float, maximum: Float): Float {
    val safeMaximum = maximum.coerceAtLeast(0f)
    val next = current + delta
    return when {
        next < 0f -> next * 0.22f
        next > safeMaximum -> safeMaximum + (next - safeMaximum) * 0.22f
        else -> next
    }
}

internal fun settlePixelPetDragPosition(
    position: Offset,
    maxX: Float,
    maxY: Float,
): Offset {
    val normalized = normalizePixelPetDragPosition(position, maxX, maxY)
    return Offset(
        snapPixelPetPosition(normalized.x) * maxX.coerceAtLeast(0f),
        snapPixelPetPosition(normalized.y) * maxY.coerceAtLeast(0f),
    )
}

private fun distanceBetween(first: Offset, second: Offset): Float = (first - second).getDistance()

internal fun normalizePixelPetDragPosition(
    position: Offset,
    maxX: Float,
    maxY: Float,
): Offset = Offset(
    x = if (maxX <= 0f) 0f else (position.x / maxX).coerceIn(0f, 1f),
    y = if (maxY <= 0f) 0f else (position.y / maxY).coerceIn(0f, 1f),
)

@Composable
private fun PixelPetDropZone(
    label: String,
    color: Color,
    compact: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(6.dp)
            .height(if (compact) 34.dp else 42.dp)
            .width(if (compact) 54.dp else 104.dp)
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.58f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )
        if (!compact) {
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PixelPetLkmHabitatBackdrop(
    state: PixelPetState,
    modifier: Modifier = Modifier,
    idlePhase: Float? = null,
    wallpaperVisible: Boolean = false,
    showFurniture: Boolean = true,
    stageMode: PixelPetStageMode = PixelPetStageMode.Card,
) {
    val palette = pixelPalette(PixelStyle.PetCompanion, isInDarkTheme())
    val animationPhase = idlePhase ?: rememberPixelPetIdlePhase()
    val context = LocalContext.current
    val hamsterWheel = remember(context, state.species, state.growthStage, state.hatched) {
        if (
            state.hatched &&
            state.species == PixelPetSpecies.Hamster &&
            state.growthStage == PixelPetGrowthStage.Adult
        ) {
            PixelPetReferenceSprites.hamsterWheel(context)
        } else {
            null
        }
    }
    val lowPowerMode = remember(context) { isPixelPetLowRam(context) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // The pet persists one position per orientation. The stage focus must use
    // the same coordinate as the draggable avatar or it appears detached on
    // tablets and landscape phones.
    val stageState = if (landscape) {
        state.copy(
            lkmPositionX = state.lkmLandscapePositionX,
            lkmPositionY = state.lkmLandscapePositionY,
        )
    } else {
        state
    }
    val furnitureActivity = state.activeFurnitureActivity()
    val activeFurnitureId = furnitureActivity?.furnitureId
    val inactiveFurniture = remember(state.furniture, activeFurnitureId) {
        state.furniture.filterNot { it.id == activeFurnitureId }
    }
    val activeFurniture = remember(state.furniture, activeFurnitureId) {
        state.furniture.filter { it.id == activeFurnitureId }
    }
    val staticState = remember(
        state.habitat,
        stageState.lkmPositionX,
        stageState.lkmPositionY,
    ) {
        PixelPetState(
            habitat = state.habitat,
            lkmPositionX = stageState.lkmPositionX,
            lkmPositionY = stageState.lkmPositionY,
        )
    }
    val staticSceneDraw: DrawScope.() -> Unit = remember(staticState, palette, wallpaperVisible, stageMode) {
        { drawPixelPetLkmStaticScene(staticState, palette, wallpaperVisible, stageMode) }
    }
    val staticFurnitureDraw: DrawScope.() -> Unit = remember(inactiveFurniture, palette) {
        { drawPixelPetFurnitureItems(inactiveFurniture, palette, phase = 0f) }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(),
            onDraw = staticSceneDraw,
        )
        hamsterWheel?.let { wheel ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                // The wheel belongs to the habitat. It deliberately renders
                // before furniture and the interactive pet so dragging Hamu
                // never pulls the wheel along with its body.
                drawPixelPetHamsterWheel(wheel, animationPhase)
            }
        }
        if (showFurniture && inactiveFurniture.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(),
                onDraw = staticFurnitureDraw,
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPixelPetLkmDynamicScene(stageState, palette, animationPhase, lowPowerMode, stageMode)
            if (showFurniture && activeFurniture.isNotEmpty()) {
                drawPixelPetFurnitureItems(
                    activeFurniture,
                    palette,
                    animationPhase,
                    activeFurnitureId,
                    furnitureActivity?.action,
                    furnitureActivity?.progress ?: 0f,
                )
            }
            val unit = (size.minDimension / 28f).coerceAtLeast(1f)
            if (!state.hatched) {
                repeat(3) { index ->
                    val pulse = ((animationPhase + index * 0.31f) % 1f)
                    drawRect(
                        palette.highlight.copy(alpha = 0.16f + pulse * 0.14f),
                        Offset(size.width * (0.16f + index * 0.22f), size.height * (0.16f + index % 2 * 0.12f)),
                        Size(unit * 0.8f, unit * 0.8f),
                    )
                }
            } else if (state.level >= 3) {
                val glow = 0.12f + (sin(animationPhase * 2f * PI).toFloat() + 1f) * 0.04f
                drawRect(
                    palette.secondary.copy(alpha = glow),
                    Offset(0f, size.height * 0.73f),
                    Size(size.width, size.height * 0.27f),
                )
            }
        }
    }
}

@Composable
internal fun rememberPixelPetIdlePhase(enabled: Boolean = true): Float {
    if (!enabled || !LocalPixelCardMotionEnabled.current) return 0f
    val context = LocalContext.current
    val powerSave = rememberPixelPetPowerSaveMode()
    val policy = remember(context, powerSave) {
        PixelPetAnimationPolicy.forDevice(
            lowRam = isPixelPetLowRam(context),
            powerSave = powerSave,
        )
    }
    val transition = rememberInfiniteTransition(label = "pixel_pet_idle")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = policy.timelineMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pixel_pet_idle_phase",
    )
    if (!LocalPixelCardMotionEnabled.current) return 0f
    return quantizePixelPetPhase(phase, policy)
}

private fun isPixelPetLowRam(context: Context): Boolean =
    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true

@Composable
private fun rememberPixelPetPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    }
    var powerSave by remember(powerManager) {
        mutableStateOf(powerManager?.isPowerSaveMode == true)
    }
    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    powerSave = powerManager?.isPowerSaveMode == true
                }
            }
        }
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return powerSave
}

private fun DrawScope.drawPixelPetAvatarHabitat(
    habitat: PixelPetHabitat,
    palette: PixelPalette,
    unit: Float,
) {
    when (habitat) {
        PixelPetHabitat.Garden -> {
            drawRect(palette.primary.copy(alpha = 0.22f), Offset(unit * 1.4f, unit * 10.2f), Size(unit * 9.2f, unit * 0.55f))
        }

        PixelPetHabitat.Cloud -> {
            drawRect(palette.highlight.copy(alpha = 0.20f), Offset(unit * 1.2f, unit * 10.0f), Size(unit * 9.6f, unit * 0.85f))
            drawRect(palette.highlight.copy(alpha = 0.14f), Offset(unit * 2.0f, unit * 9.2f), Size(unit * 2.7f, unit * 0.75f))
        }

        PixelPetHabitat.Moon -> {
            drawRect(palette.secondary.copy(alpha = 0.22f), Offset(unit * 1.4f, unit * 10.15f), Size(unit * 9.2f, unit * 0.55f))
            drawRect(palette.highlight.copy(alpha = 0.34f), Offset(unit * 8.8f, unit * 1.0f), Size(unit * 0.42f, unit * 0.42f))
        }

        PixelPetHabitat.Lagoon -> {
            drawRect(palette.primary.copy(alpha = 0.24f), Offset(unit * 1.0f, unit * 10.1f), Size(unit * 10f, unit * 0.58f))
            drawLine(palette.highlight.copy(alpha = 0.30f), Offset(unit * 2f, unit * 9.5f), Offset(unit * 5f, unit * 9.5f), unit * 0.24f)
        }
    }
}

private fun DrawScope.drawPixelPetLkmStaticScene(
    state: PixelPetState,
    palette: PixelPalette,
    wallpaperVisible: Boolean = false,
    stageMode: PixelPetStageMode = PixelPetStageMode.Card,
) {
    val unit = (size.minDimension / 22f).coerceAtLeast(1f)
    val ground = size.height * 0.73f
    val horizon = size.height * 0.46f
    val drift = 0f
    val warmLight = palette.highlight.copy(alpha = 0.72f)
    val darkLine = palette.shadow.copy(alpha = 0.42f)

    // A translucent pixel diorama keeps user wallpapers visible while making
    // each habitat read as a complete living space instead of a flat overlay.
    drawRect(
        palette.background.copy(alpha = when {
            wallpaperVisible -> 0.28f
            stageMode == PixelPetStageMode.Immersive -> 0.78f
            else -> 0.62f
        }),
        Offset.Zero,
        Size(size.width, size.height),
    )
    when (state.habitat) {
        PixelPetHabitat.Garden -> {
            drawRect(palette.backgroundAlt.copy(alpha = 0.32f), Offset(0f, 0f), Size(size.width, horizon))
            drawRect(palette.secondary.copy(alpha = 0.14f), Offset(0f, horizon), Size(size.width, ground - horizon))
            drawRect(palette.primary.copy(alpha = 0.30f), Offset(0f, ground), Size(size.width, size.height - ground))
            drawRect(warmLight, Offset(size.width * 0.78f, unit * 1.5f), Size(unit * 2.4f, unit * 0.65f))
            drawRect(warmLight, Offset(size.width * 0.82f, unit * 0.9f), Size(unit * 1.6f, unit * 0.6f))
            drawRect(palette.primary.copy(alpha = 0.22f), Offset(-unit, ground - unit * 3.2f), Size(size.width * 0.62f, unit * 3.2f))
            drawRect(palette.secondary.copy(alpha = 0.20f), Offset(size.width * 0.42f, ground - unit * 2.3f), Size(size.width * 0.68f, unit * 2.3f))
            drawRect(darkLine, Offset(size.width * 0.10f, ground - unit * 4.4f), Size(unit * 0.9f, unit * 4.4f))
            drawRect(palette.primary.copy(alpha = 0.58f), Offset(size.width * 0.03f, ground - unit * 6.0f), Size(unit * 4.2f, unit * 2.8f))
            drawRect(palette.secondary.copy(alpha = 0.58f), Offset(size.width * 0.16f, ground - unit * 6.8f), Size(unit * 2.2f, unit * 1.2f))
            drawRect(darkLine, Offset(size.width * 0.68f, ground - unit * 3.2f), Size(unit * 0.65f, unit * 3.2f))
            repeat(4) { index ->
                val x = size.width * (0.65f + index * 0.07f)
                drawRect(warmLight.copy(alpha = 0.44f), Offset(x, ground - unit * (2.7f + index % 2)), Size(unit * 0.32f, unit * 0.32f))
            }
            repeat(5) { index ->
                val x = size.width * (0.04f + index * 0.20f)
                drawLine(palette.outline.copy(alpha = 0.46f), Offset(x, ground + unit * 0.6f), Offset(x + unit * 0.7f, ground - unit * 1.1f), unit * 0.28f)
                drawRect(palette.secondary.copy(alpha = 0.66f), Offset(x + unit * 0.55f, ground - unit * 1.9f), Size(unit * 0.7f, unit * 0.65f))
            }
            drawRect(palette.outline.copy(alpha = 0.52f), Offset(size.width * 0.78f, ground - unit * 2.5f), Size(unit * 0.55f, unit * 2.5f))
            repeat(3) { index ->
                drawRect(palette.outline.copy(alpha = 0.42f), Offset(size.width * (0.74f + index * 0.07f), ground - unit * (2.5f - index * 0.45f)), Size(unit * 1.7f, unit * 0.28f))
            }
        }

        PixelPetHabitat.Cloud -> {
            val cloud = palette.highlight.copy(alpha = 0.38f)
            drawRect(palette.backgroundAlt.copy(alpha = 0.32f), Offset.Zero, Size(size.width, ground))
            drawRect(palette.secondary.copy(alpha = 0.24f), Offset(0f, ground), Size(size.width, size.height - ground))
            drawRect(warmLight.copy(alpha = 0.46f), Offset(size.width * 0.14f, unit * 1.5f), Size(unit * 2.4f, unit * 0.7f))
            drawRect(cloud, Offset(size.width * 0.04f + drift, ground - unit * 2.0f), Size(size.width * 0.32f, unit * 1.6f))
            drawRect(cloud, Offset(size.width * 0.12f + drift, ground - unit * 2.8f), Size(size.width * 0.16f, unit * 1.3f))
            drawRect(cloud, Offset(size.width * 0.62f - drift, ground - unit * 1.4f), Size(size.width * 0.28f, unit * 1.2f))
            drawRect(cloud, Offset(size.width * 0.72f - drift, ground - unit * 2.1f), Size(size.width * 0.12f, unit * 1.2f))
            drawRect(darkLine, Offset(size.width * 0.16f, ground), Size(size.width * 0.58f, unit * 0.65f))
            drawRect(palette.highlight.copy(alpha = 0.56f), Offset(size.width * 0.24f, ground - unit * 0.2f), Size(size.width * 0.28f, unit * 0.55f))
            drawRect(palette.secondary.copy(alpha = 0.68f), Offset(size.width * 0.22f, ground - unit * 3.4f), Size(unit * 4.2f, unit * 1.8f))
            drawRect(palette.primary.copy(alpha = 0.68f), Offset(size.width * 0.30f, ground - unit * 4.2f), Size(unit * 2.7f, unit * 0.9f))
            drawRect(darkLine, Offset(size.width * 0.39f, ground - unit * 3.0f), Size(unit * 0.35f, unit * 3.0f))
            drawRect(palette.secondary.copy(alpha = 0.58f), Offset(size.width * 0.74f, ground - unit * 3.0f), Size(unit * 0.35f, unit * 3.0f))
            drawRect(palette.primary.copy(alpha = 0.56f), Offset(size.width * 0.67f, ground - unit * 4.0f), Size(unit * 1.7f, unit * 1.0f))
            drawLine(palette.highlight.copy(alpha = 0.38f), Offset(size.width * 0.76f, unit * 1.4f), Offset(size.width * 0.76f, ground - unit * 3.0f), unit * 0.18f)
        }

        PixelPetHabitat.Moon -> {
            drawRect(
                palette.shadow.copy(alpha = if (wallpaperVisible) 0.46f else 0.78f),
                Offset.Zero,
                Size(size.width, size.height),
            )
            drawRect(palette.backgroundAlt.copy(alpha = 0.30f), Offset(0f, horizon), Size(size.width, ground - horizon))
            drawRect(palette.secondary.copy(alpha = 0.18f), Offset(0f, ground), Size(size.width, size.height - ground))
            drawCircle(palette.highlight.copy(alpha = 0.46f), radius = unit * 2.5f, center = Offset(size.width * 0.82f, unit * 3.1f))
            drawCircle(palette.shadow.copy(alpha = 0.78f), radius = unit * 2.05f, center = Offset(size.width * 0.91f, unit * 2.55f))
            repeat(10) { index ->
                val sparkle = 0.28f + (index % 3) * 0.08f
                drawRect(palette.highlight.copy(alpha = sparkle), Offset(size.width * (0.06f + index * 0.095f), unit * (1.0f + index % 4 * 1.2f)), Size(unit * 0.34f, unit * 0.34f))
            }
            drawRect(palette.primary.copy(alpha = 0.42f), Offset(-unit, ground - unit * 3.0f), Size(size.width * 0.56f, unit * 3.0f))
            drawRect(palette.outline.copy(alpha = 0.42f), Offset(size.width * 0.46f, ground - unit * 1.8f), Size(size.width * 0.64f, unit * 1.8f))
            drawRect(palette.highlight.copy(alpha = 0.44f), Offset(size.width * 0.52f, ground - unit * 2.5f), Size(unit * 2.1f, unit * 0.72f))
            drawRect(palette.secondary.copy(alpha = 0.58f), Offset(size.width * 0.14f, ground - unit * 2.8f), Size(unit * 0.55f, unit * 2.8f))
            drawRect(palette.highlight.copy(alpha = 0.66f), Offset(size.width * 0.08f, ground - unit * 3.5f), Size(unit * 1.7f, unit * 0.75f))
        }

        PixelPetHabitat.Lagoon -> {
            drawRect(palette.backgroundAlt.copy(alpha = 0.28f), Offset.Zero, Size(size.width, horizon))
            drawRect(palette.primary.copy(alpha = 0.34f), Offset(0f, horizon), Size(size.width, size.height - horizon))
            drawRect(palette.secondary.copy(alpha = 0.30f), Offset(0f, ground), Size(size.width, size.height - ground))
            drawRect(warmLight.copy(alpha = 0.44f), Offset(size.width * 0.74f, unit * 1.8f), Size(unit * 2.0f, unit * 0.65f))
            drawRect(warmLight.copy(alpha = 0.28f), Offset(size.width * 0.79f, unit * 1.1f), Size(unit * 1.0f, unit * 0.45f))
            repeat(5) { index ->
                val y = horizon + unit * (1.3f + index * 1.65f) + drift * (index + 1) * 0.18f
                drawLine(palette.highlight.copy(alpha = 0.26f - index * 0.025f), Offset(0f, y), Offset(size.width, y), unit * 0.25f)
            }
            drawRect(darkLine, Offset(size.width * 0.08f, ground - unit * 1.4f), Size(unit * 0.5f, unit * 4.0f))
            repeat(4) { index ->
                drawLine(palette.outline.copy(alpha = 0.48f), Offset(size.width * 0.05f, ground - unit * (1.4f - index * 0.24f)), Offset(size.width * (0.28f + index * 0.02f), ground - unit * (1.4f - index * 0.24f)), unit * 0.26f)
            }
            drawRect(palette.secondary.copy(alpha = 0.72f), Offset(size.width * 0.62f, ground - unit * 2.2f), Size(unit * 3.9f, unit * 1.1f))
            drawRect(palette.outline.copy(alpha = 0.54f), Offset(size.width * 0.72f, ground - unit * 4.2f), Size(unit * 0.35f, unit * 2.0f))
            drawRect(palette.primary.copy(alpha = 0.72f), Offset(size.width * 0.62f, ground - unit * 3.8f), Size(unit * 2.4f, unit * 1.0f))
            drawRect(palette.highlight.copy(alpha = 0.58f), Offset(size.width * 0.68f, ground - unit * 3.35f), Size(unit * 1.2f, unit * 0.4f))
            drawRect(palette.secondary.copy(alpha = 0.78f), Offset(size.width * 0.38f + drift, ground - unit * 1.1f), Size(unit * 2.8f, unit * 0.65f))
            drawRect(palette.highlight.copy(alpha = 0.46f), Offset(size.width * 0.47f + drift, ground - unit * 1.55f), Size(unit * 0.9f, unit * 0.5f))
        }
    }
    drawPixelPetStaticHabitatDetails(state.habitat, palette, unit, ground, stageMode)
    if (stageMode == PixelPetStageMode.Card) {
        drawPixelPetCardStageClearance(state, palette, unit, ground, wallpaperVisible)
    }
    drawPixelPetTimeLayer(palette, unit, isPixelPetNight())
}

private fun DrawScope.drawPixelPetStaticHabitatDetails(
    habitat: PixelPetHabitat,
    palette: PixelPalette,
    unit: Float,
    ground: Float,
    stageMode: PixelPetStageMode,
) {
    when (habitat) {
        PixelPetHabitat.Garden -> {
            if (stageMode != PixelPetStageMode.Card) {
                repeat(5) { index ->
                    val width = unit * (2.4f - index * 0.18f)
                    val x = size.width * 0.48f - width / 2f + (index % 2) * unit * 0.35f
                    val y = ground + unit * (0.65f + index * 0.7f)
                    drawRect(palette.highlight.copy(alpha = 0.24f), Offset(x, y), Size(width, unit * 0.34f))
                }
            }
            drawRect(palette.shadow.copy(alpha = 0.34f), Offset(size.width * 0.085f, ground - unit * 5.1f), Size(unit * 2.0f, unit * 2.1f))
            drawRect(palette.highlight.copy(alpha = 0.48f), Offset(size.width * 0.10f, ground - unit * 4.75f), Size(unit * 1.25f, unit * 0.82f))
            repeat(4) { index ->
                val x = size.width * (0.72f + index * 0.052f)
                drawRect(palette.outline.copy(alpha = 0.38f), Offset(x, ground - unit * 1.2f), Size(unit * 0.22f, unit * 2.0f))
            }
        }
        PixelPetHabitat.Cloud -> {
            if (stageMode != PixelPetStageMode.Card) {
                repeat(4) { index ->
                    drawRect(
                        palette.shadow.copy(alpha = 0.16f + index * 0.035f),
                        Offset(size.width * (0.19f + index * 0.06f), ground + unit * (0.62f + index * 0.45f)),
                        Size(size.width * (0.48f - index * 0.12f), unit * 0.42f),
                    )
                }
            }
            val hub = Offset(size.width * 0.76f, ground - unit * 4.7f)
            drawRect(palette.secondary.copy(alpha = 0.72f), Offset(hub.x - unit * 0.25f, hub.y), Size(unit * 0.5f, unit * 4.1f))
            repeat(4) { index ->
                val angle = index * PI.toFloat() / 2f
                val end = Offset(hub.x + kotlin.math.cos(angle) * unit * 2.0f, hub.y + sin(angle) * unit * 2.0f)
                drawLine(palette.highlight.copy(alpha = 0.52f), hub, end, unit * 0.22f)
            }
            drawRect(palette.highlight.copy(alpha = 0.72f), Offset(hub.x - unit * 0.38f, hub.y - unit * 0.38f), Size(unit * 0.76f, unit * 0.76f))
        }
        PixelPetHabitat.Moon -> {
            repeat(if (stageMode == PixelPetStageMode.Card) 2 else 5) { index ->
                val x = size.width * (0.12f + index * 0.19f)
                val y = ground + unit * (0.85f + (index % 2) * 0.72f)
                drawOval(
                    palette.shadow.copy(alpha = 0.22f),
                    Offset(x, y),
                    Size(unit * (1.7f + index % 2), unit * 0.52f),
                )
            }
            repeat(3) { index ->
                val x = size.width * (0.58f + index * 0.08f)
                val height = unit * (1.25f + index * 0.65f)
                drawRect(palette.highlight.copy(alpha = 0.38f + index * 0.10f), Offset(x, ground - height), Size(unit * 0.48f, height))
                drawRect(palette.secondary.copy(alpha = 0.44f), Offset(x + unit * 0.48f, ground - height * 0.72f), Size(unit * 0.28f, height * 0.72f))
            }
        }
        PixelPetHabitat.Lagoon -> {
            repeat(7) { index ->
                val x = size.width * (0.08f + index * 0.055f)
                drawRect(palette.outline.copy(alpha = 0.42f), Offset(x, ground - unit * 0.8f), Size(unit * 1.5f, unit * 0.26f))
            }
            repeat(3) { index ->
                val x = size.width * (0.13f + index * 0.13f)
                drawRect(palette.shadow.copy(alpha = 0.40f), Offset(x, ground - unit * 0.4f), Size(unit * 0.30f, unit * 3.2f))
            }
            repeat(4) { index ->
                val x = size.width * (0.82f + index * 0.03f)
                drawLine(palette.secondary.copy(alpha = 0.52f), Offset(x, ground + unit * 1.4f), Offset(x - unit * 0.25f, ground - unit * (0.8f + index % 2)), unit * 0.16f)
            }
        }
    }
}

/**
 * The card keeps scenery at its edges and reserves an unframed, low-noise
 * center for the movable pet. This is a translucent habitat wash rather than
 * a second card, so it works over both the built-in scene and user wallpaper.
 */
private fun DrawScope.drawPixelPetCardStageClearance(
    state: PixelPetState,
    palette: PixelPalette,
    unit: Float,
    ground: Float,
    wallpaperVisible: Boolean,
) {
    val hitSize = pixelPetLkmInteractiveHitSize(false, PixelPetStageMode.Card).value * density
    val centerX = pixelPetStageFocusPosition(state.lkmPositionX, size.width, hitSize)
    val centerY = pixelPetStageFocusPosition(state.lkmPositionY, size.height, hitSize)
    val halfWidth = (unit * 6.8f).coerceAtMost(size.width * 0.32f)
    val top = (centerY - unit * 4.9f).coerceIn(0f, size.height)
    val bottom = (centerY + unit * 4.8f).coerceIn(top, (ground + unit * 1.2f).coerceAtMost(size.height))
    val left = (centerX - halfWidth).coerceAtLeast(0f)
    val right = (centerX + halfWidth).coerceAtMost(size.width)
    val alpha = if (wallpaperVisible) 0.16f else 0.30f
    drawRect(
        color = palette.background.copy(alpha = alpha),
        topLeft = Offset(left, top),
        size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
    )
    drawRect(
        color = palette.backgroundAlt.copy(alpha = alpha * 0.54f),
        topLeft = Offset(left, (bottom - unit * 1.5f).coerceAtLeast(top)),
        size = Size((right - left).coerceAtLeast(0f), unit * 1.5f),
    )
}

private fun DrawScope.drawPixelPetLkmDynamicScene(
    state: PixelPetState,
    palette: PixelPalette,
    phase: Float,
    lowPowerMode: Boolean,
    stageMode: PixelPetStageMode,
) {
    val unit = (size.minDimension / 22f).coerceAtLeast(1f)
    val ground = size.height * 0.73f
    val drift = sin(phase * 2f * PI).toFloat() * unit * 0.85f
    drawPixelPetStageFocus(state, palette, phase, stageMode, unit, ground)
    drawPixelPetWeatherLayer(
        weather = state.currentWeather(),
        palette = palette,
        unit = unit,
        phase = phase,
        lowPowerMode = lowPowerMode,
    )
    drawPixelPetEnvironmentalReaction(state, palette, unit, ground, phase)
    drawPixelPetHabitatForeground(state.habitat, palette, unit, ground, phase)
    if (state.displayAction() == PixelPetAction.Sleeping) {
        val sleepAlpha = 0.18f + ((sin(phase * 2f * PI).toFloat() + 1f) / 2f) * 0.12f
        drawRect(palette.shadow.copy(alpha = sleepAlpha), Offset(0f, 0f), Size(size.width, size.height * 0.30f))
        repeat(4) { index ->
            val x = size.width * (0.16f + index * 0.21f)
            val y = size.height * (0.14f + (index % 2) * 0.08f)
            drawRect(palette.highlight.copy(alpha = 0.45f + sleepAlpha), Offset(x, y), Size(unit * 0.34f, unit * 0.34f))
        }
    } else if (state.displayAction() == PixelPetAction.Exploring) {
        repeat(4) { index ->
            val progress = ((phase + index * 0.18f) % 1f)
            val x = size.width * (0.18f + progress * 0.66f)
            val y = size.height * (0.66f + (index % 2) * 0.035f)
            drawRect(palette.highlight.copy(alpha = 0.24f + (1f - progress) * 0.32f), Offset(x, y), Size(unit * 0.42f, unit * 0.20f))
        }
    } else if (state.displayAction() == PixelPetAction.Happy) {
        repeat(4) { index ->
            val sparkle = 0.34f + ((sin((phase + index * 0.21f) * 2f * PI).toFloat() + 1f) / 2f) * 0.44f
            val x = size.width * (0.18f + index * 0.20f)
            val y = size.height * (0.26f + (index % 2) * 0.12f)
            drawRect(palette.highlight.copy(alpha = sparkle), Offset(x, y), Size(unit * 0.32f, unit * 0.32f))
            drawRect(palette.secondary.copy(alpha = sparkle * 0.8f), Offset(x - unit * 0.18f, y + unit * 0.44f), Size(unit * 0.68f, unit * 0.18f))
        }
    } else if (state.displayAction() == PixelPetAction.Eating && state.activeFurnitureId == null) {
        drawRect(palette.secondary.copy(alpha = 0.68f), Offset(size.width * 0.18f, ground - unit * 0.92f), Size(unit * 1.9f, unit * 0.52f))
        drawRect(palette.highlight.copy(alpha = 0.72f), Offset(size.width * 0.28f, ground - unit * 1.25f), Size(unit * 0.72f, unit * 0.34f))
    } else if (state.displayAction() == PixelPetAction.Frightened) {
        val shake = sin(phase * 8f * PI).toFloat() * unit * 0.25f
        drawLine(
            palette.highlight.copy(alpha = 0.60f),
            Offset(size.width * 0.24f + shake, size.height * 0.30f),
            Offset(size.width * 0.28f + shake, size.height * 0.34f),
            unit * 0.18f,
        )
        drawLine(
            palette.highlight.copy(alpha = 0.60f),
            Offset(size.width * 0.76f - shake, size.height * 0.30f),
            Offset(size.width * 0.72f - shake, size.height * 0.34f),
            unit * 0.18f,
        )
    } else if (state.displayAction() == PixelPetAction.Petted) {
        repeat(3) { index ->
            val shimmer = 0.32f + ((sin((phase + index * 0.18f) * 2f * PI).toFloat() + 1f) / 2f) * 0.38f
            drawRect(
                palette.highlight.copy(alpha = shimmer),
                Offset(size.width * (0.38f + index * 0.08f), size.height * (0.38f - index * 0.05f)),
                Size(unit * 0.34f, unit * 0.34f),
            )
        }
    } else if (state.displayAction() == PixelPetAction.Playing && state.activeFurnitureId == null) {
        val ballX = size.width * (0.16f + ((phase * 1.8f) % 0.56f))
        val ballY = ground - unit * (1.1f + abs(sin(phase * 4f * PI).toFloat()) * 1.8f)
        drawRect(palette.secondary.copy(alpha = 0.86f), Offset(ballX, ballY), Size(unit * 0.72f, unit * 0.72f))
        drawRect(palette.highlight.copy(alpha = 0.78f), Offset(ballX + unit * 0.16f, ballY + unit * 0.16f), Size(unit * 0.24f, unit * 0.24f))
    } else if (state.displayAction() == PixelPetAction.Watching) {
        drawRect(palette.highlight.copy(alpha = 0.62f), Offset(size.width * 0.74f, size.height * 0.18f), Size(unit * 0.45f, unit * 0.45f))
        drawLine(palette.highlight.copy(alpha = 0.42f), Offset(size.width * 0.70f, size.height * 0.27f), Offset(size.width * 0.58f, size.height * 0.36f), unit * 0.16f)
    } else if (state.displayAction() == PixelPetAction.Cleaning) {
        repeat(3) { index ->
            drawCircle(
                palette.highlight.copy(alpha = 0.35f + index * 0.08f),
                radius = unit * (0.18f + index * 0.05f),
                center = Offset(size.width * (0.25f + index * 0.08f), ground - unit * (1.2f + index * 0.72f)),
            )
        }
    } else if (state.displayAction() == PixelPetAction.Calling) {
        repeat(2) { index ->
            drawLine(
                palette.secondary.copy(alpha = 0.72f - index * 0.18f),
                Offset(size.width * 0.68f, size.height * (0.52f - index * 0.06f)),
                Offset(size.width * (0.80f + index * 0.06f), size.height * (0.45f - index * 0.09f)),
                unit * 0.18f,
            )
        }
    }
    val particleCount = if (lowPowerMode) 3 else 8
    repeat(particleCount) { index ->
        val progress = (phase + index * 0.17f) % 1f
        val x = size.width * (0.08f + ((index * 0.19f) % 0.82f)) + drift * (index % 2)
        val y = size.height * (0.12f + ((progress + index * 0.07f) % 0.64f))
        val particleColor = when (state.habitat) {
            PixelPetHabitat.Garden -> if (index % 2 == 0) palette.highlight else palette.secondary
            PixelPetHabitat.Cloud -> palette.highlight
            PixelPetHabitat.Moon -> palette.highlight
            PixelPetHabitat.Lagoon -> palette.primary
        }
        drawRect(
            particleColor.copy(alpha = if (state.habitat == PixelPetHabitat.Moon) 0.64f else 0.34f),
            Offset(x, y),
            Size(unit * if (index % 3 == 0) 0.36f else 0.22f, unit * 0.22f),
        )
    }
    if (state.isHungry) {
        drawRect(palette.secondary.copy(alpha = 0.84f), Offset(size.width * 0.06f, size.height * 0.22f), Size(unit * 1.4f, unit * 0.6f))
        drawRect(palette.highlight.copy(alpha = 0.84f), Offset(size.width * 0.06f + unit * 0.4f, size.height * 0.16f), Size(unit * 0.6f, unit * 0.6f))
    } else if (!state.hatched) {
        drawRect(palette.highlight.copy(alpha = 0.82f), Offset(size.width * 0.48f, size.height * 0.22f), Size(unit * 0.7f, unit * 0.7f))
        drawRect(palette.highlight.copy(alpha = 0.48f), Offset(size.width * 0.48f - unit * 0.35f, size.height * 0.22f + unit * 0.3f), Size(unit * 1.4f, unit * 0.2f))
    } else if (state.canCheckIn) {
        drawRect(palette.secondary.copy(alpha = 0.84f), Offset(size.width * 0.86f, size.height * 0.16f), Size(unit * 0.42f, unit * 1.9f))
        drawRect(palette.highlight.copy(alpha = 0.76f), Offset(size.width * 0.86f, size.height * 0.16f), Size(unit * 1.55f, unit * 0.7f))
    }
}

/** A grounded focal region makes the LKM card read as a deliberate pet stage. */
private fun DrawScope.drawPixelPetStageFocus(
    state: PixelPetState,
    palette: PixelPalette,
    phase: Float,
    stageMode: PixelPetStageMode,
    unit: Float,
    ground: Float,
) {
    // Keep the stage focus centered on the same hit box used by the draggable
    // pet. Clamping the focus independently made a pet near the top look like
    // it was floating above a detached shadow.
    val hitSize = pixelPetLkmInteractiveHitSize(
        compact = false,
        stageMode = stageMode,
    ).value * density
    val x = pixelPetStageFocusPosition(state.lkmPositionX, size.width, hitSize)
    val y = pixelPetStageFocusPosition(state.lkmPositionY, size.height, hitSize)
    val breathing = (sin(phase * 2f * PI).toFloat() + 1f) / 2f
    val radius = if (stageMode == PixelPetStageMode.Immersive) unit * 6.8f else unit * 4.2f
    drawOval(
        palette.shadow.copy(alpha = if (stageMode == PixelPetStageMode.Immersive) 0.24f else 0.16f),
        Offset(x - radius, (y + unit * 3.8f).coerceAtMost(ground + unit * 1.5f)),
        Size(radius * 2f, unit * (1.1f + breathing * 0.25f)),
    )
    drawRect(
        palette.highlight.copy(alpha = 0.05f + breathing * 0.05f),
        Offset(x - radius * 0.66f, y - unit * 4.0f),
        Size(radius * 1.32f, unit * 5.6f),
    )
    if (stageMode == PixelPetStageMode.Immersive) {
        drawLine(
            palette.highlight.copy(alpha = 0.18f + breathing * 0.10f),
            Offset(x - radius * 0.78f, y + unit * 5.0f),
            Offset(x + radius * 0.78f, y + unit * 5.0f),
            unit * 0.18f,
        )
    }
}

internal fun pixelPetStageFocusPosition(
    normalizedPosition: Float,
    viewportSize: Float,
    hitSize: Float,
): Float {
    val safeViewport = viewportSize.coerceAtLeast(0f)
    val safeHitSize = hitSize.coerceIn(0f, safeViewport)
    return normalizedPosition.coerceIn(0f, 1f) * (safeViewport - safeHitSize) + safeHitSize / 2f
}

private fun DrawScope.drawPixelPetEnvironmentalReaction(
    state: PixelPetState,
    palette: PixelPalette,
    unit: Float,
    ground: Float,
    phase: Float,
) {
    val weather = state.currentWeather()
    val hasLamp = state.furniture.any { it.kind == PixelPetFurnitureKind.Lamp && it.durability > 0 }
    val hasShelter = hasLamp || state.furniture.any {
        it.kind == PixelPetFurnitureKind.Bed && it.durability > 0
    }
    when {
        weather == PixelPetWeather.Drizzle || weather == PixelPetWeather.Mist -> {
            if (hasShelter) {
                // A compact eave/dry patch communicates that the pet has found shelter.
                drawRect(
                    palette.shadow.copy(alpha = 0.30f),
                    Offset(size.width * 0.32f, ground - unit * 5.0f),
                    Size(size.width * 0.36f, unit * 0.42f),
                )
                drawRect(
                    palette.highlight.copy(alpha = 0.12f),
                    Offset(size.width * 0.35f, ground - unit * 0.55f),
                    Size(size.width * 0.30f, unit * 0.30f),
                )
            } else {
                val ripple = 0.16f + (sin(phase * 2f * PI).toFloat() + 1f) * 0.05f
                drawLine(
                    palette.highlight.copy(alpha = ripple),
                    Offset(size.width * 0.38f, ground + unit * 0.55f),
                    Offset(size.width * 0.62f, ground + unit * 0.55f),
                    unit * 0.16f,
                )
            }
        }
        weather == PixelPetWeather.Starlit || weather == PixelPetWeather.Meteor -> {
            val reflection = 0.12f + (sin(phase * 2f * PI).toFloat() + 1f) * 0.035f
            drawRect(
                palette.highlight.copy(alpha = reflection),
                Offset(size.width * 0.36f, ground + unit * 0.40f),
                Size(size.width * 0.28f, unit * 0.18f),
            )
        }
        hasLamp -> {
            drawRect(
                palette.highlight.copy(alpha = 0.06f),
                Offset(size.width * 0.34f, ground - unit * 4.6f),
                Size(size.width * 0.32f, unit * 4.4f),
            )
        }
    }
}

private fun DrawScope.drawPixelPetTimeLayer(
    palette: PixelPalette,
    unit: Float,
    night: Boolean,
) {
    if (night) {
        drawRect(palette.shadow.copy(alpha = 0.18f), Offset.Zero, Size(size.width, size.height * 0.68f))
        drawRect(palette.highlight.copy(alpha = 0.22f), Offset(size.width * 0.82f, unit * 1.1f), Size(unit * 0.62f, unit * 0.62f))
    } else {
        drawRect(palette.highlight.copy(alpha = 0.05f), Offset.Zero, Size(size.width, size.height * 0.34f))
    }
}

private fun DrawScope.drawPixelPetWeatherLayer(
    weather: PixelPetWeather,
    palette: PixelPalette,
    unit: Float,
    phase: Float,
    lowPowerMode: Boolean,
) {
    when (weather) {
        PixelPetWeather.Clear -> {
            val glow = 0.18f + ((sin(phase * 2f * PI).toFloat() + 1f) / 2f) * 0.08f
            drawRect(palette.highlight.copy(alpha = glow), Offset(size.width * 0.76f, unit * 1.2f), Size(unit * 1.65f, unit * 0.62f))
            drawRect(palette.highlight.copy(alpha = glow * 0.7f), Offset(size.width * 0.84f, unit * 0.74f), Size(unit * 0.78f, unit * 0.46f))
        }
        PixelPetWeather.Breezy -> {
            repeat(if (lowPowerMode) 2 else 5) { index ->
                val y = size.height * (0.17f + index * 0.09f)
                val x = size.width * ((phase + index * 0.19f) % 1f)
                drawLine(
                    palette.highlight.copy(alpha = 0.20f + index * 0.025f),
                    Offset(x, y),
                    Offset((x + unit * (1.8f + index * 0.35f)).coerceAtMost(size.width), y - unit * 0.18f),
                    unit * 0.16f,
                )
            }
        }
        PixelPetWeather.Drizzle -> {
            repeat(if (lowPowerMode) 7 else 15) { index ->
                val x = size.width * ((index * 0.087f + phase * 0.45f) % 1f)
                val y = size.height * ((index * 0.143f + phase * 1.3f) % 0.78f)
                drawLine(
                    palette.highlight.copy(alpha = 0.24f),
                    Offset(x, y),
                    Offset(x - unit * 0.42f, y + unit * 1.25f),
                    unit * 0.15f,
                )
            }
        }
        PixelPetWeather.Starlit -> {
            repeat(if (lowPowerMode) 4 else 9) { index ->
                val sparkle = 0.28f + ((sin((phase + index * 0.16f) * 2f * PI).toFloat() + 1f) / 2f) * 0.38f
                drawRect(
                    palette.highlight.copy(alpha = sparkle),
                    Offset(size.width * (0.08f + (index * 0.113f) % 0.82f), unit * (1.2f + index % 4 * 0.9f)),
                    Size(unit * 0.24f, unit * 0.24f),
                )
            }
        }
        PixelPetWeather.Meteor -> {
            drawLine(
                palette.highlight.copy(alpha = 0.72f),
                Offset(size.width * 0.18f + phase * size.width * 0.32f, unit * 1.4f),
                Offset(size.width * 0.34f + phase * size.width * 0.32f, unit * 2.4f),
                unit * 0.24f,
            )
            drawLine(
                palette.secondary.copy(alpha = 0.34f),
                Offset(size.width * 0.12f + phase * size.width * 0.32f, unit * 1.0f),
                Offset(size.width * 0.34f + phase * size.width * 0.32f, unit * 2.4f),
                unit * 0.12f,
            )
        }
        PixelPetWeather.Mist -> {
            val drift = sin(phase * 2f * PI).toFloat() * unit * 1.5f
            repeat(if (lowPowerMode) 2 else 4) { index ->
                val y = size.height * (0.32f + index * 0.10f)
                drawRect(
                    palette.highlight.copy(alpha = 0.08f + index * 0.02f),
                    Offset(-unit * 2f + drift * (index % 2 * 2 - 1), y),
                    Size(size.width * 0.72f, unit * 0.62f),
                )
            }
        }
    }
}

private fun DrawScope.drawPixelPetHabitatForeground(
    habitat: PixelPetHabitat,
    palette: PixelPalette,
    unit: Float,
    ground: Float,
    phase: Float,
) {
    when (habitat) {
        PixelPetHabitat.Garden -> {
            repeat(6) { index ->
                val x = size.width * (0.08f + index * 0.17f)
                val height = unit * (0.55f + (index % 3) * 0.22f)
                drawLine(
                    palette.outline.copy(alpha = 0.42f),
                    Offset(x, ground + unit * 0.25f),
                    Offset(x + unit * 0.16f, ground - height),
                    unit * 0.18f,
                )
                if (index % 2 == 0) {
                    drawRect(
                        palette.highlight.copy(alpha = 0.68f),
                        Offset(x + unit * 0.10f, ground - height - unit * 0.22f),
                        Size(unit * 0.46f, unit * 0.34f),
                    )
                }
            }
            repeat(3) { index ->
                val x = size.width * (0.22f + index * 0.27f)
                drawRect(palette.secondary.copy(alpha = 0.38f), Offset(x, ground + unit * 0.72f), Size(unit * 2.2f, unit * 0.22f))
            }
        }

        PixelPetHabitat.Cloud -> {
            repeat(4) { index ->
                val drift = sin((phase + index * 0.24f) * 2f * PI).toFloat() * unit * 0.9f
                val x = size.width * (0.08f + index * 0.24f) + drift
                val y = size.height * (0.16f + (index % 2) * 0.12f)
                drawRect(palette.highlight.copy(alpha = 0.26f), Offset(x, y), Size(unit * 1.1f, unit * 0.34f))
                drawRect(palette.highlight.copy(alpha = 0.18f), Offset(x + unit * 0.35f, y - unit * 0.30f), Size(unit * 0.62f, unit * 0.30f))
            }
            drawLine(
                palette.highlight.copy(alpha = 0.24f),
                Offset(size.width * 0.16f, ground + unit * 0.24f),
                Offset(size.width * 0.76f, ground + unit * 0.24f),
                unit * 0.18f,
            )
        }

        PixelPetHabitat.Moon -> {
            repeat(7) { index ->
                val sparkle = 0.28f + ((sin((phase + index * 0.17f) * 2f * PI).toFloat() + 1f) / 2f) * 0.34f
                val x = size.width * (0.08f + (index * 0.137f) % 0.84f)
                val y = size.height * (0.10f + (index % 4) * 0.09f)
                drawRect(palette.highlight.copy(alpha = sparkle), Offset(x, y), Size(unit * 0.24f, unit * 0.24f))
            }
            drawRect(palette.outline.copy(alpha = 0.28f), Offset(size.width * 0.18f, ground + unit * 0.5f), Size(size.width * 0.64f, unit * 0.22f))
            drawRect(palette.secondary.copy(alpha = 0.22f), Offset(size.width * 0.36f, ground + unit * 0.82f), Size(size.width * 0.28f, unit * 0.18f))
        }

        PixelPetHabitat.Lagoon -> {
            repeat(5) { index ->
                val y = ground + unit * (0.55f + index * 0.55f)
                val shift = sin((phase + index * 0.12f) * 2f * PI).toFloat() * unit * 0.8f
                drawLine(
                    palette.highlight.copy(alpha = 0.18f - index * 0.018f),
                    Offset(size.width * 0.10f + shift, y),
                    Offset(size.width * 0.36f + shift, y),
                    unit * 0.16f,
                )
            }
            repeat(3) { index ->
                val x = size.width * (0.18f + index * 0.29f)
                val y = size.height * (0.32f + (index % 2) * 0.16f)
                drawCircle(palette.highlight.copy(alpha = 0.34f), radius = unit * 0.20f, center = Offset(x, y))
                drawCircle(palette.highlight.copy(alpha = 0.18f), radius = unit * 0.42f, center = Offset(x, y))
            }
            drawLine(
                palette.outline.copy(alpha = 0.36f),
                Offset(size.width * 0.14f, ground + unit * 0.2f),
                Offset(size.width * 0.86f, ground + unit * 0.2f),
                unit * 0.18f,
            )
        }
    }
}

@Composable
fun PixelPetProgressBar(
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val safeProgress = (value.coerceIn(0, 100) / 100f)
    Box(
        modifier = modifier
            .height(6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safeProgress)
                .height(6.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
    }
}

fun currentPixelPetDay(nowMillis: Long = System.currentTimeMillis()): Long = pixelPetDayKey(nowMillis)

internal fun pixelPetCountdownLabel(remainingMillis: Long): String {
    val totalSeconds = (remainingMillis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun previousPixelPetDay(nowMillis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }.let(::pixelPetDayKey)
}

private fun pixelPetDayKey(nowMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = nowMillis
}.let(::pixelPetDayKey)

private fun pixelPetDayKey(calendar: Calendar): Long {
    return calendar.get(Calendar.YEAR).toLong() * 1_000L + calendar.get(Calendar.DAY_OF_YEAR)
}

private fun isPixelPetPreferenceKey(key: String?): Boolean = when (key) {
    null,
    PIXEL_PET_ENABLED_KEY,
    KEY_HATCHED,
    KEY_NAME,
    KEY_HUNGER,
    KEY_AFFECTION,
    KEY_GROWTH,
    KEY_COINS,
    KEY_LAST_CHECK_IN_DAY,
    KEY_LAST_OBSERVED_DAY,
    KEY_CHECK_IN_STREAK,
    KEY_LAST_INTERACTION_AT,
    KEY_LAST_ACTION,
    KEY_LAST_ACTION_AT,
    KEY_QUEUED_ACTION,
    KEY_LAST_HABITAT_INTERACTION_AT,
    KEY_TOTAL_HABITAT_INTERACTIONS,
    KEY_LAST_WELLBEING_AT,
    KEY_HABITAT,
    KEY_WEATHER_OVERRIDE,
    KEY_ACCESSORY,
    KEY_OWNED_ACCESSORIES,
    KEY_EQUIPPED_ACCESSORIES,
    KEY_UNLOCKED_HABITATS,
    KEY_TOTAL_INTERACTIONS,
    KEY_TOTAL_FEEDS,
    KEY_HIGHEST_STREAK,
    KEY_DAILY_TASK_DAY,
    KEY_DAILY_INTERACTIONS,
    KEY_DAILY_FEEDS,
    KEY_DAILY_HABITAT_INTERACTIONS,
    KEY_DAILY_HABITAT_CHANGED,
    KEY_CLAIMED_DAILY_TASKS,
    KEY_CLAIMED_ACHIEVEMENTS,
    KEY_SAVED_LOOKS,
    KEY_REMINDER_ENABLED,
    KEY_SPECIES,
    KEY_INCUBATION_STARTED_AT,
    KEY_TEACHING_ENERGY,
    KEY_TEACHINGS,
    KEY_PERSONALITY,
    KEY_MEMORIES,
    KEY_CHAT_MESSAGES,
    KEY_LKM_POSITION_X,
    KEY_LKM_POSITION_Y,
    KEY_LKM_LANDSCAPE_POSITION_X,
    KEY_LKM_LANDSCAPE_POSITION_Y,
    KEY_LKM_POSITION_LOCKED,
    KEY_ENERGY,
    KEY_CLEANLINESS,
    KEY_MOOD_VALUE,
    KEY_SLEEP_QUALITY,
    KEY_EXPLORATION,
    KEY_LAST_NEEDS_AT,
    KEY_FURNITURE,
    KEY_OWNED_FURNITURE,
    KEY_FOOD_INVENTORY,
    KEY_ACTIVE_FURNITURE_ID,
    KEY_LAST_FURNITURE_INTERACTION_AT,
    -> true
    else -> key.startsWith("pixel_pet")
}

private fun Context.pixelPetPreferences(): SharedPreferences =
    getSharedPreferences(PIXEL_PET_PREFS, Context.MODE_PRIVATE)
