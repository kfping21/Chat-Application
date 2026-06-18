package com.zjgsu.treehole.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.zjgsu.treehole.network.DeepSeekApi
import com.zjgsu.treehole.network.RetrofitClient
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.zjgsu.treehole.cache.DraftManager

class PostSecretFragment : Fragment() {

    private var selectedMood: MoodItem? = null
    private var isSubmitting = false
    private val selectedImageUris = mutableListOf<Uri>()
    private var aiCall: okhttp3.Call? = null

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
            if (selectedImageUris.size < 2) {
                selectedImageUris.add(it)
                updateImagePreviews()
            } else {
                Toast.makeText(requireContext(), "最多只能添加 2 张图片哦", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_post_secret, container, false)

    private fun getBase64FromUri(uri: Uri): String? {
        return try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(requireContext().contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
            
            // Downscale to max 512px for extreme speed
            val maxDim = 512f
            val scale = Math.min(maxDim / bitmap.width, maxDim / bitmap.height)
            val scaledBitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap
            
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etContent = view.findViewById<EditText>(R.id.et_content)
        val tvCount = view.findViewById<TextView>(R.id.tv_char_count)
        val rvMoods = view.findViewById<RecyclerView>(R.id.rv_moods)
        val btnSubmit = view.findViewById<View>(R.id.btn_submit)
        val btnClose = view.findViewById<View>(R.id.btn_close)
        val btnAddImage = view.findViewById<View>(R.id.btn_add_image)
        val btnAi = view.findViewById<View>(R.id.btn_ai_inspiration)
        val llAiDefault = view.findViewById<View>(R.id.ll_ai_default)
        val llAiGenerating = view.findViewById<View>(R.id.ll_ai_generating)
        val btnAiCancel = view.findViewById<View>(R.id.btn_ai_cancel)
        val llAiGenerated = view.findViewById<View>(R.id.ll_ai_generated)
        val btnAiOptimize = view.findViewById<View>(R.id.btn_ai_optimize)
        val btnAiRevoke = view.findViewById<View>(R.id.btn_ai_revoke)
        val btnAiRegenerate = view.findViewById<View>(R.id.btn_ai_regenerate)
        val flPreview1 = view.findViewById<View>(R.id.fl_preview_1)
        val flPreview2 = view.findViewById<View>(R.id.fl_preview_2)
        val btnRemove1 = view.findViewById<View>(R.id.btn_remove_image_1)
        val btnRemove2 = view.findViewById<View>(R.id.btn_remove_image_2)

        // Add button animations
        ClickAnimations.addButtonPressAnimation(btnSubmit)
        ClickAnimations.addButtonPressAnimation(btnClose)
        ClickAnimations.addButtonPressAnimation(btnAddImage)
        ClickAnimations.addButtonPressAnimation(btnAi)
        ClickAnimations.addButtonPressAnimation(btnAiCancel)
        ClickAnimations.addButtonPressAnimation(btnAiOptimize)
        ClickAnimations.addButtonPressAnimation(btnAiRevoke)
        ClickAnimations.addButtonPressAnimation(btnAiRegenerate)

        // Load draft
        val draft = DraftManager.getDraft(requireContext())
        if (draft.isNotEmpty()) {
            etContent.setText(draft)
        }

        // Close button: navigate back
        btnClose.setOnClickListener { findNavController().navigateUp() }

        // Add image button
        btnAddImage.setOnClickListener {
            if (selectedImageUris.size >= 2) {
                Toast.makeText(requireContext(), "最多只能添加 2 张图片哦", Toast.LENGTH_SHORT).show()
            } else {
                openImagePicker()
            }
        }
        
        btnAiCancel.setOnClickListener {
            aiCall?.cancel()
            llAiGenerating.visibility = View.GONE
            llAiDefault.visibility = View.VISIBLE
        }

        btnAi.setOnClickListener {
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(requireContext(), "请先上传一张照片让AI看看哦", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Switch UI to generating state
            llAiDefault.visibility = View.GONE
            llAiGenerating.visibility = View.VISIBLE
            
            val base64 = getBase64FromUri(selectedImageUris[0])

            aiCall = DeepSeekApi.generatePostInspiration(base64) { text, isError ->
                activity?.runOnUiThread {
                    if (isError) {
                        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
                        llAiGenerating.visibility = View.GONE
                        llAiDefault.visibility = View.VISIBLE
                    } else {
                        llAiGenerating.visibility = View.GONE
                        llAiGenerated.visibility = View.VISIBLE
                        etContent.setText(text)
                    }
                }
            }
        }
        
        btnAiRevoke.setOnClickListener {
            llAiGenerated.visibility = View.GONE
            llAiDefault.visibility = View.VISIBLE
            etContent.setText("")
        }
        
        btnAiRegenerate.setOnClickListener {
            llAiGenerated.visibility = View.GONE
            llAiGenerating.visibility = View.VISIBLE
            
            val base64 = if (selectedImageUris.isNotEmpty()) getBase64FromUri(selectedImageUris[0]) else null
            
            aiCall = DeepSeekApi.generatePostInspiration(base64) { text, isError ->
                activity?.runOnUiThread {
                    if (isError) {
                        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
                        llAiGenerating.visibility = View.GONE
                        llAiGenerated.visibility = View.VISIBLE
                    } else {
                        llAiGenerating.visibility = View.GONE
                        llAiGenerated.visibility = View.VISIBLE
                        etContent.setText(text)
                    }
                }
            }
        }
        
        btnAiOptimize.setOnClickListener {
            Toast.makeText(requireContext(), "AI 优化Tips：试试多添加一些情感词汇会更动人哦～", Toast.LENGTH_SHORT).show()
        }

        btnRemove1.setOnClickListener {
            if (selectedImageUris.size > 0) {
                selectedImageUris.removeAt(0)
                updateImagePreviews()
            }
        }

        btnRemove2.setOnClickListener {
            if (selectedImageUris.size > 1) {
                selectedImageUris.removeAt(1)
                updateImagePreviews()
            }
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
            if (btnSubmit is TextView) btnSubmit.text = "埋下秘密中..."

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
                    val moodBody = selectedMood!!.name.toRequestBody("text/plain".toMediaTypeOrNull())

                    val imageParts = mutableListOf<MultipartBody.Part>()
                    selectedImageUris.forEachIndexed { index, uri ->
                        val bytes = withContext(Dispatchers.IO) {
                            requireContext().contentResolver.openInputStream(uri)?.readBytes()
                        }
                        if (bytes != null) {
                            val requestFile = okhttp3.RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
                            imageParts.add(MultipartBody.Part.createFormData("images", "image_$index.jpg", requestFile))
                        }
                    }

                    val response = withContext(Dispatchers.IO) {
                        RetrofitClient.postsApi.createPost(contentBody, moodBody, imageParts)
                    }
                    if (response.isSuccessful) {
                        DraftManager.clearDraft(requireContext())
                        Toast.makeText(requireContext(), "秘密已埋下 ✨", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(requireContext(), "发布失败", Toast.LENGTH_SHORT).show()
                        isSubmitting = false
                        btnSubmit.isEnabled = true
                        if (btnSubmit is TextView) btnSubmit.text = "发布"
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                    isSubmitting = false
                    btnSubmit.isEnabled = true
                    if (btnSubmit is TextView) btnSubmit.text = "发布"
                }
            }
        }
    }

    private fun openImagePicker() {
        pickImage.launch("image/*")
    }

    private fun updateImagePreviews() {
        val flPreview1 = view?.findViewById<View>(R.id.fl_preview_1)
        val flPreview2 = view?.findViewById<View>(R.id.fl_preview_2)
        val ivPreview1 = view?.findViewById<ImageView>(R.id.iv_image_preview_1)
        val ivPreview2 = view?.findViewById<ImageView>(R.id.iv_image_preview_2)

        if (flPreview1 == null || flPreview2 == null || ivPreview1 == null || ivPreview2 == null) return

        if (selectedImageUris.size > 0) {
            flPreview1.isVisible = true
            Glide.with(this).load(selectedImageUris[0]).centerCrop().into(ivPreview1)
        } else {
            flPreview1.isVisible = false
            ivPreview1.setImageURI(null)
        }

        if (selectedImageUris.size > 1) {
            flPreview2.isVisible = true
            Glide.with(this).load(selectedImageUris[1]).centerCrop().into(ivPreview2)
        } else {
            flPreview2.isVisible = false
            ivPreview2.setImageURI(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val etContent = view?.findViewById<EditText>(R.id.et_content)
        val content = etContent?.text?.toString() ?: ""
        if (!isSubmitting) {
            DraftManager.saveDraft(requireContext(), content)
        }
    }
}
