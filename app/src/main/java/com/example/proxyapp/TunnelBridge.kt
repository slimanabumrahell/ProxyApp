package com.example.proxyapp

object TunnelBridge {
    init { System.loadLibrary("hev-jni") }
    @JvmStatic external fun start(configPath: String, fd: Int)
    @JvmStatic external fun stop()
}
