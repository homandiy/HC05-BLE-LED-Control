package com.homan.huang.bletoled.device

import android.content.BroadcastReceiver
import android.content.Context

import android.content.Intent

import android.content.IntentFilter
import com.homan.huang.bletoled.common.lgi
import java.lang.ref.WeakReference


class ReceiverManager private constructor(context: Context) {
    private val cReference: WeakReference<Context> = WeakReference(context)

    fun registerReceiver(
            receiver: BroadcastReceiver,
            intentFilter: IntentFilter
    ): Intent? {
        receivers.add(receiver)
        val intent: Intent? = cReference.get()?.registerReceiver(receiver, intentFilter)
        lgi("$tag registered receiver: $receiver  with filter: $intentFilter")
        lgi("$tag receiver Intent: $intent")
        return intent
    }

    fun isReceiverRegistered(receiver: BroadcastReceiver): Boolean {
        val registered = receivers.contains(receiver)
        lgi("$tag is receiver $receiver registered? $registered")
        return registered
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        if (isReceiverRegistered(receiver)) {
            receivers.remove(receiver)
            cReference.get()?.unregisterReceiver(receiver)
            lgi("$tag unregistered receiver: $receiver")
        }
    }

    companion object {
        private const val tag = "ReceiverMG: "
        private val receivers: MutableList<BroadcastReceiver> = ArrayList()
        private var ref: ReceiverManager? = null
        @Synchronized
        fun init(context: Context): ReceiverManager? {
            if (ref == null) ref = ReceiverManager(context)
            return ref
        }
    }

}