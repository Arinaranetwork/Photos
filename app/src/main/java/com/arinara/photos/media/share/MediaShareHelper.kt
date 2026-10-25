// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.photos.media.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.arinara.photos.data.model.MediaItem
import com.arinara.photos.data.model.MediaType
import java.io.File

object MediaShareHelper {

    fun shareMediaItem(
        context: Context,
        item: MediaItem,
        chooserTitle: String = "Share ${item.title}"
    ): Boolean {
        return try {
            val shareUri = resolveShareableUri(context, item) ?: return false
            val mimeType = item.mimeType.ifBlank {
                when (item.mediaType) {
                    MediaType.VIDEO -> "video/*"
                    MediaType.AUDIO -> "audio/*"
                    else -> "image/*"
                }
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                clipData = ClipData.newRawUri(item.title, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        title: String = file.nameWithoutExtension
    ): Boolean {
        if (!file.exists()) return false
        return try {
            val authority = "${context.packageName}.fileprovider"
            val shareUri = FileProvider.getUriForFile(context, authority, file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                clipData = ClipData.newRawUri(title, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Share $title").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun resolveShareableUri(context: Context, item: MediaItem): Uri? {
        val rawUriStr = item.uriString.trim()
        if (rawUriStr.startsWith("content://", ignoreCase = true)) {
            return Uri.parse(rawUriStr)
        }
        val file = File(item.filePath)
        if (file.exists()) {
            return try {
                val authority = "${context.packageName}.fileprovider"
                FileProvider.getUriForFile(context, authority, file)
            } catch (_: Throwable) {
                // Copy to cache/share_staging if path is outside FileProvider roots
                val stagingDir = File(context.cacheDir, "share_staging").apply { mkdirs() }
                val stagedFile = File(stagingDir, file.name.ifBlank { "shared_${System.currentTimeMillis()}" })
                file.copyTo(stagedFile, overwrite = true)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", stagedFile)
            }
        }
        if (rawUriStr.isNotBlank()) {
            return Uri.parse(rawUriStr)
        }
        return null
    }
}
