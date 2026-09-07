package com.example.contextos.storage

interface SecureStorageManager {
    suspend fun saveSecureString(key: String, value: String)
    suspend fun getSecureString(key: String): String?
    suspend fun deleteSecureString(key: String)
}
