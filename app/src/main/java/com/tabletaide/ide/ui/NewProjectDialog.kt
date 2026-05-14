package com.tabletaide.ide.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tabletaide.ide.data.StarterProjectTemplate

@Composable
fun NewProjectDialog(
    selectedParentLabel: String?,
    onPickLocation: () -> Unit,
    onDismiss: () -> Unit,
    onCreate: (String, StarterProjectTemplate) -> Unit,
) {
    var projectName by remember { mutableStateOf("MyProject") }
    var template by remember { mutableStateOf(StarterProjectTemplate.BLANK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New project") },
        text = {
            Column(modifier = Modifier.widthIn(min = 320.dp, max = 520.dp)) {
                Text(
                    text = "Create a starter workspace with a small, modern folder structure.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                )
                Text(
                    text = "Template",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                StarterProjectTemplate.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = template == option,
                                onClick = { template = option },
                            )
                            .padding(vertical = 6.dp),
                    ) {
                        RadioButton(
                            selected = template == option,
                            onClick = { template = option },
                        )
                        Column(modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                            Text(option.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
                Text(
                    text = selectedParentLabel ?: "Choose a parent folder for the new project.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(
                    onClick = onPickLocation,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(if (selectedParentLabel == null) "Choose location" else "Change location")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(
                enabled = projectName.trim().isNotEmpty() && selectedParentLabel != null,
                onClick = { onCreate(projectName.trim(), template) },
            ) {
                Text("Create")
            }
        },
    )
}
