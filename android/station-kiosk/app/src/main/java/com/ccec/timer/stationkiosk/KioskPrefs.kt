package com.ccec.timer.stationkiosk

import android.content.Context

object KioskPrefs {
    private const val NAME = "ccec_station_kiosk"

    const val KEY_BASE_URL = "base_url"
    const val KEY_STATION = "station_code"
    const val KEY_USE_NATIVE_MQTT = "use_native_mqtt"
    const val KEY_MQTT_BROKER = "mqtt_broker_uri"
    const val KEY_MQTT_USER = "mqtt_user"
    const val KEY_MQTT_PASSWORD = "mqtt_password"
    const val KEY_MQTT_TOPIC_PREFIX = "mqtt_topic_prefix"

    fun baseUrl(context: Context, default: String): String =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, null)?.trim().orEmpty()
            .ifEmpty { default }

    fun stationCode(context: Context, default: String): String =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_STATION, null)?.trim().orEmpty()
            .ifEmpty { default }

    fun useNativeMqtt(context: Context, default: Boolean = true): Boolean =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_NATIVE_MQTT, default)

    fun mqttBrokerUri(context: Context, default: String): String =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_MQTT_BROKER, null)?.trim().orEmpty()
            .ifEmpty { default }

    fun mqttUser(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_MQTT_USER, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun mqttPassword(context: Context): String? =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_MQTT_PASSWORD, null)?.takeIf { !it.isNullOrEmpty() }

    fun mqttTopicPrefix(context: Context, default: String): String =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .getString(KEY_MQTT_TOPIC_PREFIX, null)?.trim().orEmpty()
            .ifEmpty { default }

    fun saveWeb(context: Context, baseUrl: String, stationCode: String) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_URL, baseUrl.trim())
            .putString(KEY_STATION, stationCode.trim())
            .apply()
    }

    fun saveFull(
        context: Context,
        baseUrl: String,
        stationCode: String,
        useNativeMqtt: Boolean,
        mqttBroker: String,
        mqttUser: String?,
        mqttPassword: String?,
        mqttTopicPrefix: String
    ) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_URL, baseUrl.trim())
            .putString(KEY_STATION, stationCode.trim())
            .putBoolean(KEY_USE_NATIVE_MQTT, useNativeMqtt)
            .putString(KEY_MQTT_BROKER, mqttBroker.trim())
            .putString(KEY_MQTT_USER, mqttUser?.trim())
            .putString(KEY_MQTT_PASSWORD, mqttPassword)
            .putString(KEY_MQTT_TOPIC_PREFIX, mqttTopicPrefix.trim().trimEnd('/'))
            .apply()
    }
}
