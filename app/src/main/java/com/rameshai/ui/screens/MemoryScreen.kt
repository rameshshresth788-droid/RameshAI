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
import com.rameshai.memory.MemoryEntity
import com.rameshai.memory.MemoryKind
import com.rameshai.memory.MemoryRepository
import com.rameshai.ui.theme.BackgroundDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(memoryRepository: MemoryRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val entries by memoryRepository.observeAll().collectAsState(initial = emptyList())

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Memory") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    TextButton(onClick = { scope.launch { memoryRepository.clearAll() } }) { Text("Clear all") }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Koi memory abhi tak save nahi hui.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
                items(entries, key = { it.id }) { entry ->
                    MemoryRow(entry) { scope.launch { memoryRepository.delete(entry) } }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun MemoryRow(entry: MemoryEntity, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (entry.kind == MemoryKind.LONG_TERM_PREFERENCE) "Preference" else "Conversation",
                style = MaterialTheme.typography.labelSmall
            )
            Text(entry.content, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
    }
}
