package com.example.summonerscompass.presentation.profile_screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.summonerscompass.LoginActivity
import com.example.summonerscompass.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

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

    //run only once
    LaunchedEffect(Unit) {
        getUserData(uid) { fetchedName, fetchedEmail ->
            name = fetchedName
            email = fetchedEmail
        }
    }


    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(id = R.drawable.profile),
            contentDescription = "Foto de Perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Nome do usuário
        name?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // E-mail do usuário
        email?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Botão para editar o perfil
        Button(onClick = {  }, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Editar Perfil")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                handleLogout(context)
            }
        ) {
            Text(text = "Log Out")
        }


    }
}

fun handleLogout(context: Context) {
    FirebaseAuth.getInstance().signOut()
    val intent = Intent(context, LoginActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

fun getUserData(uid: String, onResult: (String?, String?) -> Unit) {
    db = FirebaseDatabase.getInstance("https://summoners-compass-default-rtdb.europe-west1.firebasedatabase.app")
    val userRef = db.getReference("users").child(uid)

    userRef.get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            val name = snapshot.child("name").getValue(String::class.java)
            val email = snapshot.child("email").getValue(String::class.java)
            onResult(name, email)
        } else {
            onResult(null, null) // User not found
        }
    }.addOnFailureListener { exception ->
        exception.printStackTrace()
        onResult(null, null) // Error occurred
    }
}

