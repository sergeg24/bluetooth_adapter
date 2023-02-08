package com.example.bluetoothadapter

//https://github.com/olegkrutskih/iQOS/tree/master/app/src/main/java/ru/krat0s/iqos
//https://github.com/MatthiasKerat/BLETutorialYt/tree/FinalApp/app/src/main/java/com/example/bletutorial

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bluetoothadapter.BinaryHelperJava.getBits
import com.example.bluetoothadapter.BinaryHelperJava.isFlag
import com.example.bluetoothadapter.adapter.ItemAdapter
import com.example.bluetoothadapter.data.DataModel
import com.example.bluetoothadapter.data.ListItem
import com.example.bluetoothadapter.databinding.FragmentDeviceListBinding
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.experimental.and

class DeviceListFragment(private var bAdapter: BluetoothAdapter) : Fragment(),
    ItemAdapter.Listener {

    private var preferences: SharedPreferences? = null
    private lateinit var binding: FragmentDeviceListBinding
    private lateinit var itemAdapter: ItemAdapter
    private val dataModel: DataModel by activityViewModels()
    private var deviceConnectGat: BluetoothGatt? = null
    private var clickDisconnectDevice = false
    private var btSocket: BluetoothSocket? = null
    private var scanner: BluetoothLeScanner? = null
    private lateinit var macAddress: String
    val UUID_DEVICE_STATUS = UUID.fromString("ecdfa4c0-b041-11e4-8b67-0002a5d5c51b")
    val UUID_BATTERY_INFORMATION = UUID.fromString("f8a54120-b041-11e4-9be7-0002a5d5c51b")
    val UUID_RRP_SERVICE = UUID.fromString("daebb240-b041-11e4-9e45-0002a5d5c51b")
    //val UUID_SCP_CONTROL_POINT = UUID.fromString("e16c6e20-b041-11e4-a4c3-0002a5d5c51b")
    //val char = UUID.fromString("f8a54120-b041-11e4-9be7-0002a5d5c51b")
    private var deviceConnect = HashMap<String, Int>()
    private var deviceConnectRefresh = HashMap<String, Int>()
    private var secondRefreshDevice = 0

    private var launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == AppCompatActivity.RESULT_OK) {
                Snackbar.make(binding.textStatus, "Bluetooth включен", Snackbar.LENGTH_LONG).show()
                getPairedDevices()
            } else {
                Snackbar.make(binding.textStatus, "Bluetooth выключен", Snackbar.LENGTH_LONG).show()
            }
        }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    initBAdapter()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filterFound = IntentFilter()
        filterFound.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        requireContext().registerReceiver(receiver, filterFound)

        dataModel.searchDevice.observe(viewLifecycleOwner) {
            launcher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        preferences =
            activity?.getSharedPreferences(BluetoothConstants.PREFERENCES, Context.MODE_PRIVATE)

        //Для определенных версий sdk нужно у пользователя запросить разрешение на использование

        if (!isPermissions()) {
            //Если ранее пользователь разрешение не давал, показываем окно с подтверждением нужного разрешения
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                )
            } else {
                requestPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }

        initRcView()
        initBAdapter()

        binding.btnMenu.setOnClickListener {
            vibrateOrSound()
            dataModel.clickMenuF1.value = true
        }

        scanner = bAdapter.bluetoothLeScanner

//        scanner = bAdapter.bluetoothLeScanner
//
//        if (itemAdapter.currentList.isNotEmpty()) {
//
//            val filters = ArrayList<ScanFilter>()
//
//            itemAdapter.currentList.forEach {
//                val filter = ScanFilter.Builder()
//                    .setDeviceAddress(it.address)
//                    .build()
//                filters.add(filter)
//            }
//
//            val settings = ScanSettings.Builder()
//                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
//                .build()
//
//            scanner?.startScan(filters, settings, scanCallbackBondDevice)
//        }

//        binding.imageButtonConnect.setColorFilter(Color.GRAY)
//
//
//        binding.imageButtonConnect.setOnClickListener {
//
//            try {
//
//                vibrateOrSound()
//
//                macAddress = preferences?.getString(BluetoothConstants.MAC, "").toString()
//                var itemCurrent = itemAdapter.currentList.find { it.address == macAddress }
//
//                if (itemCurrent?.isChecked == true) {
//
//                    if (deviceConnectGat != null) {
//                        deviceConnectGat!!.disconnect()
//                        deviceConnectGat!!.close()
//                        binding.imageButtonConnect.setColorFilter(Color.GRAY)
//                        deviceConnectGat = null
//                        Snackbar.make(binding.textStatus, "Соединение разорвано", Snackbar.LENGTH_LONG).show()
//                        return@setOnClickListener
//                    }
//
//                    binding.imageButtonConnect.setColorFilter(Color.YELLOW)
//                    bAdapter.cancelDiscovery()
//
//                    val filter = ScanFilter.Builder()
//                        .setDeviceAddress(itemCurrent.address)
//                        .build()
//
//                    val filters = ArrayList<ScanFilter>()
//                    filters.add(filter)
//
//                    val settings = ScanSettings.Builder()
//                        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
//                        .build()
//
//                    clickDisconnectDevice = if (clickDisconnectDevice && deviceConnectGat == null) {
//                        scanner?.stopScan(scanCallbackSelectedDevice)
//                        binding.imageButtonConnect.setColorFilter(Color.GRAY)
//                        Snackbar.make(binding.textStatus, "Поиск остановлен", Snackbar.LENGTH_LONG).show()
//                        false
//                    } else {
//                        scanner?.startScan(filters, settings, scanCallbackSelectedDevice)
//                        Snackbar.make(binding.textStatus, "Поиск устройства", Snackbar.LENGTH_LONG).show()
//                        true
//                    }
//
//                } else {
//                    Snackbar.make(binding.textStatus, "Выберете устройство", Snackbar.LENGTH_LONG).show()
//                }
//
//            } catch (e : SecurityException) {
//                Log.e(TAG, e.message.toString())
//            }
//        }

        binding.bBluetoothStatus.setOnClickListener {
            vibrateOrSound()
            if (isPermissions()) {
                //Отображаем окно включения bluetooth
                launcher?.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                //startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } else {
                Snackbar.make(
                    binding.textStatus,
                    "Нет разрешения на использование Bluetooth",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val text = "Соединение установлено"
                //sendNotification(requireContext(), text)
                Snackbar.make(binding.textStatus, text, Snackbar.LENGTH_LONG).show()
                clickDisconnectDevice = false
                gatt.discoverServices()
            } else {
                val text = "Соединение разорвано"
                //sendNotification(requireContext(), text)
                Snackbar.make(binding.textStatus, text, Snackbar.LENGTH_LONG).show()
                deviceConnectGat = null
                gatt.close()
                gatt.disconnect()
            }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                //printGattTable()
                services?.forEach { service ->
                    if (service.uuid == UUID_RRP_SERVICE) {
                        try {
//                            val char = service.getCharacteristic(UUID_DEVICE_STATUS)
//                            if (!setCharacteristicNotification(char, true)) {
//                                Log.d("MyLog", "UUID_DEVICE_STATUS notification failed")
//                            }
//                            val descriptor = char.getDescriptor(convertFromInteger(0x2902))
//                            writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)

                            val char1 = service.getCharacteristic(UUID_BATTERY_INFORMATION)
                            if (!setCharacteristicNotification(char1, true)) {
                                Log.d("MyLog", "UUID_BATTERY_INFORMATION notification failed")
                            }
                            val descriptor1 = char1.getDescriptor(convertFromInteger(0x2902))

//                            Log.d("MyLog", Build.VERSION_CODES.O.toString())
//                            Log.d("MyLog", Build.VERSION_CODES.S.toString())
//                            Log.d("MyLog", Build.VERSION.SDK_INT.toString())

                            /** huawei samsung
                             *   26      26
                             *   31      31
                             *   29      33
                             */

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                descriptor1.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                writeDescriptor(descriptor1)
                            } else {
                                writeDescriptor(descriptor1, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            }

                        } catch (e: SecurityException) {
                            Log.d(TAG, e.message.toString())
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            with(characteristic) {
                when(uuid) {
                    UUID_DEVICE_STATUS -> {
                        val dev = DeviceStatus(characteristic)
                        var text = when(dev.chargerSystemStatus?.holderState.toString()) {
                            "UNPLUGGED" -> "закрыта"
                            "READY_TO_USE" -> "открыта"
                            else -> ""
                        }
                        Snackbar.make(binding.textStatus, "Крышка $text", Snackbar.LENGTH_LONG).show()
                    }
                    else -> Unit
                }
            }

            if (characteristic.uuid == UUID_DEVICE_STATUS) {
                val dev = DeviceStatus(characteristic)

//                Log.e(
//                    TAG,
//                    "onCharacteristicRead chargerState: ${dev.chargerSystemStatus?.chargerState}"
//                )
//                Log.e(
//                    TAG,
//                    "onCharacteristicRead holderState: ${dev.chargerSystemStatus?.holderState}"
//                )
//                Log.e(
//                    TAG,
//                    "onCharacteristicRead puffCount: ${dev.holder1LastExperienceInfo?.puffCount}"
//                )
//                Log.e(
//                    TAG,
//                    "onCharacteristicRead isNewInformationWasRead: ${dev.holder1LastExperienceInfo?.isNewInformationWasRead}"
//                )
//                Log.e(
//                    TAG,
//                    "onCharacteristicRead isIntensivePuffing: ${dev.holder1LastExperienceInfo?.isIntensivePuffing}"
//                )
//                Log.e(
//                    TAG,
//                    "onCharacteristicRead isDutyCycleFault: ${dev.holder1LastExperienceInfo?.isDutyCycleFault}"
//                )
//                Log.e(
//                    TAG,
//                    "onCharacteristicRead endOfHeatReason: ${dev.holder1LastExperienceInfo?.endOfHeatReason}"
//                )
            }

            if (characteristic.uuid == UUID_BATTERY_INFORMATION) {
                val batt = BatteryInformation(characteristic)

                Snackbar.make(binding.textStatus, "chargerBatteryTemperature: ${batt.chargerBatteryTemperature}", Snackbar.LENGTH_LONG).show()

                Log.e(TAG, "onCharacteristicRead chargerBatteryLevel: ${batt.chargerBatteryLevel}")
                Log.e(
                    TAG,
                    "onCharacteristicRead chargerBatteryTemperature: ${batt.chargerBatteryTemperature}"
                )
                Log.e(
                    TAG,
                    "onCharacteristicRead chargerBatteryVoltage: ${batt.chargerBatteryVoltage}"
                )
                Log.e(TAG, "onCharacteristicRead holder1BatteryLevel: ${batt.holder1BatteryLevel}")
                Log.e(
                    TAG,
                    "onCharacteristicRead holder1BatteryTemperature: ${batt.holder1BatteryTemperature}"
                )
                Log.e(
                    TAG,
                    "onCharacteristicRead holder1BatteryVoltage: ${batt.holder1BatteryVoltage}"
                )
                Log.e(TAG, "onCharacteristicRead holder2BatteryLevel: ${batt.holder2BatteryLevel}")
                Log.e(
                    TAG,
                    "onCharacteristicRead holder2BatteryTemperature: ${batt.holder2BatteryTemperature}"
                )
                Log.e(
                    TAG,
                    "onCharacteristicRead holder2BatteryVoltage: ${batt.holder2BatteryVoltage}"
                )
            }

        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.e(
                TAG,
                "onCharacteristicRead characteristic ${characteristic?.uuid}, value ${characteristic?.value}, status $status, format ${
                    getValueFormat(characteristic!!.properties)
                }"
            )
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.equals(UUID_BATTERY_INFORMATION)) {
                characteristic.value
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            Log.e(
                TAG,
                "onCharacteristicWrite characteristic ${characteristic?.uuid}, value ${characteristic?.value}, status $status"
            )
        }
    }

    private var scanCallbackSelectedDevice = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null && result.device.address == macAddress) {
                scanner?.stopScan(this)
                val text = "Устройство ${result.device.name} найдено, подключаюсь..."
                Snackbar.make(binding.textStatus, text, Snackbar.LENGTH_LONG).show()
                deviceConnectGat = result.device.connectGatt(requireContext(), false, gattCallback)
            }
        }
    }

//    private var scanCallbackBondDevice = object : ScanCallback() {
//        override fun onScanResult(callbackType: Int, result: ScanResult?) {
//            result?.let {
//                refreshDevices(result)
//            }
//        }
//    }

//    private fun refreshDevices(deviceResult: ScanResult) {
//        val device = deviceResult.device
//        deviceConnectRefresh[device.address] = deviceResult.rssi
//        if (secondRefreshDevice >= 5) {
//            deviceConnect = deviceConnectRefresh
//            deviceConnectRefresh = HashMap()
//            getPairedDevices()
//            secondRefreshDevice = 0
//        }
//        secondRefreshDevice++
//    }

    private fun initRcView() {
        binding.rcViewPaired.layoutManager = LinearLayoutManager(requireContext())
        itemAdapter = ItemAdapter(this@DeviceListFragment)
        binding.rcViewPaired.adapter = itemAdapter
    }

    private fun getPairedDevices() {
        try {
            val list = ArrayList<ListItem>()
            val devicesList = bAdapter.bondedDevices
            devicesList.forEach { device ->
                list.add(
                    ListItem(
                        device.name,
                        device.address,
                        0,
                        preferences?.getString(BluetoothConstants.MAC, "") == device.address,
                        null
                    )
                )
            }
            binding.emptyList.visibility = if (list.isEmpty()) View.VISIBLE else View.INVISIBLE
            itemAdapter.submitList(list)
        } catch (e: SecurityException) {}
    }

    fun convertFromInteger(i: Int): UUID {
        val MSB = 0x0000000000001000L
        val LSB = -0x7fffff7fa064cb05L
        val value = (i and -0x1).toLong()
        return UUID(MSB or (value shl 32), LSB)
    }

    private fun getValueFormat(props: Int): Int {
        return when {
            (BluetoothGattCharacteristic.FORMAT_FLOAT and props != 0) -> BluetoothGattCharacteristic.FORMAT_FLOAT
            (BluetoothGattCharacteristic.FORMAT_SFLOAT and props != 0) -> BluetoothGattCharacteristic.FORMAT_SFLOAT
            (BluetoothGattCharacteristic.FORMAT_SINT16 and props != 0) -> BluetoothGattCharacteristic.FORMAT_SINT16
            (BluetoothGattCharacteristic.FORMAT_SINT32 and props != 0) -> BluetoothGattCharacteristic.FORMAT_SINT32
            (BluetoothGattCharacteristic.FORMAT_SINT8 and props != 0) -> BluetoothGattCharacteristic.FORMAT_SINT8
            (BluetoothGattCharacteristic.FORMAT_UINT16 and props != 0) -> BluetoothGattCharacteristic.FORMAT_UINT16
            (BluetoothGattCharacteristic.FORMAT_UINT32 and props != 0) -> BluetoothGattCharacteristic.FORMAT_UINT32
            (BluetoothGattCharacteristic.FORMAT_UINT8 and props != 0) -> BluetoothGattCharacteristic.FORMAT_UINT8
            else -> 0
        }
    }

    private fun isPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_DENIED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_DENIED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_DENIED
        }
    }

    override fun onResume() {
        super.onResume()
        initBAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            requireContext().unregisterReceiver(receiver)
            btSocket?.close()
        } catch (e: Exception) {
        }
    }

    private fun initBAdapter() {
        val adapterIsEnabled = bAdapter.isEnabled
        if (adapterIsEnabled) {
            binding.bBluetoothStatus.setColorFilter(Color.GREEN)
        } else {
            binding.bBluetoothStatus.setColorFilter(Color.RED)
            binding.emptyList.text = "Bluetooth выключен"
            binding.emptyList.visibility = View.VISIBLE
        }
        dataModel.searchDeviceStatus.value = adapterIsEnabled
        getPairedDevices()
    }

    private fun saveMac(mac: String) {
        val editor = preferences?.edit()
        editor?.putString(BluetoothConstants.MAC, mac)
        editor?.apply()
    }

    private fun initSocket(mac: String) {
        val device = bAdapter.getRemoteDevice(mac)
        val myUUID = UUID.fromString("00001112-0000-1000-8000-00805f9b34fb")
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        btSocket = device.createRfcommSocketToServiceRecord(myUUID)
    }

    private val secondLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {

        }
    }

    override fun onClick(device: ListItem) {
        vibrateOrSound()
        //saveMac(device.address)
        //getPairedDevices()
        val intent = Intent(requireContext(), SecondActivity::class.java).apply {
            putExtra("name", device.name)
            putExtra("address", device.address)
         }
        secondLauncher.launch(intent)
    }
}

object BinaryHelperJava {

    fun convertFourBytesToLong(
        paramByte1: Byte,
        paramByte2: Byte,
        paramByte3: Byte,
        paramByte4: Byte
    ): Long {
        return ((paramByte4 and 0xFF.toByte()).toLong() shl 24) or ((paramByte3 and 0xFF.toByte()).toLong() shl 16) or ((paramByte2 and 0xFF.toByte()).toLong() shl 8) or (paramByte1 and 0xFF.toByte()).toLong()
    }

    fun getBits(paramInt1: Int, paramInt2: Int, paramInt3: Int): Int {
        return paramInt1 and 255 ushr 8 - paramInt3 shl paramInt2 shr paramInt2
    }

    fun isFlag(paramInt1: Int, paramInt2: Int): Boolean {
        return paramInt1 shr paramInt2 and 0x1 != 0
    }

    fun arrayToHexString(paramArrayOfByte: ByteArray?): String {
        if (paramArrayOfByte == null) {
            return "null"
        }
        if (paramArrayOfByte.size == 0) {
            return "[]"
        }
        val localStringBuilder = java.lang.StringBuilder(paramArrayOfByte.size * 6)
        localStringBuilder.append("HEX[")
        localStringBuilder.append(
            String.format(
                "%02X ", *arrayOf<Any>(
                    java.lang.Byte.valueOf(
                        paramArrayOfByte[0]
                    )
                )
            )
        )
        var i = 1
        while (i < paramArrayOfByte.size) {
            localStringBuilder.append(", ")
            localStringBuilder.append(
                String.format(
                    "%02X ", *arrayOf<Any>(
                        java.lang.Byte.valueOf(
                            paramArrayOfByte[i]
                        )
                    )
                )
            )
            i += 1
        }
        localStringBuilder.append(']')
        return localStringBuilder.toString()
    }
}

class BatteryInformation(characteristic: BluetoothGattCharacteristic) {
    var chargerBatteryLevel: Int? = null
    var chargerBatteryTemperature: Int? = null
    var chargerBatteryVoltage: Int? = null
    val flags: Int
    var holder1BatteryLevel: Int? = null
    var holder1BatteryTemperature: Int? = null
    var holder1BatteryVoltage: Int? = null
    var holder2BatteryLevel: Int? = null
    var holder2BatteryTemperature: Int? = null
    var holder2BatteryVoltage: Int? = null

    fun isFlag(position: Int): Boolean {
        return BinaryHelperJava.isFlag(flags, position)
    }

    init {

        flags = characteristic.getIntValue(18, 0)
        var offset = 0 + 2
        if (isFlag(0)) {
            chargerBatteryLevel = characteristic.getIntValue(17, offset)
            offset++
        }
        if (isFlag(1)) {
            chargerBatteryTemperature = characteristic.getIntValue(17, offset)
            offset++
        }
        if (isFlag(2)) {
            chargerBatteryVoltage = characteristic.getIntValue(18, offset)
            offset += 2
            //            offset++;
        }
        if (isFlag(3)) {
            holder1BatteryLevel = characteristic.getIntValue(17, offset)
            offset++
        }
        if (isFlag(4)) {
            holder1BatteryTemperature = characteristic.getIntValue(17, offset)
            offset++
        }
        if (isFlag(5)) {
            holder1BatteryVoltage = characteristic.getIntValue(18, offset)
            offset += 2
        }
        if (isFlag(6)) {
            holder2BatteryLevel = characteristic.getIntValue(17, offset)
            offset++
        }
        if (isFlag(7)) {
            holder2BatteryTemperature = characteristic.getIntValue(17, offset)
            offset++
        }
        if (isFlag(8)) {
            holder2BatteryVoltage = characteristic.getIntValue(18, offset)
        }
    }

    override fun toString(): String {
        return """BatteryInformation{
 flags (binary)=${Integer.toBinaryString(flags)},
 chargerBatteryLevel=${chargerBatteryLevel},
 chargerBatteryTemperature=${chargerBatteryTemperature},
 chargerBatteryVoltage=${chargerBatteryVoltage},
 holder1BatteryLevel=${holder1BatteryLevel},
 holder1BatteryTemperature=${holder1BatteryTemperature},
 holder1BatteryVoltage=${holder1BatteryVoltage},
 holder2BatteryLevel=${holder2BatteryLevel},
 holder2BatteryTemperature=${holder2BatteryTemperature},
 holder2BatteryVoltage=${holder2BatteryVoltage}
}"""
    }
}

class DeviceStatus(characteristic: BluetoothGattCharacteristic) {
    private val flags: Int
    private var chargerSystemError: ChargerSystemError? = null
    var chargerSystemStatus: ChargerSystemStatus? = null
    private var holder1Error: HolderErrorFlags? = null
    var holder1LastExperienceInfo: ExperienceInfo? = null
    private var holder1SystemError: Long? = null
    private var holder1TemperatureStatus: HolderAmbientTemperature? = null
    private var holder1Warning: HolderWarningFlags? = null
    private var holder2Error: HolderErrorFlags? = null
    var holder2LastExperienceInfo: ExperienceInfo? = null
    private var holder2SystemError: Long? = null
    private var holder2TemperatureStatus: HolderAmbientTemperature? = null
    private var holder2Warning: HolderWarningFlags? = null

    init {
        val value = characteristic.value

        flags = characteristic.getIntValue(18, 0)
        var offset = 0 + 2
        try {
            if (isFlag(0)) {
                chargerSystemError =
                    ChargerSystemError.Companion.fromInt(characteristic.getIntValue(17, offset))
                offset++
            }
            if (isFlag(1)) {
                chargerSystemStatus = ChargerSystemStatus(
                    characteristic.getIntValue(17, offset),
                    characteristic.getIntValue(17, offset + 1)
                )
                offset++
                offset++
            }
            if (isFlag(2) || isFlag(7)) {
                val holdersTemperatureStatus = characteristic.getIntValue(17, offset)
                offset++
                if (isFlag(2)) {
                    holder1TemperatureStatus = HolderAmbientTemperature.Companion.fromInt(
                        getBits(
                            holdersTemperatureStatus,
                            0,
                            2
                        )
                    )
                }
                if (isFlag(7)) {
                    holder2TemperatureStatus = HolderAmbientTemperature.Companion.fromInt(
                        getBits(
                            holdersTemperatureStatus,
                            2,
                            2
                        )
                    )
                }
            }
            if (isFlag(3)) {
                holder1Error =
                    HolderErrorFlags.fromInt(characteristic.getIntValue(17, offset))
                offset++
            }
            if (isFlag(4)) {
                holder1Warning =
                    HolderWarningFlags.fromInt(characteristic.getIntValue(17, offset))
                offset++
            }
            if (isFlag(5)) {
                holder1SystemError = BinaryHelperJava.convertFourBytesToLong(
                    value[offset],
                    value[offset + 1], value[offset + 2], value[offset + 3]
                )
                offset += 4
            }
            if (isFlag(6)) {
                holder1LastExperienceInfo = ExperienceInfo(
                    characteristic.getIntValue(17, offset),
                    characteristic.getIntValue(17, offset + 1)
                )
                offset++
                offset++
            }
            if (isFlag(8)) {
                holder2Error =
                    HolderErrorFlags.fromInt(characteristic.getIntValue(17, offset))
                offset++
            }
            if (isFlag(9)) {
                holder2Warning =
                    HolderWarningFlags.fromInt(characteristic.getIntValue(17, offset))
                offset++
            }
            if (isFlag(10)) {
                holder2SystemError = BinaryHelperJava.convertFourBytesToLong(
                    value[offset],
                    value[offset + 1], value[offset + 2], value[offset + 3]
                )
                offset += 4
            }
            if (isFlag(11)) {
                holder2LastExperienceInfo = ExperienceInfo(
                    characteristic.getIntValue(17, offset),
                    characteristic.getIntValue(17, offset + 1)
                )
            }
            //this.isFullyReceived = true;
        } catch (e: NullPointerException) {
            Log.w(TAG, "Is not fully received")
        } catch (e2: ArrayIndexOutOfBoundsException) {
            Log.w(TAG, "Is not fully received1")
        }
    }

    private fun isFlag(position: Int): Boolean {
        return isFlag(flags, position)
    }
}