package com.example.summonerscompass.presentation.glossary_screen

import Champion
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: GlossaryScreenViewModel
) {
    val champions by viewModel.glossary.collectAsState()
    val squares by viewModel.squares.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getGlossary()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Glossary",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(150.dp)
                    .padding(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = champions.size / 169f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = "${champions.size}/169 \n Champions found",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val data = champions.zip(squares)
                items(data) { (champion, bitmap) ->
                    println("Champion: ${champion?.name}, Bitmap: $bitmap")
                    if (champion != null) {
                        ChampionItem(champion, bitmap)
                    }
                }
            }
        }
    }

}

@Composable
fun ChampionItem(champion: Champion, square: Bitmap) {
    Box(
        modifier = Modifier
            .height(150.dp) // Defina uma altura menor para os itens
            .fillMaxWidth() // Use o máximo de largura disponível
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = square.asImageBitmap(),
                contentDescription = "${champion.name} Square",
                modifier = Modifier
                    .size(70.dp)
                    .padding(8.dp) // Ajuste os tamanhos para caber melhor
            )

            Text(
                text = champion.name,
                fontSize = 14.sp, // Fonte menor para texto legível
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "HP: ${champion.stats.hp}",
                fontSize = 12.sp
            )
            Text(
                text = "MP: ${champion.stats.mp}",
                fontSize = 12.sp
            )
            Text(
                text = "Attack: ${champion.info.attack}",
                fontSize = 12.sp
            )
            Text(
                text = "Magic: ${champion.info.magic}",
                fontSize = 12.sp
            )
            Text(
                text = "Defense: ${champion.info.defense}",
                fontSize = 12.sp
            )
        }
    }
}

