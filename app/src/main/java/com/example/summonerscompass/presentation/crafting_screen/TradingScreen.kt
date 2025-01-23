package com.example.summonerscompass.presentation.crafting_screen

import Item
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.summonerscompass.models.Trade
import com.example.summonerscompass.models.User
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    modifier: Modifier = Modifier,
    navController: NavController?,
    viewModel: CraftingScreenViewModel
) {
    val inventory by viewModel.inventory.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val trades by viewModel.requests.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("New Trade", "Active Trades", "Trade Requests")

    LaunchedEffect(Unit) {
        viewModel.getInventory()
        viewModel.getFriends()
        viewModel.getTradeRequests()
        viewModel.getTrades()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trading") },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> NewTrade(viewModel)
                1 -> ActiveTrades(viewModel)
                2 -> TradeRequests(viewModel)

            }
        }

    }

}

@Composable
fun NewTrade(
    viewModel: CraftingScreenViewModel,
) {
    val context = LocalContext.current

    var selectedFriend by remember { mutableStateOf<User?>(null) }
    var yourSelectedItem by remember { mutableStateOf<Item?>(null) }
    var friendSelectedItem by remember { mutableStateOf<Item?>(null) }

    var tradeLocation by remember { mutableStateOf(LatLng(38.7169, -9.1399)) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(tradeLocation, 15f)
    }

    var tradeDate by remember { mutableStateOf("") }
    var tradeTime by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Select Friend")

        FriendsDropdown(
            viewModel = viewModel,
            selectedFriend = selectedFriend,
            onFriendSelected = { selectedFriend = it }
        )

        if (selectedFriend != null) {
            ItemsDropdown(
                viewModel = viewModel,
                selectedItem = yourSelectedItem,
                onItemSelected = { yourSelectedItem = it }
            )

            FriendItemsDropdown(
                viewModel = viewModel,
                selectedItem = friendSelectedItem,
                onItemSelected = { friendSelectedItem = it },
                email = selectedFriend!!.email
            )

            DatePicker(onDateSelected = { tradeDate = it })
            Spacer(modifier = Modifier.height(16.dp))
            TimePicker(onTimeSelected = { tradeTime = it })

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
                            tradeLocation = latLng
                        }
                    ) {

                        Marker(
                            state = MarkerState(position = tradeLocation),
                            title = "Trade Location",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (selectedFriend != null && yourSelectedItem != null && friendSelectedItem != null &&
                    tradeDate != "" && tradeTime != "") {
                    val senderItemId = yourSelectedItem!!.image.full.dropLast(4)
                    val receiverItemId = friendSelectedItem!!.image.full.dropLast(4)
                    viewModel.sendTradeRequest(selectedFriend!!.email, senderItemId, receiverItemId,
                                               tradeLocation, tradeDate, tradeTime)
                } else {
                    Toast.makeText(context, "Insuficient trade details", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Send Trade Request")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePicker(
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedDate = remember { mutableStateOf("") }

    Button(onClick = {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                selectedDate.value = formattedDate
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.show()
    }) {
        Text(if (selectedDate.value.isEmpty()) "Pick Date" else selectedDate.value)
    }
}

@Composable
fun TimePicker(
    onTimeSelected: (String) -> Unit // Callback for selected time
) {
    val context = LocalContext.current
    val selectedTime = remember { mutableStateOf("") }

    Button(onClick = {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                val formattedTime = String.format("%02d:%02d", hour, minute)
                selectedTime.value = formattedTime
                onTimeSelected(formattedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }) {
        Text(if (selectedTime.value.isEmpty()) "Pick Time" else selectedTime.value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendItemsDropdown(
    viewModel: CraftingScreenViewModel,
    selectedItem: Item?,
    onItemSelected: (Item) -> Unit,
    email: String
) {
    var inventory = ArrayList<Item?>()
    viewModel.getInventoryByEmail(email) { items ->
        inventory = items as ArrayList<Item?>
    }

    var expanded by remember { mutableStateOf(false) } // Controls menu visibility
    var selectedItemName by remember { mutableStateOf(selectedItem?.name ?: "") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedItemName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            label = { Text("Select Friend's Item") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            inventory.forEach { item ->
                item?.let {
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            selectedItemName = item.name
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsDropdown(
    viewModel: CraftingScreenViewModel,
    selectedItem: Item?,
    onItemSelected: (Item) -> Unit
) {
    val inventory by viewModel.inventory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getInventory()
    }

    var expanded by remember { mutableStateOf(false) } // Controls menu visibility
    var selectedItemName by remember { mutableStateOf(selectedItem?.name ?: "") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedItemName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(),
            label = { Text("Select Your Item") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            inventory.forEach { (item, count) ->
                item?.let {
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            selectedItemName = item.name
                            onItemSelected(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsDropdown(
    viewModel: CraftingScreenViewModel,
    selectedFriend: User?,
    onFriendSelected: (User) -> Unit
) {
    val friends by viewModel.friends.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getFriends()
    }

    var expanded by remember { mutableStateOf(false) } // Controls menu visibility
    var selectedFriendName by remember { mutableStateOf(selectedFriend?.name ?: "") }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded } // Toggle menu on click
    ) {
        // The text field for the dropdown
        TextField(
            value = selectedFriendName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(), // Required for positioning
            label = { Text("Select Friend") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            friends.forEach { friend ->
                DropdownMenuItem(
                    text = { Text(friend.name) },
                    onClick = {
                        selectedFriendName = friend.name
                        onFriendSelected(friend)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TradeRequests(
    viewModel: CraftingScreenViewModel
)  {
    val requests by viewModel.requests.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getTradeRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests) { request ->
                RequestItem(request, viewModel)
            }
        }
    }
}

@Composable
fun RequestItem(request: Trade, viewModel: CraftingScreenViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(370.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "From: ${request.sender.name}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .height(130.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "You get:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Image(
                        bitmap = request.receivingSquare.asImageBitmap(),
                        contentDescription = "${request.receivingItem.name} Square",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(8.dp)
                    )
                    Text(
                        text = request.receivingItem.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Trading icon in the center
            Icon(
                imageVector = Icons.Default.SwapHoriz, // Replace with a trading icon of your choice
                contentDescription = "Trade Icon",
                modifier = Modifier.size(32.dp), // Adjust size as needed
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .height(130.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "They get:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Image(
                        bitmap = request.sendingSquare.asImageBitmap(),
                        contentDescription = "${request.sendingItem.name} Square",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(8.dp)
                    )
                    Text(
                        text = request.sendingItem.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location: (${request.location.latitude},${request.location.longitude})",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Date: ${request.date}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(32.dp))
            Text(
                text = "Time: ${request.time}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D9A42)),
                onClick = { viewModel.acceptTradeRequest(request)}
            ) {
                Text("Accept")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4E3F)),
                onClick = { viewModel.rejectTradeRequest(request)}
            ) {
                Text("Reject")
            }
        }
    }
}

@Composable
fun ActiveTrades(
    viewModel: CraftingScreenViewModel
)  {
    val trades by viewModel.trades.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getTrades()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trades) { trade ->
                TradeItem(trade, viewModel)
            }
        }
    }
}

@Composable
fun TradeItem(trade: Trade, viewModel: CraftingScreenViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(370.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "From: ${trade.sender.name}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .height(130.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "You get:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Image(
                        bitmap = trade.receivingSquare.asImageBitmap(),
                        contentDescription = "${trade.receivingItem.name} Square",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(8.dp)
                    )
                    Text(
                        text = trade.receivingItem.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Trading icon in the center
            Icon(
                imageVector = Icons.Default.SwapHoriz, // Replace with a trading icon of your choice
                contentDescription = "Trade Icon",
                modifier = Modifier.size(32.dp), // Adjust size as needed
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .height(130.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "They get:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Image(
                        bitmap = trade.sendingSquare.asImageBitmap(),
                        contentDescription = "${trade.sendingItem.name} Square",
                        modifier = Modifier
                            .size(70.dp)
                            .padding(8.dp)
                    )
                    Text(
                        text = trade.sendingItem.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Location: (${trade.location.latitude},${trade.location.longitude})",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Date: ${trade.date}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(32.dp))
            Text(
                text = "Time: ${trade.time}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D9A42)),
                onClick = { viewModel.confirmTrade(trade)}
            ) {
                Text("Confirm")
            }
        }
    }
}