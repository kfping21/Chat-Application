package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjgsu.treehole.R
import com.zjgsu.treehole.cache.DraftManager

class DraftsBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.layout_drafts_bottom_sheet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvEmpty = view.findViewById<TextView>(R.id.tv_empty_drafts)
        val llDraftItem = view.findViewById<LinearLayout>(R.id.ll_draft_item)
        val tvDraftContent = view.findViewById<TextView>(R.id.tv_draft_content)
        val btnClear = view.findViewById<Button>(R.id.btn_clear_drafts)

        val draft = DraftManager.getDraft(requireContext())

        if (draft.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            llDraftItem.visibility = View.GONE
            btnClear.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            llDraftItem.visibility = View.VISIBLE
            btnClear.visibility = View.VISIBLE
            tvDraftContent.text = draft
        }

        llDraftItem.setOnClickListener {
            // Navigate to PostSecretFragment
            val navController = requireActivity().supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)?.findNavController()
            
            navController?.navigate(R.id.nav_post)
            dismiss()
        }

        btnClear.setOnClickListener {
            DraftManager.clearDraft(requireContext())
            Toast.makeText(requireContext(), "草稿已清空", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}
