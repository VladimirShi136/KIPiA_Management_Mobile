package com.kipia.management.mobile.ui.components.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kipia.management.mobile.ui.theme.Dimens

// ── Data-классы ───────────────────────────────────────────────────────────────

data class StatItemData(
    val count: Int,
    val label: String,
    val color: Color
)

/**
 * Группа элементов статистики с необязательным заголовком.
 */
data class StatGroup(
    val title: String? = null,
    val items: List<StatItemData>
)

// ── Public composable ─────────────────────────────────────────────────────────

/**
 * Один элемент статистики: крупное число + подпись снизу.
 */
@Composable
fun StatItem(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

/**
 * Универсальная карточка статистики.
 *
 * - 1 группа  -> простая строка элементов (как DeviceStatistics)
 * - 2 группы  -> адаптивный layout: рядом на широком экране,
 *               стопкой с разделителем на узком
 *
 * @param groups           1 или 2 группы [StatGroup]
 * @param wideThresholdDp  минимальная ширина карточки в dp для горизонтального
 *                         layout (актуально только при 2 группах, по умолчанию 360)
 */
@Composable
fun StatCard(
    groups: List<StatGroup>,
    modifier: Modifier = Modifier,
    wideThresholdDp: Int = 360
) {
    require(groups.size in 1..2) { "StatCard: поддерживается 1 или 2 группы" }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        if (groups.size == 1) {
            StatGroupContent(
                group = groups[0],
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall)
            )
        } else {
            AdaptiveTwoGroupContent(
                left = groups[0],
                right = groups[1],
                wideThresholdDp = wideThresholdDp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall)
            )
        }
    }
}

// ── Private composable ────────────────────────────────────────────────────────

@Composable
private fun StatGroupContent(
    group: StatGroup,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (group.title != null) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = Dimens.spacingXSmall)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)
        ) {
            group.items.forEach { item ->
                StatItem(
                    count = item.count,
                    label = item.label,
                    color = item.color,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AdaptiveTwoGroupContent(
    left: StatGroup,
    right: StatGroup,
    wideThresholdDp: Int,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var widthDp by remember { mutableStateOf(Int.MAX_VALUE) }

    Column(
        modifier = modifier.onSizeChanged { size ->
            widthDp = with(density) { size.width.toDp() }.value.toInt()
        }
    ) {
        if (widthDp >= wideThresholdDp) {
            // Широкий экран — две группы рядом
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatGroupContent(
                    group = left,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(
                    modifier = Modifier
                        .padding(horizontal = Dimens.spacingSmall)
                        .height(52.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                StatGroupContent(
                    group = right,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Узкий экран — две группы стопкой
            StatGroupContent(
                group = left,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.spacingSmall),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            StatGroupContent(
                group = right,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}