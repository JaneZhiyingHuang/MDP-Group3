package fi.oamk.petnotes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fi.oamk.petnotes.viewmodel.HomeScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel,
    navController: NavController // Pass NavController to HomeScreen
) {
    val isUserLoggedIn = homeScreenViewModel.isUserLoggedIn()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("", fontWeight = FontWeight.Bold) }, // Adding a simple top bar
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFEFEF))
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) } // BottomNavigationBar
    ) { paddingValues ->

        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isUserLoggedIn) {
                Text(
                    text = "This is the homepage",
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(16.dp) // Add padding to the text for better readability
                )
            } else {
                Text(
                    text = "Please log in to continue.",
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
