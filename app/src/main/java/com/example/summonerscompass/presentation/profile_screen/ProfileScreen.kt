package com.example.summonerscompass.presentation.profile_screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.summonerscompass.LoginActivity
import com.example.summonerscompass.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private lateinit var db: FirebaseDatabase

/**
 * ProfileScreen composable that displays user profile information and allows image upload
 * @param uid User ID for Firebase authentication
 * @param modifier Modifier for customizing the composable
 * @param navController Navigation controller for screen navigation
 */
@Composable
fun ProfileScreen(
    uid: String,
    modifier: Modifier = Modifier,
    navController: NavController?
) {
    // Context for Android-specific operations
    val context = LocalContext.current

    // State variables to hold user data
    var name by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var profileImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Initialize Firebase Database with specific instance URL
    db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    // Activity launcher for gallery image selection
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64Image = encodeImageToBase64(it, context)
            if (base64Image != null) {
                saveImageToDatabase(uid, base64Image) // Save image to Firebase
                profileImageBitmap = decodeBase64ToBitmap(base64Image)?.asImageBitmap() // Update UI
            }
        }
    }

    // Load user data when the screen is first displayed
    LaunchedEffect(Unit) {
        val userData = getUserData(uid)
        name = userData.first
        email = userData.second
        userData.third?.let { base64Image ->
            profileImageBitmap = decodeBase64ToBitmap(base64Image)?.asImageBitmap()
        }
    }

    // Main UI layout
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Profile image container with edit button
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .size(120.dp)
        ) {  // Removed the clip and background from the Box
            // First layer: Profile image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                if (profileImageBitmap != null) {
                    Image(
                        bitmap = profileImageBitmap!!,
                        contentDescription = "Foto de Perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.profile),
                        contentDescription = "Foto de Perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                    )
                }
            }

            // Second layer: Edit icon (will now appear on top)
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar Foto de Perfil",
                tint = Color.Black,
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 2.dp, y = 2.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape
                    )
                    .background(Color.White, CircleShape)
                    .padding(6.dp)
                    .clickable {
                        galleryLauncher.launch("image/*")
                    }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display user's name
        name?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Display user's email
        email?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController?.navigate("friends_screen") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Social")
        }

        // Logout button
        Button(
            onClick = { handleLogout(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Log Out")
        }
    }
}

/**
 * Converts an image URI to Base64 string format
 * @param uri URI of the selected image
 * @param context Android context for content resolution
 * @return Base64 encoded string of the image or null if conversion fails
 */
fun encodeImageToBase64(uri: Uri, context: Context): String? {
    return try {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: Exception) {
        Log.e("Firebase", "Erro ao converter a imagem para o formato de Base64", e)
        null
    }
}

/**
 * Converts a Base64 encoded string back to a Bitmap
 * @param base64Image Base64 encoded string of the image
 * @return Bitmap of the decoded image or null if decoding fails
 */
fun decodeBase64ToBitmap(base64Image: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        Log.e("Firebase", "Erro ao descodificar a imagem para o formato de Base64", e)
        null
    }
}

/**
 * Saves the Base64 encoded image to Firebase Realtime Database
 * @param uid User ID for database reference
 * @param base64Image Base64 encoded string of the image
 */
fun saveImageToDatabase(uid: String, base64Image: String) {
    val userRef = db.getReference("users").child(uid)
    userRef.child("profileImage").setValue(base64Image)
        .addOnSuccessListener {
            Log.d("Firebase", "Imagem guardada com sucesso no Realtime Database")
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Erro ao guardar imagem", exception)
        }
}

/**
 * Retrieves user data from Firebase Realtime Database
 * @param uid User ID for database query
 * @return Triple containing name, email, and profile image (all nullable)
 */
suspend fun getUserData(uid: String): Triple<String?, String?, String?> = withContext(Dispatchers.IO) {
    val userRef = db.getReference("users").child(uid)
    try {
        val snapshot = userRef.get().await()
        val name = snapshot.child("name").getValue(String::class.java)
        val email = snapshot.child("email").getValue(String::class.java)
        val profileImage = snapshot.child("profileImage").getValue(String::class.java)
        Triple(name, email, profileImage)
    } catch (e: Exception) {
        Log.e("Firebase", "Erro ao buscar dados do usuário", e)
        Triple(null, null, null)
    }
}

/**
 * Handles user logout by clearing Firebase authentication and navigating to login screen
 * @param context Android context for starting the login activity
 */
fun handleLogout(context: Context) {
    FirebaseAuth.getInstance().signOut()
    val intent = Intent(context, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}
