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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.summonerscompass.R
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
    val userPower by viewModel.userPower.collectAsState()

    val context = LocalContext.current

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

            PowerLevelBar(
                power = userPower,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PowerLevelBar(
    power: Int,
    modifier: Modifier = Modifier
) {
    val (level, progress) = calculateLevelAndProgress(power)

    println("PowerLevelBar -> Power: $power, Level: $level, Progress: $progress")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(16.dp)
    ) {
        Text(
            text = "Level $level",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        progress.coerceIn(
                            0f,
                            1f
                        )
                    )
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Green)
            )
        }

        Text(
            text = "Power: $power",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


fun calculateLevelAndProgress(power: Int): Pair<Int, Float> {
    var level = 1
    var powerForCurrentLevel = 0
    var totalPowerForNextLevel = 100

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

    return Pair(level, progress.coerceIn(0f, 1f))
}


@Composable
fun MapScreen(viewModel: HomeScreenViewModel) {
    val userLocation by viewModel.userLocation.collectAsState()
    val pinLocation by viewModel.pinLocation.collectAsState()
    val randomSprites by viewModel.randomSprites.collectAsState()
    val energyPools by viewModel.energyPools.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    val context = LocalContext.current
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
                .height(200.dp)
                .clip(RoundedCornerShape(3.dp))
                .border(1.dp, Color.Gray)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    viewModel.updatePinLocation(latLng)
                }
            ) {
                Marker(
                    state = MarkerState(position = userLocation),
                    title = "Your Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )

                pinLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Teleport Here",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        onClick = {
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

                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.be_icon)
                val scaledBitmap =
                    Bitmap.createScaledBitmap(bitmap, 100, 100, false) // Ajuste o tamanho
                val icon = BitmapDescriptorFactory.fromBitmap(scaledBitmap)

                energyPools.forEach { pool ->
                    Circle(
                        center = pool.position,
                        radius = radius.toDouble(),
                        fillColor = Color(0x330000FF),
                        strokeColor = Color.Blue,
                        strokeWidth = 2f
                    )

                    Marker(
                        state = MarkerState(
                            position = LatLng(
                                pool.position.latitude - 0.0003,
                                pool.position.longitude
                            )
                        ),
                        title = "Energy Pool (+${pool.powerValue} Power)",
                        icon = icon
                    )
                }
            }
        }

        Button(onClick = {
            pinLocation?.let {
                viewModel.teleportTo(it)
                Toast.makeText(context, "Teleported Successfully", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Teleport")
        }

        Button(onClick = {
            userLocation.let {
                viewModel.consumeSprites(it, radius)
                viewModel.consumeEnergyPools(it, radius)
                Toast.makeText(context, "Nearby Spirits Consumed", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Consume")
        }

        Button(onClick = {
            userLocation.let {
                viewModel.consumeEnergyPools(it, radius)
                Toast.makeText(context, "Nearby Energy Pools Consumed", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Consume Energy")
        }
    }
}
