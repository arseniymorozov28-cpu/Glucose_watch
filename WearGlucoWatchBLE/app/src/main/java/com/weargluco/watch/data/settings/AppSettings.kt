package com.weargluco.watch.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings(private val context: Context) {

    companion object {
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_PASSWORD = stringPreferencesKey("password")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_TOKEN_EXPIRES = stringPreferencesKey("token_expires")
        private val KEY_PATIENT_ID = stringPreferencesKey("patient_id")
        private val KEY_PATIENT_NAME = stringPreferencesKey("patient_name")
        private val KEY_REGION = stringPreferencesKey("region")
        private val KEY_ACCOUNT_ID = stringPreferencesKey("account_id")
        private val KEY_LATEST_GLUCOSE = stringPreferencesKey("latest_glucose")
        private val KEY_LATEST_TREND = stringPreferencesKey("latest_trend")
        private val KEY_LATEST_TREND_LABEL = stringPreferencesKey("latest_trend_label")
        private val KEY_LATEST_TIMESTAMP = stringPreferencesKey("latest_timestamp")
        private val KEY_LATEST_TARGET_LOW = stringPreferencesKey("latest_target_low")
        private val KEY_LATEST_TARGET_HIGH = stringPreferencesKey("latest_target_high")
    }

    val email: Flow<String> = context.dataStore.data.map { it[KEY_EMAIL] ?: "" }
    val password: Flow<String> = context.dataStore.data.map { it[KEY_PASSWORD] ?: "" }
    val token: Flow<String> = context.dataStore.data.map { it[KEY_TOKEN] ?: "" }
    val tokenExpires: Flow<String> = context.dataStore.data.map { it[KEY_TOKEN_EXPIRES] ?: "0" }
    val patientId: Flow<String> = context.dataStore.data.map { it[KEY_PATIENT_ID] ?: "" }
    val patientName: Flow<String> = context.dataStore.data.map { it[KEY_PATIENT_NAME] ?: "" }
    val region: Flow<String> = context.dataStore.data.map { it[KEY_REGION] ?: "eu" }
    val accountId: Flow<String> = context.dataStore.data.map { it[KEY_ACCOUNT_ID] ?: "" }
    val latestGlucose: Flow<String> = context.dataStore.data.map { it[KEY_LATEST_GLUCOSE] ?: "" }
    val latestTrend: Flow<String> = context.dataStore.data.map { it[KEY_LATEST_TREND] ?: "" }
    val latestTrendLabel: Flow<String> = context.dataStore.data.map { it[KEY_LATEST_TREND_LABEL] ?: "" }
    val latestTimestamp: Flow<String> = context.dataStore.data.map { it[KEY_LATEST_TIMESTAMP] ?: "" }
    val latestTargetLow: Flow<String> = context.dataStore.data.map { it[KEY_LATEST_TARGET_LOW] ?: "" }
    val latestTargetHigh: Flow<String> = context.dataStore.data.map { it[KEY_LATEST_TARGET_HIGH] ?: "" }

    suspend fun saveCredentials(email: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EMAIL] = email
            prefs[KEY_PASSWORD] = password
        }
    }

    suspend fun saveToken(token: String, expires: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_TOKEN_EXPIRES] = expires.toString()
        }
    }

    suspend fun savePatientInfo(patientId: String, patientName: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PATIENT_ID] = patientId
            prefs[KEY_PATIENT_NAME] = patientName
        }
    }

    suspend fun saveRegion(region: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REGION] = region
        }
    }

    suspend fun saveAccountId(accountId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCOUNT_ID] = accountId
        }
    }

    suspend fun saveLatestGlucose(
        value: Double,
        trendSymbol: String,
        trendLabel: String,
        timestamp: String,
        targetLow: Double,
        targetHigh: Double
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LATEST_GLUCOSE] = value.toString()
            prefs[KEY_LATEST_TREND] = trendSymbol
            prefs[KEY_LATEST_TREND_LABEL] = trendLabel
            prefs[KEY_LATEST_TIMESTAMP] = timestamp
            prefs[KEY_LATEST_TARGET_LOW] = targetLow.toString()
            prefs[KEY_LATEST_TARGET_HIGH] = targetHigh.toString()
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean {
        val t = context.dataStore.data.first()[KEY_TOKEN] ?: ""
        return t.isNotEmpty()
    }
}
