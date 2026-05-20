package com.weargluco.watch.data.ble

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import com.weargluco.watch.data.api.LibreLinkUpClient
import com.weargluco.watch.data.models.BleGlucoseData
import com.weargluco.watch.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class GlucoseBLEReceiverService : Service() {

    companion object {
        private const val TAG = "GlucoseBLEReceiver"
        val SERVICE_UUID = UUID.fromString("4F4F4C55-434C-5545-474C-55434F0000")
        val GLUCOSE_CHAR_UUID = UUID.fromString("4F4F4C55-434C-5545-474C-55434F0001")
        val METADATA_CHAR_UUID = UUID.fromString("4F4F4C55-434C-5545-474C-55434F0002")

        const val ACTION_DATA_UPDATED = "com.weargluco.watch.GLUCOSE_BLE_UPDATED"
        const val EXTRA_GLUCOSE_DATA = "glucose_data"

        var lastGlucoseData: BleGlucoseData? = null
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private var isScanning = false
    private var isConnected = false
    private var reconnectJob: Job? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name?.contains("GlucoWatch", ignoreCase = true) == true) {
                Log.d(TAG, "Found GlucoWatch peripheral: ${device.address}")
                stopScan()
                connectToDevice(device)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    isConnected = true
                    Log.d(TAG, "Connected to peripheral")
                    gatt.discoverServices()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    isConnected = false
                    Log.d(TAG, "Disconnected from peripheral")
                    gatt.close()
                    scheduleReconnect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val glucoseChar = service.getCharacteristic(GLUCOSE_CHAR_UUID)
                    if (glucoseChar != null) {
                        gatt.readCharacteristic(glucoseChar)
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                if (data != null) {
                    parseAndBroadcast(data)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch {
            startScanning()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        reconnectJob?.cancel()
        stopScan()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        super.onDestroy()
    }

    private fun startScanning() {
        if (isScanning || isConnected) return

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        isScanning = true
        bleScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Log.d(TAG, "Started BLE scanning for GlucoWatch peripheral")
    }

    private fun stopScan() {
        if (isScanning) {
            bleScanner.stopScan(scanCallback)
            isScanning = false
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        Log.d(TAG, "Connecting to ${device.address}")
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(10_000)
            startScanning()
        }
    }

    private fun parseAndBroadcast(data: ByteArray) {
        try {
            val jsonString = String(data)
            val glucoseData = LibreLinkUpClient.jsonParser
                .decodeFromString<BleGlucoseData>(jsonString)

            lastGlucoseData = glucoseData

            val intent = Intent(ACTION_DATA_UPDATED)
            intent.putExtra(EXTRA_GLUCOSE_DATA, glucoseData.value)
            sendBroadcast(intent)

            Log.d(TAG, "BLE glucose data: ${glucoseData.value} mmol/L, trend: ${glucoseData.trendLabel}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse BLE data", e)
        }
    }
}
