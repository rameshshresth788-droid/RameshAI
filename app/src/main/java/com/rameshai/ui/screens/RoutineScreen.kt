package com.rameshai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rameshai.routine.RoutineItem
import com.rameshai.routine.RoutineRepository
import com.rameshai.ui.theme.BackgroundDark
import kotlinx.coroutines.launch

/**
 * Fully user-editable routine list (spec section 13). No medical or health
 * claims are made anywhere in this screen or its data model — it only reflects
 * activities and times the user themselves types in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(routineRepository: RoutineRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { items = routineRepository.getAll() }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Routine") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") } }
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Routine abhi set nahi hai. + se add karo.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
                items(items.sortedBy { it.time }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${item.time} — ${item.activity}", style = MaterialTheme.typography.bodyMedium)
                            Text(item.dayCategory.name.lowercase(), style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = {
                            val updated = items - item
                            items = updated
                            scope.launch { routineRepository.saveAll(updated) }
                        }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                    Divider()
                }
            }
        }
    }

    if (showAddDialog) {
        var time by remember { mutableStateOf("") }
        var activity by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Routine Item") },
            text = {
                Column {
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (HH:mm)") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = activity, onValueChange = { activity = it }, label = { Text("Activity") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (time.isNotBlank() && activity.isNotBlank()) {
                        val updated = items + RoutineItem(time, activity)
                        items = updated
                        scope.launch { routineRepository.saveAll(updated) }
                    }
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}
