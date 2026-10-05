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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xuanran.xposed.api.ModuleConfig
import dev.xuanran.xposedscaffold.BuildConfig
import dev.xuanran.xposedscaffold.ModulePreferences
import dev.xuanran.xposedscaffold.R

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    // Both flavors publish their framework-backed preference handle through ModulePreferences.
    // A non-null handle is therefore a useful UI-level signal that the framework channel is ready.
    val active = ModulePreferences.preferences != null
    // Keep device/build facts as plain data so the list remains easy to extend without adding a
    // separate composable for every row.
    val rows = listOf(
        "模块版本" to "${packageInfo.versionName} (${packageInfo.longVersionCode})",
        "构建时间" to BuildConfig.BUILD_TIME.replace('T', ' ').substringBefore('.').removeSuffix("Z"),
        "设备型号" to listOf(Build.MANUFACTURER, Build.MODEL).distinct().joinToString(" "),
        "Android 版本" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "目标应用" to BuildConfig.XPOSED_TARGET_PACKAGES.split(',').joinToString("\n"),
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
        item { ActivationCard(active) }
        item {
            Text(
                "设备信息",
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
private fun ActivationCard(active: Boolean) {
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
                null,
            )
            Column(Modifier.weight(1f)) {
                Text(if (active) "模块已激活" else "等待框架连接", fontWeight = FontWeight.Bold)
                Text(ModulePreferences.status, style = MaterialTheme.typography.bodyMedium)
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
