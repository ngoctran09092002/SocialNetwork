package com.example.socialnetwork.services

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class CloudinaryService(
    private val cloudName: String,
    private val uploadPreset: String
) {
    private val client = OkHttpClient()
    private val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    suspend fun uploadImage(imageUri: Uri, contentResolver: ContentResolver): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Tạo file tạm từ URI
                val tempFile = createTempFileFromUri(imageUri, contentResolver)
                if (tempFile == null) {
                    Log.e("Cloudinary", "Failed to create temp file")
                    return@withContext null
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        tempFile.name,
                        tempFile.asRequestBody("image/*".toMediaType())
                    )
                    .addFormDataPart("upload_preset", uploadPreset)
                    .build()

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                // Thực hiện upload - bây giờ đã ở background thread
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val secureUrl = json.getString("secure_url")
                    Log.d("Cloudinary", "Upload success: $secureUrl")
                    return@withContext secureUrl
                } else {
                    Log.e("Cloudinary", "Upload failed: ${response.code}")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("Cloudinary", "Error uploading: ${e.message}", e)
                return@withContext null
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri, contentResolver: ContentResolver): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("cloudinary_upload_", ".jpg")
            tempFile.deleteOnExit()

            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            Log.e("Cloudinary", "Error creating temp file: ${e.message}", e)
            null
        }
    }
}