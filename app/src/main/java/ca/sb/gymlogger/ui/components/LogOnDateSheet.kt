package ca.sb.gymlogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ca.sb.gymlogger.data.dayLabel
import ca.sb.gymlogger.data.formatDateKeyShort

/** The three workout types the user can log. */
enum class WorkoutType { CARDIO, WEIGHTS, STRETCHING }

/**
 * Modal bottom sheet shown when the user taps an empty day on the calendar.
 * Offers the three workout types; selecting one calls [onTypeSelected] with that type
 * (and the date the sheet was opened for, which the caller already knows).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogOnDateSheet(
    dateKey: Int,
    onTypeSelected: (WorkoutType) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 32.dp)
        ) {
            Text(
                text = "LOG WORKOUT FOR",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dayLabel(dateKey).let {
                    // Add the date subtitle for TODAY/YESTERDAY labels so user knows the actual date
                    if (it == "TODAY" || it == "YESTERDAY") "$it · ${formatDateKeyShort(dateKey)}" else it
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            TypeButton(
                label = "Cardio",
                subtitle = "Treadmill, distance & pace",
                icon = Icons.Filled.DirectionsRun,
                onClick = { onTypeSelected(WorkoutType.CARDIO) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TypeButton(
                label = "Weights",
                subtitle = "Sets, reps & weight",
                icon = Icons.Filled.FitnessCenter,
                onClick = { onTypeSelected(WorkoutType.WEIGHTS) }
            )
            Spacer(modifier = Modifier.height(10.dp))
            TypeButton(
                label = "Stretching",
                subtitle = "Mobility & recovery",
                icon = Icons.Filled.SelfImprovement,
                onClick = { onTypeSelected(WorkoutType.STRETCHING) }
            )
        }
    }
}

@Composable
private fun TypeButton(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
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
