package me.weishu.kernelsu.ui.webui

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import me.weishu.kernelsu.ui.screen.module.ModuleCardWallpaperEntry
import me.weishu.kernelsu.ui.screen.module.ModuleWallpaperVariant
import me.weishu.kernelsu.ui.screen.module.kpmCardWallpaperId
import me.weishu.kernelsu.ui.screen.module.readModuleCardWallpaperSnapshot
import me.weishu.kernelsu.ui.util.KpmEntry
import me.weishu.kernelsu.ui.util.MediaVisualSettings
import me.weishu.kernelsu.ui.util.loadCustomImageBitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Serves only copied KPM wallpaper files from the app's private image directory.
 * WebView never receives the original file:// URI, and a failed decode simply
 * leaves the KPatch-Next card unchanged.
 */
internal class KpmWallpaperAssetHandler(
    private val context: Context,
) : WebViewAssetLoader.PathHandler {
    private val assets = ConcurrentHashMap<String, ModuleCardWallpaperEntry>()
    private val encodedCache = object : LinkedHashMap<String, ByteArray>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            return size > MAX_ENCODED_CACHE_ENTRIES
        }
    }

    @Synchronized
    fun update(entries: List<KpmEntry>, dark: Boolean): String {
        val nextAssets = mutableMapOf<String, ModuleCardWallpaperEntry>()
        val nextDescriptors = mutableMapOf<String, KpmWallpaperDescriptor>()
        entries.forEach { kpm ->
            val snapshot = readModuleCardWallpaperSnapshot(context, kpmCardWallpaperId(kpm.id))
            val collection = snapshot.collection(
                if (dark) ModuleWallpaperVariant.Night else ModuleWallpaperVariant.Day,
                fallbackToDay = true,
            )
            if (collection.entries.isEmpty()) return@forEach
            val images = collection.entries.mapNotNull { entry ->
                val token = assetToken(kpm.id, entry)
                nextAssets[token] = entry
                KpmWallpaperImage(
                    url = assetUrl(token),
                    visualSettings = entry.visualSettings.normalized(),
                    autoContrast = entry.autoContrast,
                )
            }
            if (images.isNotEmpty()) {
                nextDescriptors[kpm.id] = KpmWallpaperDescriptor(
                    id = kpm.id,
                    name = kpm.name.ifBlank { kpm.id },
                    images = images,
                    carouselEnabled = collection.carouselEnabled,
                    intervalMillis = collection.intervalMillis,
                    selectedIndex = collection.selectedIndex,
                    order = collection.carouselOrder.value,
                )
            }
        }
        assets.clear()
        assets.putAll(nextAssets)
        return JSONArray().also { array ->
            nextDescriptors.values.forEach { descriptor -> array.put(descriptor.toJson()) }
        }.toString()
    }

    override fun handle(path: String): WebResourceResponse? {
        val token = path.trim('/').removeSuffix(".png")
        val entry = assets[token] ?: return null
        val bytes = synchronized(encodedCache) { encodedCache[token] }
            ?: encode(entry)?.also { encoded ->
                synchronized(encodedCache) { encodedCache[token] = encoded }
            }
            ?: return null
        return WebResourceResponse(
            "image/png",
            null,
            200,
            "OK",
            mapOf("Cache-Control" to "private, max-age=300"),
            ByteArrayInputStream(bytes),
        )
    }

    private fun encode(entry: ModuleCardWallpaperEntry): ByteArray? {
        val file = Uri.parse(entry.uriString).path?.let(::File) ?: return null
        val root = File(context.filesDir, "custom-images").canonicalFile
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return null
        if (canonical.path != root.path && !canonical.path.startsWith(root.path + File.separator)) return null
        val bitmap = loadCustomImageBitmap(
            context = context,
            uriString = Uri.fromFile(canonical).toString(),
            maxSide = MAX_WALLPAPER_SIDE,
            crop = entry.crop,
        ) ?: return null
        return ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)) return null
            output.toByteArray()
        }
    }

    private fun assetToken(kpmId: String, entry: ModuleCardWallpaperEntry): String {
        val source = buildString {
            append(kpmId).append('|').append(entry.uriString).append('|').append(entry.crop)
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .take(TOKEN_BYTES)
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    private fun assetUrl(token: String): String =
        "https://mui.kernelsu.org/apkesu-kpm/$token.png"

    private data class KpmWallpaperDescriptor(
        val id: String,
        val name: String,
        val images: List<KpmWallpaperImage>,
        val carouselEnabled: Boolean,
        val intervalMillis: Long,
        val selectedIndex: Int,
        val order: String,
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("id", id)
            .put("name", name)
            .put("images", JSONArray().also { array -> images.forEach { array.put(it.toJson()) } })
            .put("carouselEnabled", carouselEnabled)
            .put("intervalMillis", intervalMillis)
            .put("selectedIndex", selectedIndex)
            .put("order", order)
    }

    private data class KpmWallpaperImage(
        val url: String,
        val visualSettings: MediaVisualSettings,
        val autoContrast: Boolean,
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("url", url)
            .put("brightness", visualSettings.brightness.toDouble())
            .put("contrast", visualSettings.contrast.toDouble())
            .put("saturation", visualSettings.saturation.toDouble())
            .put("blur", visualSettings.blurRadius.toDouble())
            .put("opacity", visualSettings.opacity.toDouble())
            .put("overlay", visualSettings.overlayAlpha.toDouble())
            .put("autoContrast", autoContrast)
    }

    companion object {
        private const val MAX_WALLPAPER_SIDE = 1600
        private const val MAX_ENCODED_CACHE_ENTRIES = 8
        private const val TOKEN_BYTES = 12
    }
}

internal fun kpmWallpaperApplyScript(configJson: String, dark: Boolean): String {
    val safeJson = JSONObject.quote(configJson)
    val readability = if (dark) "rgba(0,0,0,.26)" else "rgba(255,255,255,.22)"
    return """
        (function() {
          const configs = JSON.parse($safeJson || '[]');
          const configById = new Map(configs.map((item) => [String(item.id), item]));
          const styleId = 'apkesu-kpm-wallpaper-style';
          const layerClass = 'apkesu-kpm-wallpaper-layer';
          const timerKey = '__apkesuKpmWallpaperTimer';
          const readText = (node) => (node?.textContent || '').toLowerCase();
          const cards = () => Array.from(document.querySelectorAll(
            '.module-card,.kpm-card,[data-kpm-id],[data-module-id]'
          ));
          const identify = (card) => {
            const explicit = [card.getAttribute('data-kpm-id'), card.getAttribute('data-module-id')]
              .filter(Boolean).map(String);
            for (const id of explicit) if (configById.has(id)) return configById.get(id);
            const text = readText(card);
            return configs
              .filter((item) => text.includes(String(item.id).toLowerCase()) ||
                text.includes(String(item.name).toLowerCase()))
              .sort((a, b) => String(b.id).length - String(a.id).length)[0] || null;
          };
          const apply = (card, config, imageIndex) => {
            const images = Array.isArray(config.images) ? config.images : [];
            const image = images[imageIndex % Math.max(images.length, 1)];
            if (!image?.url) return;
            let layer = Array.from(card.children).find((child) => child.classList.contains(layerClass));
            if (!layer) {
              layer = document.createElement('div');
              layer.className = layerClass;
              card.insertBefore(layer, card.firstChild);
            }
            const overlay = image.autoContrast ? '$readability' : 'rgba(0,0,0,0)';
            layer.style.backgroundImage = 'linear-gradient(' + overlay + ', ' + overlay + '), url("' + image.url + '")';
            layer.style.filter = 'brightness(' + (1 + Number(image.brightness || 0)) + ') contrast(' +
              Number(image.contrast || 1) + ') saturate(' + Number(image.saturation || 1) + ') blur(' +
              Number(image.blur || 0) + 'px)';
            layer.style.opacity = String(Math.max(0.1, Math.min(1, Number(image.opacity || 1))));
            layer.style.setProperty('--apkesu-kpm-overlay', String(image.overlay || 0));
          };
          const bind = (card) => {
            const config = identify(card);
            if (!config) return;
            card.classList.add('apkesu-kpm-wallpaper-host');
            const fingerprint = JSON.stringify(config);
            if (card.__apkesuKpmWallpaperFingerprint === fingerprint) return;
            card.__apkesuKpmWallpaperFingerprint = fingerprint;
            if (card[timerKey]) clearInterval(card[timerKey]);
            let index = Math.max(0, Number(config.selectedIndex || 0));
            apply(card, config, index);
            if (config.carouselEnabled && config.images?.length > 1) {
              card[timerKey] = setInterval(() => {
                index = config.order === 'random'
                  ? Math.floor(Math.random() * config.images.length)
                  : (index + 1) % config.images.length;
                apply(card, config, index);
              }, Math.max(3000, Number(config.intervalMillis || 5000)));
            }
          };
          let style = document.getElementById(styleId);
          if (!style) {
            style = document.createElement('style');
            style.id = styleId;
            (document.head || document.documentElement).appendChild(style);
          }
          style.textContent = '.apkesu-kpm-wallpaper-host{position:relative!important;overflow:hidden!important}' +
            '.apkesu-kpm-wallpaper-host > .' + layerClass + '{position:absolute!important;inset:0!important;z-index:0!important;pointer-events:none!important;background-position:center!important;background-repeat:no-repeat!important;background-size:cover!important;transition:opacity 180ms ease,background-image 180ms ease!important}' +
            '.apkesu-kpm-wallpaper-host > *:not(.' + layerClass + '){position:relative!important;z-index:1!important}' +
            '.apkesu-kpm-wallpaper-host > .' + layerClass + '::after{content:"";position:absolute;inset:0;background:rgba(0,0,0,var(--apkesu-kpm-overlay,0));pointer-events:none!important}';
          cards().forEach(bind);
          if (window.__apkesuKpmWallpaperObserver) window.__apkesuKpmWallpaperObserver.disconnect();
          window.__apkesuKpmWallpaperObserver = new MutationObserver(() => cards().forEach(bind));
          window.__apkesuKpmWallpaperObserver.observe(document.documentElement, { childList: true, subtree: true });
        })();
    """.trimIndent()
}
