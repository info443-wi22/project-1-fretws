package edu.uw.minh2804.rekognition

import android.graphics.Bitmap
import android.os.Looper
import android.util.Base64
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
        fun annotate_withoutMocking_returnsCorrectText() {
            val actualTextAnnotatedResponse = runBlocking {
                annotator.annotate(TestUtils.realBitmapForTextEndpoint)
            }
            val expectedTextAnnotatedResponse = TestUtils.TextAnnotationResponseWrapper().rawTextAnnotatedResponse
            assertEquals(expectedTextAnnotatedResponse, actualTextAnnotatedResponse)
        }

        @Test
        fun annotate_works() {
            val firebaseFunctions = spyk<FirebaseFunctionsService>()
            val slot = slot<JsonObject>()

            coEvery {
                firebaseFunctions.requestAnnotation(
                    requestBody = capture(slot)
                )
            } answers {
                println(slot.captured)
                TestUtils.TextAnnotationResponseWrapper().rawTextAnnotatedResponse
            }

//            annotator.annotate(TestUtils.smallBitmap)
            runBlocking {
                annotator.annotate(Bitmap.createBitmap(32, 32, Bitmap.Config.ALPHA_8))
            }
        }

        @get:Rule
        var coroutinesTestRule = CoroutineTestRule()

        @Test
        // Caution: This test is not sensitive to changes in the toString64 extension of the Bitmap
        // class, whereas the real annotate method is sensitive to these changes
        fun annotate_passesCorrectJson() = coroutinesTestRule.testDispatcher.runBlockingTest {
            mockkStatic(Base64::class)
            every { Base64.encodeToString(any(), any()) } returns TestUtils.base64EncodedImage
//            val bitmap = spyk<Bitmap>()
            val bitmap = mockk<Bitmap>(relaxed = true)
//             mock Base64
            every {
                // toString64 is an extension of the bitmap class and should be tested outside of
                // this test suite, because it is technically subject to change
                bitmap.toString64()
            } returns TestUtils.base64EncodedImage

            val functionsService = spyk<FirebaseFunctionsService>()
            val slot = slot<JsonObject>()
            coEvery {
                functionsService.requestAnnotation(
                    requestBody = capture(slot)
                )
            } answers {
                println(slot.captured)
                TestUtils.TextAnnotationResponseWrapper().rawTextAnnotatedResponse
            }

            mockkStatic(Looper::class)
            every {
                Looper.getMainLooper()
            } returns null

            // Assert that annotate Returns the right annotation
            runBlocking {
//                annotator.annotate(TestUtils.smallBitmap)
                annotator.annotate(bitmap)
            }


            // Verify that requestAnnotation receives the right input
//            coVerify {
//                functionsService.requestAnnotation()
//            }

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
            assertEquals(expectedString, actualLabelsReadyForDisplay)
        }
    }

    @ExperimentalCoroutinesApi
    class CoroutineTestRule(val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()) : TestWatcher() {
        val testDispatcherProvider = object : TestUtils.DispatcherProvider {
            override fun default(): CoroutineDispatcher = testDispatcher
            override fun io(): CoroutineDispatcher = testDispatcher
            override fun main(): CoroutineDispatcher = testDispatcher
            override fun unconfined(): CoroutineDispatcher = testDispatcher
        }

        override fun starting(description: Description?) {
            super.starting(description)
            Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description?) {
            super.finished(description)
            Dispatchers.resetMain()
            testDispatcher.cleanupTestCoroutines()
        }
    }
}
