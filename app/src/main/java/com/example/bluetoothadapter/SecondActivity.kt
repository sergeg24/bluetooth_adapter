package com.example.bluetoothadapter

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.ScrollingMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.bluetoothadapter.data.ListItem
import com.example.bluetoothadapter.databinding.ActivitySecondBinding
import java.util.*

class SecondActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecondBinding
    private var scanner: BluetoothLeScanner? = null
    private  var putName: String? = ""
    private var putAddress: String? = ""
    private var clickDisconnectDevice = false
    private var deviceConnectGat: BluetoothGatt? = null
    private var MAXIMUM_CONNECTION_ATTEMPTS = 3
    private var currentConnectionAttempt = 0
    val UUID_RRP_SERVICE = UUID.fromString("daebb240-b041-11e4-9e45-0002a5d5c51b")
    val UUID_DEVICE_STATUS = UUID.fromString("ecdfa4c0-b041-11e4-8b67-0002a5d5c51b")
    val UUID_BATTERY_INFORMATION = UUID.fromString("f8a54120-b041-11e4-9be7-0002a5d5c51b")

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecondBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bAdapter = bManager.adapter

        scanner = bAdapter.bluetoothLeScanner

        putName = intent.getStringExtra("name")
        putAddress = intent.getStringExtra("address")

        binding.apply {

            secondTextViewName.text = putName
            secondTextViewAdress.text = putAddress

            btnConnect.setOnClickListener {
                currentConnectionAttempt = 0
                startReceiving()
            }

            terminal.text = "...\n"
            terminal.movementMethod = ScrollingMovementMethod()

            btnMainPrevActivity.setOnClickListener {
                finish()
            }
        }
    }

    private fun startReceiving(reconnect: Boolean = false) {

        val filter = ScanFilter.Builder()
            .setDeviceAddress(putAddress)
            .build()

        val filters = ArrayList<ScanFilter>()
            filters.add(filter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {

            binding.btnConnect.text = "Остановить"

            if (deviceConnectGat != null && !reconnect) {
                deviceConnectGat!!.disconnect()
                deviceConnectGat!!.close()
                binding.imageButtonConnect.setColorFilter(Color.RED)
                deviceConnectGat = null
                clickDisconnectDevice = false
                scanner?.stopScan(scanCallbackSelectedDevice)
                binding.terminal.append(colorText("Соединение разорвано", Color.RED))
                binding.btnConnect.text = "Соединиться"
                return
            }

            clickDisconnectDevice = if (clickDisconnectDevice && deviceConnectGat == null) {
                scanner?.stopScan(scanCallbackSelectedDevice)
                binding.imageButtonConnect.setColorFilter(Color.GRAY)
                binding.terminal.append("Поиск остановлен\n")
                binding.btnConnect.text = "Соединиться"
                false
            } else {
                scanner?.startScan(filters, settings, scanCallbackSelectedDevice)
                binding.terminal.append("Поиск устройства...\n")
                true
            }

        } catch (e: SecurityException) {
            Log.d(TAG, e.message.toString())
        }
    }

    /**
     * Опускаем текст терминала вниз
     */
    private fun terminalScrollDown() {
        binding.apply {
             terminal.layout?.let {
                val scrollDelta = it.getLineBottom(terminal.lineCount - 1) - terminal.scrollY - terminal.height
                if (scrollDelta > 0) {
                    terminal.scrollBy(0, scrollDelta + 40)
                }
            }
        }
    }

    /**
     * Меняет цвет текста
     */
    private fun colorText(
        text: String,
        color: Int = Color.WHITE,
    ): SpannableString {
        val spannable = SpannableString(text + "\n")
        val start = text.indexOf(text)
        val end = start + text.length
        val colorSpan = ForegroundColorSpan(color)
        spannable.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    private var scanCallbackSelectedDevice = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                try {
                    scanner?.stopScan(this)
                    binding.terminal.append(colorText("Устройство ${it.device.name} найдено, подключаюсь...", Color.CYAN))
                    terminalScrollDown()
                    deviceConnectGat = it.device.connectGatt(this@SecondActivity, false, gattCallback)
                } catch (e: SecurityException) {}
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        runOnUiThread {
                            binding.btnConnect.text = "Соединено"
                            binding.terminal.append(colorText("Соединение установлено", Color.GREEN))
                            terminalScrollDown()
                            binding.imageButtonConnect.setColorFilter(Color.GREEN)
                            clickDisconnectDevice = false
                        }
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        gatt.close()
                    }

                } else {
                    gatt.close()
                    currentConnectionAttempt += 1
                    if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                        runOnUiThread {
                            binding.terminal.append(colorText("Попытка подключения $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS", Color.YELLOW))
                            terminalScrollDown()
                            binding.imageButtonConnect.setColorFilter(Color.YELLOW)
                        }
                        startReceiving(true)
                    } else {
                        binding.imageButtonConnect.setColorFilter(Color.RED)
                        binding.btnConnect.text = "Соединиться"
                        binding.terminal.append(colorText("Не удалось подключиться к устройству ble", Color.RED))
                    }
                }
            } catch (e: SecurityException) {}
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
                            val descriptor1 = char1.getDescriptor(convertFromInteger(0x2902))

//                            Log.d("MyLog", Build.VERSION_CODES.O.toString())
//                            Log.d("MyLog", Build.VERSION_CODES.S.toString())
//                            Log.d("MyLog", Build.VERSION.SDK_INT.toString())

                            /** huawei samsung
                             *   26      26
                             *   31      31
                             *   29      33
                             */

                            setCharacteristicNotification(char1, true)

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

                val battery = BatteryInformation(characteristic)

                var textTerminal = "-----------------------\n"
                    textTerminal += "chargerBatteryLevel: ${battery.chargerBatteryLevel}\n"
                    textTerminal += "chargerBatteryTemperature: ${battery.chargerBatteryTemperature}\n"
                    textTerminal += "chargerBatteryVoltage: ${battery.chargerBatteryVoltage}\n"
                    textTerminal += "holder1BatteryLevel: ${battery.holder1BatteryLevel}\n"
                    textTerminal += "holder1BatteryTemperature: ${battery.holder1BatteryTemperature}\n"
                    textTerminal += "holder1BatteryVoltage: ${battery.holder1BatteryVoltage}\n"
                    textTerminal += "holder2BatteryLevel: ${battery.holder2BatteryLevel}\n"
                    textTerminal += "holder2BatteryTemperature: ${battery.holder2BatteryTemperature}\n"
                    textTerminal += "holder2BatteryVoltage: ${battery.holder2BatteryVoltage}\n"

                runOnUiThread {
                    binding.terminal.append(textTerminal)
                    terminalScrollDown()
                }
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
    }
}