package com.pigeonpost

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. The Google Maps SDK initialises itself from the API key in
 * the manifest, so no map setup is needed here.
 */
@HiltAndroidApp
class PigeonPostApp : Application()
