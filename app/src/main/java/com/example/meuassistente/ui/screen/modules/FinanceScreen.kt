package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.repository.FinanceRepository
import com.example.meuassistente.domain.rule.FinanceRules
import com.example.meuassistente.domain.usecase.finance.ConfirmPaymentUseCase
import com.example.meuassistente.domain.usecase.finance.CreateFinancialObligationUseCase
import com.example.meuassistente.domain.usecase.finance.SummarizeObligationsUseCase
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.TimeZone

@Composable
fun FinanceScreen(
    financeRepository: FinanceRepository,
    createObligationUseCase: CreateFinancialObligationUseCase,
    confirmPaymentUseCase: ConfirmPaymentUseCase,
    summarizeObligationsUseCase: SummarizeObligationsUseCase,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val obligations by financeRepository.observeObligations().collectAsState(initial = emptyList())
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val nowUtc = System.currentTimeMillis()
    val summary = remember(obligations, nowUtc) {
        summarizeObligationsUseCase(obligations, emptyList(), nowUtc)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Financeiro",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onBack) {
                Text("Voltar")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryBox(
                label = "Em aberto",
                value = FinanceRules.formatCentsBrl(summary.openTotalCents),
                modifier = Modifier.weight(1f)
            )
            SummaryBox(
                label = "Vencido",
                value = FinanceRules.formatCentsBrl(summary.overdueTotalCents),
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = {
                        titleInput = it
                        errorMessage = null
                    },
                    label = { Text("Descrição da conta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        errorMessage = null
                    },
                    label = { Text("Valor em R$ (ex.: 129,90)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Button(
                    onClick = {
                        val parsedCents = parseCurrencyToCents(amountInput)
                        if (parsedCents == null || parsedCents <= 0L) {
                            errorMessage = "Informe um valor válido maior que zero."
                            return@Button
                        }
                        scope.launch {
                            runCatching {
                                val currentNow = System.currentTimeMillis()
                                createObligationUseCase(
                                    FinancialObligation(
                                        title = titleInput,
                                        amountCents = parsedCents,
                                        dueAtUtc = currentNow + 3L * 24 * 3600_000L,
                                        timezoneId = TimeZone.getDefault().id,
                                        status = ObligationStatus.OPEN,
                                        createdAtUtc = currentNow
                                    )
                                )
                            }.onSuccess {
                                titleInput = ""
                                amountInput = ""
                                errorMessage = null
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Falha ao salvar obrigação."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adicionar obrigação")
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(obligations, key = { it.id }) { obligation ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = obligation.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${FinanceRules.formatCentsBrl(obligation.amountCents)} • ${obligation.status.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (obligation.status == ObligationStatus.OPEN) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        confirmPaymentUseCase(
                                            obligation = obligation,
                                            paidAmountCents = obligation.amountCents,
                                            paidAtUtc = System.currentTimeMillis()
                                        )
                                    }
                                }
                            ) {
                                Text("Pagar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun parseCurrencyToCents(raw: String): Long? {
    val cleaned = raw.trim()
        .replace("R$", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", ".")
        .trim()
    if (cleaned.isEmpty()) return null
    val bd = cleaned.toBigDecimalOrNull() ?: return null
    return bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
}
