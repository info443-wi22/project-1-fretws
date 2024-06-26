/** Shane Fretwell: I was responsible for the contents of this file, and Tom refactored it to use more concise syntax **/

package edu.uw.minh2804.rekognition.services

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive

// This is a builder class to build the http request body for the object recognition Firebase's function.
object ObjectRecognitionRequest {
    internal const val requestDescriptor = "LABEL_DETECTION"
    private const val maxNumberLabels = 3
    private val feature = JsonObject().also {
        it.add("type", JsonPrimitive(requestDescriptor))
        it.add("maxResults", JsonPrimitive(maxNumberLabels))
    }
    private val features = JsonArray().also { it.add(feature) }

    fun createRequest(base64encoded: String): JsonObject {
        return JsonObject().also { request ->
            val image = JsonObject().also { it.add("content", JsonPrimitive(base64encoded)) }
            request.add("image", image)
            request.add("features", features)
        }
    }
}