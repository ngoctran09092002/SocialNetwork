package com.example.socialnetwork.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.socialnetwork.core.interfaces.IMediaService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Implement IMediaService dùng Imgur API (miễn phí, không cần Firebase Storage trả phí).
 * Nhận URI ảnh → nén → upload Imgur → trả về link ảnh trực tiếp.
 *
 */
class MediaServiceImpl(private val context: Context) : IMediaService {

    companion object {
        private const val IMGBB_API_KEY = "abdc7af838a5fc77833c92263cd2ae84"
        private const val IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload"
    }

    override suspend fun uploadImage(fileUri: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(fileUri)
        val compressedBytes = compressImage(uri)
        uploadToImgbb(compressedBytes)
    }

    private fun uploadToImgbb(imageBytes: ByteArray): String {
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val body = "key=$IMGBB_API_KEY&image=${java.net.URLEncoder.encode(base64Image, "UTF-8")}"

        val connection = URL(IMGBB_UPLOAD_URL).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            doOutput = true
        }

        connection.outputStream.use { it.write(body.toByteArray()) }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown"
            throw Exception("ImgBB upload thất bại: HTTP $responseCode - $error")
        }

        val response = connection.inputStream.bufferedReader().readText()
        val json = JSONObject(response)
        // Trả về direct link ảnh (dùng được thẳng trong Glide/Picasso)
        return json.getJSONObject("data").getString("url")
    }

    // Nén ảnh: resize về tối đa 1080px + JPEG quality 80 để tránh OOM
    private fun compressImage(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        bounds.inSampleSize = calculateInSampleSize(bounds, maxSize = 1080)
        bounds.inJustDecodeBounds = false

        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: throw IllegalStateException("Không đọc được ảnh từ URI: $uri")

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
        bitmap.recycle()
        return output.toByteArray()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, maxSize: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var sampleSize = 1
        if (height > maxSize || width > maxSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / sampleSize >= maxSize && halfWidth / sampleSize >= maxSize) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
}
