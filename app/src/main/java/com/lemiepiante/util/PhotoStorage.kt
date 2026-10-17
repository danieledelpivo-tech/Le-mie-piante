package com.lemiepiante.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Le foto vengono copiate nella memoria privata dell'app. */
object PhotoStorage {
    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun newPhotoFile(context: Context): File = File(dir(context), "plant_${System.currentTimeMillis()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun copyFromUri(context: Context, uri: Uri): File? = runCatching {
        val file = newPhotoFile(context)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        file
    }.getOrNull()

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }
}
