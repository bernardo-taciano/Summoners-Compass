package com.example.summonerscompass.presentation.profile_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.summonerscompass.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    navController: NavController?,
    viewModel: FriendsScreenViewModel
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Friends", "Requests")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Social") },
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
                0 -> FriendsList(viewModel)  // Display Friends List
                1 -> RequestsList(viewModel) // Display Requests List
            }
        }
    }
}
//
//@Composable
//fun RequestsList(viewModel: FriendsScreenViewModel) {
//    val requests by viewModel.requests.collectAsState()
//    var friendEmail by remember { mutableStateOf("") }
//
//    LaunchedEffect(Unit) {
//        viewModel.getRequests()
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            TextField(
//                value = friendEmail,
//                onValueChange = { friendEmail = it },
//                label = { Text("Enter email") },
//                modifier = Modifier.weight(1f)
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Button(onClick = {
//                viewModel.sendFriendRequest(friendEmail)
//            }) {
//                Text("Send Request")
//            }
//        }
//        LazyVerticalGrid(
//            columns = GridCells.Fixed(1),
//            contentPadding = PaddingValues(16.dp),
//            modifier = Modifier.fillMaxSize(),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            items(requests) { user ->
//                RequestItem(user, viewModel)
//            }
//        }
//    }
//}

@Composable
fun FriendsList(viewModel: FriendsScreenViewModel) {
    val friends by viewModel.friends.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getFriends()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(friends) { friend ->
                FriendItem(friend, viewModel)
            }
        }
    }
}

@Composable
fun FriendItem(friend: User, viewModel: FriendsScreenViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile and details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Profile picture placeholder
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Gray), // Placeholder color
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = friend.name.first().toString(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Friend details
                Column {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Power - ${friend.power}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Remove button
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4E3F)), // Red color
                shape = RoundedCornerShape(8.dp),
                onClick = {
                    viewModel.removeFriend(friend.email)
                }
            ) {
                Text("Remove", color = Color.White)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsList(viewModel: FriendsScreenViewModel) {
    val requests by viewModel.requests.collectAsState()
    var friendEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.getRequests()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Send Friend Request",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = friendEmail,
                onValueChange = { friendEmail = it },
                label = { Text("Enter email") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.sendFriendRequest(friendEmail)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Send Request")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Pending Requests",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests) { user ->
                RequestItem(user, viewModel)
            }
        }
    }
}

@Composable
fun RequestItem(user: User, viewModel: FriendsScreenViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D9A42)),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { viewModel.acceptFriendRequest(user.email) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC4E3F)),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { viewModel.rejectFriendRequest(user.email) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Reject icon",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

//@Composable
//fun RequestItem(user: User, viewModel: FriendsScreenViewModel) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(120.dp)
//            .padding(8.dp),
//        shape = RoundedCornerShape(16.dp),
//        elevation = CardDefaults.cardElevation(4.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surface
//        )
//    ) {
//        Column(
//            modifier = Modifier.fillMaxWidth(),
//            verticalArrangement = Arrangement.SpaceBetween
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(8.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = user.name,
//                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Text(
//                    text = "Power: ${user.power}",
//                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
//                    color = MaterialTheme.colorScheme.onSurface
//                )
//                Spacer(modifier = Modifier.width(16.dp))
//                Button(
//                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
//                    onClick = { viewModel.acceptFriendRequest(user.email) }
//                ) {
//                    Text("A")
//                }
//                Spacer(modifier = Modifier.width(16.dp))
//                Button(
//                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
//                    onClick = { viewModel.rejectFriendRequest(user.email) }
//                ) {
//                    Text("R")
//                }
//            }
//
//        }
//
//    }
//}