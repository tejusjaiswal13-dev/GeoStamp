package com.geostamp.app

import android.app.Application
import org.osmdroid.config.Configuration

class GeoStampApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid configuration - required for tile caching and downloading
        Configuration.getInstance().userAgentValue = packageName
    }
}
