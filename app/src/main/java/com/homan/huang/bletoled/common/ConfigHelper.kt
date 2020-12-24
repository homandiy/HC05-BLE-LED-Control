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

// BLE code
const val UUID_LED_ON = "538688b3-54ee-36ed-a830-f7ec4b0f24bb"
const val UUID_LED_OFF = "031048cc-2fad-3d80-bc69-a35119a12f49"

const val ERR_BROKEN_PIPE = "Socket: Broken pipe"


