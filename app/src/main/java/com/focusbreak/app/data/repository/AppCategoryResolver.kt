package com.focusbreak.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.focusbreak.app.domain.UsageCategory

/** Clasifica una app en Social / Vídeo / Productividad / Otros (categoría declarada por la app + excepciones). */
class AppCategoryResolver(private val context: Context) {
    private val cache = HashMap<String, UsageCategory>()

    fun categoryOf(packageName: String): UsageCategory =
        cache.getOrPut(packageName) { OVERRIDES[packageName] ?: fromSystem(packageName) }

    private fun fromSystem(packageName: String): UsageCategory = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        when (info.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> UsageCategory.SOCIAL
            ApplicationInfo.CATEGORY_VIDEO -> UsageCategory.VIDEO
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> UsageCategory.PRODUCTIVITY
            else -> UsageCategory.OTHER
        }
    }.getOrDefault(UsageCategory.OTHER)

    private companion object {
        // Muchas apps no declaran categoría; se corrigen las más comunes
        val OVERRIDES = mapOf(
            "com.instagram.android" to UsageCategory.SOCIAL,
            "com.facebook.katana" to UsageCategory.SOCIAL,
            "com.twitter.android" to UsageCategory.SOCIAL,
            "com.snapchat.android" to UsageCategory.SOCIAL,
            "com.zhiliaoapp.musically" to UsageCategory.SOCIAL,       // TikTok
            "com.ss.android.ugc.trill" to UsageCategory.SOCIAL,       // TikTok (otras regiones)
            "com.whatsapp" to UsageCategory.SOCIAL,
            "org.telegram.messenger" to UsageCategory.SOCIAL,
            "com.reddit.frontpage" to UsageCategory.SOCIAL,
            "com.google.android.youtube" to UsageCategory.VIDEO,
            "com.netflix.mediaclient" to UsageCategory.VIDEO,
            "tv.twitch.android.app" to UsageCategory.VIDEO,
            "com.amazon.avod.thirdpartyclient" to UsageCategory.VIDEO,
        )
    }
}
