package dev.xuanran.xposedscaffold.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposedscaffold.BuildConfig
import dev.xuanran.xposedscaffold.ConnectionState
import dev.xuanran.xposedscaffold.ModuleUiState
import dev.xuanran.xposedscaffold.R

@Composable
fun HomeScreen(uiState: ModuleUiState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    // Both flavors publish their framework-backed preference handle through ModuleRepository.
    // A non-null handle is therefore a useful UI-level signal that the framework channel is ready.
    val active = uiState.preferences != null
    // Keep device/build facts as plain data so the list remains easy to extend without adding a
    // separate composable for every row.
    val rows = listOf(
        stringResource(R.string.info_module_version) to "${packageInfo.versionName} (${packageInfo.longVersionCode})",
        stringResource(R.string.info_build_time) to BuildConfig.BUILD_TIME.replace('T', ' ').substringBefore('.').removeSuffix("Z"),
        stringResource(R.string.info_device) to listOf(Build.MANUFACTURER, Build.MODEL).distinct().joinToString(" "),
        stringResource(R.string.info_android_version) to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        stringResource(R.string.info_target_apps) to BuildConfig.XPOSED_TARGET_PACKAGES.split(',').joinToString("\n"),
    )
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                ModuleConfig.NAME,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(vertical = 26.dp),
            )
        }
        item { ActivationCard(active, uiState.connection) }
        item {
            Text(
                stringResource(R.string.device_information),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 2.dp),
            )
        }
        items(rows) { (label, value) -> InfoCard(label, value) }
    }
}

@Composable
private fun ActivationCard(active: Boolean, connection: ConnectionState) {
    val status = stringResource(
        when (connection) {
            ConnectionState.CONNECTING -> R.string.connection_connecting
            ConnectionState.LEGACY_CONNECTED -> R.string.connection_legacy
            ConnectionState.MODERN_CONNECTED -> R.string.connection_modern
            ConnectionState.DISCONNECTED -> R.string.connection_disconnected
            ConnectionState.UNSUPPORTED -> R.string.connection_unsupported
        },
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(
                painterResource(
                    if (active) R.drawable.ic_check_circle else R.drawable.ic_warning,
                ),
                stringResource(if (active) R.string.module_active else R.string.module_waiting),
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(if (active) R.string.module_active else R.string.module_waiting), fontWeight = FontWeight.Bold)
                Text(status, style = MaterialTheme.typography.bodyMedium)
            }
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(
                    BuildConfig.XPOSED_API_LABEL,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                value,
                modifier = Modifier.padding(top = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
