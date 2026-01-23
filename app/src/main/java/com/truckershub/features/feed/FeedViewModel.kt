package com.truckershub.features.feed

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.truckershub.core.data.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class FeedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- SERVER URL ---
    // Hier deine URL eintragen (https ist okay, wir ignorieren Fehler!)
    private val UPLOAD_SERVER_URL = "https://inetfacts.de/feed/upload_feed.php"

    // Wir nutzen jetzt unseren toleranten Client statt dem Standard-Client
    private val client = getUnsafeOkHttpClient()

    var posts by mutableStateOf<List<Post>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
    var isUploading by mutableStateOf(false)

    init {
        loadPosts()
    }

    fun loadPosts() {
        isLoading = true
        val yesterday = Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000))

        firestore.collection("posts")
            .whereGreaterThan("timestamp", yesterday)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(id = doc.id)
                    }
                    isLoading = false
                }
            }
    }

    fun toggleLike(post: Post) {
        val user = auth.currentUser ?: return
        val postRef = firestore.collection("posts").document(post.id)
        if (post.likes.contains(user.uid)) {
            postRef.update("likes", FieldValue.arrayRemove(user.uid))
        } else {
            postRef.update("likes", FieldValue.arrayUnion(user.uid))
        }
    }

    fun createPost(context: Context, text: String, imageUri: Uri?, onResult: (Boolean) -> Unit) {
        if (text.isBlank() && imageUri == null) return

        isUploading = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                var finalImageUrl = ""

                if (imageUri != null) {
                    val uploadedUrl = uploadImageToMyServer(context, imageUri)
                    if (uploadedUrl != null) {
                        finalImageUrl = uploadedUrl
                    } else {
                        withContext(Dispatchers.Main) {
                            isUploading = false
                            onResult(false)
                        }
                        return@launch
                    }
                }

                savePostToFirestore(text, finalImageUrl) { success ->
                    viewModelScope.launch(Dispatchers.Main) {
                        isUploading = false
                        onResult(success)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("FeedViewModel", "Error: ${e.message}")
                withContext(Dispatchers.Main) {
                    isUploading = false
                    onResult(false)
                }
            }
        }
    }

    private fun uploadImageToMyServer(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "temp_upload.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.name, requestBody)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_SERVER_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseString = response.body?.string()
                val json = JSONObject(responseString ?: "")
                if (json.optString("status") == "success") {
                    json.optString("url")
                } else {
                    null
                }
            } else {
                Log.e("UploadError", "Code: ${response.code}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun savePostToFirestore(text: String, imageUrl: String, onInternalResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("funkName") ?: document.getString("firstName") ?: "Unbekannt"
                val userAvatar = document.getString("profileImageUrl") ?: ""

                val newPost = Post(
                    userId = user.uid,
                    userName = userName,
                    userAvatarUrl = userAvatar,
                    text = text,
                    imageUrl = imageUrl,
                    timestamp = null
                )

                firestore.collection("posts")
                    .add(newPost)
                    .addOnSuccessListener { onInternalResult(true) }
                    .addOnFailureListener { onInternalResult(false) }
            }
            .addOnFailureListener { onInternalResult(false) }
    }

    // --- DER SSL-TÜRSTEHER 😎 ---
    // Diese Funktion erstellt einen Client, der ALLES akzeptiert
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            // Erstelle einen TrustManager, der nicht prüft
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            // Installiere den TrustManager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Akzeptiere jeden Hostnamen
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}