package com.homan.huang.bletoled.di

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.homan.huang.bletoled.common.ConfigHelper
import com.homan.huang.bletoled.device.BleHc05Observer
import com.homan.huang.bletoled.device.BluetoothHelper
import com.homan.huang.bletoled.device.DeviceStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

// provide Application context
@InstallIn(ApplicationComponent::class)

// provide Activity context
//@InstallIn(ActivityComponent::class)
@Module
class BleModule {

    @Provides
    fun provideAddress(): String = ConfigHelper.getAddress()

    @Provides
    @Singleton
    fun provideBleStatus()
        : MutableLiveData<DeviceStatus> = MutableLiveData<DeviceStatus>()

    @Provides
    fun provideBleHelper(
        @ApplicationContext context: Context,
        address: String
    ): BluetoothHelper = BluetoothHelper(context, address)

    //register BroadcastReceivers
    @Provides
    fun provideBleObserver(
        @ApplicationContext context: Context,
        address: String,
        bleStatus: MutableLiveData<DeviceStatus>
    ): BleHc05Observer = BleHc05Observer(context, address, bleStatus)




}