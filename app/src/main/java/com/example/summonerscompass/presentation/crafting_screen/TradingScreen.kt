package com.example.summonerscompass.presentation.crafting_screen

import Item
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun TradingScreen(
    modifier: Modifier = Modifier,
    navController: NavController?,
    viewModel: CraftingScreenViewModel
) {
    val inventory by viewModel.inventory.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getInventory()
    }

}