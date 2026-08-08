package com.example.firefighterterminal.presentation.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.firefighterterminal.data.repository.FireDataRepository
import com.example.firefighterterminal.data.repository.RescuePriorityCalculator
import com.example.firefighterterminal.domain.model.Position
import com.example.firefighterterminal.domain.model.RescuePriority

class FireMapViewModel(application: Application) : AndroidViewModel(application) {

    val mapConfig = FireDataRepository.mapConfig
    val fireData = FireDataRepository.fireData

    private val _priorities = MutableLiveData<List<RescuePriority>>()
    val priorities: LiveData<List<RescuePriority>> = _priorities

    private val calculator = RescuePriorityCalculator()

    init {
        FireDataRepository.initialize()
        FireDataRepository.fireData.observeForever { data ->
            data?.let { computePriorities(it) }
        }
    }

    private fun computePriorities(data: com.example.firefighterterminal.data.repository.FireDataSnapshot) {
        val lights = data.lightConfigs?.map { (id, cfg) ->
            RescuePriorityCalculator.LightState(id, Position(cfg.x, cfg.y), data.directions[id] ?: 0)
        } ?: emptyList()
        _priorities.postValue(calculator.compute(lights, data.fires, emptyList()))
    }

    override fun onCleared() { super.onCleared() }
}
