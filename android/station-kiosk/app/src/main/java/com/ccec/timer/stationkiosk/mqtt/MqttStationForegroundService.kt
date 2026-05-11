package com.ccec.timer.stationkiosk.mqtt

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.ccec.timer.stationkiosk.DeviceIdentity
import com.ccec.timer.stationkiosk.KioskPrefs
import com.ccec.timer.stationkiosk.BuildConfig
import com.ccec.timer.stationkiosk.MainActivity
import com.ccec.timer.stationkiosk.R
import com.ccec.timer.stationkiosk.data.AppDatabase
import com.ccec.timer.stationkiosk.data.CachedSnapshot
import com.ccec.timer.stationkiosk.model.StationSnapshotDto
import com.ccec.timer.stationkiosk.ui.StationUiHub
import com.ccec.timer.stationkiosk.ui.StationUiState
import com.ccec.timer.stationkiosk.ui.UiSource
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets

/**
 * 原生 MQTT：订阅工位快照、设备管控指令；缓存快照到 Room；定时上报告警遥测。
 */
class MqttStationForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val gson = Gson()
    private val io = Dispatchers.IO
    private var client: MqttClient? = null
    private val telemetryHandler = Handler(Looper.getMainLooper())
    private var telemetryRunnable: Runnable? = null
    @Volatile
    private var lastLiveAtMillis: Long = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pending = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_mqtt_title))
            .setContentText(getString(R.string.notif_mqtt_text))
            .setSmallIcon(R.drawable.ic_app)
            .setContentIntent(pending)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notification)

        val station = KioskPrefs.stationCode(this, getString(R.string.default_station_code))
        val broker = normalizeBrokerUri(KioskPrefs.mqttBrokerUri(this, getString(R.string.default_mqtt_broker)))
        val topicPrefix = KioskPrefs.mqttTopicPrefix(this, "ccec/station")
        val user = KioskPrefs.mqttUser(this)
        val pass = KioskPrefs.mqttPassword(this)
        val deviceId = DeviceIdentity.getId(this)

        scope.launch(io) {
            try {
                client?.disconnect()
            } catch (_: Exception) {
            }
            client = null
            connectAndSubscribe(broker, station, topicPrefix, user, pass, deviceId)
        }
        scheduleTelemetry(deviceId, station)
        return START_STICKY
    }

    override fun onDestroy() {
        telemetryRunnable?.let { telemetryHandler.removeCallbacks(it) }
        try {
            client?.disconnect()
        } catch (_: Exception) {
        }
        client = null
        super.onDestroy()
    }

    private fun connectAndSubscribe(
        brokerUri: String,
        station: String,
        topicPrefix: String,
        user: String?,
        pass: String?,
        deviceId: String
    ) {
        lastLiveAtMillis = 0L
        val snapshotTopic = "$topicPrefix/$station/snapshot"
        val cmdTopic = "ccec/device/$deviceId/cmd"
        try {
            val cid = mqttClientId(deviceId)
            val c = MqttClient(brokerUri, cid, MemoryPersistence())
            val opts = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
                if (!user.isNullOrBlank()) {
                    userName = user
                    password = pass?.toCharArray()
                }
            }
            c.setCallback(object : MqttCallbackExtended {
                override fun connectionLost(cause: Throwable?) {
                    scope.launch(io) {
                        emitCacheOrDisconnected(station)
                    }
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    val payload = String(message.payload, StandardCharsets.UTF_8)
                    scope.launch(io) {
                        when {
                            topic.endsWith("/snapshot") -> handleSnapshotPayload(station, payload)
                            topic.endsWith("/cmd") -> handleDeviceCommand(payload)
                            else -> {}
                        }
                    }
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {}

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    scope.launch(io) {
                        preloadCache(station)
                    }
                }
            })
            c.connect(opts)
            c.subscribe(snapshotTopic, 1)
            c.subscribe(cmdTopic, 1)
            client = c
        } catch (e: Exception) {
            scope.launch(io) { emitCacheOrDisconnected(station) }
        }
    }

    private suspend fun handleSnapshotPayload(station: String, payload: String) {
        try {
            val dto = gson.fromJson(payload, StationSnapshotDto::class.java)
            val db = AppDatabase.get(applicationContext)
            db.snapshotDao().upsert(
                CachedSnapshot(stationCode = station, jsonPayload = payload, updatedAtMillis = System.currentTimeMillis())
            )
            lastLiveAtMillis = System.currentTimeMillis()
            StationUiHub.emit(StationUiState(dto, UiSource.LIVE, lastLiveAtMillis))
        } catch (_: Exception) {
            emitCacheOrDisconnected(station)
        }
    }

    private suspend fun preloadCache(station: String) {
        val db = AppDatabase.get(applicationContext)
        val row = db.snapshotDao().getByStation(station) ?: return
        try {
            val dto = gson.fromJson(row.jsonPayload, StationSnapshotDto::class.java)
            if (lastLiveAtMillis == 0L) {
                StationUiHub.emit(StationUiState(dto, UiSource.CACHE, row.updatedAtMillis))
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun emitCacheOrDisconnected(station: String) {
        val db = AppDatabase.get(applicationContext)
        val row = db.snapshotDao().getByStation(station)
        if (row != null) {
            try {
                val dto = gson.fromJson(row.jsonPayload, StationSnapshotDto::class.java)
                StationUiHub.emit(StationUiState(dto, UiSource.CACHE, row.updatedAtMillis))
                return
            } catch (_: Exception) {
            }
        }
        StationUiHub.emit(
            StationUiState(
                StationSnapshotDto(stationCode = station, status = "OFFLINE"),
                UiSource.DISCONNECTED,
                System.currentTimeMillis()
            )
        )
    }

    private suspend fun handleDeviceCommand(payload: String) {
        val root = try {
            gson.fromJson(payload, JsonObject::class.java)
        } catch (_: Exception) {
            return
        }
        val cmd = root.getAsJsonPrimitive("cmd")?.asString ?: return
        val db = AppDatabase.get(applicationContext)
        when (cmd.uppercase()) {
            "CLEAR_CACHE" -> db.snapshotDao().clearAll()
            "RECONNECT", "RELOAD_CONFIG" -> {
                Handler(Looper.getMainLooper()).post {
                    stop(applicationContext)
                    start(applicationContext)
                }
            }
            else -> {}
        }
    }

    private fun scheduleTelemetry(deviceId: String, station: String) {
        telemetryRunnable?.let { telemetryHandler.removeCallbacks(it) }
        val r = object : Runnable {
            override fun run() {
                publishTelemetry(deviceId, station)
                telemetryHandler.postDelayed(this, TELEMETRY_INTERVAL_MS)
            }
        }
        telemetryRunnable = r
        telemetryHandler.postDelayed(r, TELEMETRY_INTERVAL_MS)
    }

    private fun publishTelemetry(deviceId: String, station: String) {
        val c = client ?: return
        if (!c.isConnected) return
        try {
            val topic = "ccec/device/$deviceId/telemetry"
            val age = if (lastLiveAtMillis > 0) System.currentTimeMillis() - lastLiveAtMillis else -1
            val json = JsonObject().apply {
                addProperty("deviceId", deviceId)
                addProperty("stationCode", station)
                addProperty("appVersion", BuildConfig.VERSION_NAME)
                addProperty("mqttConnected", c.isConnected)
                addProperty("lastSnapshotAgeMs", age)
                addProperty("mode", "NATIVE_MQTT")
            }.toString()
            val msg = MqttMessage(json.toByteArray(StandardCharsets.UTF_8))
            msg.qos = 0
            c.publish(topic, msg)
        } catch (_: Exception) {
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(ch)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ccec_mqtt_station"
        private const val NOTIF_ID = 7001
        private const val TELEMETRY_INTERVAL_MS = 60_000L

        /** MQTT 3.1 clientId ≤ 23 字符 */
        fun mqttClientId(deviceId: String): String {
            val compact = deviceId.replace("-", "")
            val suffix = compact.take(18)
            return ("ccec$suffix").take(23)
        }

        fun normalizeBrokerUri(raw: String): String {
            var u = raw.trim()
            if (u.startsWith("mqtt://", ignoreCase = true)) {
                u = "tcp://" + u.substring(7)
            }
            if (u.startsWith("mqtts://", ignoreCase = true)) {
                u = "ssl://" + u.substring(8)
            }
            return u
        }

        fun start(ctx: Context) {
            val i = Intent(ctx, MqttStationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, MqttStationForegroundService::class.java))
        }
    }
}
