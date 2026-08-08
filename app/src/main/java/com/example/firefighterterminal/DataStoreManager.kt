package com.example.firefighterterminal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "firefighter_data")

class DataStoreManager(private val context: Context) {

    companion object {
        private val LAST_DEVICE = stringPreferencesKey("last_device_address")
        private val FIRE_TIMELINE = stringPreferencesKey("fire_timeline")
        private val MAP_CONFIG = stringPreferencesKey("map_config")
    }

    /** 保存最后连接的设备地址 */
    suspend fun saveLastDevice(address: String) {
        context.dataStore.edit { it[LAST_DEVICE] = address }
    }

    /** 获取最后连接的设备地址 */
    fun getLastDevice(): Flow<String?> {
        return context.dataStore.data.map { it[LAST_DEVICE] }
    }

    /** 保存火灾时间线条目 */
    suspend fun appendTimeline(event: String) {
        val ts = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "$ts  $event"
        context.dataStore.edit { prefs ->
            val current = prefs[FIRE_TIMELINE] ?: ""
            prefs[FIRE_TIMELINE] = "$entry\n$current".take(10000)
        }
    }

    /** 获取火灾时间线 */
    fun getTimeline(): Flow<String> {
        return context.dataStore.data.map { it[FIRE_TIMELINE] ?: "" }
    }

    /** 保存地图配置 JSON */
    suspend fun saveMapConfig(json: String) {
        context.dataStore.edit { it[MAP_CONFIG] = json }
    }

    /** 获取地图配置 JSON */
    fun getMapConfig(): Flow<String?> {
        return context.dataStore.data.map { it[MAP_CONFIG] }
    }

    /** 清除所有数据 */
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
