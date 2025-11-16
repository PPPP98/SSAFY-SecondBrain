package com.example.secondbrain.ui.note

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.secondbrain.data.model.Note

/**
 * ViewPager2 어댑터
 * 노트 간 스와이프 네비게이션 제공
 */
class NotePagerAdapter(
    fragmentActivity: FragmentActivity,
    private val notes: List<Note>
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = notes.size

    override fun createFragment(position: Int): Fragment {
        return NoteContentFragment.newInstance(notes[position])
    }

    fun getNoteIdAt(position: Int): Long {
        return notes.getOrNull(position)?.id ?: -1L
    }
}
