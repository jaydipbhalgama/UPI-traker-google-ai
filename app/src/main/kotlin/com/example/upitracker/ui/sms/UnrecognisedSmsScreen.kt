package com.example.upitracker.ui.sms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.upitracker.data.sms.UnrecognisedSmsItem
import com.example.upitracker.ui.components.AppCard
import com.example.upitracker.ui.components.EmptyState
import com.example.upitracker.ui.theme.Dimens
import com.example.upitracker.ui.theme.JetBrainsMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UnrecognisedSmsScreen(
    uiState: UnrecognisedSmsUiState,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    when (uiState) {
        is UnrecognisedSmsUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is UnrecognisedSmsUiState.NoPermission -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(Dimens.dp24),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.Warning,
                    title = "SMS Permission Required",
                    hint = "Allow READ_SMS permission to inspect bank messages that were not matched automatically.",
                    actionText = "Grant Permission",
                    onActionClick = onRequestPermission
                )
            }
        }
        is UnrecognisedSmsUiState.Success -> {
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(Dimens.dp24),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.CheckCircle,
                        title = "All bank SMS recognised",
                        hint = "Every recent bank message in your inbox was parsed cleanly."
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.dp12)
                ) {
                    items(uiState.items) { item ->
                        UnrecognisedSmsCard(
                            item = item,
                            formattedDate = dateFormat.format(Date(item.timestamp)),
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(item.body))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UnrecognisedSmsCard(
    item: UnrecognisedSmsItem,
    formattedDate: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.sender,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Dimens.dp8))

            Text(
                text = item.body,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Dimens.dp12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onCopy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget)
                ) {
                    Text("Copy text")
                }
            }
        }
    }
}
