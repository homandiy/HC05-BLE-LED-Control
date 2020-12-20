package com.homan.huang.bletoled.common

object ConfigHelper {
    private const val address = "98d3:32:70b1c9"
    private const val pass = "4343"
    fun getAddress(): String = address
    fun getPass(): String = pass
}

// Bluetooth
const val BLUETOOTH_ON = "ON"
const val BLUETOOTH_OFF = "OFF"
const val BLUETOOTH_READY = "Bluetooth is ready."
const val BLUETOOTH_NOT_ON = "Bye, Bluetooth is OFF!"
const val DEVICE_NAME = "LED Control"

const val SEC = 1000L


