package com.homan.huang.bletoled.common

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.homan.huang.bletoled.common.lgd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.reflect.KFunction2

// Toast: len: 0-short, 1-long
fun msg(context: Context, s: String, len: Int) =
    if (len > 0) Toast.makeText(context, s, LENGTH_LONG).show()
    else Toast.makeText(context, s, LENGTH_SHORT).show()


