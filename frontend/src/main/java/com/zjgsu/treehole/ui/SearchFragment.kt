package com.zjgsu.treehole.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.SecretAdapter
import com.zjgsu.treehole.cache.SearchHistoryManager
import com.zjgsu.treehole.model.Secret
import com.zjgsu.treehole.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var btnSearchAction: TextView
    private lateinit var btnClearHistory: ImageView

    private lateinit var llHistory: LinearLayout
    private lateinit var rvHistory: RecyclerView
    private lateinit var llResults: LinearLayout
    private lateinit var rvResults: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    private val historyList = mutableListOf<String>()
    private lateinit var historyAdapter: SearchHistoryAdapter
    private lateinit var resultAdapter: SecretAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearch = view.findViewById(R.id.et_search_query)
        btnBack = view.findViewById(R.id.btn_back_search)
        btnSearchAction = view.findViewById(R.id.btn_search_action)
        btnClearHistory = view.findViewById(R.id.btn_clear_history)

        llHistory = view.findViewById(R.id.ll_search_history)
        rvHistory = view.findViewById(R.id.rv_search_history)
        llResults = view.findViewById(R.id.ll_search_results)
        rvResults = view.findViewById(R.id.rv_search_results)
        progressBar = view.findViewById(R.id.progress_bar_search)
        tvEmpty = view.findViewById(R.id.tv_empty_search)

        setupListeners()
        setupHistoryAdapter()
        loadHistory()
        
        // Focus search bar
        etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            hideKeyboard()
            findNavController().navigateUp()
        }

        btnSearchAction.setOnClickListener {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                performSearch(query)
            }
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }

        btnClearHistory.setOnClickListener {
            SearchHistoryManager.clearHistory(requireContext())
            loadHistory()
        }
    }

    private fun setupHistoryAdapter() {
        historyAdapter = SearchHistoryAdapter(
            historyList,
            onItemClick = { query ->
                etSearch.setText(query)
                etSearch.setSelection(query.length)
                performSearch(query)
            },
            onDeleteClick = { query ->
                val prefs = requireContext().getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)
                val newList = SearchHistoryManager.getHistory(requireContext()).toMutableList()
                newList.remove(query)
                prefs.edit().putString("search_history_list", com.google.gson.Gson().toJson(newList)).apply()
                loadHistory()
            }
        )
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter
    }

    private fun loadHistory() {
        val history = SearchHistoryManager.getHistory(requireContext())
        historyList.clear()
        historyList.addAll(history)
        historyAdapter.notifyDataSetChanged()

        if (historyList.isEmpty()) {
            llHistory.visibility = View.GONE
        } else {
            llHistory.visibility = View.VISIBLE
        }
    }

    private fun performSearch(query: String) {
        hideKeyboard()
        SearchHistoryManager.addHistory(requireContext(), query)
        
        llHistory.visibility = View.GONE
        llResults.visibility = View.GONE
        tvEmpty.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.postsApi.searchPosts(query, 1, 50)
                }
                
                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val posts = response.body()?.posts ?: emptyList()
                    if (posts.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        llResults.visibility = View.VISIBLE
                        showResults(posts)
                    }
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun showResults(posts: List<com.zjgsu.treehole.network.PostDto>) {
        val secrets = posts.map { post ->
            Secret(
                id = post.id,
                content = post.content,
                mood = post.mood,
                likes = post.likes,
                comments = post.commentCount,
                timeAgo = com.zjgsu.treehole.util.TimeUtils.formatTimeAgo(post.createdAt),
                avatar = post.user?.avatar ?: "",
                nickname = post.user?.nickname ?: "匿名用户",
                isLiked = post.isLiked,
                userId = post.user?.id ?: "",
                imageUrls = post.imageUrls
            )
        }

        resultAdapter = SecretAdapter(secrets)
        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = resultAdapter
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    inner class SearchHistoryAdapter(
        private val items: List<String>,
        private val onItemClick: (String) -> Unit,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvKeyword: TextView = view.findViewById(R.id.tv_history_keyword)
            val btnDelete: ImageView = view.findViewById(R.id.btn_delete_history)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val keyword = items[position]
            holder.tvKeyword.text = keyword
            holder.itemView.setOnClickListener { onItemClick(keyword) }
            holder.btnDelete.setOnClickListener { onDeleteClick(keyword) }
        }

        override fun getItemCount() = items.size
    }
}
