package com.example.proxyapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import hev.htproxy.TProxyService
import java.io.File

class ProxyVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startForeground(1, buildNotification())
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (tun != null) return

        val p = getSharedPreferences("proxy", Context.MODE_PRIVATE)
        val host = p.getString("host", "") ?: ""
        val port = p.getInt("port", 0)
        val user = p.getString("user", "") ?: ""
        val pass = p.getString("pass", "") ?: ""
        val udp = p.getBoolean("udp", false)

        val builder = Builder()
            .setSession("ProxyApp")
            .setMtu(1500)
            .addAddress("10.10.10.1", 32)
            .addRoute("0.0.0.0", 0)
            .addAddress("fd00::1", 128)
            .addRoute("::", 0)
            .addDnsServer("10.10.10.2")

        try { builder.addDisallowedApplication(packageName) } catch (_: Exception) {}

        tun = builder.establish() ?: return

        val udpMode = if (udp) "udp" else "tcp"
        val lines = mutableListOf(
            "tunnel:",
            "  mtu: 1500",
            "  ipv4: 10.10.10.1",
            "socks5:",
            "  port: $port",
            "  address: '$host'",
            "  udp: '$udpMode'"
        )
        if (user.isNotEmpty()) {
            lines.add("  username: '$user'")
            lines.add("  password: '$pass'")
        }
        lines.addAll(
            listOf(
                "mapdns:",
                "  address: 10.10.10.2",
                "  port: 53",
                "  network: 240.0.0.0",
                "  netmask: 240.0.0.0",
                "  cache-size: 10000"
            )
        )
        val yaml = lines.joinToString("\n") + "\n"

        val cfg = File(cacheDir, "tunnel.yml")
        cfg.writeText(yaml)

        val fd = tun!!.fd
        Thread {
            try {
                Log.e("ProxyApp", "starting tunnel $host:$port udp=$udp")
                TProxyService.TProxyStartService(cfg.absolutePath, fd)
            } catch (t: Throwable) {
                Log.e("ProxyApp", "tunnel failed", t)
            }
        }.start()
        running = true
    }

    private fun stopVpn() {
        try { TProxyService.TProxyStopService() } catch (_: Throwable) {}
        tun?.close()
        tun = null
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("vpn", "VPN", NotificationManager.IMPORTANCE_LOW)
        )
        return Notification.Builder(this, "vpn")
            .setContentTitle("ProxyApp")
            .setContentText("VPN is running")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.example.proxyapp.STOP"
        @Volatile var running = false
    }
}
