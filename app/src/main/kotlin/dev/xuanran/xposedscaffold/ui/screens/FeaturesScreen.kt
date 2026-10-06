package dev.xuanran.xposedscaffold.ui.screens

import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.xuanran.xposed.api.HookUiType
import dev.xuanran.xposed.api.BooleanOption
import dev.xuanran.xposed.api.ChoiceOption
import dev.xuanran.xposed.api.HookOption
import dev.xuanran.xposed.api.IntRangeOption
import dev.xuanran.xposed.api.StringOption
import dev.xuanran.xposed.runtime.HookRecord
import dev.xuanran.xposed.runtime.HookRegistry
import dev.xuanran.xposed.runtime.enabledKey
import dev.xuanran.xposed.runtime.optionKey
import dev.xuanran.xposedscaffold.ModuleUiState
import dev.xuanran.xposedscaffold.ModuleViewModel
import dev.xuanran.xposedscaffold.R

@Composable
fun FeaturesScreen(uiState: ModuleUiState, viewModel: ModuleViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = uiState.preferences
    val records = remember { HookRegistry.all() }
    var query by remember { mutableStateOf("") }
    var selectedRecord by remember { mutableStateOf<HookRecord?>(null) }
    val visible = records.filter { record ->
        val metadata = record.feature.metadata
        query.isBlank() || listOf(metadata.title, metadata.description)
            .plus(metadata.keywords)
            .any { it.contains(query, ignoreCase = true) }
    }
    // The first path segment is the list section; deeper segments remain available in details.
    val grouped = visible.groupBy { it.feature.metadata.path.firstOrNull() ?: "常规" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(stringResource(R.string.features_title), fontSize = 36.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 18.dp))
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), stringResource(R.string.search)) },
                placeholder = { Text(stringResource(R.string.search_features)) },
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
                                revision = uiState.preferenceRevision,
                                onEnabledChange = { viewModel.setEnabled(record.feature.metadata.id, it) },
                                onAction = { viewModel.runAction(context, record) },
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
            Text(stringResource(R.string.no_matching_features), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(40.dp))
        }
    }
    selectedRecord?.let { record ->
        FeatureDetails(record, uiState, viewModel) { selectedRecord = null }
    }
}

@Composable
private fun FeatureRow(
    record: HookRecord,
    preferences: SharedPreferences?,
    revision: Long,
    onEnabledChange: (Boolean) -> Unit,
    onAction: () -> Unit,
    onOpen: () -> Unit,
) {
    val feature = record.feature
    val metadata = feature.metadata
    var enabled by remember(metadata.id, preferences, revision) {
        mutableStateOf(metadata.uiType == HookUiType.API ||
            preferences?.getBoolean(enabledKey(metadata.id), false) == true)
    }
    val icon = when (metadata.uiType) {
        HookUiType.SWITCH -> R.drawable.ic_bolt
        HookUiType.ACTION -> R.drawable.ic_refresh
        HookUiType.API -> R.drawable.ic_extension
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
                    Icon(
                    painterResource(icon),
                    metadata.title,
                    Modifier.padding(10.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        trailingContent = {
            when (metadata.uiType) {
                HookUiType.SWITCH -> Switch(
                    checked = enabled,
                    enabled = preferences != null,
                    onCheckedChange = {
                        enabled = it
                        onEnabledChange(it)
                    },
                )
                HookUiType.API -> Text(stringResource(R.string.feature_automatic), color = MaterialTheme.colorScheme.primary)
                HookUiType.ACTION -> Text(stringResource(R.string.feature_run), color = MaterialTheme.colorScheme.primary)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureDetails(
    record: HookRecord,
    uiState: ModuleUiState,
    viewModel: ModuleViewModel,
    onDismiss: () -> Unit,
) {
    val metadata = record.feature.metadata
    val preferences = uiState.preferences
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
                Text(stringResource(R.string.preferences_unavailable), color = MaterialTheme.colorScheme.error)
            }
            if (preferences != null) {
                record.feature.options.forEach { option ->
                    OptionEditor(metadata.id, option, preferences, uiState.preferenceRevision, viewModel)
                }
            }
        }
    }
}

@Composable
private fun OptionEditor(
    hookId: String,
    option: HookOption,
    preferences: SharedPreferences,
    revision: Long,
    viewModel: ModuleViewModel,
) {
    // Runtime and UI must share this stable namespace. Renaming a title does not invalidate the
    // stored value as long as the HookItem id and option key remain unchanged.
    val key = optionKey(hookId, option.key)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        when (option) {
            is BooleanOption -> {
                var value by remember(key, revision) { mutableStateOf(preferences.getBoolean(key, option.default)) }
                ListItem(
                    headlineContent = { Text(option.title) },
                    supportingContent = { Text(option.description) },
                    trailingContent = {
                        Switch(value, {
                            value = it
                            viewModel.setBoolean(hookId, option, it)
                        })
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            is StringOption -> {
                var value by remember(key, revision) {
                    mutableStateOf(preferences.getString(key, option.default) ?: option.default)
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        viewModel.setString(hookId, option, it)
                    },
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    label = { Text(option.title) },
                    supportingText = { Text(option.description) },
                    singleLine = true,
                )
            }
            is IntRangeOption -> {
                var value by remember(key, revision) { mutableStateOf(preferences.getInt(key, option.default)) }
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(option.title, fontWeight = FontWeight.SemiBold)
                        Text(value.toString(), color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = value.toFloat(),
                        onValueChange = { value = it.toInt() },
                        onValueChangeFinished = { viewModel.setInt(hookId, option, value) },
                        valueRange = option.range.first.toFloat()..option.range.last.toFloat(),
                        steps = (option.range.last - option.range.first - 1).coerceAtLeast(0),
                    )
                    Text(option.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is ChoiceOption -> {
                var expanded by remember { mutableStateOf(false) }
                var value by remember(key, revision) {
                    mutableStateOf(preferences.getString(key, option.default) ?: option.default)
                }
                val selected = option.choices.firstOrNull { it.value == value } ?: option.choices.first()
                Box {
                    ListItem(
                        modifier = Modifier.clickable { expanded = true },
                        headlineContent = { Text(option.title) },
                        supportingContent = { Text(option.description) },
                        trailingContent = { Text(selected.label, color = MaterialTheme.colorScheme.primary) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        option.choices.forEach { choice ->
                            DropdownMenuItem(
                                text = { Text(choice.label) },
                                onClick = {
                                    value = choice.value
                                    viewModel.setString(hookId, option, choice.value)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
