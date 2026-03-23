package com.zjgsu.treehole.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.model.MockData

class MyHollowFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_my_hollow, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<RecyclerView>(R.id.rv_my_secrets)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = SecretAdapter(MockData.mySecrets)

        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            // Logout placeholder
        }
    }
}
