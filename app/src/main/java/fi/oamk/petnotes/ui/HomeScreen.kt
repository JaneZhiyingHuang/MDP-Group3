package fi.oamk.petnotes.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import fi.oamk.petnotes.R
import fi.oamk.petnotes.model.Pet
import fi.oamk.petnotes.model.PetDataStore
import fi.oamk.petnotes.ui.theme.InputColor
import fi.oamk.petnotes.viewmodel.HomeScreenViewModel
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.Line
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = viewModel(),
    navController: NavController,
) {
    val context = LocalContext.current
    val isUserLoggedIn = homeScreenViewModel.isUserLoggedIn()
    var pets by remember { mutableStateOf(listOf<Pet>()) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val userId = homeScreenViewModel.getUserId()

    LaunchedEffect(context) {
        Log.d("HomeScreen", "User logged in: $isUserLoggedIn")
        PetDataStore.getSelectedPetId(context).collect { petId ->
            if (isUserLoggedIn) {
                val fetchedPets = homeScreenViewModel.fetchPets()
                pets = fetchedPets

                //if there is a selected pet in datastore / default :just the first pet
                selectedPet = fetchedPets.find { it.id == petId } ?: fetchedPets.firstOrNull()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("addNewPet") }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_add_circle_outline_26),
                            contentDescription = "Add New Pet"
                        )
                    }
                },
                actions = {
                    if (pets.isNotEmpty()) {
                        SelectedPetDropdown(
                            pets = pets,
                            selectedPet = selectedPet,
                            onPetSelected = { pet ->
                                selectedPet = pet
                                coroutineScope.launch {
                                    PetDataStore.setSelectedPetId(context, pet.id)
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEFEFEF))
            )
        },
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isUserLoggedIn) {
                if (selectedPet != null) {
                    PetCard(selectedPet!!,userId = userId, navController = navController)

                    // Pass the userId to the WeightCard
                    WeightTrendCard(pet = selectedPet!!, userId = userId, navController = navController)

                } else {
                    NoPetsCard(navController)
                }
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



@Composable
fun PetCard(pet: Pet, userId: String, navController: NavController) {
    Card(
        onClick ={ navController.navigate("profile/${pet.id}") } ,
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier
            .padding(top = 10.dp)
            .width(352.dp)

    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(InputColor)
                ) {
                    if (pet.petImageUri.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(pet.petImageUri),
                            contentDescription = "Pet Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(26.dp))
                Column {
                    Text(text = pet.name, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp))
                    Text(text = pet.gender, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    Text(text = pet.dateOfBirth, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                    Text(text = calculateAge(pet.dateOfBirth), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "*Medical Condition: ${pet.medicalCondition}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun NoPetsCard(navController: NavController) {
    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        modifier = Modifier.padding(top = 150.dp).width(352.dp).height(214.dp),
        onClick = { navController.navigate("addNewPet") }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painter = painterResource(id = R.drawable.baseline_add_circle_outline_26), contentDescription = "Add New Pet")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Add your first pet to start!", style = TextStyle(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun WeightTrendCard(pet: Pet, userId: String, navController: NavController) {
    // Store the weight entries to plot the trend
    var weightEntries by remember { mutableStateOf<List<Pair<Long, Float>>>(emptyList()) }

    // Fetch the weight entries from Firestore
    LaunchedEffect(pet.id) {
        val db = FirebaseFirestore.getInstance()
        try {
            val weightSnapshot = db.collection("pet_weights")
                .whereEqualTo("petId", pet.id)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()

            val fetchedEntries = weightSnapshot.documents.mapNotNull { doc ->
                val dateString = doc.getString("date")
                val weight = doc.getDouble("weight")?.toFloat()

                // Parse the date string into a Date object if it exists
                val dateMillis = if (dateString != null) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
                    date?.time // Convert to milliseconds
                } else {
                    null
                }

                if (dateMillis != null && weight != null) {
                    Pair(dateMillis, weight)
                } else {
                    null
                }
            }

            weightEntries = fetchedEntries.sortedBy { it.first } // Sort entries by date
        } catch (e: Exception) {
            Log.e("WeightTrendCard", "Error fetching weight data", e)
        }
    }

    // If no weight entries, display a "No data" message
    if (weightEntries.isEmpty()) {
        Card(
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .padding(20.dp)
                .width(352.dp)
                .clickable {
                    // Navigate to weight screen when clicked
                    navController.navigate("weight_screen/$userId/${pet.id}")
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp), // Add some padding around the box
                contentAlignment = Alignment.Center // Center the content
            ) {
                Text(
                    text = "Start adding your first pet weight data", // Text to be centered
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

        }
    } else {
        // Process the data for charting
        val chartData = weightEntries.map { (dateMillis, weight) ->
            // Convert to X-Y pairs where X is the time (in millis) and Y is the weight
            Pair(dateMillis.toFloat(), weight)
        }
        val dateLabels = weightEntries.map { (dateMillis, _) ->
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(dateMillis))
        }

        // Display the weight trend in a chart
        Card(
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .padding(20.dp)
                .width(352.dp)
                .clickable {
                    // Navigate to weight screen when clicked
                    navController.navigate("weight_screen/$userId/${pet.id}")
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // Title Text
                Text(
                    "Weight Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                // Line Chart
                LineChart(
                    data = remember {
                        listOf(
                            Line(
                                label = "Pet Weight",
                                values = chartData.map { it.second.toDouble() },
                                color = SolidColor(Color.Blue),
                                dotProperties = DotProperties(
                                    enabled = true,
                                    color = SolidColor(Color.White),
                                    strokeColor = SolidColor(Color.Blue)
                                ),
                            )
                        )
                    },
                    modifier = Modifier
                        .width(350.dp)
                        .height(200.dp)
                )

                // Display Date Labels Below Chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dateLabels.forEachIndexed { index, dateLabel ->
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier

                        )
                    }
                }
            }
        }
    }
}


fun calculateAge(dateOfBirth: String?): String {
    if (dateOfBirth.isNullOrEmpty()) return "Unknown Age"
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val birthDate = LocalDate.parse(dateOfBirth, formatter)
        val period = Period.between(birthDate, LocalDate.now())
        "${period.years} years and ${period.months} months"
    } catch (e: DateTimeParseException) {
        "Invalid Date Format"
    }
}
