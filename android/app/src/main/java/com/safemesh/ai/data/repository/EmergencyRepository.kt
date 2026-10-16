package com.safemesh.ai.data.repository

import com.safemesh.ai.data.api.ApiClient
import com.safemesh.ai.data.local.PreferencesManager
import com.safemesh.ai.data.location.LocationService
import com.safemesh.ai.data.model.EmergencyContact
import com.safemesh.ai.data.model.EmergencyRequest
import com.safemesh.ai.data.model.EmergencyResponse
import com.safemesh.ai.data.model.LocationData
import com.safemesh.ai.data.model.LocationPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EmergencyRepository(
    private val preferencesManager: PreferencesManager,
    private val locationService: LocationService
) {
    val contactsFlow: Flow<List<EmergencyContact>> = preferencesManager.contactsFlow
    val userNameFlow: Flow<String> = preferencesManager.userNameFlow
    val backendUrlFlow: Flow<String> = preferencesManager.backendUrlFlow

    suspend fun addContact(contact: EmergencyContact) {
        val current = contactsFlow.first().toMutableList()
        current.add(contact)
        preferencesManager.saveContacts(current)
    }

    suspend fun removeContact(id: String) {
        val current = contactsFlow.first().filter { it.id != id }
        preferencesManager.saveContacts(current)
    }

    suspend fun updateUserName(name: String) {
        preferencesManager.saveUserName(name)
    }

    suspend fun updateBackendUrl(url: String) {
        preferencesManager.saveBackendUrl(url)
    }

    suspend fun getLocation(): Result<LocationData> {
        return locationService.getCurrentLocation()
    }

    suspend fun dispatchEmergency(
        reason: String,
        location: LocationData?
    ): Result<EmergencyResponse> {
        val contacts = contactsFlow.first()
        if (contacts.isEmpty()) {
            return Result.failure(IllegalStateException("No emergency contacts configured. Please add contacts in SOS Contacts."))
        }

        val userName = userNameFlow.first()
        val backendUrl = backendUrlFlow.first()

        val locationPayload = location?.let {
            LocationPayload(it.latitude, it.longitude)
        }

        val request = EmergencyRequest(
            contacts = contacts,
            reason = reason,
            userName = userName,
            location = locationPayload
        )

        return try {
            val apiService = ApiClient.getService(backendUrl)
            val response = apiService.dispatchEmergency(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Server returned error (${response.code()}): ${errorBody ?: response.message()}"))
            }
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Cannot connect to backend server at $backendUrl. Ensure Node server is running on LAN."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out to $backendUrl."))
        } catch (e: Exception) {
            Result.failure(Exception("Dispatch failed: ${e.localizedMessage ?: "Network error"}"))
        }
    }
}
