package com.homan.huang.bletoled.ui.main

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleOwner
import com.homan.huang.bletoled.R
import com.homan.huang.bletoled.common.*
import com.homan.huang.bletoled.device.BleHc05Observer
import com.homan.huang.bletoled.device.DeviceStatus.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_NOTIFICATION_POLICY
)

private const val BLUETOOTH_REQUEST_CODE = 9191

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var bleObserver: BleHc05Observer

    //region vars:
    private val mainVM: MainViewModel by viewModels()
    private val fragmentMG = supportFragmentManager
    private val infoTV: TextView by
        lazy { findViewById(R.id.infoTV) }
    private val discoveryTV: TextView by
        lazy { findViewById(R.id.discoverTV) }
    private val progressBar: ProgressBar by
        lazy { findViewById(R.id.progressBar) }
    private val onBT: Button by
        lazy { findViewById(R.id.onBT) }
    private val offBT: Button by
        lazy { findViewById(R.id.offBT) }
    //endregion

    //region contracts:
    private val bluetoothContract = registerForActivityResult(
            BluetoothSwitchContract()
    ) { result ->
        val tag = "BluetoothContract:"
        when (result) {
            BLUETOOTH_ON -> {
                msg(this, BLUETOOTH_READY, 1)
                lgd("$tag Bluetooth Switch is ON. Move to Step 1.")
                //progressBar.visibility = View.VISIBLE
                mainVM.checkBondedList()
            }
            BLUETOOTH_OFF -> {
                lgd("$tag Terminate App.")
                msg(this, BLUETOOTH_NOT_ON, 0)
                finish()
            }
        }
    }

    private val reqMultiplePermissions = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            lgd("MainAct-Permission: ${it.key} = ${it.value}")
        }
        mainVM.checkSwitch()
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Hide
        progressBar.visibility = View.GONE
        discoveryTV.visibility = View.GONE

        onBT.visibility = View.GONE
        onBT.setOnClickListener {
            offBT.visibility = View.VISIBLE
            onBT.visibility = View.GONE
            mainVM.turnOn()
        }

        offBT.visibility = View.GONE
        offBT.setOnClickListener {
            offBT.visibility = View.GONE
            onBT.visibility = View.VISIBLE
            mainVM.turnOff()
        }

        // check permission + Step 0
        reqMultiplePermissions.launch(REQUIRED_PERMISSIONS)

        // Register BroadcastReceivers
        val lifecycleOwner = this as LifecycleOwner
        lifecycleOwner.lifecycle.addObserver(bleObserver)

        /*
            Step 0: Check Bluetooth Switch
            Step 1: Check Bonded List.
                Found: Step 3;
                Not Found: Step 2
            Step 2: Discover Device
                Found: Step 3;
                Not Found: Device Not Found
            Step 3: Bond to Device
         */
        mainVM.deviceStatus.observe(
                this,
                { status ->
                    when (status) {
                        SWITCHING -> {
                            val info = "Switching Bluetooth ON..."
                            infoTV.text = info
                            bluetoothContract.launch(BLUETOOTH_REQUEST_CODE)
                        }
                        PAIRING -> {
                            val info = "Searching Bonded List..."
                            infoTV.text = info
                            mainVM.checkBondedList()
                            progressBar.visibility = View.VISIBLE
                        }
                        DISCOVERING -> {
                            val info = "Discovering device in range..."
                            infoTV.text = info
                            mainVM.discovering()
                        }
                        BONDING -> {
                            val info = "Device Found in Record!\nBonding..."
                            infoTV.text = info
                            mainVM.bonding()
                        }
                        DISCOVERED -> {
                            val pass = "Your Pass: ${ConfigHelper.getPass()}"
                            discoveryTV.text = pass
                            discoveryTV.visibility = View.VISIBLE
                        }
                        BONDED -> {
                            lgd("MainAct: Bonded successful!")

                            discoveryTV.text = ""
                            discoveryTV.visibility = View.GONE

                            val info = "Bonded to $DEVICE_NAME."
                            infoTV.text = info
                            msg(this, info, 1)
                        }
                        CONNECTED -> {
                            val info = "Device connected..."
                            infoTV.text = info
                            onBT.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE
                        }
                        FAIL -> {
                            val info = "FAIL to create connection!"
                            infoTV.text = info
                            progressBar.visibility = View.GONE
                        }
                        NOT_FOUND -> {
                            val info = "Device NOT Found!"
                            infoTV.text = info
                            msg(this, info, 1)
                        }
                        else -> {
                            val info = "Illegal Process Error..."
                            infoTV.text = info
                        }
                    }
                }
        )
    }
}