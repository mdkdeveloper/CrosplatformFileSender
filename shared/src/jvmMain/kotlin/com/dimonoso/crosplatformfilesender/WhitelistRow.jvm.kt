@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.dimonoso.crosplatformfilesender

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import crosplatformfilesender.shared.generated.resources.delete_policy_ask
import crosplatformfilesender.shared.generated.resources.delete_policy_default
import crosplatformfilesender.shared.generated.resources.delete_policy_none
import crosplatformfilesender.shared.generated.resources.delete_policy_permanent
import crosplatformfilesender.shared.generated.resources.delete_policy_trash
import crosplatformfilesender.shared.generated.resources.remove
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun WhitelistRow(
    folder: WhitelistFolder,
    canMoveFolderToTrash: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onDeletePolicyChange: (WhitelistDeletePolicyOverride) -> Unit,
    onRemove: () -> Unit,
) {
    RowSurface {
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = folder.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.UseDefault,
                    label = stringResource(Res.string.delete_policy_default),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.DoNothing,
                    label = stringResource(Res.string.delete_policy_none),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.Ask,
                    label = stringResource(Res.string.delete_policy_ask),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
                if (canMoveFolderToTrash) {
                    WhitelistDeletePolicyButton(
                        policy = WhitelistDeletePolicyOverride.Trash,
                        label = stringResource(Res.string.delete_policy_trash),
                        selectedPolicy = folder.deletePolicyOverride,
                        onPolicyChange = onDeletePolicyChange,
                    )
                }
                WhitelistDeletePolicyButton(
                    policy = WhitelistDeletePolicyOverride.Permanent,
                    label = stringResource(Res.string.delete_policy_permanent),
                    selectedPolicy = folder.deletePolicyOverride,
                    onPolicyChange = onDeletePolicyChange,
                )
            }
        }
        Switch(
            checked = folder.enabled,
            onCheckedChange = onEnabledChange,
        )
        TextButton(onClick = onRemove) {
            Text(stringResource(Res.string.remove))
        }
    }
}
