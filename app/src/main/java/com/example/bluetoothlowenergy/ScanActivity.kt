package com.example.bluetoothlowenergy

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("MissingPermission")
class ScanActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val scanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            // Handle click on a scan result
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val index = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (index != -1) {
                scanResults[index] = result
                scanResultAdapter.notifyItemChanged(index)
            } else {
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                val index = scanResults.indexOfFirst { it.device.address == result.device.address }
                if (index != -1) {
                    scanResults[index] = result
                    scanResultAdapter.notifyItemChanged(index)
                } else {
                    scanResults.add(result)
                    scanResultAdapter.notifyItemInserted(scanResults.size - 1)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            // Handle scan failure
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        val scanResultsRecyclerView = findViewById<RecyclerView>(R.id.scan_results_recycler_view)
        scanResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        scanResultsRecyclerView.adapter = scanResultAdapter

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        scanner?.startScan(scanCallback)
    }

    override fun onPause() {
        super.onPause()
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        scanner?.stopScan(scanCallback)
    }

    private class ScanResultAdapter(
        private val items: List<ScanResult>,
        private val onClick: (ScanResult) -> Unit
    ) : RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.device_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item)
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val deviceNameTextView: TextView = itemView.findViewById(R.id.device_name)

            fun bind(result: ScanResult) {
                if (ActivityCompat.checkSelfPermission(
                        itemView.context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                deviceNameTextView.text = result.device.name ?: "Unnamed"
                itemView.setOnClickListener { onClick(result) }
            }
        }
    }
}