package com.zjgsu.treehole.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.treehole.R
import com.zjgsu.treehole.adapter.MoodAdapter
import com.zjgsu.treehole.model.MoodItem
import com.zjgsu.treehole.network.CreatePostRequest
import com.zjgsu.treehole.network.RetrofitClient
import kotlinx.coroutines.launch
import java.util.Locale

class PostSecretFragment : Fragment() {

    private var selectedMood: MoodItem? = null
    private var isSubmitting = false
    private var selectedImageUri: Uri? = null

    private val moods = listOf(
        MoodItem("孤独", "🌙", "#4C7BFE", "#673AB7"),
        MoodItem("开心", "🌿", "#F5C024", "#FFA726"),
        MoodItem("后悔", "🍂", "#673AB7", "#9C27B0"),
        MoodItem("焦虑", "☁️", "#F472B6", "#EC4899"),
        MoodItem("平静", "🌊", "#34D399", "#10B981"),
        MoodItem("迷茫", "🌫️", "#FB923C", "#F59E0B"),
        MoodItem("感动", "💫", "#EC4899", "#F87171"),
        MoodItem("释然", "🕊️", "#8B5CF6", "#A78BFA")
    )

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            val preview = view?.findViewById<ImageView>(R.id.iv_image_preview)
            preview?.setImageURI(it)
            preview?.isVisible = true
        }
    }

    private val speechInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spokenText.isEmpty()) return@registerForActivityResult

        val etContent = view?.findViewById<EditText>(R.id.et_content) ?: return@registerForActivityResult
        val current = etContent.text?.toString().orEmpty()
        val merged = if (current.isBlank()) spokenText else "$current $spokenText"
        etContent.setText(merged.take(500))
        etContent.setSelection(etContent.text.length)
    }

    private val requestAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            openSpeechInput()
        } else {
            Toast.makeText(requireContext(), "需要麦克风权限才能语音输入", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_post_secret, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etContent = view.findViewById<EditText>(R.id.et_content)
        val tvCount = view.findViewById<TextView>(R.id.tv_char_count)
        val rvMoods = view.findViewById<RecyclerView>(R.id.rv_moods)
        val btnSubmit = view.findViewById<Button>(R.id.btn_submit)
        val btnClose = view.findViewById<View>(R.id.btn_close)
        val btnAddImage = view.findViewById<ImageButton>(R.id.btn_add_image)
        val btnVoice = view.findViewById<TextView>(R.id.btn_voice)
        val ivPreview = view.findViewById<ImageView>(R.id.iv_image_preview)

        // Add button animations
        ClickAnimations.addButtonPressAnimation(btnSubmit)
        ClickAnimations.addButtonPressAnimation(btnClose)
        ClickAnimations.addButtonPressAnimation(btnAddImage)
        ClickAnimations.addButtonPressAnimation(btnVoice)

        // Close button: navigate back
        btnClose.setOnClickListener { findNavController().navigateUp() }

        // Add image button (system picker, no storage permission needed)
        btnAddImage.setOnClickListener {
            openImagePicker()
        }

        btnVoice.setOnClickListener {
            if (hasRecordAudioPermission()) {
                openSpeechInput()
            } else {
                requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        // Image preview click to remove
        ivPreview.setOnClickListener {
            selectedImageUri = null
            ivPreview.isVisible = false
            ivPreview.setImageURI(null)
        }

        // Character counter
        etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCount.text = "${s?.length ?: 0}/500"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Moods RecyclerView (horizontal)
        rvMoods.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvMoods.adapter = MoodAdapter(moods) { mood ->
            selectedMood = mood
        }

        // Submit
        btnSubmit.setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            val content = etContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "请输入你的秘密", Toast.LENGTH_SHORT).show()
                etContent.requestFocus()
                return@setOnClickListener
            }
            if (selectedMood == null) {
                Toast.makeText(requireContext(), "请选择心情", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            isSubmitting = true
            btnSubmit.isEnabled = false
            btnSubmit.text = "埋下秘密中..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.postsApi.createPost(
                        CreatePostRequest(content, selectedMood!!.name)
                    )
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "秘密已埋下 ✨", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "发布失败", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        btnSubmit.isEnabled = true
                        btnSubmit.text = getString(R.string.btn_post)
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                    isSubmitting = false
                    btnSubmit.isEnabled = true
                    btnSubmit.text = getString(R.string.btn_post)
                }
            }
        }
    }

    private fun openImagePicker() {
        pickImage.launch("image/*")
    }

    private fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出你想发布的内容")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }
        }
        try {
            speechInputLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "当前设备不支持语音识别", Toast.LENGTH_SHORT).show()
        }
    }
}
