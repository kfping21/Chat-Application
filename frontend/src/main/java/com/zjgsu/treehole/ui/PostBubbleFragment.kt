package com.zjgsu.treehole.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R

class PostBubbleFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_bubble, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val btnPublish = view.findViewById<Button>(R.id.btn_publish)
        val etContent = view.findViewById<EditText>(R.id.et_content)
        val tvCharCount = view.findViewById<TextView>(R.id.tv_char_count)
        val rvTags = view.findViewById<RecyclerView>(R.id.rv_tags)

        btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCharCount.text = "${s?.length ?: 0}/20"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPublish.setOnClickListener {
            val text = etContent.text.toString()
            if (text.isBlank()) {
                Toast.makeText(requireContext(), "冒泡内容不能为空哦", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "冒泡发布成功！", Toast.LENGTH_SHORT).show()
            BubblePondFragment.pendingSelfBubble = text
            findNavController().navigateUp()
        }

        val tags = listOf(
            Pair("💬", "找聊天搭子"), Pair("🎧", "找连麦搭子"), Pair("📚", "找自习搭子"), Pair("🎮", "找游戏搭子"),
            Pair("🍉", "树洞吐槽DD"), Pair("💼", "搬砖打工DD"), Pair("🎓", "考研党DD"), Pair("✈️", "热爱旅行DD"),
            Pair("👼", "情绪安慰DD"), Pair("🍜", "干饭贼香"), Pair("🐟", "快乐摸鱼"), Pair("🙋", "提个问题")
        )

        rvTags.layoutManager = GridLayoutManager(requireContext(), 4)
        rvTags.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_item_bubble_tag, parent, false)
                return object : RecyclerView.ViewHolder(itemView) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tag = tags[position]
                val tvIcon = holder.itemView.findViewById<TextView>(R.id.tv_tag_icon)
                val tvText = holder.itemView.findViewById<TextView>(R.id.tv_tag_text)
                tvIcon.text = tag.first
                tvText.text = tag.second
                
                holder.itemView.setOnClickListener {
                    val currentText = etContent.text.toString()
                    val prefix = "${tag.first} ${tag.second}\n"
                    etContent.setText(prefix + currentText)
                    etContent.setSelection(etContent.text.length)
                }
            }

            override fun getItemCount() = tags.size
        }
    }

}
