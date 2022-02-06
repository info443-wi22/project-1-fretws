package edu.uw.minh2804.rekognition

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import edu.uw.minh2804.rekognition.services.FirebaseAuthService
import edu.uw.minh2804.rekognition.services.FirebaseFunctionsService
import edu.uw.minh2804.rekognition.services.ObjectRecognitionRequest
import io.mockk.*
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MockkElementsTestSuite {
    object ObjBeingMocked {
        fun add(a: Int, b: Int) = a + b
    }

    object FirebaseWrapper {
        val num = 5

        fun add(a: Int, b: Int) = a + b
        fun subtract5(a: Int) = sub(a, 5)
        fun subtract_numFrom(a: Int) = sub(a, _num)
        fun return_reqs() = _reqs
        fun return_funcs() = _funcs
        suspend fun suspendFun(except: Boolean = false): Int {
            val result = suspendCoroutine<Int> { continuation ->
                Thread.sleep(1000)
                continuation.resume(15)
            }
            return result
        }

        private val _num
            get() = 10
        private fun sub(a: Int, b: Int) = a - b

        private val _reqs
            get() = ObjectRecognitionRequest
//        private val _funcs = FirebaseFunctionsService
        private val _funcs
            get() = FirebaseFunctionsService
    }

    @Test
    fun mockkFirebaseWrapper() {
        val obj = spyk<FirebaseWrapper>()

        // Mock that passes through to original function
        every {
            obj.add(any(), any())
        } answers {callOriginal()}

        assertEquals(
            3, obj.add(1, 2)
        )

        // Specific mock parameters return non-default behavior
        every {
            obj.add(1, 2)
        } returns 55

        assertEquals(
            55, obj.add(1, 2)
        )

        // Mock a private function
        every {
            obj["sub"](ofType(Int::class), 5)
        } returns 100

        // subtract5 calls the mocked private function
        assertEquals(100, obj.subtract5(12))

        // Mock a public property
        every {
            obj getProperty "num"
        } returns 33

        assertEquals(33, obj.num)

        // Mock a private property
        every {
            obj getProperty "_num"
        } returns 22

        assertEquals(10, obj.subtract_numFrom(32))

        // Mock a private object property
        every {
            obj getProperty "_reqs"
        } returns null

        assertNull(obj.return_reqs())

        // Mock a private firebase property
        every {
            obj getProperty "_funcs"
        } returns null

        assertNull(obj.return_funcs())

        coEvery {
            obj.suspendFun()
        } returns 2

        val suspendActual = runBlocking {obj.suspendFun()}
        assertEquals(2, suspendActual)
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

//    @Test
//    fun requestAnnotation_canBeMocked() {
//        mockkObject(FirebaseFunctionsService)
//        val expectedOutput = AnnotateImageResponse(null, TestUtils.realObjectRecognitionData)
//        coEvery {
//            FirebaseFunctionsService.requestAnnotation(any())
//        } returns expectedOutput
//        val emptyRequest = ObjectRecognitionRequest.createRequest("")
//        lateinit var mockedAnnotation: AnnotateImageResponse
//        runBlocking {
//            mockedAnnotation = FirebaseFunctionsService.requestAnnotation(emptyRequest)
//        }
//        assertEquals(mockedAnnotation, expectedOutput)
//    }

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

