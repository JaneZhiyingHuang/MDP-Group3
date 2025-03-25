package fi.oamk.petnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import fi.oamk.petnotes.model.Pet
import fi.oamk.petnotes.viewmodel.HomeScreenViewModel
import fi.oamk.petnotes.viewmodel.PetTagsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    navController: NavController,
    homeScreenViewModel: HomeScreenViewModel,
    petTagsViewModel: PetTagsViewModel
) {
    // Check if the user is logged in
    val isUserLoggedIn = remember { FirebaseAuth.getInstance().currentUser != null }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var pets by remember { mutableStateOf(listOf<Pet>()) }
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var dropdownWidth by remember { mutableStateOf(0.dp) }
    var selectedTag by remember { mutableStateOf("All") }
    var tags by remember { mutableStateOf(listOf<String>()) }
    var showDialog by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf(TextFieldValue("")) }
    var userInput by remember { mutableStateOf("") }

    val defaultTags = listOf("All", "Vomit", "Stool", "Cough", "Vet", "Water Intake", "Emotion")

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            coroutineScope.launch {
                val fetchedPets = homeScreenViewModel.getPets() // Fetch pets from Firestore
                if (fetchedPets.isNotEmpty()) {
                    pets = fetchedPets
                    selectedPet = fetchedPets.first()
                    tags = selectedPet?.tags?.takeIf { it.isNotEmpty() } ?: defaultTags
                }
            }
        }
    }

    LaunchedEffect(selectedPet) {
        tags = selectedPet?.tags?.takeIf { it.isNotEmpty() } ?: defaultTags
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .onGloballyPositioned { coordinates ->
                                        dropdownWidth = with(density) {
                                            coordinates.size.width.toDp()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedPet?.name ?: "Select Pet",
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select Pet")
                                }
                            }
                            Box {
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.width(dropdownWidth)
                                ) {
                                    pets.forEach { pet ->
                                        DropdownMenuItem(
                                            text = { Text(pet.name) },
                                            onClick = {
                                                selectedPet = pet
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .wrapContentHeight(align = Alignment.Top),
                        maxItemsInEachRow = 6
                    ) {
                        tags.forEach { tag ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                var showDeleteSign by remember { mutableStateOf(false) }
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = { Text(tag) },
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .background(if (showDeleteSign) Color.Red else Color.Transparent)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    showDeleteSign = true
                                                }
                                            )
                                        }
                                )

                                if (showDeleteSign) {
                                    IconButton(onClick = {
                                        showDialog = true
                                        newTag = TextFieldValue(tag)
                                    }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Tag")
                                    }
                                }
                            }
                        }
                        FilterChip(
                            selected = false,
                            onClick = { showDialog = true },
                            label = { Text("Add +") }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Add New Abnormal Behaviors:")
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Date Selection
                        var selectedDate by remember { mutableStateOf("Date") }
                        var selectedMonth by remember { mutableStateOf("Month") }
                        var selectedYear by remember { mutableStateOf("Year") }
                        var selectedTag by remember { mutableStateOf("Tag") }

                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        val years = (2020..2030).map { it.toString() }

                        DateSelector(selectedDate) { selectedDate = it }
                        DropdownSelector(selectedMonth, months) { selectedMonth = it }
                        DropdownSelector(selectedYear, years) { selectedYear = it }
                        DropdownSelector(selectedTag, tags) { selectedTag = it }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .height(200.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        TextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(12.dp),
                            placeholder = { Text("Write the description of your pet's abnormal behaviors") },
                            singleLine = false
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { /* to be written */ },
                        ) {
                            Text("Add Photos")
                        }
                        Button(
                            onClick = { /* to be written */ },
                        ) {
                            Text("Add Documents")
                        }
                        Button(
                            onClick = { /* to be written */ },
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }


if (showDialog) {
        BasicAlertDialog(
            onDismissRequest = { showDialog = false }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), // Ensure proper padding inside the Card
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Add a new tag")

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            placeholder = { Text("Enter tag name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    if (newTag.text.isNotBlank()) {
                                        val updatedTags = tags.toMutableList().apply { add(newTag.text.trim()) }
                                        tags = updatedTags // Update the local state

                                        selectedPet?.id?.let { petId ->
                                            coroutineScope.launch {
                                                petTagsViewModel.updatePetTags(petId, updatedTags,
                                                    onSuccess = {
                                                        newTag = TextFieldValue("")
                                                        showDialog = false
                                                    },
                                                    onFailure = { e ->
                                                        // Handle failure
                                                    })
                                            }
                                        }
                                    }
                                    showDialog = false
                                }
                            ) {
                                Text("Add")
                            }

                            Button(
                                onClick = { showDialog = false }
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateSelector(selectedValue: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val days = (1..31).map { it.toString() }

    Box {
        Card(
            modifier = Modifier
                .width(90.dp)
                .clickable { expanded = true }
                .padding(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = selectedValue, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            days.forEach { day ->
                DropdownMenuItem(
                    text = { Text(day) },
                    onClick = {
                        onValueChange(day)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DropdownSelector(selectedValue: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .width(100.dp)
                .clickable { expanded = true }
                .padding(4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = selectedValue, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Select")
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

