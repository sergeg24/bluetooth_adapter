package com.example.bluetoothadapter.data

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bluetoothadapter.data.ListItem

open class DataModel : ViewModel() {

    val searchDevice: MutableLiveData<BluetoothDevice> by lazy {
        MutableLiveData<BluetoothDevice>()
    }

    val searchDeviceStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val devices: MutableLiveData<List<ListItem>> = MutableLiveData()

    val clickMenuF1: MutableLiveData<Boolean> = MutableLiveData()
}