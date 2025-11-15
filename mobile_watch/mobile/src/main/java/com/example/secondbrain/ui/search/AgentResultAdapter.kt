package com.example.secondbrain.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.secondbrain.R
import com.example.secondbrain.data.model.AgentNoteResult

/**
 * AI 에이전트 추천 결과를 표시하는 RecyclerView 어댑터
 */
class AgentResultAdapter(
    private val onItemClick: (Long) -> Unit
) : ListAdapter<AgentNoteResult, AgentResultAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent_result, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvNoteTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvNotePreview: TextView = itemView.findViewById(R.id.tvNotePreview)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)

        fun bind(result: AgentNoteResult) {
            tvNoteTitle.text = result.title

            // 유사도 점수 표시
            result.similarityScore?.let { score ->
                val percentage = (score * 100).toInt()
                tvNotePreview.text = "유사도: ${percentage}%"
                tvNotePreview.visibility = View.VISIBLE
            } ?: run {
                tvNotePreview.visibility = View.GONE
            }

            // 추천 이유는 항상 숨김 (API에서 제공 안 함)
            tvReason.visibility = View.GONE

            itemView.setOnClickListener {
                onItemClick(result.id)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AgentNoteResult>() {
        override fun areItemsTheSame(
            oldItem: AgentNoteResult,
            newItem: AgentNoteResult
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: AgentNoteResult,
            newItem: AgentNoteResult
        ): Boolean {
            return oldItem == newItem
        }
    }
}
