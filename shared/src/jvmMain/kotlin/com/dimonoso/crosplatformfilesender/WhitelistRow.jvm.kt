@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dimonoso.crosplatformfilesender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistDeletePolicyOverride
import com.dimonoso.crosplatformfilesender.filesystem.WhitelistFolder
import crosplatformfilesender.shared.generated.resources.Res
import crosplatformfilesender.shared.generated.resources.remove
import crosplatformfilesender.shared.generated.resources.whitelist_path_unavailable
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun WhitelistRow(
    folder: WhitelistFolder,
    pathAvailable: Boolean,
    canMoveFolderToTrash: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDeletePolicyChange: (WhitelistDeletePolicyOverride) -> Unit,
    onRemove: () -> Unit,
) {
    RowSurface {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(folder.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = folder.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SingleChoiceDropdown(
                modifier = Modifier.fillMaxWidth(),
                selected = folder.deletePolicyOverride,
                options = whitelistDeletePolicyOptions(canMoveFolderToTrash, folder.deletePolicyOverride),
                label = { whitelistDeletePolicyLabel(it) },
                onSelected = onDeletePolicyChange,
            )
            if (!pathAvailable) {
                Text(
                    text = stringResource(Res.string.whitelist_path_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(
            checked = folder.enabled,
            onCheckedChange = onEnabledChange,
            enabled = folder.enabled || pathAvailable,
        )
        TextButton(onClick = onRemove) {
            Text(stringResource(Res.string.remove))
        }
    }
}
