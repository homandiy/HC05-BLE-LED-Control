package com.homan.huang.bletoled.ui.main

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homan.huang.bletoled.common.SEC
import com.homan.huang.bletoled.common.lgd
import com.homan.huang.bletoled.device.BluetoothHelper
import com.homan.huang.bletoled.device.DeviceStatus
import com.homan.huang.bletoled.device.DeviceStatus.*
import kotlinx.coroutines.*


class MainViewModel @ViewModelInject constructor(
        val bleHelper: BluetoothHelper,
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
        val result = bleHelper.checkDeviceBonding()
        lgd("$tag Bonding...$result")

        if (result) {
            deviceStatus.postValue(CONNECTED)
            //bleHelper.discoverService()
        } else {
            deviceStatus.postValue(FAIL)
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
        bleHelper.ledSwitch("101")
    }

    fun turnOff() {
        bleHelper.ledSwitch("011")
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
}

