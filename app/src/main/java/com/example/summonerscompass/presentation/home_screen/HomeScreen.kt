package com.example.summonerscompass.presentation.home_screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.summonerscompass.R
import com.example.summonerscompass.ui.theme.Purple40
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
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
    val userPower by viewModel.userPower.collectAsState(initial = 0)
    val context = LocalContext.current
    val (level, progress) = calculateLevelAndProgress(userPower)

    LaunchedEffect(Unit) {
        viewModel.fetchUserPower()
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Summoner's Compass",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                MapScreen(viewModel)
            }

            CustomProgressBar(
                level = level,
                progress = progress,
                power = userPower,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}


@Composable
fun CustomProgressBar(level: Int, progress: Float, power: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(16.dp)
            .background(
                color = Color(0xFFEDF3FF), // Fundo do card
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp) // Ajuste o tamanho se necessário
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary), // Cor da barra preenchida
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Lvl",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Normal,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "$level",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }


            // Texto com a percentagem e descrição
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$power Power - ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                }

                // Barra de progresso
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFD6D8DB)) // Cor de fundo da barra
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress) // Preenchimento conforme o progresso
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary) // Cor da barra preenchida
                    )
                }
            }
        }
    }
}


fun calculateLevelAndProgress(power: Int): Pair<Int, Float> {
    var level = 1
    var powerForCurrentLevel = 0 // Alterado para começar do nível zero
    var totalPowerForNextLevel = 100 // Primeiro nível termina com 100

    while (power >= totalPowerForNextLevel) {
        level++
        powerForCurrentLevel = totalPowerForNextLevel
        totalPowerForNextLevel += 100 * level
    }

    val progress = if (totalPowerForNextLevel > powerForCurrentLevel) {
        (power - powerForCurrentLevel).toFloat() / (totalPowerForNextLevel - powerForCurrentLevel)
    } else {
        0f
    }

    return Pair(level, progress.coerceIn(0f, 1f)) // Garante que o progresso está entre 0 e 1
}



@Composable
fun MapScreen(viewModel: HomeScreenViewModel) {
    val userLocation by viewModel.userLocation.collectAsState()
    val userBearing by viewModel.userBearing.collectAsState()
    val pinLocation by viewModel.pinLocation.collectAsState()
    val randomSprites by viewModel.randomSprites.collectAsState()
    val energyPools by viewModel.energyPools.collectAsState()

    var isLoading = remember { false }

    when(userLocation) {
        LatLng(0.0,0.0) -> isLoading = true
        else -> isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(userLocation, 15f)
        }

        var moved = false // para so fazer a animação uma vez

        LaunchedEffect(userLocation) {
            if (!moved) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLng(userLocation),
                    durationMs = 1000
                )
                moved = true
            }
        }

        val context = LocalContext.current
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (!LocationManager.hasLocationPermissions(context)) {
            RequestLocationPermissions(
                onPermissionsGranted = {
                    viewModel.startLocationUpdates()
                },
                onPermissionsDenied = {
                    Toast.makeText(
                        context,
                        "Location permissions are required for this app",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }

        val radius = 50f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, Color.Gray)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        viewModel.updatePinLocation(latLng)
                    },
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        isBuildingEnabled = true,
                        mapType = MapType.NORMAL,
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        compassEnabled = true,
                        zoomControlsEnabled = true
                    )
                ) {
                    Marker(
                        state = MarkerState(position = userLocation),
                        title = "You",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        rotation = userBearing,
                        flat = true,
                        zIndex = 1f
                    )

                    // Pin location marker (unchanged)
                    pinLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Teleport Here",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        )
                    }

                    // Exibe sprites aleatórios no mapa
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

                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.be_icon)
                    val scaledBitmap =
                        Bitmap.createScaledBitmap(bitmap, 100, 100, false) // Ajuste o tamanho
                    val icon = BitmapDescriptorFactory.fromBitmap(scaledBitmap)

                    energyPools.forEach { pool ->
                        Circle(
                            center = pool.position,
                            radius = radius.toDouble(),
                            fillColor = Color(0x330000FF), // Azul com transparência
                            strokeColor = Color.Blue, // Azul mais forte na borda
                            strokeWidth = 2f
                        )

                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    pool.position.latitude - 0.0003,
                                    pool.position.longitude
                                )
                            ), // Deslocar ícone para baixo
                            title = "Energy Pool (+${pool.powerValue} Power)",
                            icon = icon
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(

            ) {
                Button(onClick = {
                    pinLocation?.let {
                        viewModel.teleportTo(it)
                        Toast.makeText(context, "Teleported Successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                }) {
                    Text("Teleport To Pin")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(onClick = { viewModel.resetLocation() }) {
                    Text("Go Back Home")
                }
            }
        }
    }
}
