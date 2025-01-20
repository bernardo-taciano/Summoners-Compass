package com.example.summonerscompass.presentation.home_screen

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: HomeScreenViewModel
) {
    val champions by viewModel.champions.collectAsState()
    val square by viewModel.championSquare.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getChampions()
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
                text = "Home Screen",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            val championMap = champions?.data
            val name = championMap?.get("Gragas")?.name
            if (name != null) {
                Text(text = name)
            }

            val res = championMap?.get("Gragas")?.image?.full
            Button(onClick = {
                if (res != null) {
                    viewModel.getChampionSquare(res)
                }
            }) {
                Text("Get Gragas Square")
            }

            square?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Champion Square",
                    modifier = Modifier.padding(16.dp)
                )
            }

            MapScreen(viewModel)


        }
    }

}


@Composable
fun MapScreen(viewModel: HomeScreenViewModel) {
    val atasehir = LatLng(38.7169, -9.1399)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(atasehir, 15f)
    }
    val context = LocalContext.current
    val pinLocation by viewModel.pinLocation.collectAsState()
    val randomSprites by viewModel.randomSprites.collectAsState()

    val radius = 50f

    // Container para a GoogleMap
    Box(
        modifier = Modifier
            .padding(16.dp), // Espaçamento opcional ao redor do container
        contentAlignment = Alignment.TopCenter // Centraliza o mapa no container
    ) {
        Box(
            modifier = Modifier
                .width(350.dp) // Defina a largura desejada
                .height(200.dp) // Defina a altura desejada
                .clip(RoundedCornerShape(16.dp)) // Bordas arredondadas opcionais
                .border(1.dp, Color.Gray) // Borda opcional
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    viewModel.updatePinLocation(latLng) // place pin on map
                }
            ) {
                // Add a pin marker if pinLocation is set
                pinLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Teleport Here",
                        onClick = {
                            viewModel.updatePinLocation(location)
                            true
                        }
                    )
                }

                randomSprites.forEach { sprite ->
                    Circle(
                        center = sprite.position,
                        radius = radius.toDouble(),
                        fillColor = Color(0x3300FF00),
                        strokeColor = Color.Green,
                        strokeWidth = 2f
                    )

                    Marker(
                        state = MarkerState(position = sprite.position),
                        title = sprite.name,
                        icon = BitmapDescriptorFactory.fromBitmap(sprite.image)
                    )
                }

            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            pinLocation?.let {
                viewModel.teleportTo(it)
                Toast.makeText(context, "Teleported Successfully", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Teleport to Pin Location")
        }

        Button(onClick = {
            pinLocation?.let {
                viewModel.consumeSprites(it, radius)
                Toast.makeText(context, "Nearby Spirits Consumed", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Consume Nearby Spirits")
        }
    }


}
