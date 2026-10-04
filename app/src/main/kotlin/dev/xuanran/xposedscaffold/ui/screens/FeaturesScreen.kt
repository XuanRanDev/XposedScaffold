package dev.xuanran.xposedscaffold.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xuanran.xposed.api.HookUiType
import dev.xuanran.xposed.api.BooleanOption
import dev.xuanran.xposed.api.HookOption
import dev.xuanran.xposed.api.IntRangeOption
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.runtime.ActionHook
import dev.xuanran.xposed.runtime.HookRecord
import dev.xuanran.xposed.runtime.HookRegistry
import dev.xuanran.xposed.runtime.enabledKey
import dev.xuanran.xposedscaffold.ModulePreferences

@Composable
fun FeaturesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = ModulePreferences.preferences
    val records = remember { HookRegistry.all() }
    var query by remember { mutableStateOf("") }
    var selectedRecord by remember { mutableStateOf<HookRecord?>(null) }
    val visible = records.filter { record ->
        val metadata = record.feature.metadata
        query.isBlank() || listOf(metadata.title, metadata.description)
            .plus(metadata.keywords)
            .any { it.contains(query, ignoreCase = true) }
    }
    val grouped = visible.groupBy { it.feature.metadata.path.firstOrNull() ?: "常规" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("功能", fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 18.dp))
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                placeholder = { Text("搜索功能") },
                singleLine = true,
            )
        }
        grouped.forEach { (category, categoryRecords) ->
            item {
                Text(
                    category,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp),
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Column {
                        categoryRecords.forEachIndexed { index, record ->
                            FeatureRow(
                                record = record,
                                preferences = preferences,
                                onAction = { (record.feature as? ActionHook)?.run(context) },
                                onOpen = { selectedRecord = record },
                            )
                            if (index != categoryRecords.lastIndex) {
                                HorizontalDivider(Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                }
            }
        }
        if (visible.isEmpty()) item {
            Text("没有匹配的功能", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(40.dp))
        }
    }
    selectedRecord?.let { record ->
        FeatureDetails(record, preferences) { selectedRecord = null }
    }
}

@Composable
private fun FeatureRow(
    record: HookRecord,
    preferences: SharedPreferences?,
    onAction: () -> Unit,
    onOpen: () -> Unit,
) {
    val feature = record.feature
    val metadata = feature.metadata
    var enabled by remember(metadata.id, preferences) {
        mutableStateOf(metadata.uiType == HookUiType.API ||
            preferences?.getBoolean(enabledKey(metadata.id), false) == true)
    }
    val icon = when (metadata.uiType) {
        HookUiType.SWITCH -> Icons.Outlined.Bolt
        HookUiType.ACTION -> Icons.Outlined.Refresh
        HookUiType.API -> Icons.Outlined.Extension
    }
    ListItem(
        modifier = Modifier.clickable {
            if (metadata.uiType == HookUiType.ACTION) onAction() else onOpen()
        },
        headlineContent = {
            Text(metadata.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(metadata.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
        trailingContent = {
            when (metadata.uiType) {
                HookUiType.SWITCH -> Switch(
                    checked = enabled,
                    enabled = preferences != null,
                    onCheckedChange = {
                        enabled = it
                        preferences?.edit()?.putBoolean(enabledKey(metadata.id), it)?.apply()
                    },
                )
                HookUiType.API -> Text("自动", color = MaterialTheme.colorScheme.primary)
                HookUiType.ACTION -> Text("执行", color = MaterialTheme.colorScheme.primary)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureDetails(
    record: HookRecord,
    preferences: SharedPreferences?,
    onDismiss: () -> Unit,
) {
    val metadata = record.feature.metadata
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(metadata.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(metadata.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                metadata.path.joinToString(" / "),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
            )
            if (preferences == null && metadata.uiType == HookUiType.SWITCH) {
                Text("Xposed 配置服务尚未连接，当前无法修改设置。", color = MaterialTheme.colorScheme.error)
            }
            if (preferences != null) {
                record.feature.options.forEach { option ->
                    OptionEditor(metadata.id, option, preferences)
                }
            }
        }
    }
}

@Composable
private fun OptionEditor(hookId: String, option: HookOption, preferences: SharedPreferences) {
    val key = "hook.$hookId.option.${option.key}"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        when (option) {
            is BooleanOption -> {
                var value by remember(key) { mutableStateOf(preferences.getBoolean(key, option.default)) }
                ListItem(
                    headlineContent = { Text(option.title) },
                    supportingContent = { Text(option.description) },
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
                    value = value,
                    onValueChange = {
                        value = it
                        preferences.edit().putString(key, it).apply()
                    },
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    label = { Text(option.title) },
                    supportingText = { Text(option.description) },
                    singleLine = true,
                )
            }
            is IntRangeOption -> {
                var value by remember(key) { mutableStateOf(preferences.getInt(key, option.default)) }
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(option.title, fontWeight = FontWeight.SemiBold)
                        Text(value.toString(), color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = value.toFloat(),
                        onValueChange = { value = it.toInt() },
                        onValueChangeFinished = { preferences.edit().putInt(key, value).apply() },
                        valueRange = option.range.first.toFloat()..option.range.last.toFloat(),
                        steps = (option.range.last - option.range.first - 1).coerceAtLeast(0),
                    )
                    Text(option.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
