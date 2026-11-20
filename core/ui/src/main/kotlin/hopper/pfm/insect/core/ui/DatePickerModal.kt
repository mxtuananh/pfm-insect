@file:OptIn(ExperimentalMaterial3Api::class)

package hopper.pfm.insect.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

@Composable
fun DatePickerModal(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDate = selectedDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onDateSelected(datePickerState.getSelectedDate())
                    onDismiss()
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = stringResource(R.string.core_ui_datepicker_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.core_ui_datepicker_button_dismiss))
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        DatePicker(state = datePickerState)
    }
}
