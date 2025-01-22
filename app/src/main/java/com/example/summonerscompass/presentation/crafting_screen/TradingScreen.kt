package com.example.summonerscompass.presentation.crafting_screen

import Item
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.summonerscompass.presentation.profile_screen.FriendsList
import com.example.summonerscompass.presentation.profile_screen.RequestsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingScreen(
    modifier: Modifier = Modifier,
    navController: NavController?,
    viewModel: CraftingScreenViewModel
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("New Trade", "Current Trades")

    LaunchedEffect(Unit) {
        viewModel.getInventory()
        viewModel.getFriends()
    }

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
                0 -> NewTrade(viewModel)
                1 -> CurrentTrades(viewModel)
            }
        }

    }

}

@Composable
fun NewTrade(
    viewModel: CraftingScreenViewModel
)  {
    val inventory by viewModel.inventory.collectAsState()
    val friends by viewModel.friends.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getInventory()
        viewModel.getFriends()
    }

    

}

@Composable
fun CurrentTrades(
    viewModel: CraftingScreenViewModel
)  {

}