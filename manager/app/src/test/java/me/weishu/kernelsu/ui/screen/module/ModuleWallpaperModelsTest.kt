package me.weishu.kernelsu.ui.screen.module

import me.weishu.kernelsu.ui.util.DEFAULT_CUSTOM_WALLPAPER_CROP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleWallpaperModelsTest {
    @Test
    fun collectionNormalizationClampsImportedValues() {
        val entries = List(MODULE_CARD_WALLPAPER_MAX_COUNT + 4) { entry("day-$it") }

        val normalized = ModuleWallpaperCollection(
            entries = entries,
            carouselEnabled = true,
            intervalMillis = 1L,
            selectedIndex = Int.MAX_VALUE,
        ).normalized()

        assertEquals(MODULE_CARD_WALLPAPER_MAX_COUNT, normalized.entries.size)
        assertEquals(MODULE_CARD_WALLPAPER_MIN_INTERVAL_MILLIS, normalized.intervalMillis)
        assertEquals(normalized.entries.lastIndex, normalized.selectedIndex)
        assertTrue(normalized.carouselEnabled)
    }

    @Test
    fun carouselIsDisabledForEmptyAndSingleImageCollections() {
        assertFalse(ModuleWallpaperCollection(carouselEnabled = true).normalized().carouselEnabled)
        assertFalse(
            ModuleWallpaperCollection(
                entries = listOf(entry("single")),
                carouselEnabled = true,
            ).normalized().carouselEnabled
        )
    }

    @Test
    fun dayAndNightCollectionsRemainIndependent() {
        val day = ModuleWallpaperCollection(
            entries = listOf(entry("day")),
            selectedIndex = 0,
        )
        val night = ModuleWallpaperCollection(
            entries = listOf(entry("night-1"), entry("night-2")),
            carouselEnabled = true,
            carouselOrder = ModuleWallpaperCarouselOrder.Random,
            intervalMillis = 12_000L,
            selectedIndex = 1,
        )

        val snapshot = ModuleCardWallpaperSnapshot(emptyList(), false)
            .withCollection(ModuleWallpaperVariant.Day, day)
            .withCollection(ModuleWallpaperVariant.Night, night)

        assertEquals(listOf("day"), snapshot.entries.map(ModuleCardWallpaperEntry::uriString))
        assertEquals(listOf("night-1", "night-2"), snapshot.nightEntries.map(ModuleCardWallpaperEntry::uriString))
        assertEquals(ModuleWallpaperCarouselOrder.Random, snapshot.nightCarouselOrder)
        assertEquals(12_000L, snapshot.nightIntervalMillis)
        assertEquals(1, snapshot.nightSelectedIndex)
        assertEquals(3, snapshot.allEntries().size)
    }

    @Test
    fun nightCollectionCanFallBackToDayWithoutChangingStoredNightState() {
        val snapshot = ModuleCardWallpaperSnapshot(
            entries = listOf(entry("day")),
            carouselEnabled = false,
        )

        assertTrue(snapshot.collection(ModuleWallpaperVariant.Night).entries.isEmpty())
        assertEquals(
            "day",
            snapshot.collection(ModuleWallpaperVariant.Night, fallbackToDay = true).entries.single().uriString,
        )
        assertTrue(snapshot.nightEntries.isEmpty())
    }

    @Test
    fun generatedStorageKeysNeverReuseThePreviousFileName() {
        val first = uniqueModuleWallpaperStorageKey("module_wallpaper")
        val second = uniqueModuleWallpaperStorageKey("module_wallpaper")

        assertTrue(first.startsWith("module_wallpaper_"))
        assertTrue(second.startsWith("module_wallpaper_"))
        assertNotEquals(first, second)
    }

    @Test
    fun mergeKeepsExistingVariantWhenBackupDoesNotContainIt() {
        val existing = ModuleCardWallpaperSnapshot(emptyList(), false)
            .withCollection(
                ModuleWallpaperVariant.Day,
                ModuleWallpaperCollection(
                    entries = listOf(entry("existing-day")),
                    intervalMillis = 20_000L,
                ),
            )
            .withCollection(
                ModuleWallpaperVariant.Night,
                ModuleWallpaperCollection(entries = listOf(entry("existing-night"))),
            )
        val incoming = ModuleCardWallpaperSnapshot(emptyList(), false)
            .withCollection(
                ModuleWallpaperVariant.Night,
                ModuleWallpaperCollection(entries = listOf(entry("imported-night"))),
            )

        val merged = mergeModuleWallpaperSnapshots(existing, incoming)

        assertEquals(listOf("existing-day"), merged.entries.map(ModuleCardWallpaperEntry::uriString))
        assertEquals(20_000L, merged.intervalMillis)
        assertEquals(
            listOf("existing-night", "imported-night"),
            merged.nightEntries.map(ModuleCardWallpaperEntry::uriString),
        )
    }

    private fun entry(id: String) = ModuleCardWallpaperEntry(
        uriString = id,
        crop = DEFAULT_CUSTOM_WALLPAPER_CROP,
    )
}
