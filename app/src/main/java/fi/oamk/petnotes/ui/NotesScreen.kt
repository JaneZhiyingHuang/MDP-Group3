package fi.oamk.petnotes.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.icu.util.Calendar
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import fi.oamk.petnotes.model.Notes
import fi.oamk.petnotes.model.Pet
import fi.oamk.petnotes.model.PetDataStore
import fi.oamk.petnotes.viewmodel.HomeScreenViewModel
import fi.oamk.petnotes.viewmodel.NotesViewModel
import fi.oamk.petnotes.viewmodel.PetTagsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    navController: NavController,
    homeScreenViewModel: HomeScreenViewModel,
    petTagsViewModel: PetTagsViewModel,
    notesViewModel: NotesViewModel
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
    var showDeleteConfirmationDialog by remember { mutableStateOf<String?>(null) }
    var fetchedNotes by remember { mutableStateOf(listOf<Notes>()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Date
    val currentDate = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(currentDate.get(Calendar.DAY_OF_MONTH).toString()) }
    var selectedMonth by remember { mutableStateOf((currentDate.get(Calendar.MONTH) + 1).toString()) }
    var selectedYear by remember { mutableStateOf(currentDate.get(Calendar.YEAR).toString()) }

    // Add state variables for note editing
    var noteToEdit by remember { mutableStateOf<Notes?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editedDescription by remember { mutableStateOf("") }
    var editedTag by remember { mutableStateOf("") }
    var showDeleteNoteDialog by remember { mutableStateOf<Notes?>(null) }

    val defaultTags = listOf("All", "Vomit", "Stool", "Cough", "Vet", "Water Intake", "Emotion")

    // State to track photo and document URIs
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var documentUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Observe upload state
    val uploadState by notesViewModel.uploadState.collectAsState()

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        photoUris = uris
    }

    // Document picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        documentUris = uris
    }

    LaunchedEffect(isUserLoggedIn) {
        if (isUserLoggedIn) {
            coroutineScope.launch {
                val fetchedPets = homeScreenViewModel.fetchPets() // Fetch pets from Firestore
                if (fetchedPets.isNotEmpty()) {
                    pets = fetchedPets

                    // get selected pet from datastore
                    PetDataStore.getSelectedPetId(context).collect { storedPetId ->
                        selectedPet = fetchedPets.find { it.id == storedPetId } ?: fetchedPets.first()
                    }

                    tags = selectedPet?.tags?.takeIf { it.isNotEmpty() } ?: defaultTags
                }
            }
        }
    }

    LaunchedEffect(selectedPet) {
        tags = selectedPet?.tags?.takeIf { it.isNotEmpty() } ?: defaultTags

        selectedPet?.id?.let { petId ->
            fetchedNotes = notesViewModel.getNotesByPetId(petId)
        }
    }

    // Handle upload state
    LaunchedEffect(uploadState) {
        when (val state = uploadState) {
            is NotesViewModel.UploadState.Success -> {
                refreshTrigger++

                selectedPet?.id?.let { petId ->
                    fetchedNotes = notesViewModel.getNotesByPetId(petId)
                }

                // Reset inputs on success
                userInput = ""
                photoUris = emptyList()
                documentUris = emptyList()
                selectedTag = "All"

                // Reset date to current
                selectedDate = currentDate.get(Calendar.DAY_OF_MONTH).toString()
                selectedMonth = (currentDate.get(Calendar.MONTH) + 1).toString()
                selectedYear = currentDate.get(Calendar.YEAR).toString()
            }
            is NotesViewModel.UploadState.Error -> {
                // Show error message
                // You could use a Snackbar or another error display mechanism
            }
            else -> {} // Idle or Loading states
        }
    }

    LaunchedEffect(refreshTrigger, selectedPet) {
        selectedPet?.id?.let { petId ->
            fetchedNotes = notesViewModel.getNotesByPetId(petId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
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
                            // Always show the "All" tag first
                            FilterChip(
                                selected = selectedTag == "All",
                                onClick = { selectedTag = "All" },
                                label = { Text("All") },
                                modifier = Modifier.padding(end = 4.dp)
                            )

                            // Display other tags with delete functionality
                            tags.filter { it != "All" }.forEach { tag ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    FilterChip(
                                        selected = selectedTag == tag,
                                        onClick = {
                                            selectedTag = tag
                                        },
                                        label = { Text(tag) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )

                                    // Only show delete icon when this specific tag is selected
                                    if (selectedTag == tag) {
                                        IconButton(
                                            onClick = {
                                                showDeleteConfirmationDialog = tag
                                            }
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete Tag",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                }
                            }

                            if (showDeleteConfirmationDialog != null) {
                                BasicAlertDialog(
                                    onDismissRequest = { showDeleteConfirmationDialog = null }
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "*Warning",
                                                color = Color.Red,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            Text(
                                                text = "Are you sure to DELETE all the notes of the tag '${showDeleteConfirmationDialog}'?",
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val tagToDelete =
                                                            showDeleteConfirmationDialog
                                                        val updatedTags = tags.toMutableList()
                                                            .apply { remove(tagToDelete) }

                                                        selectedPet?.id?.let { petId ->
                                                            coroutineScope.launch {
                                                                petTagsViewModel.updatePetTags(
                                                                    petId,
                                                                    updatedTags,
                                                                    onSuccess = {
                                                                        tags = updatedTags
                                                                        selectedTag = "All"
                                                                        showDeleteConfirmationDialog =
                                                                            null
                                                                    },
                                                                    onFailure = {
                                                                        // Optional: Add error handling
                                                                        showDeleteConfirmationDialog =
                                                                            null
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Text("Yes")
                                                }
                                                Button(
                                                    onClick = {
                                                        showDeleteConfirmationDialog = null
                                                    }
                                                ) {
                                                    Text("No")
                                                }
                                            }
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
                Text(
                    text = "Add New Abnormal Behaviors:",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Date Selection
                            val months = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")
                            val years = (2020..2030).map { it.toString() }
                            val displayTag =
                                if (selectedTag == "All") tags.firstOrNull { it != "All" }
                                    ?: "Vomit" else selectedTag

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
                                onClick = { photoPickerLauncher.launch("image/*") },
                            ) {
                                Text("Add Photos (${photoUris.size})")
                            }
                            Button(
                                onClick = { documentPickerLauncher.launch("*/*") },
                            ) {
                                Text("Add Documents (${documentUris.size})")
                            }
                        }

                        Button(
                            onClick = {
                                if (selectedPet == null) {
                                    // Show error that no pet is selected
                                    return@Button
                                }

                                if (userInput.isBlank()) {
                                    // Show error that description is empty
                                    return@Button
                                }

                                // Create a Calendar instance for the timestamp
                                val calendar = Calendar.getInstance()
                                calendar.set(
                                    selectedYear.toInt(),
                                    selectedMonth.toInt() - 1,
                                    selectedDate.toInt()
                                )
                                val userSelectedTimestamp = calendar.timeInMillis

                                coroutineScope.launch {
                                    notesViewModel.uploadAndCreateNote(
                                        photoUris = photoUris,
                                        documentUris = documentUris,
                                        selectedPet = selectedPet,
                                        userInput = userInput,
                                        selectedYear = selectedYear.toInt(),
                                        selectedMonth = selectedMonth.toInt(),
                                        selectedDate = selectedDate.toInt(),
                                        selectedTag = selectedTag,
                                        userSelectedTimestamp = userSelectedTimestamp
                                    )
                                }
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text("Confirm")
                        }
                    }
                }

                if (fetchedNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your Pet's Notes:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Display notes
            items(fetchedNotes.filter { note ->
                selectedTag == "All" || note.tag == selectedTag
            }) { note ->
                NoteCard(
                    note = note,
                    onEdit = {
                        noteToEdit = it
                        editedDescription = it.description
                        editedTag = it.tag
                        showEditDialog = true
                    },
                    onDelete = { showDeleteNoteDialog = it }
                )
            }
            Log.d("NotesScreen", "Displaying ${fetchedNotes.size} notes")
        }
    }

    if (showEditDialog && noteToEdit != null) {
        BasicAlertDialog(
            onDismissRequest = {
                showEditDialog = false
                noteToEdit = null
            }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Note",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    DropdownSelector(
                        selectedValue = editedTag,
                        options = tags.filter { it != "All" },
                        onValueChange = { editedTag = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        singleLine = false
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    noteToEdit?.let { note ->
                                        val updatedNote = note.copy(
                                            description = editedDescription,
                                            tag = editedTag
                                        )
                                        notesViewModel.updateNote(
                                            updatedNote,
                                            onSuccess = {
                                                Toast.makeText(context, "Note updated successfully", Toast.LENGTH_SHORT).show()
                                                refreshTrigger++
                                                showEditDialog = false
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, "Failed to update note: $error", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = {
                                showDeleteNoteDialog = noteToEdit
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Delete")
                        }

                        Button(
                            onClick = {
                                showEditDialog = false
                                noteToEdit = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteNoteDialog != null) {
        BasicAlertDialog(
            onDismissRequest = { showDeleteNoteDialog = null }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "*Warning",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Are you sure you want to delete this note? This action cannot be undone.",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    showDeleteNoteDialog?.let { note ->
                                        notesViewModel.deleteNote(
                                            note,
                                            onSuccess = {
                                                Toast.makeText(context, "Note deleted successfully", Toast.LENGTH_SHORT).show()
                                                refreshTrigger++
                                                showDeleteNoteDialog = null
                                            },
                                            onFailure = { error ->
                                                Toast.makeText(context, "Failed to delete note: $error", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red
                            )
                        ) {
                            Text("Delete")
                        }
                        Button(
                            onClick = { showDeleteNoteDialog = null }
                        ) {
                            Text("Cancel")
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
                .width(80.dp)
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

@Composable
fun NoteCard(
    note: Notes,
    onEdit: (Notes) -> Unit = {},
    onDelete: (Notes) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.getFormattedDate(),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = note.tag,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = note.description,
                    fontSize = 16.sp
                )

                // Display photos if available
                if (note.photoUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Photos:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(note.photoUrls) { photoUrl ->
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Pet photo",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                                placeholder = ColorPainter(Color(0xFFE0E0E0))
                            )
                        }
                    }
                }

                // Display document names if available
                if (note.documentUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Documents:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        note.documentUrls.forEach { documentUrl ->
                            val fileName = documentUrl.substringAfterLast("/")
                            val context = LocalContext.current
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val intent =
                                            Intent(Intent.ACTION_VIEW, Uri.parse(documentUrl))
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: ActivityNotFoundException) {
                                            Toast.makeText(
                                                context,
                                                "No application found to open this document",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = "Document",
                                    tint = Color.Blue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = fileName,
                                    fontSize = 14.sp,
                                    color = Color.Blue,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                    }
                }
            }

            IconButton(onClick = { onEdit(note) }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Note",
                    tint = Color.Gray
                )
            }
        }
    }
}

