package com.example.secondbrain.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.secondbrain.R
import com.example.secondbrain.data.model.Note
import java.text.SimpleDateFormat
import java.util.*

/**
 * 노트 콘텐츠를 표시하는 Fragment
 * ViewPager2에서 개별 노트를 표시하기 위해 사용됨
 */
class NoteContentFragment : Fragment() {

    private lateinit var tvTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvContent: TextView

    companion object {
        private const val ARG_NOTE_ID = "note_id"
        private const val ARG_NOTE_TITLE = "note_title"
        private const val ARG_NOTE_CONTENT = "note_content"
        private const val ARG_NOTE_DATE = "note_date"

        fun newInstance(note: Note): NoteContentFragment {
            return NoteContentFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_NOTE_ID, note.id)
                    putString(ARG_NOTE_TITLE, note.title)
                    putString(ARG_NOTE_CONTENT, note.content)
                    putString(ARG_NOTE_DATE, note.createdAt)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_note_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        displayNoteContent()
    }

    private fun initializeViews(view: View) {
        tvTitle = view.findViewById(R.id.tvTitle)
        tvDate = view.findViewById(R.id.tvDate)
        tvContent = view.findViewById(R.id.tvContent)
    }

    private fun displayNoteContent() {
        arguments?.let { args ->
            tvTitle.text = args.getString(ARG_NOTE_TITLE, "제목 없음")
            tvContent.text = args.getString(ARG_NOTE_CONTENT, "내용 없음")

            val createdAt = args.getString(ARG_NOTE_DATE, "")
            tvDate.text = formatDate(createdAt)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }
}
