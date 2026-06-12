package com.trueedu.tong.repository.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "broker_credentials",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(accountId: Long, appKey: String, appSecret: String) {
        prefs.edit()
            .putString("${accountId}_appKey", appKey)
            .putString("${accountId}_appSecret", appSecret)
            .apply()
    }

    fun getAppKey(accountId: Long): String =
        prefs.getString("${accountId}_appKey", "") ?: ""

    fun getAppSecret(accountId: Long): String =
        prefs.getString("${accountId}_appSecret", "") ?: ""

    fun savePassword(accountId: Long, password: String) {
        prefs.edit()
            .putString("${accountId}_password", password)
            .apply()
    }

    fun getPassword(accountId: Long): String =
        prefs.getString("${accountId}_password", "") ?: ""

    fun deletePassword(accountId: Long) {
        prefs.edit().remove("${accountId}_password").apply()
    }

    fun deleteCredentials(accountId: Long) {
        prefs.edit()
            .remove("${accountId}_appKey")
            .remove("${accountId}_appSecret")
            .apply()
        deletePassword(accountId)
    }

    fun saveToken(accountId: Long, accessToken: String, expiredAtMs: Long) {
        prefs.edit()
            .putString("${accountId}_accessToken", accessToken)
            .putLong("${accountId}_tokenExpiredAt", expiredAtMs)
            .apply()
    }

    fun getAccessToken(accountId: Long): String =
        prefs.getString("${accountId}_accessToken", "") ?: ""

    fun getTokenExpiredAt(accountId: Long): Long =
        prefs.getLong("${accountId}_tokenExpiredAt", 0L)

    fun clearToken(accountId: Long) {
        prefs.edit()
            .remove("${accountId}_accessToken")
            .remove("${accountId}_tokenExpiredAt")
            .apply()
    }
}
