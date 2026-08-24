package com.pigeonpost

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class PigeonPostApp : Application() {

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid()
    }

    /**
     * osmdroid must be configured once, before any MapView is created, or tile loading
     * fails outright. OpenStreetMap's tile policy requires an identifying user agent,
     * and the tile cache is kept inside the app's own cache directory so no external
     * storage permission is needed.
     */
    private fun configureOsmdroid() {
        val prefs = getSharedPreferences(OSMDROID_PREFS, Context.MODE_PRIVATE)
        Configuration.getInstance().apply {
            load(this@PigeonPostApp, prefs)
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid").apply { mkdirs() }
            osmdroidTileCache = File(osmdroidBasePath, "tiles").apply { mkdirs() }
        }
    }

    private companion object {
        const val OSMDROID_PREFS = "osmdroid_prefs"
    }
}
