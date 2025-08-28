package com.phoenix.luminacn.overlay.kitsugui


import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // 添加: 用于点击交互
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Keyboard // 添加: 按键图标
import androidx.compose.material.icons.outlined.Shortcut
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox // 添加: 用于显示绑定状态
import androidx.compose.material3.CheckboxDefaults // 添加: 自定义Checkbox颜色
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // 添加: 监听绑定状态
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // 添加: 获取Context
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import com.phoenix.luminacn.R // 添加: 为了stringResource
import com.phoenix.luminacn.constructors.BoolValue
import com.phoenix.luminacn.constructors.CheatCategory
import com.phoenix.luminacn.constructors.Element
import com.phoenix.luminacn.constructors.EnumValue
import com.phoenix.luminacn.constructors.FloatValue
import com.phoenix.luminacn.constructors.GameManager
import com.phoenix.luminacn.constructors.IntValue
import com.phoenix.luminacn.constructors.KeyBindingManager // 添加: 绑定状态管理器
import com.phoenix.luminacn.constructors.ListValue
import com.phoenix.luminacn.constructors.StringeValue
import com.phoenix.luminacn.overlay.manager.OverlayManager
import com.phoenix.luminacn.service.KeyCaptureService // 添加: 绑定逻辑服务
import com.phoenix.luminacn.util.translatedSelf
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ModuleContent(
    cheatCategory: CheatCategory,
    onOpenSettings: ((Element) -> Unit)? = null
) {
    val modules = GameManager.elements
        .fastFilter { it.category === cheatCategory && it.name != "ChatListener" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(modules.size) {
            val module = modules[it]
            ModuleCard(module, onOpenSettings)
        }
    }
}

@Composable
private fun ModuleCard(
    element: Element,
    onOpenSettings: ((Element) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val deepBlue = Color(0xFF1E90FF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = element.isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = { element.isEnabled = it }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = androidx.compose.animation.core.EaseInOutCubic
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (element.displayNameResId != null) stringResource(id = element.displayNameResId!!) else element.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )

                        Text(
                            text = "启用这个模块",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onOpenSettings != null) {
                            IconButton(
                                onClick = {
                                    isExpanded = !isExpanded
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "设置",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        if (element.overlayShortcutButton != null) {
                            ShortcutContent(element)
                        }

                        // 添加: 调用KeyBindContent
                        KeyBindContent(element)

                        element.values.forEach { value ->
                            when (value) {
                                is BoolValue -> BoolValueContent(value)
                                is IntValue -> IntValueContent(value)
                                is FloatValue -> FloatValueContent(value)
                                is ListValue -> ChoiceValueContent(value)
                                is EnumValue<*> -> EnumValueContent(value)
                                is StringeValue -> StringeValueContent(value)
                            }
                        }
                    }
                }
            }

            val accentBarHeight by animateFloatAsState(
                targetValue = if (isExpanded) 150f else 70f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = androidx.compose.animation.core.EaseInOutCubic
                ),
                label = "AccentBarHeight"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(8.dp)
                    .height(accentBarHeight.dp)
                    .background(
                        color = if (element.isEnabled && !isExpanded) deepBlue else Color.Transparent,
                        shape = RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 12.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 12.dp
                        )
                    )
            )
        }
    }
}

// ... 其他函数保持不变 ...
@Composable
private fun ModuleShortcutContent(element: Element) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
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
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shortcut,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "快捷键",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        Switch(
            checked = element.isShortcutDisplayed,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedTrackColor = Color(0xFF4A4A4A),
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.size(40.dp, 20.dp)
        )
    }
}

@Composable
private fun ChoiceValueContent(value: ListValue) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = if (value.nameResId != 0) stringResource(id = value.nameResId) else value.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            value.listItems.forEach { item ->
                val isSelected = value.value == item
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (value.value != item) {
                            value.value = item
                        }
                    },
                    label = {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) Color.Transparent else Color.Gray.copy(alpha = 0.5f)
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = if (isSelected)
                            Color(0xFFFFF8F8)
                        else
                            Color(0xFF3A3A3A),
                        labelColor = if (isSelected)
                            Color.Black
                        else
                            Color.White,
                        selectedContainerColor = Color(0xFF232323),
                        selectedLabelColor = Color.White
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (value.nameResId != 0) stringResource(id = value.nameResId) else value.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )

            Text(
                text = String.format(Locale.US, "%.2f", value.value),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFFFFFFF)
            )
        }

        val colors = SliderDefaults.colors(
            thumbColor = Color(0xFFFFFFFF),
            activeTrackColor = Color(0xFFFFFFFF),
            inactiveTrackColor = Color(0xFF4A4A4A),
        )

        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            Slider(
                value = animateFloatAsState(
                    targetValue = value.value,
                    label = "",
                ).value,
                valueRange = value.range,
                colors = colors,
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(),
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = colors,
                        thumbSize = DpSize(20.dp, 20.dp),
                        enabled = true
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "%.1f", value.range.start),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = String.format(Locale.US, "%.1f", value.range.endInclusive),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntValueContent(value: IntValue) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (value.nameResId != 0) stringResource(id = value.nameResId) else value.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )

            Text(
                text = value.value.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFFFFFFFF)
            )
        }

        val colors = SliderDefaults.colors(
            thumbColor = Color(0xFFFFFFFF),
            activeTrackColor = Color(0xFFFFFFFF),
            inactiveTrackColor = Color(0xFF4A4A4A),
        )

        val interactionSource = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
        ) {
            Slider(
                value = animateFloatAsState(
                    targetValue = value.value.toFloat(),
                    label = "",
                ).value,
                valueRange = value.range.toFloatRange(),
                colors = colors,
                steps = (value.range.last - value.range.first - 1).coerceAtLeast(0),
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(),
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = colors,
                        thumbSize = DpSize(20.dp, 20.dp),
                        enabled = true
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value.range.first.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = value.range.last.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun BoolValueContent(value: BoolValue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .toggleable(
                value = value.value,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = { value.value = it }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (value.nameResId != 0) stringResource(id = value.nameResId) else value.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
        Switch(
            checked = value.value,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = Color(0xFF9C9C9C),
                uncheckedTrackColor = Color(0xFF4A4A4A),
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun <T : Enum<T>> EnumValueContent(value: EnumValue<T>) {
    Column(
        Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
    ) {
        Text(
            value.name.translatedSelf,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
        ) {
            value.enumClass.enumConstants?.forEach { enumValue ->
                ElevatedFilterChip(
                    selected = value.value == enumValue,
                    onClick = {
                        if (value.value != enumValue) {
                            value.value = enumValue
                        }
                    },
                    label = {
                        Text(enumValue.name.translatedSelf)
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

@Composable
private fun StringeValueContent(value: StringeValue) {
    Column(
        Modifier
            .padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
    ) {
        Text(
            value.name.translatedSelf,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        OutlinedTextField(
            value = value.value,
            onValueChange = { value.value = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
private fun ShortcutContent(element: Element) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
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
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Shortcut,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "快捷键",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
        }

        Switch(
            checked = element.isShortcutDisplayed,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.Gray,
                checkedTrackColor = Color(0xFFFFFFFF),
                uncheckedTrackColor = Color(0xFF4A4A4A),
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// 添加: 新增实体按键绑定UI组件, 风格与ShortcutContent保持一致
@Composable
private fun KeyBindContent(element: Element) {
    val context = LocalContext.current
    val bindings by KeyBindingManager.bindings.collectAsState()
    val isBound = bindings.containsKey(element.name)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clickable { // 修改: 使用clickable而不是toggleable
                if (isBound) {
                    KeyBindingManager.removeBinding(element.name)
                } else {
                    KeyCaptureService.requestBind(context, element)
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Keyboard, // 添加: 使用键盘图标
                contentDescription = "实体按键绑定",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )

            Text(
                text = "实体按键绑定",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
        }

        // 修改: 使用Checkbox代替Switch来显示状态
        Checkbox(
            checked = isBound,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.White,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color(0xFF2A2A2A) // 对勾颜色与背景色一致，突出显示
            )
        )
    }
}

private fun IntRange.toFloatRange() = first.toFloat()..last.toFloat()