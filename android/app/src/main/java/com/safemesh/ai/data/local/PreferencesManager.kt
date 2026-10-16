package com.safemesh.ai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safemesh.ai.data.model.EmergencyContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safemesh_preferences")

class PreferencesManager(private val context: Context) {
    private val gson = Gson()

    companion object {
        val KEY_CONTACTS = stringPreferencesKey("sos_contacts")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_BACKEND_URL = stringPreferencesKey("backend_url")

        const val DEFAULT_USER_NAME = "Shubham Chaubey"
        // Default local backend URL (10.0.2.2 for emulator, localhost fallback)
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:3001/"
    }

    val contactsFlow: Flow<List<EmergencyContact>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_CONTACTS] ?: "[]"
        try {
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            gson.fromJson<List<EmergencyContact>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveContacts(contacts: List<EmergencyContact>) {
        val json = gson.toJson(contacts)
        context.dataStore.edit { preferences ->
            preferences[KEY_CONTACTS] = json
        }
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[KEY_USER_NAME]
        if (saved.isNullOrBlank() || saved == "Alex Chen") {
            DEFAULT_USER_NAME
        } else {
            saved
        }
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
        }
    }

    val backendUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_BACKEND_URL] ?: DEFAULT_BACKEND_URL
    }

    suspend fun saveBackendUrl(url: String) {
        // Ensure trailing slash for Retrofit base URL
        val formatted = if (url.endsWith("/")) url else "$url/"
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKEND_URL] = formatted
        }
    }
}
