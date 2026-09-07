package com.example.motogokmp

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object AppContext {
    lateinit var instance: Context
}