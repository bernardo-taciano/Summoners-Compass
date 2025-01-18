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

@Composable
fun ProfileScreen(
    uid: String,
    modifier: Modifier = Modifier,
    navController: NavController?
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var profileImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")

    // Launcher para abrir a galeria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64Image = encodeImageToBase64(it, context)
            if (base64Image != null) {
                saveImageToDatabase(uid, base64Image) // Salvar no Realtime Database
                profileImageBitmap = decodeBase64ToBitmap(base64Image)?.asImageBitmap() // Atualizar a UI
            }
        }
    }

    // Buscar dados do utilizador
    LaunchedEffect(Unit) {
        val userData = getUserData(uid)
        name = userData.first
        email = userData.second
        userData.third?.let { base64Image ->
            profileImageBitmap = decodeBase64ToBitmap(base64Image)?.asImageBitmap()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Imagem de perfil com ícone de lápis
        Box(
            contentAlignment = Alignment.BottomEnd,
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

            // Ícone para editar a foto
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editar Foto de Perfil",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .padding(4.dp)
                    .clickable {
                        galleryLauncher.launch("image/*") // Abre a galeria ao clicar
                    }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nome do utilizador
        name?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // E-mail do utilizador
        email?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Botão para abrir a galeria
        Button(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Selecionar Foto da Galeria")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para logout
        Button(
            onClick = { handleLogout(context) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Log Out")
        }
    }
}

// Função para converter uma imagem para Base64
fun encodeImageToBase64(uri: Uri, context: Context): String? {
    return try {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: Exception) {
        Log.e("Firebase", "Erro ao converter imagem para Base64", e)
        null
    }
}

// Função para decodificar uma string Base64 em Bitmap
fun decodeBase64ToBitmap(base64Image: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        Log.e("Firebase", "Erro ao decodificar imagem Base64", e)
        null
    }
}

// Função para salvar a imagem no Firebase Realtime Database
fun saveImageToDatabase(uid: String, base64Image: String) {
    val userRef = db.getReference("users").child(uid)
    userRef.child("profileImage").setValue(base64Image)
        .addOnSuccessListener {
            Log.d("Firebase", "Imagem salva com sucesso no Realtime Database")
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Erro ao salvar a imagem", exception)
        }
}

// Função para buscar dados do utilizador do Firebase
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

// Função para logout
fun handleLogout(context: Context) {
    FirebaseAuth.getInstance().signOut()
    val intent = Intent(context, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}
