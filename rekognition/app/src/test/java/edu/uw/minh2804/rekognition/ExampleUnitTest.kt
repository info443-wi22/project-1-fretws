package edu.uw.minh2804.rekognition

import edu.uw.minh2804.rekognition.services.AnnotateImageResponse
import edu.uw.minh2804.rekognition.services.EntityAnnotation
import edu.uw.minh2804.rekognition.services.FirebaseAuthService
import edu.uw.minh2804.rekognition.services.FirebaseFunctionsService
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
            val textAnnotationResponse = TestUtils.ExampleTextAnnotationResponse(textResponseToTest)
            val actualTextReadyForDisplay = annotator.onAnnotated(textAnnotationResponse.rawTextAnnotatedResponse)
            val expectedTextReadyForDisplay = textAnnotationResponse.expectedTextReadyForDisplay
            assertEquals(actualTextReadyForDisplay, expectedTextReadyForDisplay)
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
