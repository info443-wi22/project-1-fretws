package edu.uw.minh2804.rekognition

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import edu.uw.minh2804.rekognition.services.AnnotateImageResponse
import edu.uw.minh2804.rekognition.services.FirebaseAuthService
import edu.uw.minh2804.rekognition.services.FirebaseFunctionsService
import edu.uw.minh2804.rekognition.services.ObjectRecognitionRequest
import io.mockk.*
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito

class MockkElementsTestSuite {
    object ObjBeingMocked {
        fun add(a: Int, b: Int) = a + b
    }

    object FirebaseWrapper {
        //        private val reqs = ObjectRecognitionRequest
        private val funcs = FirebaseFunctionsService
        fun add(a: Int, b: Int) = a + b
    }

    @Test
    fun mockkFirebaseWrapper() {
//        val obj = mockk<ObjBeingMocked>(relaxed = true)
        val obj = spyk<FirebaseWrapper>()

        every {
            obj.add(any(), any())
        } answers {callOriginal()}

        assertEquals(
            3, obj.add(1, 2)
        )

        every {
            obj.add(1, 2)
        } returns 55

        assertEquals(
            55, obj.add(1, 2)
        )
    }

    @Test
    fun mockkObjectExampleTest() {
        mockkObject(ObjBeingMocked) // applies mocking to an Object

        assertEquals(3, ObjBeingMocked.add(1, 2))

        every { ObjBeingMocked.add(1, 2) } returns 55

        assertEquals(55, ObjBeingMocked.add(1, 2))
    }

    @Test
    fun mockkObjectExampleTestDifferentSyntax() {
//        val obj = mockk<ObjBeingMocked>(relaxed = true)
        val obj = mockk<ObjBeingMocked>()

        every {
            obj.add(any(), any())
        } answers {callOriginal()}

        assertEquals(
            3, obj.add(1, 2)
        )

        every {
            obj.add(1, 2)
        } returns 55

        assertEquals(
            55, obj.add(1, 2)
        )
    }

    @Test
    fun requestAnnotation_canBeMocked() {
        mockkObject(FirebaseFunctionsService)
        val expectedOutput = AnnotateImageResponse(null, TestUtils.realObjectRecognitionData)
        coEvery {
            FirebaseFunctionsService.requestAnnotation(any())
        } returns expectedOutput
        val emptyRequest = ObjectRecognitionRequest.createRequest("")
        lateinit var mockedAnnotation: AnnotateImageResponse
        runBlocking {
            mockedAnnotation = FirebaseFunctionsService.requestAnnotation(emptyRequest)
        }
        assertEquals(mockedAnnotation, expectedOutput)
    }

    @Test
    fun firebaseAuth_withMockkObject() {
        mockkObject(Firebase)
        every {
            Firebase.auth
        } returns object: FirebaseAuth(FirebaseApp.getInstance()) { }
        mockkObject(FirebaseAuthService)
        every { FirebaseAuthService.isAuthenticated() } returns true
        assertTrue(FirebaseAuthService.isAuthenticated())
        unmockkAll()
    }

    @Test
    fun firebaseAuth_withMockk() {
        val mockFirebaseAuthService: FirebaseAuthService = mockk()
        every { mockFirebaseAuthService.isAuthenticated() } returns true
        assertTrue(FirebaseAuthService.isAuthenticated())
    }
}

class MockitoElementsTestSuite {
    @Test
    fun firebaseAuth_isAuthenticated_returnsTrue() {
        val mockFirebase = Mockito.mock(Firebase::class.java)
        Mockito.`when`(
            mockFirebase.auth
        ).thenReturn(null)
        val mockFirebaseAuthService = Mockito.mock(FirebaseAuthService::class.java)
        Mockito.`when`(mockFirebaseAuthService.isAuthenticated()).thenReturn(true)
        assertTrue(mockFirebaseAuthService.isAuthenticated())
    }
}

