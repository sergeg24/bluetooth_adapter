package com.example.bluetoothadapter.data

import android.bluetooth.BluetoothDevice

data class ListItem(
    var name: String,
    var address: String,
    val rssi: Int,
    var isChecked: Boolean = false,
    val device: BluetoothDevice? = null,
)