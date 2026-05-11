package com.ccec.timer.stationkiosk

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ccec.timer.stationkiosk.mqtt.MqttStationForegroundService
import com.ccec.timer.stationkiosk.ui.StationUiHub
import com.ccec.timer.stationkiosk.ui.StationUiState
import com.ccec.timer.stationkiosk.ui.UiSource
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var nativeRoot: LinearLayout
    private lateinit var textStation: TextView
    private lateinit var textTimer: TextView
    private lateinit var textMeta: TextView
    private lateinit var textStatus: TextView
    private lateinit var textBanner: TextView

    private var lastConfigKey: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        nativeRoot = findViewById(R.id.native_root)
        webView = findViewById(R.id.webview)
        textStation = findViewById(R.id.text_station)
        textTimer = findViewById(R.id.text_timer)
        textMeta = findViewById(R.id.text_meta)
        textStatus = findViewById(R.id.text_status)
        textBanner = findViewById(R.id.text_banner)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return false
            }
        }

        findViewById<View>(R.id.settings_hit).setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
                    webView.goBack()
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                StationUiHub.state.collect { state ->
                    if (state != null && KioskPrefs.useNativeMqtt(this@MainActivity, true)) {
                        bindNative(state)
                    }
                }
            }
        }

        applyDisplayMode(force = true)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        applyDisplayMode(force = false)
    }

    private fun applyDisplayMode(force: Boolean) {
        val defaultBase = getString(R.string.default_server_base_url)
        val defaultStation = getString(R.string.default_station_code)
        val base = KioskPrefs.baseUrl(this, defaultBase).trimEnd('/')
        val station = KioskPrefs.stationCode(this, defaultStation)
        val native = KioskPrefs.useNativeMqtt(this, true)
        val key = "$native|$base|$station|${KioskPrefs.mqttBrokerUri(this, "")}|${KioskPrefs.mqttTopicPrefix(this, "")}"
        if (!force && key == lastConfigKey) {
            return
        }
        lastConfigKey = key

        if (native) {
            nativeRoot.visibility = View.VISIBLE
            webView.visibility = View.GONE
            textStation.text = station
            MqttStationForegroundService.stop(this)
            MqttStationForegroundService.start(this)
        } else {
            nativeRoot.visibility = View.GONE
            webView.visibility = View.VISIBLE
            MqttStationForegroundService.stop(this)
            loadWebUrl(base, station)
        }
    }

    private fun loadWebUrl(base: String, station: String) {
        val uri = Uri.parse("$base/").buildUpon()
            .appendQueryParameter("station", station)
            .build()
        webView.loadUrl(uri.toString())
    }

    private fun bindNative(state: StationUiState) {
        val s = state.snapshot
        val remain = s.remain.coerceAtLeast(0)
        val mm = (remain / 60).toString().padStart(2, '0')
        val sec = (remain % 60).toString().padStart(2, '0')
        textTimer.text = "$mm:$sec"
        textMeta.text = "${s.so ?: "-"} / ${s.esn ?: "-"} / ${s.engineType ?: "-"}"
        textStatus.text = s.status ?: "UNKNOWN"
        applyStatusColor(s.color)
        textBanner.text = when (state.source) {
            UiSource.LIVE -> getString(R.string.banner_live)
            UiSource.CACHE -> getString(R.string.banner_cache)
            UiSource.DISCONNECTED -> getString(R.string.banner_disconnected)
        }
    }

    private fun applyStatusColor(colorName: String?) {
        val bg = when (colorName) {
            "GREEN" -> Color.parseColor("#1a3d1a")
            "YELLOW" -> Color.parseColor("#4d4000")
            "RED" -> Color.parseColor("#4d1515")
            "BLUE" -> Color.parseColor("#152a4d")
            "PURPLE" -> Color.parseColor("#301540")
            else -> Color.parseColor("#333333")
        }
        textStatus.setBackgroundColor(bg)
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
