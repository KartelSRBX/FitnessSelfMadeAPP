package com.kartel.fitness.core.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Ручной экспорт данных (roadmap п.8 "Хранение данных") — не облако и не
 * синхронизация, но защита от полной потери данных при сбросе/потере телефона.
 * Пишет снапшот в кэш приложения и отдаёт content:// Uri для шаринга через
 * Intent.ACTION_SEND ("Поделиться").
 */
object DataExporter {
    fun writeAndGetShareUri(context: Context, json: String): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "fitness-export-${System.currentTimeMillis()}.json")
        file.writeText(json)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
