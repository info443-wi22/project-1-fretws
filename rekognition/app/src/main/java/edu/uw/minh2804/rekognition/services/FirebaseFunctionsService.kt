/** Shane Fretwell: I was responsible for the contents of this file, except for the data classes which Tom implemented **/

package edu.uw.minh2804.rekognition.services

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import com.google.android.material.tabs.TabLayout
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import edu.uw.minh2804.rekognition.R
import edu.uw.minh2804.rekognition.extensions.toString64
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.parcelize.Parcelize

// This interface is used to map callbacks with a user's selected option in the tab bar.
interface Annotator {
    // Gets a unique identifier of the type of annotation this Annotator provides
    fun getIdentifierText(context: Context): String
    // Formats the annotation response into a string, if the response is valid
    fun onAnnotated(result: AnnotateImageResponse): String? // REFACTOR if storing formatted results, then don't need to have separate annotate and onAnnotated methods
    // Annotates an image with a description, formatted in an AnnotateImageResponse
    suspend fun annotate(image: Bitmap): AnnotateImageResponse
}

// This service is responsible for invoking web requests to Firebase.
object FirebaseFunctionsService {
    // Coding in a getter instead of an initial value makes this object mockable by mockk
    private val functions
        get() = Firebase.functions

    // The annotator endpoints are matched to their corresponding tab by the text the tab contains.
    // This way, the position of each tab is inconsequential
    fun getAnnotatorForSelectedTab(context: Context, tabLayout: TabLayout): Annotator? {
        val selectedTab = tabLayout.getTabAt(tabLayout.selectedTabPosition)
        for (annotator in Annotator.values()) {
            if (selectedTab?.text == annotator.getIdentifierText(context)) {
                return annotator
            }
        }
        return null
    }

    // This enum class encapsulates the differences between the text and object recognition
    // endpoints, allowing for code referencing these endpoints to be agnostic of these differences
    enum class Annotator : edu.uw.minh2804.rekognition.services.Annotator {
        TEXT {
            // The string resource labeling each tab in the camera activity is a unique identifier
            // of the endpoint, and is widely accessible across the code base.
            override fun getIdentifierText(context: Context): String {
                return context.getString(R.string.tab_item_text_annotation)
            }

            override fun onAnnotated(result: AnnotateImageResponse): String? {
                return result.fullTextAnnotation?.text
            }

            override suspend fun annotate(image: Bitmap): AnnotateImageResponse {
                val base64StringifiedImage = image.toString64()
                val request = TextRecognitionRequest.createRequest(base64StringifiedImage)
                return requestAnnotation(request)
            }
        },
        OBJECT {
            override fun getIdentifierText(context: Context): String {
                return context.getString(R.string.tab_item_object_annotation)
            }

            override fun onAnnotated(result: AnnotateImageResponse): String? {
                return if (result.labelAnnotations.any()) {
                    result.labelAnnotations.joinToString { it.description }
                } else null
            }

            override suspend fun annotate(image: Bitmap) = requestAnnotation(
                ObjectRecognitionRequest.createRequest(image.toString64())
            )
        }
    }

    // Communicates with the Firebase image annotation API while being agnostic of the annotations requested
    internal suspend fun requestAnnotation(requestBody: JsonObject): AnnotateImageResponse {
        if (!FirebaseAuthService.isAuthenticated()) {
            FirebaseAuthService.signIn()
        }
        val resultsArray = suspendCoroutine<JsonElement> { continuation ->
            functions
                .getHttpsCallable("annotateImage")
                .call(requestBody.toString())
                .addOnSuccessListener { successfulResponse ->
                    continuation.resume(
                        JsonParser.parseString(Gson().toJson(successfulResponse.data))
                    )
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
        }
        return formatAnnotationResult(resultsArray)
    }

    // REFACTOR: This was added for testing purposes
    internal fun formatAnnotationResult(resultsArray: JsonElement): AnnotateImageResponse {
        // We only call for one image at a time, so there will only ever be one result in the array
        val soleAnnotation = resultsArray.asJsonArray.first()
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