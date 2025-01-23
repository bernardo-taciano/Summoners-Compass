package com.example.summonerscompass.presentation.profile_screen

import Item
import android.Manifest
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.summonerscompass.presentation.crafting_screen.CraftingScreenViewModel
import com.example.summonerscompass.routes.Routes
import com.google.android.material.color.MaterialColors

private const val BF = "1038"
private const val ROD = "1058"
private const val BELT = "1011"

@Composable
fun CraftingScreen(
    modifier: Modifier = Modifier,
    navController: NavController?,
    viewModel: CraftingScreenViewModel
) {
    val inventory by viewModel.inventory.collectAsState()
    val squares by viewModel.squares.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedItems = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        viewModel.getInventory()
    }

    val context = LocalContext.current
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var isPermissionRequestLaunched by remember { mutableStateOf(false) }

    // Define the permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted = isGranted
        isPermissionRequestLaunched = false
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            viewModel.startObjectDetection(bitmap, onObjectDetected = {
                    detectedObject -> when(detectedObject) {
                "Metal" -> viewModel.addItemToInventory(ROD)
                "Cutlery" -> viewModel.addItemToInventory(BF)
                "Glasses" -> viewModel.addItemToInventory(BELT)
            }
            })
        }
    }


    Scaffold { innerPadding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LaunchedEffect(cameraPermissionGranted) {
                    if (!isPermissionRequestLaunched && !cameraPermissionGranted) {
                        isPermissionRequestLaunched = true
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                Text(
                    text = "Crafting System",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )

                if (cameraPermissionGranted) {
                    Button(onClick = { launcher.launch(null) }) {
                        Text("Scan Item")
                    }
                }

                Button(onClick = { navController?.navigate(Routes.tradingScreen) }) {
                    Text("Trade")
                }

                Text(
                    text = "Inventory",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )


                Button(
                    onClick = {
                        if (selectedItems.size == 2) {
                            val selected = selectedItems.toList()
                            viewModel.combineItems(selected[0], selected[1])
                            selectedItems.clear()
                        } else {
                            Toast.makeText(context, "Invalid selection", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Combine Items")
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val data = inventory.zip(squares)
                    items(data) { (pair, bitmap) ->
                        val item = pair.first
                        val count = pair.second

                        if (item != null) {
                            val itemId = item.image.full.dropLast(4)
                            Box(
                                modifier = Modifier
                                    .height(160.dp)
                                    .fillMaxWidth()
                                    .background(
                                        if (selectedItems.size == 2 && selectedItems.first() == itemId
                                            && selectedItems.last() == itemId) Color(0xFFE1BEE7)
                                        else if (selectedItems.contains(itemId)) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(8.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null // remove o quadrado cinza
                                    ) {
                                        if (selectedItems.count { it == itemId } < count && selectedItems.size < 2) {
                                            selectedItems.add(itemId)
                                        } else if (selectedItems.contains(itemId)) {
                                            selectedItems.remove(itemId)
                                        }
                                    },
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "${item.name} Square",
                                        modifier = Modifier
                                            .size(70.dp)
                                            .padding(8.dp)
                                    )
                                    Text(
                                        text = item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Count: $count",
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove item",
                                        tint = Color.White,
                                        modifier = Modifier.clickable { viewModel.removeItemFromInventory(itemId) }
                                    )

                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Item(item: Item,count: Int, square: Bitmap) {
    Box(
        modifier = Modifier
            .height(150.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = square.asImageBitmap(),
                contentDescription = "${item.name} Square",
                modifier = Modifier
                    .size(70.dp)
                    .padding(8.dp)
            )

            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Count: $count",
                fontSize = 12.sp
            )
        }
    }
}
