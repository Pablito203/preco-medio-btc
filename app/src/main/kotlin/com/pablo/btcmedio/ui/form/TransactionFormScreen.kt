package com.pablo.btcmedio.ui.form

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pablo.btcmedio.core.draft.DraftField
import com.pablo.btcmedio.core.draft.DraftIssue
import com.pablo.btcmedio.core.draft.Severity
import com.pablo.btcmedio.core.model.TransactionType
import com.pablo.btcmedio.ui.format.Formatters
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    state: FormState,
    actions: FormActions,
    onDone: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Editar transação" else "Nova transação") },
                navigationIcon = { TextButton(onClick = onDone) { Text("Cancelar") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.draft.rawText?.let { RawTextPanel(it) }

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.draft.type == TransactionType.BUY,
                    onClick = { actions.onTypeChange(TransactionType.BUY) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Compra") }
                SegmentedButton(
                    selected = state.draft.type == TransactionType.SELL,
                    onClick = { actions.onTypeChange(TransactionType.SELL) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Venda") }
            }

            DateTimeField(
                epochMillis = state.draft.occurredAt,
                issue = state.issueFor(DraftField.DATE),
                onChange = actions.onDateChange,
            )

            NumberField("Valor em reais", state.fiatText, actions.onFiatChange, state.issueFor(DraftField.FIAT))
            NumberField("Taxa", state.feeText, actions.onFeeChange, state.issueFor(DraftField.FEE))
            NumberField(
                "Quantidade de Bitcoin",
                state.satsText,
                actions.onSatsChange,
                state.issueFor(DraftField.SATS),
            )
            NumberField("Cotação", state.priceText, actions.onPriceChange, state.issueFor(DraftField.PRICE))

            OutlinedTextField(
                value = state.noteText,
                onValueChange = actions.onNoteChange,
                label = { Text("Observação") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.canComplete) {
                OutlinedButton(onClick = actions.onCompleteMissing, modifier = Modifier.fillMaxWidth()) {
                    Text("Completar campo faltante")
                }
            }

            state.saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = actions.onSave,
                    enabled = state.canSave,
                    modifier = Modifier.weight(1f),
                ) { Text("Salvar") }

                if (state.isEditing) {
                    OutlinedButton(onClick = actions.onDelete, modifier = Modifier.weight(1f)) {
                        Text("Excluir")
                    }
                }
            }
        }
    }
}

@Composable
private fun RawTextPanel(rawText: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            TextButton(onClick = { expanded = !expanded }) { Text("Texto reconhecido") }
            if (expanded) {
                Text(rawText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * Data e hora em duas etapas: calendário e depois relógio.
 *
 * O campo é somente leitura e abre os seletores ao toque — digitar data à mão
 * em teclado de celular é a principal fonte de erro de digitação em formulário.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(
    epochMillis: Long?,
    issue: DraftIssue?,
    onChange: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    OutlinedTextField(
        value = epochMillis?.let { Formatters.dateTime(it) } ?: "",
        onValueChange = {},
        readOnly = true,
        enabled = false,
        label = { Text("Data e hora") },
        isError = issue?.severity == Severity.ERROR,
        supportingText = { issue?.let { Text(it.message) } },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDate = true },
    )

    if (showDate) {
        // O DatePicker trabalha em UTC; converto para não deslocar o dia.
        val initialUtc = (epochMillis ?: System.currentTimeMillis())
            .let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)

        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { utc ->
                        pickedDate = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDate = false
                    showTime = true
                }) { Text("Continuar") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancelar") } },
        ) { DatePicker(state = dateState) }
    }

    if (showTime) {
        val current = epochMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
        val timeState = rememberTimePickerState(
            initialHour = current?.hour ?: 0,
            initialMinute = current?.minute ?: 0,
            is24Hour = true,
        )

        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Hora da transação") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val day = pickedDate
                        ?: epochMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                        ?: LocalDate.now()
                    val moment = LocalDateTime.of(day, java.time.LocalTime.of(timeState.hour, timeState.minute))
                    onChange(moment.atZone(zone).toInstant().toEpochMilli())
                    showTime = false
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    issue: DraftIssue?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = issue?.severity == Severity.ERROR,
        supportingText = { issue?.let { Text(it.message) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
