package edu.uw.minh2804.rekognition

import android.graphics.Bitmap
import android.util.Base64
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import edu.uw.minh2804.rekognition.extensions.toString64
import edu.uw.minh2804.rekognition.services.*
import io.mockk.*
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@DisplayName("Text Annotator Endpoint")
class TextAnnotatorTestSuite {
    private val textAnnotator = FirebaseFunctionsService.Annotator.TEXT

    @Nested
    @DisplayName("Testing onAnnotated")
    inner class OnAnnotated {
        @Test
        fun onAnnotated_returnsExpectedFormat_normalCase() =
            assertTextResponse(TestUtils.realTextRecognitionData)

        @Test
        fun onAnnotated_returnsExpectedFormat_emptyCase() = assertTextResponse("")

        @Test
        fun onAnnotated_returnsExpectedFormat_nullCase() = assertTextResponse(null)

        private fun assertTextResponse(textResponseToTest: String?) {
            val textAnnotationResponse = TestUtils.TextAnnotationResponseWrapper(textResponseToTest)
            val actualTextReadyForDisplay =
                textAnnotator.onAnnotated(textAnnotationResponse.rawTextAnnotatedResponse)
            val expectedTextReadyForDisplay = textAnnotationResponse.expectedTextReadyForDisplay
            assertEquals("Correct text was generated", expectedTextReadyForDisplay, actualTextReadyForDisplay)
        }
    }

    @Test
    @DisplayName("Testing annotate")
    // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
    // class, whereas the real annotate method is sensitive to these changes
    fun annotate_passesCorrectJson() =
        assertAnnotatorPassesCorrectJson(textAnnotator, TestUtils.Endpoint.TEXT)

    @Test
    @DisplayName("Testing annotate formatAnnotationResult")
    fun formatAnnotationResult_formatsCorrectly() {
        val actualFormattedResponse =
            RecognitionJsonResponse.toKotlin(TestUtils.Endpoint.TEXT.exampleJsonResponse)
        val expectedFormattedResponse = TestUtils.Endpoint.TEXT.exampleImageAnnotation
        assertEquals(expectedFormattedResponse, actualFormattedResponse)
    }
}

@DisplayName("Object Annotator Endpoint")
class ObjectAnnotatorTestSuite {
    private val objectAnnotator = FirebaseFunctionsService.Annotator.OBJECT

    @Nested
    @DisplayName("Testing onAnnotated")
    inner class OnAnnotated {
        @Test
        fun onAnnotated_returnsExpectedFormat_normalCase() = assertObjectResponseFormatsToString(
            TestUtils.realObjectRecognitionData,
            TestUtils.realObjectRecognitionData.joinToString { it.description }
        )

        @Test
        fun onAnnotated_returnsExpectedFormat_emptyCase() =
            assertObjectResponseFormatsToString(listOf(), null)

        private fun assertObjectResponseFormatsToString(
            objectAnnotations: List<EntityAnnotation>,
            expectedString: String?
        ) {
            val actualLabelsReadyForDisplay = objectAnnotator.onAnnotated(
                AnnotateImageResponse(null, objectAnnotations)
            )
            assertEquals(expectedString, actualLabelsReadyForDisplay)
        }
    }

    @Test
    @DisplayName("Testing annotate")
    // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
    // class, whereas the real annotate method is sensitive to these changes
    fun annotate_passesCorrectJson() = assertAnnotatorPassesCorrectJson(objectAnnotator, TestUtils.Endpoint.OBJECT)

    @Test
    @DisplayName("Testing annotate formatAnnotationResult")
    fun formatAnnotationResult_formatsCorrectly() {
        val actualFormattedResponse = RecognitionJsonResponse.toKotlin(TestUtils.Endpoint.OBJECT.exampleJsonResponse)
        val expectedFormattedResponse = TestUtils.Endpoint.OBJECT.exampleImageAnnotation
        assertEquals(expectedFormattedResponse, actualFormattedResponse)
    }
}

fun assertAnnotatorPassesCorrectJson(annotator: Annotator, endpoint: TestUtils.Endpoint) {
    mockkStatic(Base64::class)
    every { Base64.encodeToString(any(), any()) } returns TestUtils.base64EncodedImage

    val bitmap = mockk<Bitmap>(relaxed = true)
    every {
        // toString64 is an extension of the bitmap class and should be tested outside of
        // this test suite, because it is technically subject to change
        bitmap.toString64()
    } returns TestUtils.base64EncodedImage

    assertEquals(TestUtils.base64EncodedImage, bitmap.toString64())

    mockkObject(Firebase)

    mockkObject(FirebaseAuthService)
    every {
        FirebaseAuthService.isAuthenticated()
    } returns true
    assertTrue(FirebaseAuthService.isAuthenticated())

    mockkObject(FirebaseFunctionsService)
    val requestAnnotationArgument = slot<JsonObject>()

    coEvery {
        FirebaseFunctionsService.requestAnnotation(
            requestBody = capture(requestAnnotationArgument)
        )
    } answers {
        println(requestAnnotationArgument.captured)
        endpoint.exampleImageAnnotation
    }

    // Assert that annotate Returns the right annotation
    runBlocking {
        annotator.annotate(bitmap)
    }

    // Verify the function is called
    coVerify {
        FirebaseFunctionsService.requestAnnotation(any())
    }

    assertJSONFormatAndContents(
        json = requestAnnotationArgument.captured,
        expectedContent = TestUtils.base64EncodedImage,
        endpoint = endpoint
    )

    unmockkAll()
}

fun assertJSONFormatAndContents(json: JsonObject, expectedContent: String, endpoint: TestUtils.Endpoint) {
    val image:JsonObject = json.getAsJsonObject("image")
    val actualContent: String = image.getAsJsonPrimitive("content").asString
    assertEquals("Image string is found and correct", expectedContent, actualContent)
    val featureArray = json.getAsJsonArray("features")
    val onlyFeature = featureArray.first().asJsonObject
    val actualEndpointDescriptor = onlyFeature.getAsJsonPrimitive("type").asString
    assertEquals("Correct endpoint is being called", endpoint.firebaseDescriptor, actualEndpointDescriptor)
}
