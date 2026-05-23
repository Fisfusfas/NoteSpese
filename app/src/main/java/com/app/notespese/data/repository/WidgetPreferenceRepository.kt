package com.app.notespese.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

private val KEY_WIDGET_GRUPPO_ID = stringPreferencesKey("widget_gruppo_id")

@Singleton
class WidgetPreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val widgetGruppoId: Flow<String> = context.widgetPrefsDataStore.data
        .map { it[KEY_WIDGET_GRUPPO_ID] ?: "" }

    suspend fun setWidgetGruppoId(gruppoId: String) {
        context.widgetPrefsDataStore.edit { it[KEY_WIDGET_GRUPPO_ID] = gruppoId }
    }

    suspend fun getWidgetGruppoId(): String =
        context.widgetPrefsDataStore.data.first()[KEY_WIDGET_GRUPPO_ID] ?: ""
}
