package me.weishu.kernelsu.ui.screen.themestore

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.ThemeAuthorGender
import me.weishu.kernelsu.ui.util.ThemeStorePackagePreview
import me.weishu.kernelsu.ui.util.ThemeStorePackagePreviewImage
import me.weishu.kernelsu.ui.util.ThemeStorePackageWarning
import java.text.DateFormat
import java.util.Date

internal data class PendingThemeStoreImport(
    val uri: Uri,
    val preview: ThemeStorePackagePreview,
    val warnings: List<ThemeStorePackageWarning>,
)

@Composable
internal fun ThemeStoreImportPreviewDialog(
    pending: PendingThemeStoreImport,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val preview = pending.preview
    val author = preview.author
    val coverBitmap = rememberThemeStorePreviewBitmap(preview.cover, maxSide = 960)
    val avatarBitmap = rememberThemeStorePreviewBitmap(author?.avatar, maxSide = 256)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_store_import_preview_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 7f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap,
                            contentDescription = stringResource(R.string.theme_store_import_preview_cover),
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ImageNotSupported,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.theme_store_import_preview_no_cover),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = stringResource(R.string.theme_store_profile_avatar_description),
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = author?.displayName
                                ?.takeIf(String::isNotBlank)
                                ?: author?.realName?.takeIf(String::isNotBlank)
                                ?: stringResource(R.string.theme_store_import_preview_unknown_author),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.theme_store_import_preview_author),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                author?.realName
                    ?.takeIf { it.isNotBlank() && it != author.displayName }
                    ?.let { realName ->
                        ThemeStorePreviewDetail(
                            label = stringResource(R.string.theme_store_profile_real_name_label),
                            value = realName,
                        )
                    }
                author?.gender
                    ?.takeIf { it != ThemeAuthorGender.Unspecified }
                    ?.let { gender ->
                        ThemeStorePreviewDetail(
                            label = stringResource(R.string.theme_store_profile_gender_label),
                            value = themeAuthorGenderLabel(gender),
                        )
                    }
                author?.bio?.takeIf(String::isNotBlank)?.let { bio ->
                    ThemeStorePreviewDetail(
                        label = stringResource(R.string.theme_store_profile_bio_label),
                        value = bio,
                    )
                }

                val packageMeta = if (preview.exportedAt > 0L) {
                    stringResource(
                        R.string.theme_store_import_preview_meta_with_date,
                        preview.version,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(preview.exportedAt)),
                    )
                } else {
                    stringResource(R.string.theme_store_import_preview_meta, preview.version)
                }
                Text(
                    text = "$packageMeta \u00B7 ${stringResource(
                        R.string.theme_store_import_preview_resource_count,
                        preview.configuredResourceCount,
                    )}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (pending.warnings.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.theme_store_import_preview_warning_count,
                                pending.warnings.size,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    pending.warnings.take(3).forEach { warning ->
                        Text(
                            text = warning.reason?.takeIf(String::isNotBlank)?.let { reason ->
                                stringResource(
                                    R.string.theme_store_import_preview_warning_item,
                                    warning.assetId,
                                    reason,
                                )
                            } ?: warning.assetId,
                            modifier = Modifier.padding(start = 26.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ThemeStorePreviewDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun themeAuthorGenderLabel(gender: ThemeAuthorGender): String {
    return stringResource(
        when (gender) {
            ThemeAuthorGender.Unspecified -> R.string.theme_store_profile_gender_unspecified
            ThemeAuthorGender.Male -> R.string.theme_store_profile_gender_male
            ThemeAuthorGender.Female -> R.string.theme_store_profile_gender_female
            ThemeAuthorGender.Other -> R.string.theme_store_profile_gender_other
        }
    )
}

@Composable
private fun rememberThemeStorePreviewBitmap(
    image: ThemeStorePackagePreviewImage?,
    maxSide: Int,
): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(null, image, maxSide) {
        value = image?.let { preview ->
            withContext(Dispatchers.Default) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(preview.bytes, 0, preview.bytes.size, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
                var sampleSize = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > maxSide) {
                    sampleSize *= 2
                }
                BitmapFactory.decodeByteArray(
                    preview.bytes,
                    0,
                    preview.bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sampleSize },
                )?.asImageBitmap()
            }
        }
    }
    return bitmap
}
