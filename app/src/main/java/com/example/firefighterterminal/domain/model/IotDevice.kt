package com.example.firefighterterminal.domain.model

import android.bluetooth.BluetoothDevice

data class IotDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val rawDevice: BluetoothDevice? = null
) {
    fun getSignalLevel(): Int = when {
        rssi > -50 -> 4
        rssi > -60 -> 3
        rssi > -70 -> 2
        rssi > -80 -> 1
        else -> 0
    }
}
