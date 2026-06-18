package com.zjgsu.treehole.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.*
import com.zjgsu.treehole.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class SetupProfileFragment : Fragment() {

    private var selectedAvatarIndex: Int = 1
    private var customAvatarUri: Uri? = null
    private lateinit var ivAvatarPreview: ImageView
    private lateinit var presetAvatars: List<ImageView>
    private lateinit var progressOverlay: View

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                customAvatarUri = uri
                ivAvatarPreview.setImageURI(uri)
                clearPresetSelection()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_setup_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivAvatarPreview = view.findViewById(R.id.iv_avatar_preview)
        progressOverlay = view.findViewById(R.id.progress_overlay)
        val etNickname = view.findViewById<EditText>(R.id.et_nickname)
        val btnComplete = view.findViewById<Button>(R.id.btn_complete)
        val ivAvatarEdit = view.findViewById<ImageView>(R.id.iv_avatar_edit)

        presetAvatars = listOf(
            view.findViewById(R.id.preset_avatar_1),
            view.findViewById(R.id.preset_avatar_2),
            view.findViewById(R.id.preset_avatar_3),
            view.findViewById(R.id.preset_avatar_4),
            view.findViewById(R.id.preset_avatar_5)
        )

        // Avatar edit button click
        ivAvatarEdit.setOnClickListener {
            showAvatarPicker()
        }

        // Preview avatar click
        ivAvatarPreview.setOnClickListener {
            showAvatarPicker()
        }

        // Preset avatar selection
        presetAvatars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedAvatarIndex = index + 1
                customAvatarUri = null
                updatePresetSelection(index)
                // Use preset avatar as preview
                val presetId = resources.getIdentifier(
                    "avatar_preset_${index + 1}",
                    "drawable",
                    requireContext().packageName
                )
                if (presetId != 0) {
                    ivAvatarPreview.setImageResource(presetId)
                }
            }
        }

        // Complete button
        btnComplete.setOnClickListener {
            val nickname = etNickname.text.toString().trim()

            if (nickname.isEmpty()) {
                Toast.makeText(requireContext(), "请输入昵称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nickname.length < 3) {
                Toast.makeText(requireContext(), "昵称至少3个字符", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnComplete.isEnabled = false
            btnComplete.text = "设置中..."
            showLoading(true)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    var finalAvatarUrl: String? = null

                    if (customAvatarUri != null) {
                        // Upload custom avatar first (with compression)
                        Toast.makeText(requireContext(), "正在压缩上传头像...", Toast.LENGTH_SHORT).show()
                        val uploadedUrl = uploadAvatar(customAvatarUri!!)
                        if (uploadedUrl != null) {
                            finalAvatarUrl = uploadedUrl
                        } else {
                            Toast.makeText(requireContext(), "头像上传失败，请重试", Toast.LENGTH_SHORT).show()
                            btnComplete.isEnabled = true
                            btnComplete.text = "完成设置"
                            showLoading(false)
                            return@launch
                        }
                    } else {
                        // Use preset avatar URL
                        finalAvatarUrl = "preset_avatar_$selectedAvatarIndex"
                    }

                    // Update profile with nickname and avatar
                    updateProfile(nickname, finalAvatarUrl)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnComplete.isEnabled = true
                    btnComplete.text = "完成设置"
                } finally {
                    showLoading(false)
                }
            }
        }

        // Set default selection
        updatePresetSelection(0)
    }

    private fun showLoading(show: Boolean) {
        progressOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showAvatarPicker() {
        val options = arrayOf("从相册选择", "使用预设头像")
        AlertDialog.Builder(requireContext())
            .setTitle("选择头像")
            .setItems(options) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> pickFromGallery()
                    1 -> { /* Already showing presets, do nothing */ }
                }
            }
            .show()
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun clearPresetSelection() {
        presetAvatars.forEach {
            it.alpha = 0.5f
            it.scaleX = 1f
            it.scaleY = 1f
        }
    }

    private fun updatePresetSelection(selectedIndex: Int) {
        presetAvatars.forEachIndexed { index, imageView ->
            if (index == selectedIndex) {
                imageView.alpha = 1f
                imageView.scaleX = 1.2f
                imageView.scaleY = 1.2f
            } else {
                imageView.alpha = 0.5f
                imageView.scaleX = 1f
                imageView.scaleY = 1f
            }
        }
    }

    private suspend fun uploadAvatar(uri: Uri): String? {
        val context = requireContext().applicationContext
        return withContext(Dispatchers.IO) {
            try {
                // Use ImageUtils for better compression
                val compressedFile = ImageUtils.compressImage(context, uri)
                if (compressedFile == null) {
                    return@withContext uploadOriginalBitmap(context, uri)
                }

                android.util.Log.d("SetupProfile", "Compressed image size: ${compressedFile.length()} bytes")

                val requestBody = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("avatar", compressedFile.name, requestBody)

                val response = RetrofitClient.authApi.uploadAvatar(part)

                // Clean up temp file
                compressedFile.delete()

                if (response.isSuccessful) {
                    response.body()?.avatarUrl
                } else {
                    android.util.Log.e("SetupProfile", "Upload failed: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("SetupProfile", "Upload error: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun uploadOriginalBitmap(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    return@withContext null
                }

                // Scale down if too large
                val maxSize = 800
                val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                    val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    bitmap
                }

                // Compress
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                if (scaledBitmap != bitmap) scaledBitmap.recycle()
                bitmap.recycle()

                // Create temp file
                val tempFile = File(context.cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(outputStream.toByteArray())
                }

                android.util.Log.d("SetupProfile", "Original compressed image size: ${tempFile.length()} bytes")

                val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("avatar", tempFile.name, requestBody)

                val response = RetrofitClient.authApi.uploadAvatar(part)
                tempFile.delete()

                if (response.isSuccessful) {
                    response.body()?.avatarUrl
                } else {
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("SetupProfile", "Upload error: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun updateProfile(nickname: String, avatarUrl: String) {
        val nicknameBody = nickname.toRequestBody("text/plain".toMediaTypeOrNull())
        val avatarBody = avatarUrl.toRequestBody("text/plain".toMediaTypeOrNull())

        val response = RetrofitClient.authApi.updateProfile(nicknameBody, avatarBody, null, null)
        if (response.isSuccessful) {
            // Save updated info
            response.body()?.user?.let { user ->
                TokenManager.saveUser(user.id, user.username, user.nickname, user.avatar)
                TokenManager.saveAvatar(user.avatar)
            }

            Toast.makeText(requireContext(), "设置成功", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_setup_to_feed)
        } else {
            Toast.makeText(requireContext(), "设置失败", Toast.LENGTH_SHORT).show()
        }
    }
}
