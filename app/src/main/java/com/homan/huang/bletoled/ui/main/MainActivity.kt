package com.homan.huang.bletoled.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.homan.huang.bletoled.R
import com.homan.huang.bletoled.common.*
import com.homan.huang.bletoled.device.BleHc05Observer
import com.homan.huang.bletoled.device.DeviceStatus.*
import com.homan.huang.bletoled.device.DeviceStatus.LED_OFF
import com.homan.huang.bletoled.device.DeviceStatus.LED_ON
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
    private val infoTV: TextView by
        lazy { findViewById(R.id.infoTV) }
    private val counterTV: TextView by
        lazy { findViewById(R.id.counterTV) }
    private val discoveryTV: TextView by
        lazy { findViewById(R.id.discoverTV) }
    private val progressBar: ProgressBar by
        lazy { findViewById(R.id.progressBar) }
    private val onBT: Button by
        lazy { findViewById(R.id.onBT) }
    private val offBT: Button by
        lazy { findViewById(R.id.offBT) }
    private val tryAgainBT: Button by
        lazy { findViewById(R.id.tryAgainBT) }

    private var newDevice = false
    private var counter = 0
    private var bonded = false
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
        counterTV.visibility = View.GONE

        onBT.visibility = View.GONE
        onBT.setOnClickListener {
            mainVM.turnOn()
        }

        offBT.visibility = View.GONE
        offBT.setOnClickListener {
            mainVM.turnOff()
        }

        tryAgainBT.visibility = View.GONE
        tryAgainBT.setOnClickListener {
            tryAgainBT.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            mainVM.checkBondedList()
        }

        // check permission + Step 0
        reqMultiplePermissions.launch(REQUIRED_PERMISSIONS)

        // Register BroadcastReceivers
        val lifecycleOwner = this as LifecycleOwner
        lifecycleOwner.lifecycle.addObserver(bleObserver)

        /*
            Step 1: Check Bluetooth Switch
            Step 2: Check Bonded List.
                Found: Step 4;
                Not Found: Step 3
            Step 3: Discover Device
                Found: Step 4;
                Not Found: Device Not Found
            Step 4: Bond to Device
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
                            progressBar.visibility = View.VISIBLE
                            mainVM.checkBondedList()
                        }
                        DISCOVERING -> {
                            val info = "Discovering device in range..."
                            infoTV.text = info
                            progressBar.visibility = View.VISIBLE
                            newDevice = true
                            mainVM.discovering()
                        }
                        BONDING -> {
                            val info = "Device Found in Record!\nBonding..."
                            infoTV.text = info
                            if (newDevice)
                                mainVM.checkNewDevice()
                            else
                                mainVM.bonding()
                        }
                        DISCOVERED -> {
                            val pass = "Your Pass: ${ConfigHelper.getPass()}"
                            discoveryTV.text = pass
                            discoveryTV.visibility = View.VISIBLE
                        }
                        BONDED -> {
                            lgd("MainAct: Bonded successful!")
                            counterTV.visibility = View.GONE
                            counter = 0
                            bonded = true
                            discoveryTV.text = ""
                            discoveryTV.visibility = View.GONE
                            progressBar.visibility = View.GONE

                            val info = "Bonded to $DEVICE_NAME."
                            infoTV.text = info
                            msg(this, info, 1)
                            mainVM.connected()
                        }
                        CONNECTED -> {
                            val info = "Device connected..."
                            infoTV.text = info
                            onBT.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE
                        }
                        DISCONNECTED -> {
                            val info = "Device Not Found!"
                            infoTV.text = info
                            onBT.visibility = View.GONE
                            tryAgainBT.visibility = View.VISIBLE
                            progressBar.visibility = View.GONE
                        }
                        FAIL -> {
                            counter = 0
                            counterTV.visibility = View.GONE
                            val info = "FAIL to create connection!" +
                                    "\nOr\nPassword is incorrect!"
                            infoTV.text = info
                            discoveryTV.visibility = View.GONE
                            progressBar.visibility = View.GONE
                            tryAgainBT.visibility = View.VISIBLE
                        }
                        NOT_FOUND -> {
                            lgd("MainAct: Device Not Found.")
                            progressBar.visibility = View.GONE
                            val info = "Device NOT Found!"
                            infoTV.setTextColor(Color.RED)
                            infoTV.text = info
                            msg(this, info, 1)
                        }
                        COUNT_DOWN -> {
                            if (!bonded) {
                                counterTV.visibility = View.VISIBLE
                                val down = 35 - counter
                                counter++
                                val timer = "35s to Enter Password: $down"
                                counterTV.text = timer
                            }
                        }
                        LED_ON -> {
                            offBT.visibility = View.VISIBLE
                            onBT.visibility = View.GONE
                            val info = "LED is ON."
                            infoTV.setTextColor(Color.BLUE)
                            infoTV.text = info
                        }
                        LED_FAIL_ON -> {
                            val info = "LED: Fail to turn ON!\n" +
                                    "Please check your distance!"
                            infoTV.setTextColor(Color.DKGRAY)
                            infoTV.text = info
                        }
                        LED_OFF -> {
                            offBT.visibility = View.GONE
                            onBT.visibility = View.VISIBLE
                            val info = "LED is OFF."
                            infoTV.setTextColor(Color.BLACK)
                            infoTV.text = info
                        }
                        LED_FAIL_OFF -> {
                            val info = "LED: Fail to turn OFF!\n" +
                                    "Please check your distance!"
                            infoTV.setTextColor(Color.DKGRAY)
                            infoTV.text = info
                        }
                        RESTART -> {
                            lgd("MainAct: Restart the App")
                            val info = "Broken Connection\n" +
                                    "Restarting the App!"
                            infoTV.setTextColor(Color.RED)
                            infoTV.text = info
                            progressBar.visibility = View.VISIBLE

                            val packageManager: PackageManager = this.packageManager
                            val intent = packageManager.getLaunchIntentForPackage(this.packageName)
                            val componentName = intent!!.component
                            val mainIntent = Intent.makeRestartActivityTask(componentName)
                            this.startActivity(mainIntent)
                            Runtime.getRuntime().exit(0)
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