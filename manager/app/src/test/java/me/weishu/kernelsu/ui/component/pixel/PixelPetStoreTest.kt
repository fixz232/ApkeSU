package me.weishu.kernelsu.ui.component.pixel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PixelPetStoreTest {
    @Test
    fun checkInUsesLocalDayAndContinuesTheStreak() {
        val previousTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            val firstMoment = localMillis(2026, Calendar.AUGUST, 18, 23, 58)
            val nextMoment = localMillis(2026, Calendar.AUGUST, 19, 0, 2)
            assertNotEquals(currentPixelPetDay(firstMoment), currentPixelPetDay(nextMoment))

            val first = PixelPetReducer.checkIn(
                PixelPetState(enabled = true, hatched = true),
                firstMoment,
            )
            val second = PixelPetReducer.checkIn(first, nextMoment)

            assertEquals(1, first.checkInStreak)
            assertEquals(2, second.checkInStreak)
            assertEquals(first.coins + 19, second.coins)
            assertEquals(second, PixelPetReducer.checkIn(second, nextMoment))
        } finally {
            TimeZone.setDefault(previousTimeZone)
        }
    }

    @Test
    fun rollingTheLocalClockBackCannotClaimTheSameDayAgain() {
        val previousTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
            val today = localMillis(2026, Calendar.AUGUST, 19, 11, 0)
            val yesterday = localMillis(2026, Calendar.AUGUST, 18, 11, 0)
            val checkedIn = PixelPetReducer.checkIn(PixelPetState(enabled = true, hatched = true), today)
            val rolledBack = PixelPetReducer.checkIn(checkedIn, yesterday)
            val restoredClock = PixelPetReducer.checkIn(rolledBack, today)

            assertEquals(checkedIn.coins, rolledBack.coins)
            assertEquals(checkedIn.lastCheckInDay, rolledBack.lastCheckInDay)
            assertEquals(checkedIn, restoredClock)
        } finally {
            TimeZone.setDefault(previousTimeZone)
        }
    }

    @Test
    fun interactionThrottleRejectsRapidRepeatedTaps() {
        val state = PixelPetState(enabled = true, hatched = true, lastInteractionAt = 1_000L)

        val throttled = PixelPetReducer.interact(state, now = 1_600L)
        val accepted = PixelPetReducer.interact(state, now = 1_750L)

        assertEquals(state.coins, throttled.coins)
        assertEquals(state.growth, throttled.growth)
        assertEquals(state.lastInteractionAt, throttled.lastInteractionAt)
        assertEquals(state.coins + 1, accepted.coins)
        assertEquals(state.growth + 1, accepted.growth)
        assertEquals(1_750L, accepted.lastInteractionAt)
    }

    @Test
    fun feedAndShopRespectBalanceBoundaries() {
        val insufficientForFood = PixelPetState(enabled = true, hatched = true, coins = 4, hunger = 42)
        val insufficientForAccessory = PixelPetState(enabled = true, hatched = true, coins = 23)

        assertEquals(insufficientForFood.coins, PixelPetReducer.feed(insufficientForFood).coins)
        assertEquals(
            insufficientForAccessory.coins,
            PixelPetReducer.buyOrEquip(insufficientForAccessory, PixelPetAccessory.LeafCrown).coins,
        )

        val purchased = PixelPetReducer.buyOrEquip(
            insufficientForAccessory.copy(coins = 24),
            PixelPetAccessory.LeafCrown,
        )
        assertEquals(0, purchased.coins)
        assertEquals(PixelPetAccessory.LeafCrown, purchased.accessory)
        assertTrue(PixelPetAccessory.LeafCrown in purchased.ownedAccessories)
    }

    @Test
    fun snapshotRoundTripRestoresPetProgressAfterProcessRestart() {
        val original = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Cat,
            name = "Piko",
            hunger = 78,
            affection = 246,
            growth = 189,
            coins = 96,
            lastCheckInDay = 2_026_231L,
            checkInStreak = 6,
            lastInteractionAt = 7_500L,
            habitat = PixelPetHabitat.Lagoon,
            accessory = PixelPetAccessory.ShellBag,
            ownedAccessories = setOf(PixelPetAccessory.LeafCrown, PixelPetAccessory.ShellBag),
            equippedAccessories = setOf(PixelPetAccessory.ShellBag),
            unlockedHabitats = PixelPetHabitat.entries.toSet(),
        )

        val restored = PixelPetSnapshot.from(original).toState()

        assertEquals(original, restored)
    }

    @Test
    fun themeEnabledChangeKeepsPetProgressAndInventory() {
        val progress = PixelPetState(
            enabled = false,
            hatched = true,
            hunger = 63,
            affection = 220,
            growth = 145,
            coins = 74,
            checkInStreak = 4,
            habitat = PixelPetHabitat.Moon,
            accessory = PixelPetAccessory.StarPin,
            ownedAccessories = setOf(PixelPetAccessory.StarPin),
        )

        val themed = PixelPetReducer.setEnabled(progress, enabled = true)

        assertEquals(progress.copy(enabled = true), themed)
    }

    @Test
    fun unequipKeepsPurchasedAccessoriesAvailable() {
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            accessory = PixelPetAccessory.StarPin,
            ownedAccessories = setOf(PixelPetAccessory.StarPin),
        )

        val unequipped = PixelPetReducer.unequip(state)

        assertEquals(null, unequipped.accessory)
        assertTrue(PixelPetAccessory.StarPin in unequipped.ownedAccessories)
    }

    @Test
    fun wellbeingRefreshAppliesTimeBasedHungerDecay() {
        val start = localMillis(2026, Calendar.AUGUST, 18, 8, 0)
        val refreshed = PixelPetReducer.refresh(
            PixelPetState(
                enabled = true,
                hatched = true,
                hunger = 50,
                affection = 10,
                lastWellbeingAt = start,
            ),
            start + 8 * 60 * 60 * 1000L,
        )

        assertEquals(44, refreshed.hunger)
        assertEquals(9, refreshed.affection)
    }

    @Test
    fun dailyTaskClaimResetsOnTheNextLocalDay() {
        val today = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            dailyTaskDay = currentPixelPetDay(today),
            dailyInteractions = 2,
        )
        val completed = PixelPetReducer.interact(state, today)
        val claimed = PixelPetReducer.claimDailyTask(completed, PixelPetDailyTask.Interact, today)
        val nextDay = PixelPetReducer.refresh(claimed, today + 24 * 60 * 60 * 1000L)

        assertTrue(claimed.isTaskComplete(PixelPetDailyTask.Interact))
        assertTrue(PixelPetDailyTask.Interact in claimed.claimedDailyTasks)
        assertEquals(0, nextDay.dailyInteractions)
        assertTrue(nextDay.claimedDailyTasks.isEmpty())
    }

    @Test
    fun interactionRewardStopsAfterDailyCapButCareStillCounts() {
        val now = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            coins = 60,
            growth = 80,
            dailyTaskDay = currentPixelPetDay(now),
            dailyInteractions = 30,
        )
        val updated = PixelPetReducer.interact(state, now)

        assertEquals(60, updated.coins)
        assertEquals(80, updated.growth)
        assertEquals(31, updated.dailyInteractions)
        assertEquals(1, updated.totalInteractions)
    }

    @Test
    fun habitatExplorationAwardsCareAndRejectsRapidRepeats() {
        val now = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val state = PixelPetState(enabled = true, hatched = true, coins = 10)

        val first = PixelPetReducer.exploreHabitat(state, now)
        val throttled = PixelPetReducer.exploreHabitat(first, now + 1_000L)
        val second = PixelPetReducer.exploreHabitat(first, now + MIN_HABITAT_INTERACTION_INTERVAL_MILLIS)

        assertEquals(state.coins + 2, first.coins)
        assertEquals(state.affection + 2, first.affection)
        assertEquals(1, first.totalHabitatInteractions)
        assertEquals(first, throttled)
        assertEquals(2, second.totalHabitatInteractions)
    }

    @Test
    fun habitatRewardStopsAtDailyCapButExplorationCountKeepsGrowing() {
        val now = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            coins = 90,
            growth = 20,
            dailyTaskDay = currentPixelPetDay(now),
            dailyHabitatInteractions = 12,
            lastHabitatInteractionAt = now - MIN_HABITAT_INTERACTION_INTERVAL_MILLIS,
        )

        val updated = PixelPetReducer.exploreHabitat(state, now)

        assertEquals(90, updated.coins)
        assertEquals(20, updated.growth)
        assertEquals(13, updated.dailyHabitatInteractions)
        assertEquals(1, updated.totalHabitatInteractions)
    }

    @Test
    fun habitatUnlocksAndAccessorySlotsCanBeCombined() {
        val now = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val grown = PixelPetReducer.refresh(
            PixelPetState(enabled = true, hatched = true, growth = 125),
            now,
        )
        val cloud = PixelPetReducer.setHabitat(grown, PixelPetHabitat.Cloud, now)
        val inventory = cloud.copy(
            ownedAccessories = setOf(
                PixelPetAccessory.LeafCrown,
                PixelPetAccessory.ShellBag,
                PixelPetAccessory.WateringCan,
            ),
        )
        val equipped = PixelPetReducer.buyOrEquip(
            PixelPetReducer.buyOrEquip(
                PixelPetReducer.buyOrEquip(inventory, PixelPetAccessory.LeafCrown, now),
                PixelPetAccessory.ShellBag,
                now,
            ),
            PixelPetAccessory.WateringCan,
            now,
        )

        assertTrue(PixelPetHabitat.Cloud in grown.unlockedHabitats)
        assertEquals(PixelPetHabitat.Cloud, cloud.habitat)
        assertEquals(3, equipped.equippedAccessories.size)
        assertTrue(PixelPetAccessory.WateringCan in equipped.equippedAccessories)
    }

    @Test
    fun achievementsAndBackupJsonRoundTrip() {
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Cat,
            totalInteractions = 25,
            coins = 40,
            unlockedHabitats = PixelPetHabitat.entries.toSet(),
            ownedAccessories = PixelPetAccessory.entries.toSet(),
            equippedAccessories = setOf(PixelPetAccessory.LeafCrown),
            savedLooks = listOf(PixelPetLook(PixelPetHabitat.Moon, setOf(PixelPetAccessory.LeafCrown))),
        )
        val claimed = PixelPetReducer.claimAchievement(state, PixelPetAchievement.Friendly)
        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(claimed).toJson()).toState()

        assertTrue(PixelPetAchievement.Friendly in claimed.claimedAchievements)
        assertEquals(claimed, restored)
    }

    @Test
    fun choosingOneSpeciesStartsFiveMinuteIncubationAndLocksTheChoice() {
        val start = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val started = PixelPetReducer.chooseSpecies(
            PixelPetState(enabled = true),
            PixelPetSpecies.Penguin,
            start,
        )
        val beforeReady = PixelPetReducer.refresh(started, start + PIXEL_PET_INCUBATION_MILLIS - 1L)
        val hatched = PixelPetReducer.refresh(started, start + PIXEL_PET_INCUBATION_MILLIS)
        val locked = PixelPetReducer.chooseSpecies(hatched, PixelPetSpecies.Dog, start + PIXEL_PET_INCUBATION_MILLIS)

        assertEquals(PixelPetSpecies.Penguin, started.species)
        assertTrue(started.isIncubating)
        assertTrue(!beforeReady.hatched)
        assertTrue(hatched.hatched)
        assertEquals(PixelPetSpecies.Penguin, hatched.species)
        assertEquals(hatched, locked)
    }

    @Test
    fun rabbitIsSelectableAndPersistsThroughHatchingAndBackup() {
        val start = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val incubating = PixelPetReducer.chooseSpecies(
            PixelPetState(enabled = true),
            PixelPetSpecies.Rabbit,
            start,
        )
        val hatched = PixelPetReducer.refresh(incubating, start + PIXEL_PET_INCUBATION_MILLIS)
        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(hatched).toJson()).toState()

        assertEquals(PixelPetSpecies.Rabbit, incubating.species)
        assertEquals(PixelPetSpecies.Rabbit, hatched.species)
        assertEquals(hatched, restored)
    }

    @Test
    fun hamsterIsSelectableAndPersistsThroughHatchingAndBackup() {
        val start = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val incubating = PixelPetReducer.chooseSpecies(
            PixelPetState(enabled = true),
            PixelPetSpecies.Hamster,
            start,
        )
        val hatched = PixelPetReducer.refresh(incubating, start + PIXEL_PET_INCUBATION_MILLIS)
        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(hatched).toJson()).toState()

        assertEquals(PixelPetSpecies.Hamster, incubating.species)
        assertEquals(PixelPetSpecies.Hamster, hatched.species)
        assertEquals(hatched, restored)
    }

    @Test
    fun petGrowthStagesFollowTheExistingGrowthValueWithoutMigration() {
        assertEquals(PixelPetGrowthStage.Egg, PixelPetState().growthStage)
        assertEquals(PixelPetGrowthStage.Baby, PixelPetState(hatched = true, growth = 24).growthStage)
        assertEquals(PixelPetGrowthStage.Young, PixelPetState(hatched = true, growth = 25).growthStage)
        assertEquals(PixelPetGrowthStage.Young, PixelPetState(hatched = true, growth = 99).growthStage)
        assertEquals(PixelPetGrowthStage.Adult, PixelPetState(hatched = true, growth = 100).growthStage)
    }

    @Test
    fun feedingCreatesTeachingEnergyAndTeachingPersistsARealLesson() {
        val fed = PixelPetReducer.feed(
            PixelPetState(
                enabled = true,
                hatched = true,
                species = PixelPetSpecies.Dog,
                coins = 20,
                hunger = 50,
            ),
        )
        val taught = PixelPetReducer.teach(fed, "My favorite snack is apple slices.")

        assertEquals(1, fed.teachingEnergy)
        assertEquals(0, taught.teachingEnergy)
        assertEquals(listOf("My favorite snack is apple slices."), taught.teachings)
        assertTrue(taught.growth > fed.growth)
    }

    @Test
    fun petChatAndCardPositionRoundTripThroughBackup() {
        val original = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Bird,
            teachings = listOf("Say hello when you are happy."),
            teachingEnergy = 2,
            chatMessages = listOf(
                PixelPetChatMessage(PixelPetChatRole.User, "Hello"),
                PixelPetChatMessage(PixelPetChatRole.Pet, "Hello, friend!"),
            ),
            lkmPositionX = 0.18f,
            lkmPositionY = 0.81f,
        )

        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(original).toJson()).toState()

        assertEquals(original, restored)
    }

    @Test
    fun actionStateAndHabitatProgressRoundTripThroughBackup() {
        val original = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Penguin,
            lastAction = PixelPetAction.Exploring,
            lastActionAt = 9_000L,
            lastHabitatInteractionAt = 8_800L,
            totalHabitatInteractions = 17,
            dailyHabitatInteractions = 4,
        )

        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(original).toJson()).toState()

        assertEquals(original, restored)
    }

    @Test
    fun oldBackupWithoutActionFieldsMigratesToSafeDefaults() {
        val restored = PixelPetSnapshot.fromJson(
            org.json.JSONObject()
                .put("enabled", true)
                .put("hatched", true)
                .put("name", "Legacy")
                .put("hunger", 60)
                .put("affection", 4)
                .put("growth", 10)
                .put("coins", 20)
                .put("lastCheckInDay", Long.MIN_VALUE)
                .put("checkInStreak", 0)
                .put("lastInteractionAt", 0L)
                .put("lastWellbeingAt", 0L)
                .put("habitat", PixelPetHabitat.Garden.name)
                .put("totalInteractions", 0)
                .put("totalFeeds", 0)
                .put("highestCheckInStreak", 0)
                .put("dailyTaskDay", Long.MIN_VALUE)
                .put("dailyInteractions", 0)
                .put("dailyFeeds", 0)
                .put("dailyHabitatChanged", false)
                .put("claimedDailyTasks", org.json.JSONArray())
                .put("claimedAchievements", org.json.JSONArray())
                .put("savedLooks", org.json.JSONArray())
                .put("reminderEnabled", false),
        ).toState()

        assertEquals(PixelPetAction.Idle, restored.lastAction)
        assertEquals(0, restored.totalHabitatInteractions)
        assertEquals(PixelPetSpecies.Cat, restored.species)
    }

    @Test
    fun longOfflineRefreshIsCappedAndDoesNotReplayForever() {
        val now = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            hunger = 100,
            affection = 100,
            lastWellbeingAt = now - 100L * 4L * 60L * 60L * 1000L,
        )

        val refreshed = PixelPetReducer.refresh(state, now)
        val next = PixelPetReducer.refresh(refreshed, now + 60L * 60L * 1000L)

        assertEquals(now, refreshed.lastWellbeingAt)
        assertEquals(0, refreshed.hunger)
        assertEquals(79, refreshed.affection)
        assertEquals(refreshed.lastWellbeingAt, next.lastWellbeingAt)
    }

    @Test
    fun legacyHatchedSaveGetsAStableDefaultSpeciesAndCardPositionIsClamped() {
        val legacy = sanitizePixelPetState(PixelPetState(enabled = true, hatched = true))
        val positioned = PixelPetReducer.setLkmPosition(legacy, -2f, 4f)
        val snapped = PixelPetReducer.setLkmPosition(legacy, 0.70f, 0.48f)

        assertEquals(PixelPetSpecies.Cat, legacy.species)
        assertEquals(0f, positioned.lkmPositionX)
        assertEquals(1f, positioned.lkmPositionY)
        assertEquals(DEFAULT_PIXEL_PET_POSITION_X, snapped.lkmPositionX)
        assertEquals(0.50f, snapped.lkmPositionY)
        assertEquals(DEFAULT_PIXEL_PET_POSITION_X, PixelPetReducer.resetLkmPosition(positioned).lkmPositionX)
    }

    @Test
    fun incubationProgressAndCardCountdownRemainDeterministic() {
        val start = localMillis(2026, Calendar.AUGUST, 18, 12, 0)
        val incubating = PixelPetReducer.chooseSpecies(
            PixelPetState(enabled = true),
            PixelPetSpecies.Cat,
            start,
        )

        assertEquals(0f, incubating.incubationProgress(start))
        assertEquals(0.5f, incubating.incubationProgress(start + PIXEL_PET_INCUBATION_MILLIS / 2L))
        assertEquals("2:30", pixelPetCountdownLabel(150_000L))
        assertEquals("0:00", pixelPetCountdownLabel(0L))
    }

    @Test
    fun sanitizeKeepsCardStateSafeForSmallLayoutsAndLongInput() {
        val sanitized = sanitizePixelPetState(
            PixelPetState(
                enabled = true,
                hatched = true,
                name = " 123456789012345678901234567890 ",
                hunger = 200,
                affection = -20,
                growth = -1,
                coins = -4,
                lkmPositionX = -2f,
                lkmPositionY = 3f,
                teachings = listOf("  A   lesson  ", "A lesson"),
            ),
        )

        assertEquals(20, sanitized.name.length)
        assertEquals(100, sanitized.hunger)
        assertEquals(0, sanitized.affection)
        assertEquals(0, sanitized.growth)
        assertEquals(0, sanitized.coins)
        assertEquals(0f, sanitized.lkmPositionX)
        assertEquals(1f, sanitized.lkmPositionY)
        assertEquals(listOf("A lesson"), sanitized.teachings)
    }

    @Test
    fun growthStageScalesKeepBabyVisibleInsideTheFullCard() {
        assertEquals(96f, pixelPetLkmInteractiveAvatarSize(compact = false).value)
        assertEquals(112f, pixelPetLkmInteractiveHitSize(compact = false).value)
    }

    @Test
    fun actionsHaveExclusiveDurationsAndLowHungerBecomesFrightened() {
        val now = 10_000L
        val happy = PixelPetState(
            enabled = true,
            hatched = true,
            hunger = 60,
            lastAction = PixelPetAction.Happy,
            lastActionAt = now,
        )
        val frightened = happy.copy(hunger = 8, lastActionAt = now - PixelPetAction.Happy.durationMillis - 1L)

        assertTrue(happy.hasRecentAction(now + PixelPetAction.Happy.durationMillis))
        assertTrue(!happy.hasRecentAction(now + PixelPetAction.Happy.durationMillis + 1L))
        assertEquals(PixelPetMood.Frightened, frightened.mood)
    }

    @Test
    fun dragPositionCanSkipSnapWithoutLeavingCardBounds() {
        val state = PixelPetState(enabled = true, hatched = true)
        val dragged = PixelPetReducer.setLkmPosition(state, 0.703f, 0.481f, snap = false)

        assertEquals(0.703f, dragged.lkmPositionX)
        assertEquals(0.481f, dragged.lkmPositionY)
        assertEquals(0f, PixelPetReducer.setLkmPosition(state, -1f, 2f, snap = false).lkmPositionX)
        assertEquals(1f, PixelPetReducer.setLkmPosition(state, -1f, 2f, snap = false).lkmPositionY)
    }

    @Test
    fun chatMessageIdAndErrorStatusSurviveSnapshotRoundTrip() {
        val message = PixelPetChatMessage(
            role = PixelPetChatRole.Pet,
            text = "temporary failure",
            status = PixelPetChatMessageStatus.Error,
            id = "stable-message-id",
        )
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Cat,
            chatMessages = listOf(message),
        )

        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(state).toJson()).toState()

        assertEquals("stable-message-id", restored.chatMessages.single().id)
        assertEquals(PixelPetChatMessageStatus.Error, restored.chatMessages.single().status)
    }

    @Test
    fun interruptedStreamingMessageIsRecoveredAsStopped() {
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Bird,
            chatMessages = listOf(
                PixelPetChatMessage(
                    role = PixelPetChatRole.Pet,
                    text = "Thinking...",
                    status = PixelPetChatMessageStatus.Generating,
                ),
            ),
        )

        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(state).toJson()).toState()

        assertEquals(PixelPetChatMessageStatus.Stopped, restored.chatMessages.single().status)
    }

    @Test
    fun higherPriorityActionFinishesBeforeQueuedPetting() {
        val now = 50_000L
        val eating = PixelPetState(
            enabled = true,
            hatched = true,
            hunger = 48,
            coins = 10,
            lastAction = PixelPetAction.Eating,
            lastActionAt = now,
        )

        val petted = PixelPetReducer.pet(eating, now + 1_000L)
        val advanced = PixelPetReducer.refresh(petted, now + PixelPetAction.Eating.durationMillis + 1L)

        assertEquals(PixelPetAction.Eating, petted.lastAction)
        assertEquals(PixelPetAction.Petted, petted.queuedAction)
        assertEquals(PixelPetAction.Petted, advanced.lastAction)
        assertEquals(null, advanced.queuedAction)
    }

    @Test
    fun personalityMemoryAndWeatherOverrideSurviveBackupRoundTrip() {
        val now = localMillis(2026, Calendar.AUGUST, 19, 14, 0)
        val base = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Rabbit,
            hunger = 62,
            coins = 18,
        )
        val personalized = PixelPetReducer.setPersonality(base, PixelPetPersonality.Curious, now)
        val fed = PixelPetReducer.feed(personalized, now + 3_000L)
        val weather = PixelPetReducer.setWeatherOverride(fed, PixelPetWeather.Mist, now + 6_000L)

        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(weather).toJson()).toState()

        assertEquals(PixelPetPersonality.Curious, restored.personality)
        assertEquals(PixelPetWeather.Mist, restored.weatherOverride)
        assertTrue(restored.memories.any { it.kind == PixelPetMemoryKind.Care })
        assertTrue(restored.memories.any { it.kind == PixelPetMemoryKind.Milestone })
    }

    @Test
    fun appearancePersistsThroughSnapshotAndChangesModelPalette() {
        val source = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Rabbit,
            appearance = PixelPetAppearance.Dusk,
        )

        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(source).toJson()).toState()

        assertEquals(PixelPetAppearance.Dusk, restored.appearance)
        assertNotEquals(
            pixelPetModelColors(PixelPetSpecies.Rabbit, PixelPetAppearance.Natural).base,
            pixelPetModelColors(PixelPetSpecies.Rabbit, PixelPetAppearance.Dusk).base,
        )
    }

    @Test
    fun weatherOverrideCanReturnToLocalHabitatWeather() {
        val now = localMillis(2026, Calendar.AUGUST, 19, 15, 0)
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            habitat = PixelPetHabitat.Lagoon,
            weatherOverride = PixelPetWeather.Drizzle,
        )

        val automatic = PixelPetReducer.setWeatherOverride(state, null, now)

        assertEquals(null, automatic.weatherOverride)
        assertTrue(automatic.currentWeather(now) in PixelPetWeather.entries)
    }

    @Test
    fun furniturePlacementAndAutomaticInteractionPersistNeeds() {
        val furniture = PixelPetFurniture(
            kind = PixelPetFurnitureKind.Bed,
            x = 0.4f,
            y = 0.7f,
        )
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            energy = 20,
            ownedFurniture = setOf(PixelPetFurnitureKind.Bed),
            furniture = listOf(furniture),
        )

        val walking = PixelPetReducer.interactWithFurniture(state, furniture.id, 20_000L)
        val updated = PixelPetReducer.refresh(
            walking,
            20_000L + PixelPetAction.Walking.durationMillis + 1L,
        )
        val restored = PixelPetSnapshot.fromJson(PixelPetSnapshot.from(updated).toJson()).toState()

        assertEquals(PixelPetAction.Sleeping, updated.lastAction)
        assertEquals(null, updated.queuedAction)
        assertTrue(updated.energy > state.energy)
        assertEquals(1, restored.furniture.single().interactions)
        assertEquals(furniture.id, restored.activeFurnitureId)
    }

    @Test
    fun preferredFoodConsumesInventoryAndImprovesMood() {
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            species = PixelPetSpecies.Cat,
            hunger = 30,
            moodValue = 30,
            foodInventory = mapOf(PixelPetFoodKind.Berry to 1),
        )

        val updated = PixelPetReducer.useFood(state, PixelPetFoodKind.Berry, 10_000L)

        assertEquals(0, updated.foodInventory[PixelPetFoodKind.Berry] ?: 0)
        assertTrue(updated.hunger > state.hunger)
        assertTrue(updated.moodValue > state.moodValue)
    }

    @Test
    fun legacyBackupMigratesAndNewBackupIncludesSchemaAndFurniture() {
        val legacy = PixelPetSnapshot.fromJson(
            org.json.JSONObject()
                .put("enabled", true)
                .put("hatched", true)
                .put("name", "Legacy"),
        ).toState()
        val current = legacy.copy(
            ownedFurniture = setOf(PixelPetFurnitureKind.Lamp),
            furniture = listOf(PixelPetFurniture(kind = PixelPetFurnitureKind.Lamp, x = 0.5f, y = 0.7f)),
        )
        val json = PixelPetSnapshot.from(current).toJson()
        val restored = PixelPetSnapshot.fromJson(json).toState()

        assertEquals(PIXEL_PET_SAVE_SCHEMA_VERSION, json.optInt("schemaVersion"))
        assertEquals("Legacy", restored.name)
        assertEquals(1, restored.furniture.size)
    }

    @Test
    fun mergingBackupUnionsFurnitureWithoutReplacingCurrentPosition() {
        val currentFurniture = PixelPetFurniture(kind = PixelPetFurnitureKind.Plant, x = 0.2f, y = 0.7f)
        val incomingFurniture = PixelPetFurniture(kind = PixelPetFurnitureKind.Aquarium, x = 0.7f, y = 0.7f)
        val current = PixelPetState(
            enabled = true,
            hatched = true,
            growth = 25,
            lkmPositionX = 0.2f,
            furniture = listOf(currentFurniture),
            ownedFurniture = setOf(PixelPetFurnitureKind.Plant),
        )
        val incoming = PixelPetState(
            enabled = true,
            hatched = true,
            growth = 60,
            lkmPositionX = 0.8f,
            furniture = listOf(incomingFurniture),
            ownedFurniture = setOf(PixelPetFurnitureKind.Aquarium),
        )

        val merged = PixelPetReducer.mergeBackup(current, incoming)

        assertEquals(60, merged.growth)
        assertEquals(0.2f, merged.lkmPositionX)
        assertEquals(2, merged.furniture.size)
        assertTrue(PixelPetFurnitureKind.Aquarium in merged.ownedFurniture)
    }

    @Test
    fun furnitureLimitRejectsNewPlacementWithoutDroppingPlacedFurniture() {
        val placed = List(MAX_PIXEL_PET_FURNITURE) { index ->
            PixelPetFurniture(
                id = "placed-$index",
                kind = PixelPetFurnitureKind.Plant,
                x = 0.08f + index * 0.04f,
                y = 0.68f,
            )
        }
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            ownedFurniture = setOf(PixelPetFurnitureKind.Plant),
            furniture = placed,
        )

        val updated = PixelPetReducer.placeFurniture(
            state,
            PixelPetFurnitureKind.Plant,
            x = 0.5f,
            y = 0.7f,
        )

        assertEquals(MAX_PIXEL_PET_FURNITURE, updated.furniture.size)
        assertEquals(placed.map(PixelPetFurniture::id), updated.furniture.map(PixelPetFurniture::id))
    }

    @Test
    fun fullHungerKeepsFoodInventoryUntouched() {
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            hunger = 100,
            foodInventory = mapOf(PixelPetFoodKind.Berry to 1),
            lastWellbeingAt = 10_000L,
            lastNeedsAt = 10_000L,
        )

        val updated = PixelPetReducer.useFood(state, PixelPetFoodKind.Berry, now = 10_000L)

        assertEquals(100, updated.hunger)
        assertEquals(1, updated.foodInventory[PixelPetFoodKind.Berry])
    }

    @Test
    fun repairingFurnitureCostsCoinsAndRestoresDurability() {
        val furniture = PixelPetFurniture(
            id = "bed-1",
            kind = PixelPetFurnitureKind.Bed,
            x = 0.5f,
            y = 0.7f,
            durability = 16,
        )
        val state = PixelPetState(
            enabled = true,
            hatched = true,
            coins = PIXEL_PET_FURNITURE_REPAIR_COST + 3,
            ownedFurniture = setOf(PixelPetFurnitureKind.Bed),
            furniture = listOf(furniture),
        )

        val repaired = PixelPetReducer.repairFurniture(state, furniture.id)

        assertEquals(100, repaired.furniture.single().durability)
        assertEquals(3, repaired.coins)
    }

    @Test
    fun newerSaveSchemaIsRejectedBeforeRestore() {
        val raw = org.json.JSONObject()
            .put("schemaVersion", PIXEL_PET_SAVE_SCHEMA_VERSION + 1)
            .put("enabled", true)
            .toString()

        val error = runCatching { PixelPetStore.previewBackup(raw) }.exceptionOrNull()

        assertTrue(error is PixelPetBackupIncompatibleException)
        assertEquals(PIXEL_PET_SAVE_SCHEMA_VERSION + 1, (error as PixelPetBackupIncompatibleException).version)
    }

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
    }
}
