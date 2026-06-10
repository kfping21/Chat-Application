package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjgsu.treehole.R

class FeedbackBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_feedback_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etFeedback = view.findViewById<EditText>(R.id.et_feedback)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit_feedback)

        try {
            ClickAnimations.addButtonPressAnimation(btnSubmit)
        } catch (e: Exception) { /* Ignore */ }

        btnSubmit.setOnClickListener {
            val feedback = etFeedback.text.toString().trim()
            if (feedback.isEmpty()) {
                Toast.makeText(requireContext(), "内容不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "感谢你的反馈 ✨ 我们会认真倾听", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}
