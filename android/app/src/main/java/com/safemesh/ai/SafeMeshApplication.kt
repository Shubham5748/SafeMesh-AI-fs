package com.safemesh.ai

import android.app.Application
import com.safemesh.ai.data.local.PreferencesManager
import com.safemesh.ai.data.location.LocationService
import com.safemesh.ai.data.repository.EmergencyRepository

class SafeMeshApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var locationService: LocationService
        private set

    lateinit var emergencyRepository: EmergencyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        locationService = LocationService(this)
        emergencyRepository = EmergencyRepository(preferencesManager, locationService)
    }
}
