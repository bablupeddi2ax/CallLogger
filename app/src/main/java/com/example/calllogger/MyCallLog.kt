package com.example.calllogger

import android.os.Parcel
import android.os.Parcelable

data class MyCallLog(
    val number: String,
    val name: String?,
    val duration: String,
    val type: String,
    val date: String
)