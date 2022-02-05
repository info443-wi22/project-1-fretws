package edu.uw.minh2804.rekognition

import android.graphics.Bitmap
import edu.uw.minh2804.rekognition.services.AnnotateImageResponse
import edu.uw.minh2804.rekognition.services.EntityAnnotation
import edu.uw.minh2804.rekognition.services.TextAnnotation
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers


class TestUtils {
    companion object {
        const val base64EncodedImage = "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"
        const val realTextRecognitionData = "WAITING?\\nPLEASE\\nTURN OFF\\nYOUR\\nENGINE\\n"
        val smallBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ALPHA_8)
        val realBitmapForTextEndpoint: Bitmap = run {
            val imageBytes = Base64.decode(base64EncodedImage, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        val realObjectRecognitionData = listOf(
            EntityAnnotation("Street", 0.87294734),
            EntityAnnotation("Snapshot", 0.8523099),
            EntityAnnotation("Town", 0.8481104)
        )
    }

    class TextAnnotationResponseWrapper(val expectedTextReadyForDisplay: String? = realTextRecognitionData) {
        private val fullTextAnnotation = expectedTextReadyForDisplay?.let {TextAnnotation(it)}
        private val labelAnnotations = List<EntityAnnotation>(size = 0, init = {EntityAnnotation("", 0.0)})
        val rawTextAnnotatedResponse = AnnotateImageResponse(fullTextAnnotation, labelAnnotations)
    }

    interface DispatcherProvider {

        fun main(): CoroutineDispatcher = Dispatchers.Main
        fun default(): CoroutineDispatcher = Dispatchers.Default
        fun io(): CoroutineDispatcher = Dispatchers.IO
        fun unconfined(): CoroutineDispatcher = Dispatchers.Unconfined

    }

    class DefaultDispatcherProvider : DispatcherProvider
}