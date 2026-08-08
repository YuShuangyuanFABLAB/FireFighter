package com.example.firefighterterminal.presentation.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.firefighterterminal.data.ble.FireMessage
import com.example.firefighterterminal.databinding.FragmentFireMapBinding
import com.example.firefighterterminal.presentation.ui.map.view.LightRenderer

class FireMapFragment : Fragment() {

    private var _binding: FragmentFireMapBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: FireMapViewModel

    companion object { fun newInstance() = FireMapFragment() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFireMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[FireMapViewModel::class.java]

        viewModel.mapConfig.observe(viewLifecycleOwner) { config: FireMessage.MapConfig? ->
            config?.let { binding.fireMapView.updateMapConfig(it) }
        }

        viewModel.priorities.observe(viewLifecycleOwner) { priorities ->
            binding.fireMapView.updatePriorities(priorities)
        }

        viewModel.fireData.observe(viewLifecycleOwner) { data ->
            binding.fireMapView.updateFireData(data)
            data?.lightConfigs?.let { configs ->
                val lightPositions = configs.map { (id, info) ->
                    LightRenderer.LightPos(id, info.x, info.y, info.type)
                }
                binding.fireMapView.updateLightConfig(lightPositions)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
