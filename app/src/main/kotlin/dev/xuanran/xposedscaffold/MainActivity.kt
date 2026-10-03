package dev.xuanran.xposedscaffold

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xuanran.xposed.api.HookState
import dev.xuanran.xposed.api.HookUiType
import dev.xuanran.xposed.api.BooleanOption
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.api.IntRangeOption
import dev.xuanran.xposed.api.HookOption
import dev.xuanran.xposed.generated.createHooks
import dev.xuanran.xposed.runtime.ActionHook
import dev.xuanran.xposed.runtime.HookRecord
import dev.xuanran.xposed.runtime.HookRegistry
import dev.xuanran.xposed.runtime.enabledKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 模块应用进程不会经过 Xposed Startup，因此在这里显式注册同一份 KSP 功能列表供 UI 使用。
        if (HookRegistry.all().isEmpty()) HookRegistry.register(createHooks())
        setContent { ScaffoldTheme { ModuleHome(this) } }
    }
}

// 集中维护颜色，后续可以无侵入替换为动态取色或品牌主题。
private val Accent = Color(0xFF9B8CFF)
private val AccentBlue = Color(0xFF64B5F6)
private val Background = Color(0xFF090D16)
private val SurfaceColor = Color(0xFF121824)
private val Success = Color(0xFF52D6A0)
private val Warning = Color(0xFFFFC857)

@Composable
private fun ScaffoldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = AccentBlue,
            background = Background,
            surface = SurfaceColor,
            onBackground = Color(0xFFF2F4FF),
            onSurface = Color(0xFFF2F4FF),
        ),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleHome(context: Context) {
    // 名称必须与 ModuleStartup/remotePreferences 使用的名称一致。
    val preferences = remember { context.getSharedPreferences("xposed_scaffold", Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    val records = remember { HookRegistry.all() }
    val visible = records.filter { record ->
        val metadata = record.feature.metadata
        query.isBlank() || listOf(metadata.title, metadata.description, metadata.path.joinToString("/"))
            .plus(metadata.keywords).any { it.contains(query, ignoreCase = true) }
    }
    // 第一段 path 作为首页分组；更深层级可在未来扩展为二级页面。
    val grouped = visible.groupBy { it.feature.metadata.path.firstOrNull() ?: "General" }

    Scaffold(containerColor = Background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header() }
            item { FrameworkCard(records.size) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Search features, paths and keywords") },
                )
            }
            grouped.forEach { (category, categoryRecords) ->
                item { SectionTitle(category, categoryRecords.size) }
                items(categoryRecords, key = { it.feature.metadata.id }) { record ->
                    HookCard(context, record, preferences)
                }
            }
            if (visible.isEmpty()) item { EmptySearch() }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(17.dp)).background(
                Brush.linearGradient(listOf(Accent, AccentBlue))
            ),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Outlined.Extension, null, tint = Color.White, modifier = Modifier.size(29.dp)) }
        Column(Modifier.padding(start = 14.dp)) {
            Text("Xposed Scaffold", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("dev.xuanran · modern module foundation", color = Color(0xFF939BB0), fontSize = 13.sp)
        }
    }
}

@Composable
private fun FrameworkCard(featureCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(Color(0xFF312A62), Color(0xFF152B46))))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(Success)
                Text("Module dashboard", modifier = Modifier.padding(start = 9.dp), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(14.dp))
            Text("$featureCount registered features", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(
                "Legacy API 82 · libxposed API 101–102 · DexKit ready",
                modifier = Modifier.padding(top = 5.dp), color = Color(0xFFBDC5DC), fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("  $count", color = Color(0xFF7D859B), fontSize = 13.sp)
    }
}

@Composable
private fun HookCard(context: Context, record: HookRecord, preferences: android.content.SharedPreferences) {
    val feature = record.feature
    val metadata = feature.metadata
    var enabled by remember(metadata.id) {
        // API 功能永远启用，普通 SwitchHook 从稳定 ID 对应的配置键恢复。
        mutableStateOf(metadata.uiType == HookUiType.API || preferences.getBoolean(enabledKey(metadata.id), false))
    }
    var showDetails by remember { mutableStateOf(false) }
    val icon = when (metadata.uiType) {
        HookUiType.SWITCH -> Icons.Outlined.Bolt
        HookUiType.ACTION -> Icons.Outlined.Refresh
        HookUiType.API -> Icons.Outlined.Extension
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { showDetails = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(Accent.copy(alpha = .13f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(metadata.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (metadata.experimental) {
                        Text("LAB", color = Warning, fontSize = 9.sp, modifier = Modifier.padding(start = 7.dp))
                    }
                }
                Text(
                    metadata.description.ifBlank { metadata.path.drop(1).joinToString(" / ") },
                    color = Color(0xFF969EB3), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            when (metadata.uiType) {
                HookUiType.SWITCH -> Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        preferences.edit().putBoolean(enabledKey(metadata.id), it).apply()
                        // 默认采用“重启宿主后生效”，避免在设置应用里直接跨进程装卸 Hook。
                    },
                )
                HookUiType.ACTION -> IconButtonArrow { (feature as? ActionHook)?.run(context) }
                HookUiType.API -> StatusDot(Success)
            }
        }
    }
    AnimatedVisibility(showDetails) {
        HookDetails(record, enabled, preferences, onDismiss = { showDetails = false })
    }
}

@Composable
private fun IconButtonArrow(onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.ArrowForwardIos, null, tint = Color(0xFF7F879A), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun HookDetails(
    record: HookRecord,
    enabled: Boolean,
    preferences: android.content.SharedPreferences,
    onDismiss: () -> Unit,
) {
    val metadata = record.feature.metadata
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, null) },
        title = { Text(metadata.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(metadata.description.ifBlank { "No description." })
                HorizontalDivider()
                DetailRow("ID", metadata.id)
                DetailRow("Path", metadata.path.joinToString(" / "))
                DetailRow("Processes", metadata.targetProcesses.joinToString())
                DetailRow("Packages", metadata.targetPackages.joinToString().ifBlank { "Any" })
                DetailRow("Configured", if (enabled) "Enabled" else "Disabled")
                DetailRow("Restart", metadata.restartPolicy.name)
                if (record.feature.options.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Options", fontWeight = FontWeight.SemiBold)
                    record.feature.options.forEach { option ->
                        HookOptionEditor(metadata.id, option, preferences)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun HookOptionEditor(
    hookId: String,
    option: HookOption,
    preferences: android.content.SharedPreferences,
) {
    // 与 core:runtime 中 optionKey 的格式保持一致，宿主和模块才能读取同一项配置。
    val key = "hook.$hookId.option.${option.key}"
    when (option) {
        is BooleanOption -> {
            var value by remember(key) { mutableStateOf(preferences.getBoolean(key, option.default)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(option.title, fontSize = 13.sp)
                    if (option.description.isNotBlank()) Text(option.description, color = Color(0xFF8E96AA), fontSize = 11.sp)
                }
                Switch(value, onCheckedChange = {
                    value = it
                    preferences.edit().putBoolean(key, it).apply()
                })
            }
        }
        is StringOption -> {
            var value by remember(key) { mutableStateOf(preferences.getString(key, option.default) ?: option.default) }
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    preferences.edit().putString(key, it).apply()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(option.title) },
                supportingText = if (option.description.isBlank()) null else ({ Text(option.description) }),
                singleLine = true,
            )
        }
        is IntRangeOption -> {
            var value by remember(key) { mutableStateOf(preferences.getInt(key, option.default)) }
            Column {
                DetailRow(option.title, value.toString())
                Slider(
                    value = value.toFloat(),
                    onValueChange = { value = it.toInt() },
                    onValueChangeFinished = { preferences.edit().putInt(key, value).apply() },
                    valueRange = option.range.first.toFloat()..option.range.last.toFloat(),
                    steps = (option.range.last - option.range.first - 1).coerceAtLeast(0),
                )
            }
        }
    }
}

@Composable private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF8E96AA), fontSize = 12.sp)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2)
    }
}

@Composable private fun StatusDot(color: Color) {
    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
}

@Composable private fun EmptySearch() {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.WarningAmber, null, tint = Color(0xFF697186), modifier = Modifier.size(36.dp))
        Text("No matching features", color = Color(0xFF8C94A8), modifier = Modifier.padding(top = 10.dp))
    }
}
