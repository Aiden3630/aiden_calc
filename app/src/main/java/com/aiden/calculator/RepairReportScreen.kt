package com.aiden.calculator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun RepairReportScreen(
    report: RepairReport?,
    topBar: @Composable () -> Unit,
    scan: () -> Unit,
    removeMissing: (MissingBlob) -> Unit,
    recover: (RecoverableOrphan) -> Unit,
    removeTemporary: (StaleTemporaryBlob) -> Unit,
) {
    var pendingTemporary by remember { mutableStateOf<StaleTemporaryBlob?>(null) }
    Scaffold(topBar = topBar) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item {
                Text(stringResource(R.string.repair_explanation), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
                Button(scan) { Text(stringResource(R.string.scan_again)) }
            }
            if (report == null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 20.dp)) }
            report?.missingBlobs?.forEach { missing ->
                item { RepairCard(stringResource(R.string.missing_blob)) { removeMissing(missing) } }
            }
            report?.recoverableOrphans?.forEach { orphan ->
                item { RepairCard(stringResource(R.string.orphan_blob), stringResource(R.string.restore)) { recover(orphan) } }
            }
            report?.staleTemporaryBlobs?.forEach { temporary ->
                item { RepairCard(stringResource(R.string.stale_temporary), stringResource(R.string.delete)) { pendingTemporary = temporary } }
            }
            if (report != null && report.missingBlobs.isEmpty() && report.recoverableOrphans.isEmpty() && report.staleTemporaryBlobs.isEmpty()) {
                item { Text(stringResource(R.string.repair_clean), modifier = Modifier.padding(vertical = 24.dp)) }
            }
        }
    }
    pendingTemporary?.let { temporary ->
        AlertDialog(
            onDismissRequest = { pendingTemporary = null },
            text = { Text(stringResource(R.string.confirm_delete_temporary)) },
            dismissButton = { TextButton({ pendingTemporary = null }) { Text(stringResource(R.string.cancel)) } },
            confirmButton = {
                TextButton({
                    removeTemporary(temporary)
                    pendingTemporary = null
                }) { Text(stringResource(R.string.delete)) }
            },
        )
    }
}

@Composable
private fun RepairCard(text: String, actionLabel: String = stringResource(R.string.remove_broken_record), action: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(text)
            TextButton(action) { Text(actionLabel) }
        }
    }
}
