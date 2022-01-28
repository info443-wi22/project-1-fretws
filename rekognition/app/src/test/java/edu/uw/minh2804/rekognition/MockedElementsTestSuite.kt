package edu.uw.minh2804.rekognition

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import edu.uw.minh2804.rekognition.services.FirebaseAuthService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito

class MockkElementsTestSuite {
    object ObjBeingMocked {
        fun add(a: Int, b: Int) = a + b
    }

    @Test
    fun mockkObjectExampleTest() {
        mockkObject(ObjBeingMocked) // applies mocking to an Object

        assertEquals(3, ObjBeingMocked.add(1, 2))

        every { ObjBeingMocked.add(1, 2) } returns 55

        assertEquals(55, ObjBeingMocked.add(1, 2))
    }

    @Test
    fun firebaseAuth_withMockkObject() {
        mockkObject(Firebase)
//        every { Firebase.auth } returns FirebaseAuth()
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

