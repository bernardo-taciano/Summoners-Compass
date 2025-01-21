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
                text = "Summoner's Compass",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Image(
                painter = painterResource(id = R.drawable.summoners_logo),
                contentDescription = "Summoner's Logo",
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
            )

            MapScreen(viewModel)
        }
    }
}


@Composable
fun MapScreen(viewModel: HomeScreenViewModel) {
    val userLocation by viewModel.userLocation.collectAsState()
    val pinLocation by viewModel.pinLocation.collectAsState()
    val randomSprites by viewModel.randomSprites.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 15f)
    }

    val context = LocalContext.current
    val radius = 50f

    Box(
        modifier = Modifier
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(350.dp)
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
