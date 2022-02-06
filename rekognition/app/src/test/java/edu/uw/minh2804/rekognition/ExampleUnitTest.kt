package edu.uw.minh2804.rekognition

import android.content.Context
import android.graphics.Bitmap
import android.os.Looper
import android.os.Process
import android.util.Base64
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.gson.JsonObject
import edu.uw.minh2804.rekognition.extensions.toString64
import edu.uw.minh2804.rekognition.services.*
import io.mockk.*
import io.mockk.every
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.manipulation.Ordering
import java.security.AccessController.getContext

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

@ExperimentalCoroutinesApi
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
            assertEquals(expectedTextReadyForDisplay, actualTextReadyForDisplay)
        }

        @Test
        // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
        // class, whereas the real annotate method is sensitive to these changes
        fun annotate_passesCorrectJson() {
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
            val slot = slot<JsonObject>()
            coEvery {
                FirebaseFunctionsService.requestAnnotation(
                    requestBody = capture(slot)
                )
            } answers {
                println(slot.captured)
                TestUtils.TextAnnotationResponseWrapper().rawTextAnnotatedResponse
            }

            // Assert that annotate Returns the right annotation
            runBlocking {
                annotator.annotate(bitmap)
            }

            // Verify the function is called
            coVerify {
                FirebaseFunctionsService.requestAnnotation(any())
            }

            // Assert that requestAnnotation receives the right input
            TestUtils.assertJSONFormat(
                json = slot.captured,
                expectedContent = TestUtils.base64EncodedImage,
                endpoint = TestUtils.Endpoint.TEXT
            )

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
            assertEquals(expectedString, actualLabelsReadyForDisplay)
        }

        @Test
        // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
        // class, whereas the real annotate method is sensitive to these changes
        fun annotate_passesCorrectJson() {
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
            val slot = slot<JsonObject>()
            coEvery {
                FirebaseFunctionsService.requestAnnotation(
                    requestBody = capture(slot)
                )
            } answers {
                println(slot.captured)
                TestUtils.TextAnnotationResponseWrapper().rawTextAnnotatedResponse
            }

            // Assert that annotate Returns the right annotation
            runBlocking {
                annotator.annotate(bitmap)
            }

            // Verify the function is called
            coVerify {
                FirebaseFunctionsService.requestAnnotation(any())
            }

            // Assert that requestAnnotation receives the right input
            TestUtils.assertJSONFormat(
                json = slot.captured,
                expectedContent = TestUtils.base64EncodedImage,
                endpoint = TestUtils.Endpoint.TEXT
            )

            unmockkAll()
        }
    }
}
