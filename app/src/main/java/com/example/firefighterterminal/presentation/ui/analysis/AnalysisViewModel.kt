package com.example.firefighterterminal.presentation.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.firefighterterminal.data.repository.FireDataRepository
import com.example.firefighterterminal.data.repository.FireDataSnapshot

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val _timelineText = MutableLiveData<String>()
    val timelineText: LiveData<String> = _timelineText

    private val _voiceText = MutableLiveData<String>()
    val voiceText: LiveData<String> = _voiceText

    private val _spreadTrend = MutableLiveData<String>()
    val spreadTrend: LiveData<String> = _spreadTrend

    private val _exitStatus = MutableLiveData<String>()
    val exitStatus: LiveData<String> = _exitStatus

    private val timelineBuilder = StringBuilder()
    private var lastFireCount = 0
    private var lastTrappedCount = 0

    init {
        FireDataRepository.initialize()
        FireDataRepository.fireData.observeForever { data -> data?.let { update(it) } }
    }

    private fun update(data: FireDataSnapshot) {
        // 火灾时间线
        val trappedCount = data.directions.count { it.value == 4 }
        if (data.fireCount != lastFireCount) {
            if (data.fireCount > lastFireCount) appendTimeline("🔥 火灾触发")
            else appendTimeline("✅ 火灾解除 (剩余${data.fireCount}处)")
            lastFireCount = data.fireCount
        }
        if (trappedCount != lastTrappedCount) {
            if (trappedCount > lastTrappedCount) appendTimeline("⚠️ 新增被困区域")
            else appendTimeline("✅ 被困区域解除")
            lastTrappedCount = trappedCount
        }
        _timelineText.postValue(timelineBuilder.toString().ifEmpty { "暂无事件" })

        // 语音播报
        _voiceText.postValue(when (data.voiceMode?.mode) {
            0 -> "系统正常，无火灾"
            1 -> "🔈 室内起火，立刻按应急绿灯指示方向逃离"
            2 -> "🔈 双闪区域立刻进入有窗房间，湿衣物堵住门缝口鼻，等待救援（随后播报疏散指引）"
            else -> "语音模块未连接"
        })

        // 火势趋势
        _spreadTrend.postValue(computeTrend(data))

        // 出口状态
        _exitStatus.postValue(computeExits(data))
    }

    private fun appendTimeline(event: String) {
        val now = System.currentTimeMillis()
        val ts = String.format("%tM:%tS", now, now)
        timelineBuilder.insert(0, "$ts  $event\n")
        if (timelineBuilder.length > 5000) timelineBuilder.setLength(5000)
    }

    private fun computeTrend(data: FireDataSnapshot): String {
        val fires = data.fires
        if (fires.isEmpty()) return "暂无火情"
        if (fires.size < 2) return "火势稳定，监控中"
        val avgX = fires.map { it.x }.average(); val avgY = fires.map { it.y }.average()
        val dh = when { avgX > 5.5 -> "东"; avgX < 3.5 -> "西"; else -> "" }
        val dv = when { avgY > 2.5 -> "南"; avgY < 1.5 -> "北"; else -> "" }
        return "蔓延方向: $dv$dh".ifEmpty { "无明显方向" }
    }

    private fun computeExits(data: FireDataSnapshot): String {
        val exits = data.mapConfig?.exits ?: return "未知"
        if (data.fires.isEmpty()) return "可用出口: ${exits.size}/${exits.size}"
        val blocked = exits.count { e -> data.fires.any { f ->
            kotlin.math.abs(e.x - f.x) + kotlin.math.abs(e.y - f.y) <= 2
        } }
        val available = exits.size - blocked
        return "可用出口: $available/${exits.size}" + if (blocked > 0) " ⚠️ $blocked 处受火情威胁" else ""
    }

    override fun onCleared() { super.onCleared() }
}
