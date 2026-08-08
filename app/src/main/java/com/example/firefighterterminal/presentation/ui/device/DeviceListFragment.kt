package com.example.firefighterterminal.presentation.ui.device

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firefighterterminal.databinding.FragmentDeviceListBinding
import com.example.firefighterterminal.domain.model.IotDevice
import com.example.firefighterterminal.presentation.adapter.DeviceListAdapter

class DeviceListFragment : Fragment() {

    private var _binding: FragmentDeviceListBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DeviceViewModel
    private var onDeviceSelectedListener: ((IotDevice) -> Unit)? = null
    private val adapter = DeviceListAdapter { device -> onDeviceSelectedListener?.invoke(device) }

    companion object { fun newInstance() = DeviceListFragment() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDeviceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[DeviceViewModel::class.java]

        binding.recyclerDevices.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDevices.adapter = adapter

        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            adapter.updateDevices(devices)
            if (devices.isEmpty()) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.recyclerDevices.visibility = View.GONE
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerDevices.visibility = View.VISIBLE
            }
        }

        viewModel.scanState.observe(viewLifecycleOwner) { state ->
            binding.btnScan.text = if (state is com.example.firefighterterminal.data.ble.ScanState.Scanning) "扫描中..." else "扫描设备"
        }

        binding.btnScan.setOnClickListener { viewModel.startScan() }
    }

    fun setOnDeviceSelectedListener(listener: (IotDevice) -> Unit) {
        onDeviceSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
