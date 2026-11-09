package com.example.proxyapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "ProxyApp is working"
        tv.textSize = 22f
        tv.setPadding(48, 96, 48, 48)
        setContentView(tv)
    }
}
