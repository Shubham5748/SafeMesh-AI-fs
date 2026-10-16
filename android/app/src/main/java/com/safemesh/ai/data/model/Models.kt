package com.safemesh.ai.data.model

import com.google.gson.annotations.SerializedName

data class EmergencyContact(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("relation") val relation: String = "Contact"
)

data class LocationData(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float? = null,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class EmergencyRequest(
    @SerializedName("contacts") val contacts: List<EmergencyContact>,
    @SerializedName("reason") val reason: String,
    @SerializedName("userName") val userName: String,
    @SerializedName("location") val location: LocationPayload?
)

data class LocationPayload(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class ContactDispatchResult(
    @SerializedName("name") val name: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("smsSID") val smsSID: String?,
    @SerializedName("callSID") val callSID: String?,
    @SerializedName("error") val error: String?
)

data class EmergencyResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("simulated") val simulated: Boolean = false,
    @SerializedName("smsSent") val smsSent: Int = 0,
    @SerializedName("callsSent") val callsSent: Int = 0,
    @SerializedName("location") val location: Boolean = false,
    @SerializedName("locationUrl") val locationUrl: String? = null,
    @SerializedName("results") val results: List<ContactDispatchResult> = emptyList(),
    @SerializedName("error") val error: String? = null
)
