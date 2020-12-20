package com.homan.huang.bletoled.device

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothDevice.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import com.homan.huang.bletoled.common.lgd
import com.homan.huang.bletoled.common.lge
import com.homan.huang.bletoled.common.lgi
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
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
    private lateinit var charUuid: UUID
    private lateinit var mBleDevice: BluetoothDevice
    private var mBTSocket: BluetoothSocket? = null
    private var mHandler: Handler? = null
    private var mConnectedThread: ConnectedThread? = null

    // check bluetooth switch
    fun isSwitchOn(): Boolean {
        return !bluetoothAdapter.isEnabled
    }


    // Step 4
    fun checkDeviceBonding(): Boolean {
        val uuids = mBleDevice.uuids
        serviceUuid = uuids[0].uuid
        charUuid = uuids[1].uuid

        // handler:
        if (mHandler == null) mHandler = newHandler
        bondThread.start()

        lgd("$tag Service UUID: $serviceUuid")
        lgd("$tag Characteristic UUID: $charUuid")



        return true
    }
    //endregion

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

    //region Step 2: Check Paired List
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
    //endregion

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
                    mHandler?.obtainMessage(CONNECTING_STATUS, -1, -1)
                            ?.sendToTarget()
                } catch (e2: IOException) {
                    //insert code to deal with this
                    lge("Socket creation failed")
                }
            }
            if (!fail) {
                mConnectedThread = mBTSocket?.let { ConnectedThread(it) }
                mConnectedThread?.setHandler(mHandler!!)
                mConnectedThread?.start()
                mHandler?.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                        ?.sendToTarget()
            }
        }
    }

    private val newHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == MESSAGE_READ) {
                var readMessage: String? = null
                try {
                    readMessage = String((msg.obj as ByteArray), StandardCharsets.UTF_8)
                } catch (e: UnsupportedEncodingException) {
                    e.printStackTrace()
                }
            }
            if (msg.what == CONNECTING_STATUS) {
                if (msg.arg1 == 1)
                    lgd("Connected to Device: " + msg.obj as String)
                else
                    lge("Connection Failed")
            }
        }
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(uuid: UUID): BluetoothSocket? {
        return mBleDevice.createRfcommSocketToServiceRecord(uuid)
        //creates secure outgoing connection with BT device using UUID
    }

    fun ledSwitch(signal: String) {
        mConnectedThread?.write(signal)
    }

    /*
        Thread holds connection
     */
    private class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        private lateinit var mHandler: Handler

        fun setHandler(handler: Handler) { mHandler = handler }

        override fun run() {
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.available()
                    if (bytes != 0) {
                        SystemClock.sleep(100) //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available() // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes) // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget() // Send the obtained bytes to the UI activity
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        fun write(input: String) {
            val bytes = input.toByteArray() //converts entered String into bytes
            try {
                mmOutStream?.write(bytes)
            } catch (e: IOException) {
                lge("ConnectedThread: Error on write: ${e.message}")
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }
    //endregion

    companion object {
        private const val tag = "BlueHelper: "
        private const val MESSAGE_READ = 2
        private const val CONNECTING_STATUS = 3

        val mRegex = "[^A-Za-z0-9 ]".toRegex()
    }


}

data class BluetoothResult(
        val uuid: UUID,
        val value: ByteArray?,
        val status: Int)