package com.homan.huang.bletoled.ui.main

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homan.huang.bletoled.common.*
import com.homan.huang.bletoled.device.BluetoothHelper
import com.homan.huang.bletoled.device.DeviceStatus
import com.homan.huang.bletoled.device.DeviceStatus.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainViewModel @ViewModelInject constructor(
        private val bleHelper: BluetoothHelper,
        val deviceStatus: MutableLiveData<DeviceStatus>
) : ViewModel()  {
    private val tag = "MainVM:"

    fun checkBondedList() {
        if (bleHelper.checkBondedList()) {
            lgd("$tag Found. Move to Step 4, Bonding.")
            deviceStatus.postValue(BONDING)
        } else {
            lgd("$tag Not Found. Move to Step 3, Discovering.")
            deviceStatus.postValue(DISCOVERING)
        }
    }

    fun bonding() {
        viewModelScope.launch {
            val result = bleHelper.checkDeviceBonding()
            lgd("$tag Bonding...$result")

            if (result) {
                deviceStatus.postValue(CONNECTED)
            } else {
                deviceStatus.postValue(FAIL)
            }
        }
    }

    fun discovering() {
        lgd("$tag Discovering...")
        bleHelper.discoverDevice()
    }

    fun checkSwitch() {
        if (bleHelper.isSwitchOn()) {
            lgd("$tag Turning Bluetooth switch ON...")
            deviceStatus.postValue(SWITCHING)
        } else {
            lgd("$tag Bluetooth switch is ON.")
            deviceStatus.postValue(PAIRING)
        }
    }

    fun turnOn() {
        viewModelScope.launch {
            if (bleHelper.bleMsg != ERR_BROKEN_PIPE) {
                var bleMsg = ""
                for (i in 1..3) {
                    bleMsg = bleHelper.ledSwitch(UUID_LED_ON)
                    lgd("$tag Retry $i; received Bluetooth message: $bleMsg")
                    if (bleMsg.contains(ON)) break
                }
                if (bleMsg.contains(ON)) {
                    deviceStatus.postValue(LED_ON)
                } else {
                    deviceStatus.postValue(LED_FAIL_ON)
                }
            } else {
                lgd("Call Restart!")
                deviceStatus.postValue(RESTART)
            }
        }
    }

    fun turnOff() {
        viewModelScope.launch {
            if (bleHelper.bleMsg != ERR_BROKEN_PIPE) {
                var bleMsg = ""
                for (i in 1..3) {
                    bleMsg = bleHelper.ledSwitch(UUID_LED_OFF)
                    lgd("$tag Retry $i; received Bluetooth message: $bleMsg")
                    if (bleMsg.contains(OFF)) break
                }
                if (bleMsg.contains(OFF)) {
                    deviceStatus.postValue(LED_OFF)
                } else {
                    deviceStatus.postValue(LED_FAIL_OFF)
                }
            } else {
                lgd("Call Restart!")
                deviceStatus.postValue(RESTART)
            }
        }
    }

    fun checkNewDevice() {
        // new password entry timeout is 40s
        viewModelScope.launch {
            repeat(40) {
                deviceStatus.postValue(COUNT_DOWN)
                delay(SEC)
            }
            if (!bleHelper.checkBondedList()) {
                lgd("Check Bonded List.")
                deviceStatus.postValue(FAIL)
            }
        }
    }

    fun connected() {
        deviceStatus.postValue(CONNECTED)
    }

    companion object {
        private const val ON = "ON"
        private const val OFF = "OFF"
    }
}

