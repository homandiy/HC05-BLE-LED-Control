    package com.homan.huang.bletoled.device

import android.bluetooth.BluetoothAdapter.*
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Parcelable
import androidx.lifecycle.Lifecycle.Event.*
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.homan.huang.bletoled.common.lgd
import com.homan.huang.bletoled.common.lge
import com.homan.huang.bletoled.device.DeviceStatus.*

class BleHc05Observer(
        private val context: Context,
        devAddr: String,
        devStatus: MutableLiveData<DeviceStatus>
) : LifecycleObserver {
    val mRegex = "[^A-Za-z0-9 ]".toRegex()
    private var hc05Address = ""

    init {
        hc05Address = mRegex.replace(devAddr, "").toUpperCase()
    }

    //region Bluetooth Discovery
    private val bleFilter = IntentFilter()
    private var devfound = false
    private lateinit var mBleDevice: BluetoothDevice

    private val bleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {
                ACTION_DISCOVERY_STARTED -> {
                    lgd(tag + "Searching for device...")
                }
                ACTION_DISCOVERY_FINISHED -> {
                    if (devfound) {
                        devStatus.postValue(DISCOVERED)

                        lgd("$tag Device Found!")
                        mBleDevice.createBond()

                        lgd("$tag Device Bond state:" +
                                " ${mBleDevice.bondState}")

                    } else {
                        lgd("$tag Device NOT Found!")
                        devStatus.postValue(NOT_FOUND)
                    }

                    lgd(tag + "Finish search!!! Found? $devfound")
                }
                ACTION_FOUND -> {
                    //bluetooth device found
                    val device =
                            intent.getParcelableExtra<Parcelable>(
                                    EXTRA_DEVICE
                            ) as BluetoothDevice?

                    lgd(tag + "Found: ${device!!.name} at" +
                            " ${device.address}")
                    val mAddress = mRegex.replace(
                            device.address, "").toUpperCase()

                    // show address
                    lgd("$tag===> H05 -- $hc05Address   ::" +
                            "  Adv -- $mAddress")
                    if (mAddress == hc05Address) {
                        lgd("$tag!!!   IcU :-) Found device   !!!")
                        devfound = true
                        mBleDevice = device
                        getDefaultAdapter().cancelDiscovery()
                    }
                }
                ACTION_BOND_STATE_CHANGED -> {
                    lgd(tag + "Checking Bonded State...")
                    val device: BluetoothDevice =
                            intent.getParcelableExtra<Parcelable>(
                                    EXTRA_DEVICE
                            ) as BluetoothDevice

                    when (device.bondState) {
                        BOND_BONDED -> {
                            lgd("$tag Bonded to device")
                            devStatus.postValue(BONDED)
                        }
                        BOND_BONDING -> {
                            lgd("$tag Bonding to device")
                            devStatus.postValue(BONDING)
                        }
                        BOND_NONE -> {
                            lge("$tag Nothing has bonded.")
                            devStatus.postValue(FAIL)
                        }
                    }
                }
            }
        }
    }
    //endregion

    //region Bluetooth Connection
    private val connectionFilter = IntentFilter()
    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_ACL_CONNECTED -> {
                    lgd(tag + "ACL:  Connected!")
                }
                ACTION_ACL_DISCONNECTED -> {
                    lge(tag + "ACL:  Disconnected.")
                }
            }
        }
    }
    //endregion

    //region Lifecycle Event
    @OnLifecycleEvent(ON_CREATE)
    fun onCreate() {
        lgd("$tag Register broadcast receivers...")
        bleFilter.addAction(ACTION_FOUND)
        bleFilter.addAction(ACTION_DISCOVERY_STARTED)
        bleFilter.addAction(ACTION_DISCOVERY_FINISHED)
        bleFilter.addAction(ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bleReceiver, bleFilter)

        connectionFilter.addAction(ACTION_ACL_CONNECTED)
        connectionFilter.addAction(ACTION_ACL_DISCONNECTED)
        context.registerReceiver(connectionReceiver, connectionFilter)
    }

    @OnLifecycleEvent(ON_RESUME)
    fun onResume() {
        try {
            lgd("$tag ==> Registered bleReceiver.")
            context.registerReceiver(bleReceiver, bleFilter)
        } catch (e: IllegalArgumentException) {
            lge("$tag bleReceiver has registered. Err: ${e.message}")
        }

        try {
            lgd("$tag ==> Registered connectionReceiver.")
            context.registerReceiver(connectionReceiver, connectionFilter)
        } catch (e: IllegalArgumentException) {
            lge("$tag connectionReceiver has registered. Err: ${e.message}")
        }
    }

    @OnLifecycleEvent(ON_PAUSE)
    fun onPause() {
        try {
            lgd("$tag ==> unRegistered bleReceiver.")
            context.unregisterReceiver(bleReceiver)
        } catch (e: java.lang.IllegalArgumentException) {
            lge("$tag bleReceiver has unregistered. Err: ${e.message}")
        }
        try {
            lgd("$tag ==> unRegistered connectionReceiver.")
            context.unregisterReceiver(connectionReceiver)
        } catch (e: IllegalArgumentException) {
            lge("$tag connectionReceiver has unregistered. Err: ${e.message}")
        }
    }

    @OnLifecycleEvent(ON_DESTROY)
    fun onDestroy() {
        try {
            lgd("$tag ==> unRegistered bleReceiver.")
            context.unregisterReceiver(bleReceiver)
        } catch (e: java.lang.IllegalArgumentException) {
            lge("$tag bleReceiver has unregistered. Err: ${e.message}")
        }
        try {
            lgd("$tag ==> unRegistered connectionReceiver.")
            context.unregisterReceiver(connectionReceiver)
        } catch (e: IllegalArgumentException) {
            lge("$tag connectionReceiver has unregistered. Err: ${e.message}")
        }
    }
    //endregion

    companion object {
        private const val tag = "BleObserver: "
    }
}

