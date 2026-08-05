package com.example.ui.remote.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.remote.AppShortcut
import com.example.ui.remote.RemoteCommand

@Composable
fun AppShortcutsRow(
    shortcuts: List<AppShortcut>,
    onCommand: (RemoteCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        shortcuts.forEach { shortcut ->
            AppShortcutButton(
                name = shortcut.name,
                backgroundColor = shortcut.backgroundColor,
                textColor = shortcut.textColor,
                onClick = { onCommand(shortcut.command) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
