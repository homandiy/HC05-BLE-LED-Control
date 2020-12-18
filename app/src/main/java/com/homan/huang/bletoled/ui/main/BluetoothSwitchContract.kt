package com.homan.huang.bletoled.ui.main

import android.app.Activity.*
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.homan.huang.bletoled.common.BLUETOOTH_OFF
import com.homan.huang.bletoled.common.BLUETOOTH_ON

class BluetoothSwitchContract:
        ActivityResultContract<Int, String?>() {

    override fun createIntent(p0: Context, p1: Int?):
            Intent {
        return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }

    override fun parseResult(resultCode: Int, intent: Intent?):
            String? {
        return if (resultCode == RESULT_OK)
            BLUETOOTH_ON
        else
            BLUETOOTH_OFF
    }
}