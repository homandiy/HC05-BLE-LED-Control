    package com.homan.huang.bletoled.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Message
import com.homan.huang.bletoled.common.ERR_BROKEN_PIPE
import com.homan.huang.bletoled.common.lgd
import com.homan.huang.bletoled.common.lge
import kotlinx.coroutines.delay
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.lang.Thread.State
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


    @SuppressLint("HandlerLeak")
class BluetoothHelper @Inject constructor(
            private val context: Context,
            private val devAddr: String
    ) {
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private lateinit var serviceUuid: UUID
    private lateinit var mBleDevice: BluetoothDevice
    var bleMsg = ""
    private var mBTSocket: BluetoothSocket? = null
    private var mHandler: Handler? = null
    private var mConnectedThread: ConnectedThread? = null
    // check connection
    private var connected = false

    // check bluetooth switch
    fun isSwitchOn(): Boolean {
        return !bluetoothAdapter.isEnabled
    }

    // Step 2: Check Paired List
    fun checkBondedList(): Boolean {
        val devices = bluetoothAdapter?.bondedDevices
        val list = ArrayList<Any>()
        // address to string
        val hcAddr = mRegex.replace(devAddr, "").toUpperCase()

        if (devices?.size!! > 0) {
            // Loop through paired devices
            for (device in devices) {
                // address to string
                val deviceAddr = mRegex.replace(
                        device.address, ""
                ).toUpperCase(Locale.ROOT)
                lgd("$tag Device address: $deviceAddr vs $hcAddr")

                if (deviceAddr == hcAddr) {
                    lgd("$tag ==========> Found HC05")
                    mBleDevice = device
                    return true
                } else {
                    lgd("$tag ! Not Found !")
                }
            }
        } else {
            lgd("$tag Nothing is in the paired list.")
        }
        return false
    }

    // Step 3
    fun discoverDevice() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter.isDiscovering){
            lgd(tag + "Canceling discovery process...")
            bluetoothAdapter.cancelDiscovery()
        }

        lgd(tag + "Start Discovery...")
        mBluetoothAdapter.startDiscovery()
    }

    // Step 4
    suspend fun checkDeviceBonding(): Boolean {
        mBleDevice.createBond()
        val uuids = mBleDevice.uuids
        serviceUuid = uuids[0].uuid
        lgd("$tag Service UUID: $serviceUuid")

        // handler:
        if (mHandler == null) mHandler = newHandler

        // start bond thread
        if (bondThread.state == State.NEW) bondThread.start()

        // communication delay
        delay(COM_DELAY)
        lgd("$tag connected? $connected")
        return connected
    }

    //region network thread with Bluetooth
    private val bondThread = object : Thread() {
        override fun run() {
            var fail = false
            try {
                mBTSocket = createBluetoothSocket(serviceUuid)
            } catch (e: IOException) {
                fail = true
                lge("Socket creation failed")
            }
            // Establish the Bluetooth socket connection.
            try {
                mBTSocket?.connect()
            } catch (e: IOException) {
                try {
                    fail = true
                    mBTSocket?.close()
                    //                                             fail
                    mHandler?.obtainMessage(CONNECTING_STATUS, -1, -1)
                            ?.sendToTarget()
                } catch (e2: IOException) {
                    //insert code to deal with this
                    lge("Socket creation failed")
                }
            }

            // socket not fail
            if (!fail) {
                connected = true
                mConnectedThread = mBTSocket?.let { ConnectedThread(it) }
                mConnectedThread?.setHandler(mHandler!!) // provide Handler
                if (mConnectedThread?.state == State.NEW) mConnectedThread?.start()
                //                                         connect
                mHandler?.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                        ?.sendToTarget() // send message
            }
        }
    }

    private val newHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            lgd("Handler: what = ${msg.what}")
            when (msg.what) {
                MESSAGE_READ -> {
                    try {
                        val readBuffer = msg.obj as ByteArray
                        bleMsg += String(readBuffer, 0, msg.arg1)
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }
                CONNECTING_STATUS -> {
                    lgd("msg = CONNECTION STATUS")
                    connected = if (msg.arg1 == CONNECTED) {
                        lgd("Connected to Device: " + msg.obj as String)
                        true
                    } else {
                        lge("Connection Failed")
                        false
                    }
                }
                BROKEN_PIPE -> {
                    bleMsg = ERR_BROKEN_PIPE
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(uuid: UUID): BluetoothSocket? {
        return mBleDevice.createRfcommSocketToServiceRecord(uuid)
    }


    suspend fun ledSwitch(signal: String): String {
        bleMsg = ""
        mConnectedThread?.write(signal)
        delay(1000)
        return bleMsg
    }

    /*
        Thread holds connection
     */
    private class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private var mmSocket: BluetoothSocket = socket
        private var mmInStream: InputStream? = mmSocket.inputStream
        private var mmOutStream: OutputStream? = mmSocket.outputStream
        private lateinit var mHandler: Handler

        fun setHandler(handler: Handler) { mHandler = handler }

        override fun run() {
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes = 0 // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                if (mmInStream != null) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream!!.read(buffer)
                        lgd("+++++ ConnectedThread: bytes size = $bytes")
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget()
                    } catch (e: IOException) {
                        lge("+++++ ConnectedThread: Error on read: ${e.message}")
                        mHandler.obtainMessage(BROKEN_PIPE, -1, -1)
                                .sendToTarget()
                        break
                    }
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        fun write(input: String) {
            val bytes = input.toByteArray() //converts entered String into bytes
            try {
                mmOutStream?.write(bytes)
            } catch (e: IOException) {
                lge("+++++ ConnectedThread: Error on write: ${e.message}")
                mHandler.obtainMessage(BROKEN_PIPE, -1, -1)
                        .sendToTarget()
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmInStream?.close()
                mmOutStream?.close()
                mmSocket.close()
            } catch (e: IOException) {
            }
        }
    }
    //endregion

    companion object {
        private const val tag = "BlueHelper: "
        private const val MESSAGE_READ = 4
        private const val CONNECTING_STATUS = 3
        private const val BROKEN_PIPE = 5

        private const val CONNECTED = 1
        private const val COM_DELAY = 3000L

        val mRegex = "[^A-Za-z0-9 ]".toRegex()
    }

}
