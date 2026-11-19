package com.truerandom.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import truerandomwindows.composeapp.generated.resources.Res
import truerandomwindows.composeapp.generated.resources.cancel
import truerandomwindows.composeapp.generated.resources.ok

@Composable
fun ConfirmationDialog(dialogEvent: DialogEvent) {
    val showDialog = dialogEvent.showDialog

    if (!showDialog) return

    val title = dialogEvent.title
    val desc = dialogEvent.desc
    val confirmLabel = dialogEvent.confirmLabel
    val onConfirm = dialogEvent.onConfirm
    val dismissLabel = dialogEvent.dismissLabel
    val onDismiss = dialogEvent.onDismiss

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            title?.let {
                Text(text = stringResource(it))
            }
        },
        text = {
            desc?.let {
                Text(text = stringResource(it))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(confirmLabel))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(dismissLabel))
            }
        }
    )
}

data class DialogEvent(
    val showDialog: Boolean,
    val title: StringResource? = null,
    val desc: StringResource? = null,
    val confirmLabel: StringResource = Res.string.ok,
    val onConfirm: (() -> Unit) = {},
    val onDismiss: (() -> Unit) = {},
    val dismissLabel: StringResource = Res.string.cancel
)