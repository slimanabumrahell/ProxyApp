package com.example.proxyapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

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

        val builder = Builder()
            .setSession("ProxyApp")
            .setMtu(1500)
            .addAddress("10.10.10.1", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("10.10.10.2")
            .addAddress("fd00::1", 128)
            .addRoute("::", 0)

        tun = builder.establish()
        running = tun != null
    }

    private fun stopVpn() {
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
