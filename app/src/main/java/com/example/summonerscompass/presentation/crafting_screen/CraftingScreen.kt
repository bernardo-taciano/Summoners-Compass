package com.example.summonerscompass.presentation.profile_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.summonerscompass.R
import com.example.summonerscompass.presentation.crafting_screen.CraftingScreenViewModel
import com.example.summonerscompass.presentation.glossary_screen.ChampionItem
import com.example.summonerscompass.presentation.glossary_screen.GlossaryScreenViewModel

@Composable
fun CraftingScreen(
    modifier: Modifier = Modifier,
    navController: NavController?,
    viewModel: CraftingScreenViewModel
) {
    val items by viewModel.items.collectAsState()
    val square by viewModel.itemSquare.collectAsState()

    viewModel.getItems()

    val itemMap = items?.data
    val longSword = itemMap?.get("1036")


    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nome do usuário
            Text(
                text = "Crafting System",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            if(longSword != null) {
                Text(text=longSword.name)
            }

            val res = longSword?.image?.full
            Button(onClick = {
                if(res != null) {
                    viewModel.getItemSquare(res)
                }
            }) {
                Text("Get Long Sword")
            }

            square?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Item Square",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}