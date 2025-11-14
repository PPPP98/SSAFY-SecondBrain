package com.example.secondbrain.ui.search

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.secondbrain.R
import com.example.secondbrain.data.model.NoteSearchResult

class SearchResultAdapter(
    private val onItemClick: (Long) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

    private var searchResults: List<NoteSearchResult> = emptyList()
    private var searchKeyword: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(searchResults[position], searchKeyword)
    }

    override fun getItemCount(): Int = searchResults.size

    fun updateResults(newResults: List<NoteSearchResult>, keyword: String = "") {
        val diffCallback = NoteSearchDiffCallback(searchResults, newResults)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        searchResults = newResults
        searchKeyword = keyword
        diffResult.dispatchUpdatesTo(this)
    }

    private class NoteSearchDiffCallback(
        private val oldList: List<NoteSearchResult>,
        private val newList: List<NoteSearchResult>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    class SearchResultViewHolder(
        itemView: View,
        private val onItemClick: (Long) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvNoteTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvNotePreview: TextView = itemView.findViewById(R.id.tvNotePreview)
        private val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)

        fun bind(result: NoteSearchResult, keyword: String) {
            // 제목에 하이라이트 적용
            tvNoteTitle.text = highlightKeyword(result.title, keyword)

            // 내용 미리보기에 하이라이트 적용
            val preview = result.content.take(100)
            tvNotePreview.text = highlightKeyword(preview, keyword)

            tvUpdatedAt.text = formatDateTime(result.updatedAt)

            itemView.setOnClickListener {
                onItemClick(result.id)
            }
        }

        private fun highlightKeyword(text: String, keyword: String): SpannableString {
            val spannableString = SpannableString(text)

            if (keyword.isNotEmpty()) {
                var startIndex = text.indexOf(keyword, ignoreCase = true)
                while (startIndex >= 0) {
                    val endIndex = startIndex + keyword.length

                    // 노란색 배경 + 볼드체로 하이라이트
                    spannableString.setSpan(
                        BackgroundColorSpan(0xFFFFE082.toInt()), // 노란색 배경
                        startIndex,
                        endIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    spannableString.setSpan(
                        StyleSpan(Typeface.BOLD),
                        startIndex,
                        endIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    startIndex = text.indexOf(keyword, endIndex, ignoreCase = true)
                }
            }

            return spannableString
        }

        private fun formatDateTime(dateTime: String): String {
            return try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Android 8.0 (API 26) 이상: DateTimeFormatter 사용
                    val inputFormatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
                    val outputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    val dateTimeParsed = java.time.LocalDateTime.parse(dateTime, inputFormatter)
                    dateTimeParsed.format(outputFormatter)
                } else {
                    // Android 7.x 이하: SimpleDateFormat 사용
                    val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                    val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    val date = inputFormat.parse(dateTime.substringBefore("."))
                    if (date != null) {
                        outputFormat.format(date)
                    } else {
                        dateTime
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SearchResultAdapter", "Date parsing failed: $dateTime", e)
                dateTime // 파싱 실패 시 원본 반환
            }
        }
    }
}
