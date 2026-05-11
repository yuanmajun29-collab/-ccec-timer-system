package com.ccec.timer.stationkiosk

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setTitle(R.string.settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(R.layout.activity_settings)

        val defaultBase = getString(R.string.default_server_base_url)
        val defaultStation = getString(R.string.default_station_code)
        val defaultMqtt = getString(R.string.default_mqtt_broker)

        val editBase = findViewById<EditText>(R.id.edit_base_url)
        val editStation = findViewById<EditText>(R.id.edit_station)
        val switchNative = findViewById<SwitchCompat>(R.id.switch_native_mqtt)
        val editMqtt = findViewById<EditText>(R.id.edit_mqtt_broker)
        val editUser = findViewById<EditText>(R.id.edit_mqtt_user)
        val editPass = findViewById<EditText>(R.id.edit_mqtt_pass)
        val editTopic = findViewById<EditText>(R.id.edit_topic_prefix)

        editBase.setText(KioskPrefs.baseUrl(this, defaultBase))
        editStation.setText(KioskPrefs.stationCode(this, defaultStation))
        switchNative.isChecked = KioskPrefs.useNativeMqtt(this, true)
        editMqtt.setText(KioskPrefs.mqttBrokerUri(this, defaultMqtt))
        editUser.setText(KioskPrefs.mqttUser(this).orEmpty())
        editPass.setText(KioskPrefs.mqttPassword(this).orEmpty())
        editTopic.setText(KioskPrefs.mqttTopicPrefix(this, "ccec/station"))

        findViewById<Button>(R.id.button_save).setOnClickListener {
            val base = editBase.text.toString().trim().trimEnd('/')
            val station = editStation.text.toString().trim()
            val nativeOn = switchNative.isChecked
            val mqttBroker = editMqtt.text.toString().trim()
            val topicPrefix = editTopic.text.toString().trim().trimEnd('/')
            val user = editUser.text.toString().trim().takeIf { it.isNotEmpty() }
            val pass = editPass.text.toString().takeIf { it.isNotEmpty() }

            if (station.isEmpty()) {
                Toast.makeText(this, "请填写工位编码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!nativeOn && (base.isEmpty() || (!base.startsWith("http://") && !base.startsWith("https://")))) {
                Toast.makeText(this, "Web 模式需填写以 http(s):// 开头的服务器地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nativeOn && (mqttBroker.isEmpty() || (!mqttBroker.startsWith("tcp://") && !mqttBroker.startsWith("ssl://") && !mqttBroker.startsWith("mqtt://") && !mqttBroker.startsWith("mqtts://")))) {
                Toast.makeText(this, "原生 MQTT 需填写 Broker（tcp/ssl/mqtt 开头）", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (topicPrefix.isEmpty()) {
                Toast.makeText(this, "请填写 MQTT topic 前缀", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val httpBase = if (base.isEmpty()) defaultBase else base
            KioskPrefs.saveFull(
                this,
                httpBase,
                station,
                nativeOn,
                if (mqttBroker.isEmpty()) defaultMqtt else mqttBroker,
                user,
                pass,
                topicPrefix
            )
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
