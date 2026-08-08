package com.example.firefighterterminal.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firefighterterminal.domain.model.IotDevice

class DeviceListAdapter(
    private val onDeviceClick: (IotDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    private var devices: List<IotDevice> = emptyList()

    fun updateDevices(list: List<IotDevice>) {
        devices = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(devices[position])
        holder.itemView.setOnClickListener { onDeviceClick(devices[position]) }
    }

    override fun getItemCount(): Int = devices.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(android.R.id.text1)
        private val addr: TextView = view.findViewById(android.R.id.text2)

        fun bind(d: IotDevice) {
            name.text = "🔵 ${d.name}"
            addr.text = "${d.address}  信号: ${d.rssi}dBm"
        }
    }
}
