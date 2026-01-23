package com.truckershub.features.feed

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.truckershub.core.data.model.Post
import com.truckershub.core.design.TextWhite
import com.truckershub.core.design.ThubBlack
import com.truckershub.core.design.ThubDarkGray
import com.truckershub.core.design.ThubNeonBlue
import com.truckershub.core.design.ThubRed
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun FeedScreen(
    onPostClick: (Post) -> Unit,
    onFabClick: () -> Unit,
    viewModel: FeedViewModel = viewModel()
) {
    var showUploadDialog by remember { mutableStateOf(false) }
    val posts = viewModel.posts
    val context = LocalContext.current // Den Context holen wir uns hier

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    // --- HIER WAR DER FEHLER ---
    if (showUploadDialog) {
        AddPostDialog(
            onDismiss = { showUploadDialog = false },
            // Jetzt nehmen wir Text UND Bild entgegen
            onSend = { text, imageUri ->
                // Und geben ALLES (inklusive Context) an das ViewModel weiter
                viewModel.createPost(context, text, imageUri) { success ->
                    if (success) {
                        showUploadDialog = false
                        Toast.makeText(context, "Gesendet! 🚛📸", Toast.LENGTH_SHORT).show()
                    } else {
                        // Fehlermeldung, falls der Server zickt
                        Toast.makeText(context, "Upload Fehler! URL geprüft? ❌", Toast.LENGTH_LONG).show()
                    }
                }
            },
            isUploading = viewModel.isUploading
        )
    }
    // ---------------------------

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                containerColor = ThubNeonBlue,
                contentColor = ThubBlack
            ) {
                Icon(Icons.Default.Add, "Neuer Post")
            }
        },
        containerColor = ThubBlack
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.padding(padding)
        ) {
            item {
                Text(
                    "DIESEL FEED ⛽",
                    color = ThubNeonBlue,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    "Aktuelles von der Straße (Löscht sich nach 24h!)",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (viewModel.isLoading) {
                item { CircularProgressIndicator(color = ThubNeonBlue, modifier = Modifier.padding(16.dp)) }
            } else if (posts.isEmpty()) {
                item { Text("Nix los auf der Straße... sei der Erste!", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
            } else {
                items(posts) { post ->
                    PostItem(
                        post = post,
                        currentUserId = currentUserId,
                        onLikeClick = { viewModel.toggleLike(post) },
                        onCommentClick = { Toast.makeText(context, "Kommentare kommen bald! 🚧", Toast.LENGTH_SHORT).show() },
                        onShareClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "TruckersHub: ${post.userName} schreibt: ${post.text}")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    currentUserId: String,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val timeString = try {
        post.timestamp?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: "Gerade eben"
    } catch (e: Exception) { "..." }

    val isLiked = post.likes.contains(currentUserId)

    Card(
        colors = CardDefaults.cardColors(containerColor = ThubDarkGray),
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
            ) {
                AsyncImage(
                    model = post.userAvatarUrl.ifEmpty { "https://www.w3schools.com/w3images/avatar2.png" },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(post.userName, color = TextWhite, fontWeight = FontWeight.Bold)
                    Text("um $timeString Uhr", color = Color.Gray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
            }

            // BILD (Wichtig: Das kommt jetzt von deinem Server!)
            if (post.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post Bild",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.Black)
                )
            }

            // ACTIONS
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) ThubRed else ThubNeonBlue,
                    modifier = Modifier.size(28.dp).clickable { onLikeClick() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("${post.likes.size}", color = TextWhite, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(24.dp))

                Icon(Icons.Default.AddComment, null, tint = TextWhite, modifier = Modifier.size(26.dp).clickable { onCommentClick() })

                Spacer(modifier = Modifier.weight(1f))

                Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.clickable { onShareClick() })
            }

            if (post.text.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    Text(text = post.text, color = TextWhite, lineHeight = 20.sp)
                }
            }
        }
    }
}