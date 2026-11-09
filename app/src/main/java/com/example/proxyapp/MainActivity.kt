package com.example.proxyapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var typeSpinner: Spinner
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private lateinit var userInput: EditText
    private lateinit var passInput: EditText
    private lateinit var udpSwitch: Switch
    private lateinit var connectBtn: Button
    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }

        root.addView(label("ProxyApp", 26f))
        root.addView(label("Proxy type", 14f))

        typeSpinner = Spinner(this)
        typeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("SOCKS5", "HTTP")
        )
        root.addView(typeSpinner)

        hostInput = field("IP / Host", InputType.TYPE_CLASS_TEXT)
        portInput = field("Port", InputType.TYPE_CLASS_NUMBER)
        userInput = field("Username", InputType.TYPE_CLASS_TEXT)
        passInput = field(
            "Password",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        root.addView(hostInput)
        root.addView(portInput)
        root.addView(userInput)
        root.addView(passInput)

        udpSwitch = Switch(this).apply {
            text = "Enable UDP"
            setPadding(0, 24, 0, 24)
        }
        root.addView(udpSwitch)

        val saveBtn = Button(this).apply { text = "Save" }
        saveBtn.setOnClickListener { save() }
        root.addView(saveBtn)

        connectBtn = Button(this)
        connectBtn.setOnClickListener { toggleConnect() }
        root.addView(connectBtn)

        val logBtn = Button(this).apply { text = "Show error log" }
        logBtn.setOnClickListener { showLog() }
        root.addView(logBtn)

        statusText = label("", 14f)
        root.addView(statusText)

        logText = TextView(this).apply {
            textSize = 11f
            setTextIsSelectable(true)
        }
        root.addView(logText)

        val scroll = ScrollView(this)
        scroll.addView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        setContentView(scroll)

        load()
        updateButton()
    }

    override fun onResume() {
        super.onResume()
        updateButton()
    }

    private fun label(text: String, size: Float) = TextView(this).apply {
        this.text = text
        textSize = size
        setPadding(0, 24, 0, 8)
    }

    private fun field(hint: String, type: Int) = EditText(this).apply {
        this.hint = hint
        inputType = type
        setSingleLine()
    }

    private fun prefs() = getSharedPreferences("proxy", Context.MODE_PRIVATE)

    private fun showLog() {
        logText.text = "Reading log..."
        Thread {
            val out = try {
                val proc = Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "-t", "300", "*:E"))
                proc.inputStream.bufferedReader().readText()
                    .takeLast(6000)
                    .ifBlank { "(log is empty)" }
            } catch (t: Throwable) {
                "Could not read log: $t"
            }
            runOnUiThread { logText.text = out }
        }.start()
    }

    private fun updateButton() {
        connectBtn.text = if (ProxyVpnService.running) "Disconnect" else "Connect"
    }

    private fun toggleConnect() {
        if (ProxyVpnService.running) {
            startService(
                Intent(this, ProxyVpnService::class.java)
                    .setAction(ProxyVpnService.ACTION_STOP)
            )
            connectBtn.text = "Connect"
            statusText.text = "Disconnected"
            return
        }

        if (prefs().getString("host", "").isNullOrEmpty()) {
            Toast.makeText(this, "Save the proxy first", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, REQ_VPN)
        } else {
            startVpn()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == RESULT_OK) startVpn()
    }

    private fun startVpn() {
        startForegroundService(Intent(this, ProxyVpnService::class.java))
        connectBtn.text = "Disconnect"
        statusText.text = "Connected"
    }

    private fun save() {
        val host = hostInput.text.toString().trim()
        val port = portInput.text.toString().trim().toIntOrNull()

        if (host.isEmpty() || port == null || port !in 1..65535) {
            Toast.makeText(this, "Enter a valid IP and port", Toast.LENGTH_SHORT).show()
            return
        }

        prefs().edit()
            .putString("type", typeSpinner.selectedItem.toString())
            .putString("host", host)
            .putInt("port", port)
            .putString("user", userInput.text.toString())
            .putString("pass", passInput.text.toString())
            .putBoolean("udp", udpSwitch.isChecked)
            .apply()

        val udpState = if (udpSwitch.isChecked) "UDP on" else "UDP off"
        statusText.text = "Saved: ${typeSpinner.selectedItem} $host:$port ($udpState)"
    }

    private fun load() {
        val p = prefs()
        val host = p.getString("host", "") ?: ""
        if (host.isEmpty()) return

        hostInput.setText(host)
        portInput.setText(p.getInt("port", 0).toString())
        userInput.setText(p.getString("user", ""))
        passInput.setText(p.getString("pass", ""))
        typeSpinner.setSelection(if (p.getString("type", "SOCKS5") == "HTTP") 1 else 0)
        udpSwitch.isChecked = p.getBoolean("udp", false)
        statusText.text = "Loaded saved proxy"
    }

    companion object {
        private const val REQ_VPN = 1
    }
}
