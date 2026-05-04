package com.flash.smasharena.domain.repository

interface AuthRepository {
    val isSignedIn: Boolean
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun createAccountWithEmail(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    fun signOut()
}
