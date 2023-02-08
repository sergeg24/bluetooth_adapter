package com.example.bluetoothadapter.fragment

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothadapter.R
import com.example.bluetoothadapter.TAG
import com.example.bluetoothadapter.adapter.ItemSearchAdapter
import com.example.bluetoothadapter.data.DataModel
import com.example.bluetoothadapter.data.ListItem
import com.example.bluetoothadapter.databinding.FragmentSearchListBinding
import com.example.bluetoothadapter.toast
import com.example.bluetoothadapter.vibrateOrSound
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.HashMap

class SearchListFragment(private var bAdapter: BluetoothAdapter) : Fragment(),
    ItemSearchAdapter.Listener {

    private lateinit var binding: FragmentSearchListBinding
    private var itemSearchList = HashMap<String, ListItem>()
    private var defaultText = ""
    private var itemSearchAdapter: ItemSearchAdapter? = null
    private val dataModel: DataModel by activityViewModels()

    private val receiver = object : BroadcastReceiver() {

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context, intent: Intent) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }

            val device = if (Build.VERSION.SDK_INT >= 33) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (device != null) {
                        val deviceName = device.name
                        val deviceHardwareAddress = device.address // MAC address
                        if (deviceName != null && deviceHardwareAddress != null) {
                            itemSearchList[deviceHardwareAddress] = ListItem(
                                deviceName,
                                deviceHardwareAddress,
                                0,
                                false,
                                device
                            )
                            dataModel.devices.value = itemSearchList.values.toList()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    binding.emptyList.visibility = View.INVISIBLE
                    binding.textView2.text = "Поиск устройств..."
                    itemSearchList.clear()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    binding.textView2.text = defaultText
                    if (itemSearchList.isNotEmpty()) {
                        binding.emptyList.visibility = View.INVISIBLE
                    } else {
                        binding.emptyList.visibility = View.VISIBLE
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    if (device != null) {
                        if (device.bondState == BluetoothDevice.BOND_BONDED) {
                            bAdapter.cancelDiscovery();
                            dataModel.searchDevice.value = device
                            binding.textView2.text = "Сопряжение успешно завершено"
                        }
                        if (device.bondState == BluetoothDevice.BOND_BONDING) {
                            binding.textView2.text = "Сопряжение..."
                        }
                        if (device.bondState == BluetoothDevice.BOND_NONE) {
                            binding.textView2.text = defaultText
                        }
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bManager = activity?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bAdapter = bManager.adapter

        val filterFound = IntentFilter()
        //filterFound.addAction(BluetoothDevice.ACTION_FOUND)
        filterFound.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        filterFound.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filterFound.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)

        requireContext().registerReceiver(receiver, filterFound)

        defaultText = binding.textView2.text as String

        binding.rcViewSearch.layoutManager = LinearLayoutManager(requireContext())
        itemSearchAdapter = ItemSearchAdapter(this)

        dataModel.devices.observe(viewLifecycleOwner) { device ->
            itemSearchAdapter!!.update(device)
        }

        binding.rcViewSearch.adapter = itemSearchAdapter

        dataModel.searchDeviceStatus.observe(activity as LifecycleOwner) { status ->
            if (status) {
                binding.bBluetoothSearch.setColorFilter(requireContext().getColor(R.color.orange))
            } else {
                binding.bBluetoothSearch.setColorFilter(Color.GRAY)
            }
        }

        if (!bAdapter.isEnabled) {
            binding.bBluetoothSearch.setColorFilter(Color.GRAY)
        }

        binding.bBluetoothSearch.setOnClickListener {

            if (!bAdapter.isEnabled) {
                dataModel.searchDevice.value = null
                Snackbar.make(binding.textStatus, "isEnabled false", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            vibrateOrSound()
            startScan()

            //if (bAdapter.isDiscovering) {
                //itemSearchAdapter.clear()
            //    bAdapter.cancelDiscovery();
            //}

            //bAdapter.startDiscovery()
        }
    }

    private var scanner: BluetoothLeScanner? = null
    private var callback: BleScanCallback? = null

    private fun startScan() {

        val filter = ScanFilter.Builder()
            //.setServiceUuid(myUUID)
            .build()

        val filters = ArrayList<ScanFilter>()
        filters.add(filter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            //.setReportDelay(0)
            .build()

        if (callback == null) {
            itemSearchAdapter!!.clear()
            binding.emptyList.visibility = View.INVISIBLE
            binding.textView2.text = "Поиск устройств..."
            itemSearchList.clear()
            callback = BleScanCallback()
            scanner = bAdapter.bluetoothLeScanner
            scanner?.startScan(filters, settings, callback)
        } else {
            binding.textView2.text = defaultText
            if (itemSearchList.isNotEmpty()) {
                binding.emptyList.visibility = View.INVISIBLE
            } else {
                binding.emptyList.visibility = View.VISIBLE
            }
            stopScan()
            callback = null
        }
    }

    private fun stopScan() {
        scanner?.stopScan(callback)
    }

    private fun addDevice(result: ScanResult?) {
        if (result != null && result.device != null) {
            val device = result.device
            var deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            if (deviceName != null && deviceHardwareAddress != null) {
                itemSearchList[deviceHardwareAddress] = ListItem(
                    deviceName,
                    deviceHardwareAddress,
                    result.rssi,
                    false,
                    device
                )
                dataModel.devices.value = itemSearchList.values.toList()
            }
        }
    }

    inner class BleScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            addDevice(result)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "onScanFailed $errorCode")
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                addDevice(result)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(receiver)
        } catch (e: Exception) {
        }
    }

    override fun onClick(listItem: ListItem) {
        vibrateOrSound()
        if (bAdapter.isEnabled) {
            stopScan()
            listItem?.device?.createBond()
        } else {
            toast(requireContext(), "Bluetooth отключен")
        }
    }
}