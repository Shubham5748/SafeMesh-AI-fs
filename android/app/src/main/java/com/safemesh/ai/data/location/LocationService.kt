package com.safemesh.ai.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.safemesh.ai.data.model.LocationData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class LocationService(private val context: Context) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(timeoutMillis: Long = 10000L): Result<LocationData> {
        return try {
            val result = withTimeoutOrNull(timeoutMillis) {
                // Try last known location first
                val lastKnown = getLastKnownLocation()
                if (lastKnown != null && (System.currentTimeMillis() - lastKnown.time) < 60000L) {
                    return@withTimeoutOrNull LocationData(
                        latitude = lastKnown.latitude,
                        longitude = lastKnown.longitude,
                        accuracy = lastKnown.accuracy,
                        timestamp = lastKnown.time
                    )
                }

                // Request fresh high-accuracy location
                val fresh = getFreshLocation()
                if (fresh != null) {
                    LocationData(
                        latitude = fresh.latitude,
                        longitude = fresh.longitude,
                        accuracy = fresh.accuracy,
                        timestamp = fresh.time
                    )
                } else if (lastKnown != null) {
                    LocationData(
                        latitude = lastKnown.latitude,
                        longitude = lastKnown.longitude,
                        accuracy = lastKnown.accuracy,
                        timestamp = lastKnown.time
                    )
                } else {
                    null
                }
            }

            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(Exception("GPS timed out or unavailable. Ensure location is enabled."))
            }
        } catch (e: SecurityException) {
            Result.failure(Exception("Location permission denied. Please grant permission in settings."))
        } catch (e: Exception) {
            Result.failure(Exception("Unable to acquire location: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { loc: Location? ->
                if (cont.isActive) cont.resume(loc)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFreshLocation(): Location? = suspendCancellableCoroutine { cont ->
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc: Location? ->
                if (cont.isActive) cont.resume(loc)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }
}
