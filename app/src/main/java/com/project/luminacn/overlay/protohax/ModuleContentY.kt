package com.project.luminacn.overlay.protohax

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import com.project.luminacn.R
import com.project.luminacn.constructors.BoolValue
import com.project.luminacn.constructors.CheatCategory
import com.project.luminacn.constructors.Element
import com.project.luminacn.constructors.FloatValue
import com.project.luminacn.constructors.GameManager
import com.project.luminacn.constructors.IntValue
import com.project.luminacn.constructors.ListValue
import com.project.luminacn.overlay.manager.OverlayManager
import com.project.luminacn.util.translatedSelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val moduleCache = HashMap<CheatCategory, List<Element>>()

private fun fetchCachedModules(cheatCategory: CheatCategory): List<Element> {
    val cachedModules = moduleCache[cheatCategory] ?: GameManager
        .elements
        .fastFilter { it.category === cheatCategory && it.name != "ChatListener" }
    moduleCache[cheatCategory] = cachedModules
    return cachedModules
}

@Composable
fun ModuleContentY(moduleCategory: CheatCategory) {
    var modules: List<Element>? by remember(moduleCategory) { mutableStateOf(moduleCache[moduleCategory]) }

    LaunchedEffect(modules) {
        if (modules == null) {
            withContext(Dispatchers.IO) {
                modules = fetchCachedModules(moduleCategory)
            }
        }
    }

    Crossfade(
        targetState = modules
    ) {
        if (it != null) {
            LazyColumn(
                Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(it.size) { index ->
                    val module = it[index]
                    ModuleCard(module)
                }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(element: Element) {
    val values = element.values
    val hasSettings = values.isNotEmpty()
    val background by animateColorAsState(
        targetValue = if (element.isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (hasSettings) {
                        element.isExpanded = !element.isExpanded
                    } else {
                        element.isEnabled = !element.isEnabled
                    }
                }
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = background
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Text(
                    element.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier,
                    color = if (element.isExpanded) contentColorFor(MaterialTheme.colorScheme.primary) else contentColorFor(
                        MaterialTheme.colorScheme.background
                    )
                )
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = element.isEnabled,
                    onCheckedChange = {
                        element.isEnabled = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedBorderColor = if (element.isExpanded) MaterialTheme.colorScheme.surface else Color.Transparent,
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = if (element.isExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.outline,
                        uncheckedThumbColor = if (element.isExpanded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier
                        .width(52.dp)
                        .height(32.dp)
                )
            }
            if (element.isExpanded && hasSettings) {
                values.fastForEach {
                    when (it) {
                        is BoolValue -> BoolValueContent(it)
                        is FloatValue -> FloatValueContent(it)
                        is IntValue -> IntValueContent(it)
                        is ListValue -> ChoiceValueContent(it)
                    }
                }
                ShortcutContent(element)
            }
        }
    }
}

@Composable
private fun ChoiceValueContent(value: ListValue) {
    Column(
        Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 5.dp)
    ) {
        Text(
            value.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surface
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
        ) {
            value.listItems.forEach {
                ElevatedFilterChip(
                    selected = value.value == it,
                    onClick = {
                        if (value.value != it) {
                            value.value = it
                        }
                    },
                    label = {
                        Text(it.name)
                    },
                    modifier = Modifier.height(30.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedContainerColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FloatValueContent(value: FloatValue) {
    Column(
        Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 5.dp)
    ) {
        Row {
            Text(
                value.name.translatedSelf,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface
            )
            Spacer(Modifier.weight(1f))
            Text(
                value.value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface
            )
        }
        val colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.surface,
            activeTrackColor = MaterialTheme.colorScheme.surface,
            activeTickColor = MaterialTheme.colorScheme.surface,
            inactiveTickColor = MaterialTheme.colorScheme.outlineVariant,
            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
        )
        val interactionSource = remember { MutableInteractionSource() }
        Slider(
            value = animateFloatAsState(
                targetValue = value.value,
                label = "",
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow
                )
            ).value,
            valueRange = value.range,
            colors = colors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    thumbSize = DpSize(4.dp, 22.dp),
                    enabled = true
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    colors = colors,
                    enabled = true,
                    sliderState = sliderState,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 4.dp
                )
            },
            onValueChange = {
                val newValue = ((it * 100.0).roundToInt() / 100.0).toFloat()
                if (value.value != newValue) {
                    value.value = newValue
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntValueContent(value: IntValue) {
    Column(
        Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 5.dp)
    ) {
        Row {
            Text(
                value.name.translatedSelf,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface
            )
            Spacer(Modifier.weight(1f))
            Text(
                value.value.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.surface
            )
        }
        val colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.surface,
            activeTrackColor = MaterialTheme.colorScheme.surface,
            activeTickColor = MaterialTheme.colorScheme.surface,
            inactiveTickColor = MaterialTheme.colorScheme.outlineVariant,
            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
        )
        val interactionSource = remember { MutableInteractionSource() }
        Slider(
            value = animateFloatAsState(
                targetValue = value.value.toFloat(),
                label = "",
                animationSpec = spring(
                    stiffness = Spring.StiffnessLow
                )
            ).value,
            valueRange = value.range.toFloatRange(),
            colors = colors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    thumbSize = DpSize(4.dp, 22.dp),
                    enabled = true
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    colors = colors,
                    enabled = true,
                    sliderState = sliderState,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 4.dp
                )
            },
            onValueChange = {
                val newValue = it.roundToInt()
                if (value.value != newValue) {
                    value.value = newValue
                }
            }
        )
    }
}

@Composable
private fun BoolValueContent(value: BoolValue) {
    Row(
        Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 5.dp)
            .toggleable(
                value = value.value,
                interactionSource = null,
                indication = null,
                onValueChange = {
                    value.value = it
                }
            )
    ) {
        Text(
            value.name.translatedSelf,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surface
        )
        Spacer(Modifier.weight(1f))
        Checkbox(
            checked = value.value,
            onCheckedChange = null,
            modifier = Modifier
                .padding(0.dp),
            colors = CheckboxDefaults.colors(
                uncheckedColor = MaterialTheme.colorScheme.surface,
                checkedColor = MaterialTheme.colorScheme.surface,
                checkmarkColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun ShortcutContent(element: Element) {
    Row(
        Modifier
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
            .toggleable(
                value = element.isShortcutDisplayed,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = {
                    element.isShortcutDisplayed = it
                    if (it) {
                        OverlayManager.showOverlayWindow(element.overlayShortcutButton)
                    } else {
                        OverlayManager.dismissOverlayWindow(element.overlayShortcutButton)
                    }
                }
            )
    ) {
        Text(
            stringResource(R.string.shortcut),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surface
        )
        Spacer(Modifier.weight(1f))
        Checkbox(
            checked = element.isShortcutDisplayed,
            onCheckedChange = null,
            modifier = Modifier
                .padding(0.dp),
            colors = CheckboxDefaults.colors(
                uncheckedColor = MaterialTheme.colorScheme.surface,
                checkedColor = MaterialTheme.colorScheme.surface,
                checkmarkColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private fun IntRange.toFloatRange() = first.toFloat()..last.toFloat()