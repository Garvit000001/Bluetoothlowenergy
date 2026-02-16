package com.example.bluetoothlowenergy

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.UUID

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity() {

    private var isAdvertising = false
    private lateinit var advertiseButton: Button
    private lateinit var scanButton: Button
    private lateinit var bluetoothSwitch: Switch

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val advertiser by lazy { bluetoothAdapter?.bluetoothLeAdvertiser }
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true
            advertiseButton.text = "Stop Advertising"
            Log.d("MainActivity", "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("MainActivity", "Advertising failed with error code: $errorCode")
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        bluetoothSwitch.isChecked = true
                        advertiseButton.isEnabled = true
                        scanButton.isEnabled = true
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        bluetoothSwitch.isChecked = false
                        advertiseButton.isEnabled = false
                        scanButton.isEnabled = false
                        stopAdvertising()
                    }
                }
            }
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.entries.any { !it.value }) {
                // Handle the case where the user denies permissions
            } else {
                setupButtons()
                setupBluetoothSwitch()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        advertiseButton = findViewById(R.id.advertise_button)
        scanButton = findViewById(R.id.scan_button)
        bluetoothSwitch = findViewById(R.id.bluetooth_switch)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        } else {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
    }

    override fun onResume() {
        super.onResume()
        bluetoothSwitch.isChecked = bluetoothAdapter?.isEnabled == true
        advertiseButton.isEnabled = bluetoothAdapter?.isEnabled == true
        scanButton.isEnabled = bluetoothAdapter?.isEnabled == true
    }

    private fun setupButtons() {
        advertiseButton.setOnClickListener {
            if (isAdvertising) {
                stopAdvertising()
            } else {
                startAdvertising()
            }
        }
        scanButton.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBluetoothSwitch() {
        bluetoothSwitch.isChecked = bluetoothAdapter?.isEnabled == true
        bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (bluetoothAdapter?.isEnabled == false) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@setOnCheckedChangeListener
                    }
                    startActivity(enableBtIntent)
                }
            } else {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun startAdvertising() {
        if (bluetoothAdapter?.isEnabled == false) {
            return
        }

        if (bluetoothAdapter?.isMultipleAdvertisementSupported == false) {
            Log.e("MainActivity", "Multiple advertisement not supported")
            return
        }

        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val parcelUuid = ParcelUuid(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(parcelUuid)
            .build()

        advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

    private fun stopAdvertising() {
        if (bluetoothAdapter?.isEnabled == false) {
            return
        }

        advertiser?.stopAdvertising(advertiseCallback)
        isAdvertising = false
        advertiseButton.text = "Start Advertising"
        Log.d("MainActivity", "Advertising stopped")
    }

    override fun onPause() {
        super.onPause()
        if (isAdvertising) {
            stopAdvertising()
        }
    }
}
