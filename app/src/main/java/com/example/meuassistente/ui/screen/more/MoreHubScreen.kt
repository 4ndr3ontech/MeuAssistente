package com.example.meuassistente.ui.screen.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meuassistente.data.local.datastore.AppPreferencesStore
import com.example.meuassistente.data.local.datastore.ThemeModePref
import com.example.meuassistente.data.local.datastore.UserPreferences
import kotlinx.coroutines.launch
import java.util.Locale

enum class MoreModuleDestination {
    FINANCE,
    ROUTINES,
    LISTS_NOTES,
    STATISTICS,
    BACKUP
}

@Composable
fun MoreHubScreen(
    preferences: UserPreferences,
    preferencesStore: AppPreferencesStore,
    onOpenModule: (MoreModuleDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val actions = listOf(
        Triple("Financeiro (centavos)", "Contas a pagar, vencimentos e pagamentos", MoreModuleDestination.FINANCE),
        Triple("Rotinas e hábitos", "Hábitos recorrentes e registro diário", MoreModuleDestination.ROUTINES),
        Triple("Listas e notas", "Checklists rápidos e anotações livres", MoreModuleDestination.LISTS_NOTES),
        Triple("Estatísticas", "Taxa de conclusão, atrasos e totais financeiros", MoreModuleDestination.STATISTICS),
        Triple("Backup e restauração", "Exportação JSON com verificação SHA-256", MoreModuleDestination.BACKUP)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Mais e Módulos",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tema do aplicativo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeModePref.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeModePref.SYSTEM -> "Sistema"
                            ThemeModePref.LIGHT -> "Claro"
                            ThemeModePref.DARK -> "Escuro"
                        }
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { scope.launch { preferencesStore.setThemeMode(mode) } },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = "Escala de texto: ${String.format(Locale.US, "%.2f", preferences.fontScale)}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Slider(
                    value = preferences.fontScale,
                    onValueChange = { value ->
                        scope.launch { preferencesStore.setFontScale(value) }
                    },
                    valueRange = 0.85f..1.35f
                )
            }
        }

        actions.forEach { (title, subtitle, destination) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenModule(destination) }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
