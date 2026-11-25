package com.example.meuassistente.ui.screen.modules

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meuassistente.domain.rule.FinanceRules
import com.example.meuassistente.domain.usecase.statistics.ComputeProductivitySnapshotUseCase
import com.example.meuassistente.domain.usecase.statistics.ProductivitySnapshot
import kotlinx.coroutines.flow.Flow

@Composable
fun StatisticsScreen(
    snapshotFlow: Flow<ProductivitySnapshot>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val computeSnapshot = remember { ComputeProductivitySnapshotUseCase() }
    val snapshot by snapshotFlow.collectAsState(
        initial = computeSnapshot(
            occurrences = emptyList(),
            routineExecutions = emptyList(),
            obligations = emptyList(),
            payments = emptyList(),
            nowUtc = 0L
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Estatísticas",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onBack) {
                Text("Voltar")
            }
        }

        StatRowCard(
            label = "Taxa de conclusão de ocorrências",
            value = "${snapshot.completionRatePercent}%"
        )
        StatRowCard(
            label = "Ocorrências pendentes",
            value = snapshot.pendingOccurrencesCount.toString()
        )
        StatRowCard(
            label = "Ocorrências concluídas",
            value = snapshot.completedOccurrencesCount.toString()
        )
        StatRowCard(
            label = "Ocorrências atrasadas",
            value = snapshot.overdueOccurrencesCount.toString()
        )
        StatRowCard(
            label = "Execuções de rotinas registradas",
            value = snapshot.routineExecutionsCount.toString()
        )
        StatRowCard(
            label = "Financeiro em aberto",
            value = FinanceRules.formatCentsBrl(snapshot.openObligationsCents)
        )
        StatRowCard(
            label = "Financeiro vencido",
            value = FinanceRules.formatCentsBrl(snapshot.overdueObligationsCents)
        )
        StatRowCard(
            label = "Financeiro pago",
            value = FinanceRules.formatCentsBrl(snapshot.paidObligationsCents)
        )
    }
}

@Composable
private fun StatRowCard(
    label: String,
    value: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
