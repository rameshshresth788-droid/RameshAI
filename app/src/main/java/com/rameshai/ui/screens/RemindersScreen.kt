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
import com.rameshai.reminders.ReminderDao
import com.rameshai.reminders.ReminderEntity
import com.rameshai.reminders.ReminderScheduler
import com.rameshai.ui.theme.BackgroundDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(dao: ReminderDao, scheduler: ReminderScheduler, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val reminders by dao.observeAll().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Reminders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Koi reminder set nahi hai. Voice se ya + button se banao.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
                items(reminders, key = { it.id }) { reminder ->
                    ReminderRow(reminder) { scope.launch { scheduler.cancel(reminder) } }
                    Divider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { message, hhmm, recurrence ->
                scope.launch { scheduler.schedule(message, hhmm, recurrence) }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ReminderRow(reminder: ReminderEntity, onDelete: () -> Unit) {
    val formatted = remember(reminder.triggerAtMillis) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(reminder.triggerAtMillis))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(reminder.message, style = MaterialTheme.typography.bodyMedium)
            Text("$formatted · ${reminder.recurrence.name.lowercase()}", style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
    }
}

@Composable
private fun AddReminderDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var message by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf("none") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Reminder") },
        text = {
            Column {
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (HH:mm)") })
                Spacer(Modifier.height(8.dp))
                Row {
                    listOf("none", "daily", "weekly").forEach { option ->
                        FilterChip(
                            selected = recurrence == option,
                            onClick = { recurrence = option },
                            label = { Text(option) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (message.isNotBlank() && time.isNotBlank()) onConfirm(message, time, recurrence) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
