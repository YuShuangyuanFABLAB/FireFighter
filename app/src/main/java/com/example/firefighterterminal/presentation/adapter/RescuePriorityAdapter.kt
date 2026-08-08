package com.example.firefighterterminal.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firefighterterminal.R
import com.example.firefighterterminal.domain.model.RescuePriority

class RescuePriorityAdapter : RecyclerView.Adapter<RescuePriorityAdapter.ViewHolder>() {

    private var items: List<RescuePriority> = emptyList()

    fun updateData(list: List<RescuePriority>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = items[position]
        holder.bind(p)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val line1: TextView = view.findViewById(android.R.id.text1)
        private val line2: TextView = view.findViewById(android.R.id.text2)

        fun bind(p: RescuePriority) {
            val color = when (p.level) {
                com.example.firefighterterminal.domain.model.PriorityLevel.P0 -> Color.parseColor("#ff6b6b")
                com.example.firefighterterminal.domain.model.PriorityLevel.P1 -> Color.parseColor("#f39c12")
                com.example.firefighterterminal.domain.model.PriorityLevel.P2 -> Color.parseColor("#ffd700")
                com.example.firefighterterminal.domain.model.PriorityLevel.P3 -> Color.parseColor("#a0a0a0")
            }
            line1.text = "${p.level.emoji} ${p.level.label} — L${p.lightId} (${p.position.x},${p.position.y})"
            line1.setTextColor(color)
            line2.text = p.reason
            line2.setTextColor(Color.argb(180, 255, 255, 255))
        }
    }
}
