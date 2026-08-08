package com.example.firefighterterminal.presentation.ui.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.firefighterterminal.databinding.FragmentAnalysisBinding

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AnalysisViewModel

    companion object { fun newInstance() = AnalysisFragment() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[AnalysisViewModel::class.java]

        viewModel.timelineText.observe(viewLifecycleOwner) { binding.textTimeline.text = it }
        viewModel.voiceText.observe(viewLifecycleOwner) { binding.textVoice.text = it }
        viewModel.spreadTrend.observe(viewLifecycleOwner) { binding.textTrend.text = it }
        viewModel.exitStatus.observe(viewLifecycleOwner) { binding.textExitStatus.text = it }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
