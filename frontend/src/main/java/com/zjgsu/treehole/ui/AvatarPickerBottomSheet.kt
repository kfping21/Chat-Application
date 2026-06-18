package com.zjgsu.treehole.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjgsu.treehole.R
import com.zjgsu.treehole.network.RetrofitClient
import com.zjgsu.treehole.network.TokenManager
import kotlinx.coroutines.CoroutineScope
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

class AvatarPickerBottomSheet : BottomSheetDialogFragment() {

    private var onAvatarSelected: ((String) -> Unit)? = null
    private var customAvatarUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadAvatar(uri)
            }
        }
    }

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Get the thumbnail from the result
        }
    }

    fun setOnAvatarSelectedListener(listener: (String) -> Unit) {
        onAvatarSelected = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.bottom_sheet_avatar_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val llFromGallery = view.findViewById<LinearLayout>(R.id.ll_from_gallery)
        val llFromCamera = view.findViewById<LinearLayout>(R.id.ll_from_camera)
        val llPresets = view.findViewById<LinearLayout>(R.id.ll_presets)
        val llPresetGrid = view.findViewById<LinearLayout>(R.id.ll_preset_grid)

        llFromGallery.setOnClickListener {
            pickFromGallery()
        }

        llFromCamera.setOnClickListener {
            // For simplicity, just show a message - camera requires more setup
            Toast.makeText(requireContext(), "请使用相册选择拍照的图片", Toast.LENGTH_SHORT).show()
        }

        llPresets.setOnClickListener {
            llPresetGrid.visibility = if (llPresetGrid.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Preset avatar clicks
        val presetViews = listOf(
            view.findViewById<ImageView>(R.id.preset_1),
            view.findViewById<ImageView>(R.id.preset_2),
            view.findViewById<ImageView>(R.id.preset_3),
            view.findViewById<ImageView>(R.id.preset_4),
            view.findViewById<ImageView>(R.id.preset_5)
        )

        presetViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val presetUrl = "preset_avatar_${index + 1}"
                onAvatarSelected?.invoke(presetUrl)
                dismiss()
            }
        }
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uploadAvatar(uri: Uri) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val avatarUrl = withContext(Dispatchers.IO) {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                    val imageBytes = outputStream.toByteArray()

                    val tempFile = File(requireContext().cacheDir, "avatar_temp.jpg")
                    FileOutputStream(tempFile).use { fos ->
                        fos.write(imageBytes)
                    }

                    val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("avatar", tempFile.name, requestBody)

                    val response = RetrofitClient.authApi.uploadAvatar(part)
                    if (response.isSuccessful) {
                        response.body()?.avatarUrl
                    } else {
                        null
                    }
                }

                if (avatarUrl != null) {
                    onAvatarSelected?.invoke(avatarUrl)
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "上传失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "上传失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "AvatarPickerBottomSheet"
    }
}
