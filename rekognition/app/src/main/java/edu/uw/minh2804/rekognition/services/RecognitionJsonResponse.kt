package edu.uw.minh2804.rekognition.services

import android.os.Parcelable
import com.google.firebase.functions.HttpsCallableResult
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.parcelize.Parcelize

object RecognitionJsonResponse {
    fun parseString(successfulResponse: HttpsCallableResult): AnnotateImageResponse {
        val parsedJsonElement = JsonParser.parseString(Gson().toJson(successfulResponse.data))
        return toKotlin(parsedJsonElement)
    }

    // REFACTOR: This was added for testing purposes
    internal fun toKotlin(resultsData: JsonElement) : AnnotateImageResponse {
        // We only call for one image at a time, so there will only ever be one result in the array
        val soleAnnotation = resultsData.asJsonArray.first()
        val nullableDataResponse = Gson().fromJson(soleAnnotation, AnnotateImageResponse::class.java)
        // Gson is able to break the null safety of the AnnotateImageResponse labelAnnotations parameter,
        // so we should repair that null safety immediately
        val nonNullable = AnnotateImageResponse(
            fullTextAnnotation = nullableDataResponse.fullTextAnnotation,
            labelAnnotations = nullableDataResponse.labelAnnotations
                ?: List<EntityAnnotation>(size = 0, init = {EntityAnnotation("", 0.0)})
        )
        return nonNullable
    }
}

// These data classes are used to serialize the Json response from Firebase functions.
// See more: https://cloud.google.com/vision/docs/reference/rest/v1/AnnotateImageResponse#textannotation

@Parcelize
data class EntityAnnotation(
    val description: String,
    val score: Double
) : Parcelable

@Parcelize
data class TextAnnotation(
    val text: String
) : Parcelable

@Parcelize
data class AnnotateImageResponse(
    val fullTextAnnotation: TextAnnotation?,
    val labelAnnotations: List<EntityAnnotation>
) : Parcelable
