package com.example.meuassistente.ui.screen.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

private data class ModuleMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: MoreModuleDestination
)

@Composable
fun MoreHubScreen(
    preferences: UserPreferences,
    preferencesStore: AppPreferencesStore,
    onOpenModule: (MoreModuleDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val actions = listOf(
        ModuleMenuItem(
            title = "Financeiro",
            subtitle = "Contas a pagar, vencimentos e pagamentos",
            icon = Icons.Rounded.Payments,
            destination = MoreModuleDestination.FINANCE
        ),
        ModuleMenuItem(
            title = "Rotinas e hábitos",
            subtitle = "Hábitos recorrentes e registro diário",
            icon = Icons.Rounded.SelfImprovement,
            destination = MoreModuleDestination.ROUTINES
        ),
        ModuleMenuItem(
            title = "Listas e notas",
            subtitle = "Checklists rápidos e anotações livres",
            icon = Icons.Rounded.Checklist,
            destination = MoreModuleDestination.LISTS_NOTES
        ),
        ModuleMenuItem(
            title = "Estatísticas",
            subtitle = "Taxa de conclusão, atrasos e totais financeiros",
            icon = Icons.Rounded.BarChart,
            destination = MoreModuleDestination.STATISTICS
        ),
        ModuleMenuItem(
            title = "Backup e restauração",
            subtitle = "Exportação JSON com verificação de integridade",
            icon = Icons.Rounded.CloudSync,
            destination = MoreModuleDestination.BACKUP
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "Mais",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Módulos complementares e aparência",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Card de Aparência
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Aparência e leitura",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeModePref.entries.forEachIndexed { index, mode ->
                        val label = when (mode) {
                            ThemeModePref.SYSTEM -> "Sistema"
                            ThemeModePref.LIGHT -> "Claro"
                            ThemeModePref.DARK -> "Escuro"
                        }
                        SegmentedButton(
                            selected = preferences.themeMode == mode,
                            onClick = { scope.launch { preferencesStore.setThemeMode(mode) } },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeModePref.entries.size
                            ),
                            label = { Text(label) }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tamanho do texto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", preferences.fontScale)}x",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = preferences.fontScale,
                        onValueChange = { value ->
                            scope.launch { preferencesStore.setFontScale(value) }
                        },
                        valueRange = 0.85f..1.35f
                    )
                }
            }
        }

        // Módulos
        actions.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onOpenModule(item.destination) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
