package dev.xuanran.xposedscaffold

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.xuanran.xposed.api.*
import dev.xuanran.xposed.generated.createHooks
import dev.xuanran.xposed.runtime.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let Activity choose readable system-bar icon colors for both light and dark Compose themes.
        enableEdgeToEdge()
        // The module's own process never passes through a host-side Xposed entrypoint.
        // Register generated features here as well so the configuration UI has the same catalogue.
        if (HookRegistry.all().isEmpty()) HookRegistry.register(createHooks())
        setContent { ScaffoldTheme { ModuleHome(this) } }
    }
}

private val Seed = Color(0xFF5367D8)

@Composable
private fun ScaffoldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = Color(0xFFBBC3FF),
                onPrimary = Color(0xFF1C2D8B),
                primaryContainer = Color(0xFF34459F),
                secondaryContainer = Color(0xFF3C465F),
                background = Color(0xFF111318),
                surface = Color(0xFF1B1D23),
                surfaceContainer = Color(0xFF1F2127),
            )
        } else {
            lightColorScheme(
                primary = Seed,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFDDE1FF),
                secondaryContainer = Color(0xFFDDE2F2),
                background = Color(0xFFF9F9FF),
                surface = Color(0xFFF9F9FF),
                surfaceContainer = Color(0xFFF0F0F7),
            )
        },
        content = content,
    )
}

/**
 * Settings-style root page inspired by WeKit's information architecture.
 *
 * One segmented surface represents one feature category. Unlike a dashboard made of isolated
 * cards, this remains easy to scan after a real module grows to dozens of generated Hook items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModuleHome(context: Context) {
    val preferences = remember { context.getSharedPreferences("xposed_scaffold", Context.MODE_PRIVATE) }
    val records = remember { HookRegistry.all() }
    var query by remember { mutableStateOf("") }
    var selectedRecord by remember { mutableStateOf<HookRecord?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val visible = records.filter { record ->
        val metadata = record.feature.metadata
        query.isBlank() || listOf(metadata.title, metadata.description, metadata.path.joinToString("/"))
            .plus(metadata.keywords)
            .any { it.contains(query, ignoreCase = true) }
    }
    val grouped = visible.groupBy { it.feature.metadata.path.firstOrNull() ?: "General" }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Xposed Scaffold", fontWeight = FontWeight.Bold)
                        Text(
                            "通用模块控制台",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = 36.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ModuleStatus(records.size) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("搜索功能") },
                )
            }
            grouped.forEach { (category, categoryRecords) ->
                item { GroupLabel(category, categoryRecords.size) }
                item {
                    SegmentedGroup {
                        categoryRecords.forEachIndexed { index, record ->
                            HookRow(context, record, preferences) { selectedRecord = record }
                            if (index != categoryRecords.lastIndex) {
                                HorizontalDivider(
                                    Modifier.padding(start = 72.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
                                )
                            }
                        }
                    }
                }
            }
            if (visible.isEmpty()) item { EmptySearch() }
        }
    }

    selectedRecord?.let {
        HookDetailsSheet(it, preferences) { selectedRecord = null }
    }
}

@Composable
private fun ModuleStatus(featureCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(24.dp),
    ) {
        ListItem(
            headlineContent = { Text("脚手架已就绪", fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text("$featureCount 个功能 · DexKit · ${BuildConfig.XPOSED_API_LABEL}") },
            leadingContent = { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(28.dp)) },
            trailingContent = {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        "READY",
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun GroupLabel(title: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 12.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text("  $count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Keeps all rows in one category visually connected instead of producing a wall of cards. */
@Composable
private fun SegmentedGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) { Column(content = content) }
}

@Composable
private fun HookRow(
    context: Context,
    record: HookRecord,
    preferences: SharedPreferences,
    onOpen: () -> Unit,
) {
    val feature = record.feature
    val metadata = feature.metadata
    var enabled by remember(metadata.id) {
        mutableStateOf(metadata.uiType == HookUiType.API || preferences.getBoolean(enabledKey(metadata.id), false))
    }
    val icon = when (metadata.uiType) {
        HookUiType.SWITCH -> Icons.Outlined.Bolt
        HookUiType.ACTION -> Icons.Outlined.Refresh
        HookUiType.API -> Icons.Outlined.Extension
    }
    val activateRow = {
        if (metadata.uiType == HookUiType.ACTION) {
            (feature as? ActionHook)?.run(context)
            Unit
        } else {
            onOpen()
        }
    }

    ListItem(
        modifier = Modifier.clickable(onClick = activateRow),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(metadata.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (metadata.experimental) {
                    Text(
                        "实验性",
                        Modifier.padding(start = 8.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        supportingContent = {
            Text(
                metadata.description.ifBlank { metadata.path.drop(1).joinToString(" / ") },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { FeatureIcon(icon) },
        trailingContent = {
            when (metadata.uiType) {
                HookUiType.SWITCH -> Row(verticalAlignment = Alignment.CenterVertically) {
                    if (feature.options.isNotEmpty()) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            "打开设置",
                            Modifier.size(14.dp).clickable(onClick = onOpen),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            preferences.edit().putBoolean(enabledKey(metadata.id), it).apply()
                        },
                    )
                }
                HookUiType.ACTION -> Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, null, Modifier.size(16.dp))
                HookUiType.API -> StatusDot(MaterialTheme.colorScheme.primary)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun FeatureIcon(icon: ImageVector) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(21.dp))
    }
}

/**
 * A bottom sheet gives future option types more room than an AlertDialog while keeping the
 * feature list visible as navigation context behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HookDetailsSheet(
    record: HookRecord,
    preferences: SharedPreferences,
    onDismiss: () -> Unit,
) {
    val metadata = record.feature.metadata
    val enabled = metadata.uiType == HookUiType.API ||
        preferences.getBoolean(enabledKey(metadata.id), false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FeatureIcon(Icons.Outlined.Info)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(metadata.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        metadata.path.joinToString(" / "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (metadata.description.isNotBlank()) {
                Text(metadata.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SegmentedGroup {
                DetailRow("状态", if (enabled) "已启用" else "未启用")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                DetailRow("目标进程", metadata.targetProcesses.joinToString())
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                DetailRow("重启策略", metadata.restartPolicy.name)
            }
            if (record.feature.options.isNotEmpty()) {
                Text(
                    "功能设置",
                    Modifier.padding(start = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
                SegmentedGroup {
                    record.feature.options.forEachIndexed { index, option ->
                        HookOptionEditor(metadata.id, option, preferences)
                        if (index != record.feature.options.lastIndex) {
                            HorizontalDivider(Modifier.padding(start = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HookOptionEditor(hookId: String, option: HookOption, preferences: SharedPreferences) {
    val key = "hook.$hookId.option.${option.key}"
    when (option) {
        is BooleanOption -> {
            var value by remember(key) { mutableStateOf(preferences.getBoolean(key, option.default)) }
            ListItem(
                headlineContent = { Text(option.title) },
                supportingContent = option.description.takeIf { it.isNotBlank() }?.let { description ->
                    { Text(description) }
                },
                trailingContent = {
                    Switch(value, {
                        value = it
                        preferences.edit().putBoolean(key, it).apply()
                    })
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
        is StringOption -> {
            var value by remember(key) {
                mutableStateOf(preferences.getString(key, option.default) ?: option.default)
            }
            OutlinedTextField(
                value,
                {
                    value = it
                    preferences.edit().putString(key, it).apply()
                },
                Modifier.fillMaxWidth().padding(16.dp),
                label = { Text(option.title) },
                supportingText = option.description.takeIf { it.isNotBlank() }?.let { description ->
                    { Text(description) }
                },
                singleLine = true,
            )
        }
        is IntRangeOption -> {
            var value by remember(key) { mutableStateOf(preferences.getInt(key, option.default)) }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                DetailRow(option.title, value.toString(), PaddingValues())
                Slider(
                    value.toFloat(),
                    { value = it.toInt() },
                    onValueChangeFinished = { preferences.edit().putInt(key, value).apply() },
                    valueRange = option.range.first.toFloat()..option.range.last.toFloat(),
                    steps = (option.range.last - option.range.first - 1).coerceAtLeast(0),
                )
                if (option.description.isNotBlank()) {
                    Text(
                        option.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    Row(
        Modifier.fillMaxWidth().padding(contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium, maxLines = 2)
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun EmptySearch() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            null,
            Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("没有匹配的功能", Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
