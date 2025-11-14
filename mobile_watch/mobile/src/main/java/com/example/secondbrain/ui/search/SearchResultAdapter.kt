package com.example.secondbrain.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.secondbrain.R
import com.example.secondbrain.data.model.NoteSearchResult

class SearchResultAdapter(
    private val onItemClick: (Long) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

    private var searchResults: List<NoteSearchResult> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(searchResults[position])
    }

    override fun getItemCount(): Int = searchResults.size

    fun updateResults(results: List<NoteSearchResult>) {
        searchResults = results
        notifyDataSetChanged()
    }

    class SearchResultViewHolder(
        itemView: View,
        private val onItemClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvNoteTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvNotePreview: TextView = itemView.findViewById(R.id.tvNotePreview)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)

        fun bind(result: NoteSearchResult) {
            tvNoteTitle.text = result.title
            tvNotePreview.text = result.content.take(100) // 100자까지만 미리보기
            tvUpdatedAt.text = formatDateTime(result.updatedAt)

            itemView.setOnClickListener {
                onItemClick(result.id)
            }
        }

        private fun formatDateTime(dateTime: String): String {
            // 간단한 날짜 포맷 (ISO 8601 형식 -> 간단한 표시)
            return try {
                val parts = dateTime.split("T")
                if (parts.size >= 2) {
                    val date = parts[0]
                    val time = parts[1].substring(0, 5) // HH:mm만 표시
                    "$date $time"
                } else {
                    dateTime
                }
            } catch (e: Exception) {
                dateTime
            }
        }
    }
}
