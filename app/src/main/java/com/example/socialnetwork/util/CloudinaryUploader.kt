package com.example.socialnetwork.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object CloudinaryUploader {

    private const val CLOUD_NAME    = "dsho2mecb"
    private const val UPLOAD_PRESET = "ml_default"
    private const val TAG           = "CloudinaryUploader"

    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        // Đọc và nén ảnh xuống tối đa 1MB
        val imageBytes = compressImage(context, uri)
        Log.d(TAG, "Compressed size: ${imageBytes.size / 1024}KB")

        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val encodedFile = java.net.URLEncoder.encode("data:image/jpeg;base64,$base64", "UTF-8")
        val body = "file=$encodedFile&upload_preset=$UPLOAD_PRESET"
        val bodyBytes = body.toByteArray(Charsets.UTF_8)

        val url = URL("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Content-Length", bodyBytes.size.toString())
            connectTimeout = 30_000
            readTimeout    = 60_000
        }

        connection.outputStream.use { it.write(bodyBytes) }

        val responseCode = connection.responseCode
        val responseText = try {
            if (responseCode == 200) connection.inputStream.bufferedReader().readText()
            else connection.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
        } catch (e: Exception) {
            "Không đọc được response: ${e.message}"
        }

        Log.d(TAG, "Response $responseCode: $responseText")

        if (responseCode != 200) {
            throw Exception("Upload thất bại ($responseCode): $responseText")
        }

        JSONObject(responseText).getString("secure_url")
    }

    private fun compressImage(context: Context, uri: Uri): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Không mở được ảnh")
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Scale xuống nếu ảnh quá lớn (max 1024px)
        val maxSize = 1024
        val bitmap = if (original.width > maxSize || original.height > maxSize) {
            val scale = maxSize.toFloat() / maxOf(original.width, original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        } else original

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        return out.toByteArray()
    }
}
