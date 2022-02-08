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

class FirebaseFunctionsServiceTestSuite {
    // Text Annotator Test Suite
    private val textAnnotator = FirebaseFunctionsService.Annotator.TEXT

    @Test
    fun text_onAnnotated_returnsExpectedFormat_normalCase() = assertTextResponse(TestUtils.realTextRecognitionData)

    @Test
    fun text_onAnnotated_returnsExpectedFormat_emptyCase() = assertTextResponse("")

    @Test
    fun text_onAnnotated_returnsExpectedFormat_nullCase() = assertTextResponse(null)

    private fun assertTextResponse(textResponseToTest: String?) {
        val textAnnotationResponse = TestUtils.TextAnnotationResponseWrapper(textResponseToTest)
        val actualTextReadyForDisplay = textAnnotator.onAnnotated(textAnnotationResponse.rawTextAnnotatedResponse)
        val expectedTextReadyForDisplay = textAnnotationResponse.expectedTextReadyForDisplay
        assertEquals(expectedTextReadyForDisplay, actualTextReadyForDisplay)
    }

    @Test
    // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
    // class, whereas the real annotate method is sensitive to these changes
    fun text_annotate_passesCorrectJson() = assertAnnotatorPassesCorrectJson(textAnnotator, TestUtils.Endpoint.TEXT)

    @Test
    fun text_formatAnnotationResult_formatsCorrectly() {
        val actualFormattedResponse = FirebaseFunctionsService.formatAnnotationResult(TestUtils.Endpoint.TEXT.exampleJsonResponse)
        val expectedFormattedResponse = TestUtils.Endpoint.TEXT.exampleImageAnnotation
        assertEquals(expectedFormattedResponse, actualFormattedResponse)
    }

    // Object Annotator Test Suite
    private val objectAnnotator = FirebaseFunctionsService.Annotator.OBJECT

    @Test
    fun object_onAnnotated_returnsExpectedFormat_normalCase() = assertObjectResponseFormatsToString(
        TestUtils.realObjectRecognitionData,
        TestUtils.realObjectRecognitionData.joinToString{ it.description }
    )

    @Test
    fun object_onAnnotated_returnsExpectedFormat_emptyCase() = assertObjectResponseFormatsToString(listOf(), null)

    private fun assertObjectResponseFormatsToString(objectAnnotations: List<EntityAnnotation>, expectedString: String?) {
        val actualLabelsReadyForDisplay = objectAnnotator.onAnnotated(
            AnnotateImageResponse(null, objectAnnotations)
        )
        assertEquals(expectedString, actualLabelsReadyForDisplay)
    }

    @Test
    // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
    // class, whereas the real annotate method is sensitive to these changes
    fun object_annotate_passesCorrectJson() = assertAnnotatorPassesCorrectJson(objectAnnotator, TestUtils.Endpoint.OBJECT)

    @Test
    fun object_formatAnnotationResult_formatsCorrectly() {
        val actualFormattedResponse = FirebaseFunctionsService.formatAnnotationResult(TestUtils.Endpoint.OBJECT.exampleJsonResponse)
        val expectedFormattedResponse = TestUtils.Endpoint.OBJECT.exampleImageAnnotation
        assertEquals(expectedFormattedResponse, actualFormattedResponse)
    }

    private fun assertAnnotatorPassesCorrectJson(annotator: Annotator, endpoint: TestUtils.Endpoint) {
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

        TestUtils.assertJSONFormatAndContents(
            json = requestAnnotationArgument.captured,
            expectedContent = TestUtils.base64EncodedImage,
            endpoint = endpoint
        )

        unmockkAll()
    }
}
