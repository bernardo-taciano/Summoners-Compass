package com.example.summonerscompass

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.summonerscompass.presentation.crafting_screen.CraftingScreenViewModel
import com.example.summonerscompass.presentation.glossary_screen.GlossaryScreen
import com.example.summonerscompass.presentation.glossary_screen.GlossaryScreenViewModel
import com.example.summonerscompass.presentation.home_screen.HomeScreen
import com.example.summonerscompass.presentation.home_screen.HomeScreenViewModel
import com.example.summonerscompass.presentation.profile_screen.CraftingScreen
import com.example.summonerscompass.presentation.profile_screen.FriendsScreen
import com.example.summonerscompass.presentation.profile_screen.FriendsScreenViewModel
import com.example.summonerscompass.presentation.profile_screen.ProfileScreen
import com.example.summonerscompass.routes.Routes
import com.example.summonerscompass.ui.theme.SummonersCompassTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.ktx.Firebase


data class  BottomNavigationItem(
    val route: String,
    val title : String,
    val selectedIcon : ImageVector,
    val unselectedIcon : ImageVector,
)

private lateinit var db: FirebaseDatabase

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth


    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth and check if the user is signed in
        auth = Firebase.auth


        if (auth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // user is signed in: proceed
        val uid = auth.uid
        enableEdgeToEdge()
        setContent {
            SummonersCompassTheme {
                MainScreen(uid = uid!!)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier, uid: String) {
    // NavController
    val navController = rememberNavController()

    // Configurar os itens da BottomNavigationBar
    val bottomNavItems = listOf(
        BottomNavigationItem(
            route = Routes.homeScreen,
            title = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
        ),
        BottomNavigationItem(
            route = Routes.glossaryScreen,
            title = "Glossary",
            selectedIcon = Icons.Filled.Menu,
            unselectedIcon = Icons.Outlined.Menu,
        ),
        BottomNavigationItem(
            route = Routes.craftingScreen,
            title = "Crafting",
            selectedIcon = Icons.Filled.Build,
            unselectedIcon = Icons.Filled.Build,
        ),
        BottomNavigationItem(
            route = Routes.profileScreen,
            title = "Profile",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
        )
    )

    // Scaffold
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(navController, bottomNavItems)
        }
    ) { innerPadding ->
        NavigationHost(navController = navController, modifier = Modifier.padding(innerPadding), uid)
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, items: List<BottomNavigationItem>) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                        }
                    }
                },
                label = { Text(text = item.title) },
                icon = {
                    Icon(
                        imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                }
            )
        }
    }
}


@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier = Modifier, uid: String) {
    NavHost(
        navController = navController,
        startDestination = Routes.homeScreen,
        modifier = modifier
    ) {
        composable(Routes.homeScreen) { HomeScreen(navController = navController, viewModel = HomeScreenViewModel()) }
        composable(Routes.glossaryScreen) { GlossaryScreen(navController = navController, viewModel = GlossaryScreenViewModel()) }
        composable(Routes.craftingScreen) { CraftingScreen(navController = navController, viewModel = CraftingScreenViewModel()) }
        composable(Routes.profileScreen) { ProfileScreen(uid, navController = navController) }
        composable(Routes.friendsScreen) {  backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            FriendsScreen(uid = uid, navController = navController, viewModel = FriendsScreenViewModel())
        }
    }
}

fun onResult(name: String?, email: String?) {

}
