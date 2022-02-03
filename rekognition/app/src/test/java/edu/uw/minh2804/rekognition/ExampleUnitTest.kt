package edu.uw.minh2804.rekognition

import android.graphics.Bitmap
import com.google.gson.JsonObject
import edu.uw.minh2804.rekognition.extensions.toString64
import edu.uw.minh2804.rekognition.services.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

class FirebaseAuthenticationServiceTestSuite {
    @Test
    fun isAuthenticated_correctReturn_signedOut() = assertFalse(FirebaseAuthService.isAuthenticated())

    @Test
    fun isAuthenticated_correctReturn_signedIn() {
        runBlocking { FirebaseAuthService.signIn() }
        assertTrue(FirebaseAuthService.isAuthenticated())
    }
}

class FirebaseFunctionsServiceTestSuite {
    class TextAnnotatorTestSuite {
        val annotator = FirebaseFunctionsService.Annotator.TEXT

        @Test
        fun onAnnotated_returnsExpectedFormat_normalCase() = assertTextResponse(TestUtils.realTextRecognitionData)

        @Test
        fun onAnnotated_returnsExpectedFormat_emptyCase() = assertTextResponse("")

        @Test
        fun onAnnotated_returnsExpectedFormat_nullCase() = assertTextResponse(null)

        private fun assertTextResponse(textResponseToTest: String?) {
            val textAnnotationResponse = TestUtils.TextAnnotationResponseWrapper(textResponseToTest)
            val actualTextReadyForDisplay = annotator.onAnnotated(textAnnotationResponse.rawTextAnnotatedResponse)
            val expectedTextReadyForDisplay = textAnnotationResponse.expectedTextReadyForDisplay
            assertEquals(actualTextReadyForDisplay, expectedTextReadyForDisplay)
        }

        @Test
        // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
        // class, whereas the real annotate method is sensitive to these changes
        fun annotate_passesCorrectJson() {
            val bitmap = mockk<Bitmap>()
//            val bitmap = mockk(TestUtils.smallBitmap)
            every {
                // toString64 is an extension of the bitmap class and should be tested outside of
                // this test suite, because it is technically subject to change
                bitmap.toString64()
            } returns TestUtils.base64EncodedImage

            val functionsService = spyk<FirebaseFunctionsService>()
            val expectedAnnotationResponseWrapper = TestUtils.TextAnnotationResponseWrapper()
            every {
                functionsService["requestAnnotation"](ofType(JsonObject::class))
            } returns expectedAnnotationResponseWrapper

            // Assert that annotate Returns the right annotation
            val actualAnnotationResponse = runBlocking {
                annotator.annotate(bitmap)
            }

            assertEquals(actualAnnotationResponse, expectedAnnotationResponseWrapper.rawTextAnnotatedResponse)

            // Verify that requestAnnotation receives the right input

            // Verify that the firebase functions are called with an annotateImage request

            unmockkAll()
        }
    }

    class ObjectAnnotatorTestSuite {
        val annotator = FirebaseFunctionsService.Annotator.OBJECT

        @Test
        fun onAnnotated_returnsExpectedFormat_normalCase() = assertObjectResponseFormatsToString(
            TestUtils.realObjectRecognitionData,
            TestUtils.realObjectRecognitionData.joinToString{ it.description }
        )

        @Test
        fun onAnnotated_returnsExpectedFormat_emptyCase() = assertObjectResponseFormatsToString(listOf(), null)

        private fun assertObjectResponseFormatsToString(objectAnnotations: List<EntityAnnotation>, expectedString: String?) {
            val actualLabelsReadyForDisplay = annotator.onAnnotated(
                AnnotateImageResponse(null, objectAnnotations)
            )
            assertEquals(actualLabelsReadyForDisplay, expectedString)
        }
    }
}
