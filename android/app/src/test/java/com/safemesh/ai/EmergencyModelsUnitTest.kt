package com.safemesh.ai

import com.google.gson.Gson
import com.safemesh.ai.data.model.EmergencyContact
import com.safemesh.ai.data.model.EmergencyRequest
import com.safemesh.ai.data.model.EmergencyResponse
import com.safemesh.ai.data.model.LocationPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyModelsUnitTest {

    private val gson = Gson()

    @Test
    fun emergencyPayloadCreation_matchesBackendExpectedContract() {
        val contact = EmergencyContact(
            id = "101",
            name = "Mom",
            phone = "9876543210",
            relation = "Mother"
        )
        val location = LocationPayload(latitude = 28.6335, longitude = 77.4475)
        val request = EmergencyRequest(
            contacts = listOf(contact),
            reason = "Manual SOS Trigger from Home",
            userName = "Shubham Chaubey",
            location = location
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"userName\":\"Shubham Chaubey\""))
        assertTrue(json.contains("\"reason\":\"Manual SOS Trigger from Home\""))
        assertTrue(json.contains("\"latitude\":28.6335"))
        assertTrue(json.contains("\"longitude\":77.4475"))
        assertTrue(json.contains("\"phone\":\"9876543210\""))
    }

    @Test
    fun contactValidation_checksCorrectFields() {
        val validContact = EmergencyContact("1", "John", "+919876543210", "Friend")
        assertTrue(validContact.name.isNotBlank())
        assertTrue(validContact.phone.isNotBlank())
        assertEquals("Friend", validContact.relation)

        val contactDefaultRel = EmergencyContact("2", "Alice", "12345")
        assertEquals("Contact", contactDefaultRel.relation)
    }

    @Test
    fun apiResultParsing_parsesSuccessResponseAccurately() {
        val rawJson = """
            {
                "success": true,
                "simulated": false,
                "smsSent": 2,
                "callsSent": 1,
                "location": true,
                "locationUrl": "https://www.google.com/maps?q=28.6335,77.4475",
                "results": [
                    {
                        "name": "Mom",
                        "phone": "+919876543210",
                        "smsSID": "SM12345",
                        "callSID": "CA67890",
                        "error": null
                    }
                ]
            }
        """.trimIndent()

        val response = gson.fromJson(rawJson, EmergencyResponse::class.java)

        assertTrue(response.success)
        assertFalse(response.simulated)
        assertEquals(2, response.smsSent)
        assertEquals(1, response.callsSent)
        assertTrue(response.location)
        assertEquals("https://www.google.com/maps?q=28.6335,77.4475", response.locationUrl)
        assertEquals(1, response.results.size)
        assertEquals("SM12345", response.results[0].smsSID)
        assertNull(response.results[0].error)
    }

    @Test
    fun apiResultParsing_parsesErrorResponseAccurately() {
        val rawJson = """
            {
                "error": "No emergency contacts provided"
            }
        """.trimIndent()

        val response = gson.fromJson(rawJson, EmergencyResponse::class.java)
        assertFalse(response.success)
        assertEquals("No emergency contacts provided", response.error)
    }
}
