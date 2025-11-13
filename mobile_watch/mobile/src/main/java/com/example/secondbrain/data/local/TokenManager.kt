package com.example.secondbrain.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore 확장 프로퍼티
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

// JWT 토큰을 안전하게 저장하고 관리하는 클래스
class TokenManager(private val context: Context) {

    companion object {
        // DataStore 키 정의
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val TOKEN_TYPE_KEY = stringPreferencesKey("token_type")
    }

    // Access Token 저장
    suspend fun saveAccessToken(token: String, tokenType: String = "Bearer") {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = token
            preferences[TOKEN_TYPE_KEY] = tokenType
        }
    }

    // Access Token 가져오기 (Flow)
    val accessToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }

    // Access Token 가져오기 (suspend 함수)
    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN_KEY]
        }.first()
    }

    // 로그인 상태 확인
    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ACCESS_TOKEN_KEY] != null
        }

    // 토큰 삭제 (로그아웃)
    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(TOKEN_TYPE_KEY)
        }
    }
}
