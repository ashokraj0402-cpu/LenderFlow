package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountStatus
import com.example.data.model.Customer
import com.example.data.model.LoanTransaction
import com.example.data.model.PortfolioKpis
import com.example.data.model.TillDateUnpaidBorrower
import com.example.data.model.TransactionType
import com.example.domain.LoanCalculator
import com.example.ui.theme.AmberFollowUp
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.ClosedSlate
import com.example.ui.theme.ClosedSoft
import com.example.ui.theme.CrimsonBorder
import com.example.ui.theme.CrimsonOverdue
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.EmeraldCollected
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.TrustBlue
import com.example.ui.theme.TrustBlueBorder
import com.example.ui.theme.TrustBlueSoft
import com.example.ui.theme.TrustBlueWash
import com.example.ui.theme.TrustNavy

@Composable
fun SleekCircularActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = containerColor,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun QuickComposerCard(
    onQuickPay: () -> Unit,
    onNewBorrower: () -> Unit,
    onTopUpOrRepay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TrustBlueBorder),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TrustBlueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "LF",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TrustBlue,
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onQuickPay)
                        .testTag("composer_quick_entry_pill"),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = "Record interest, top-up, or repayment…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = TrustBlueBorder,
                thickness = 0.75.dp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComposerFooterAction(
                    icon = Icons.Filled.Payments,
                    label = "Pay Interest",
                    tint = EmeraldCollected,
                    onClick = onQuickPay,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("composer_pay_interest"),
                )
                VerticalDivider(
                    modifier = Modifier.height(22.dp),
                    color = TrustBlueBorder,
                )
                ComposerFooterAction(
                    icon = Icons.Filled.PersonAdd,
                    label = "New Loan",
                    tint = TrustBlue,
                    onClick = onNewBorrower,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_add_borrower_button"),
                )
                VerticalDivider(
                    modifier = Modifier.height(22.dp),
                    color = TrustBlueBorder,
                )
                ComposerFooterAction(
                    icon = Icons.Filled.AddCircleOutline,
                    label = "Top-Up",
                    tint = AmberFollowUp,
                    onClick = onTopUpOrRepay,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("composer_top_up"),
                )
            }
        }
    }
}

@Composable
private fun ComposerFooterAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TrustNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    accentBackground: Color,
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false,
    valueColor: Color = TrustNavy,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (highlightBorder) 1.5.dp else 1.dp,
            color = if (highlightBorder) accentColor.copy(alpha = 0.55f) else TrustBlueBorder,
        ),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accentBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMono,
                ),
                color = if (highlightBorder) accentColor else valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MonthTillDateCard(
    todayIso: String,
    kpis: PortfolioKpis,
    unpaidBorrowers: List<TillDateUnpaidBorrower>,
    onPayBorrower: (Customer) -> Unit,
    onViewBorrower: (Customer) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasUnpaid = kpis.monthTillDateUnpaid > 0.0
    val progress = if (kpis.monthTillDateExpected > 0.0) {
        (kpis.monthTillDateReceived / kpis.monthTillDateExpected).toFloat().coerceIn(0f, 1f)
    } else {
        1f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_month_till_date_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (hasUnpaid) 1.25.dp else 1.dp,
            color = if (hasUnpaid) CrimsonBorder else TrustBlueBorder,
        ),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Month Till Date Due",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TrustNavy,
                    )
                    Text(
                        text = "${LoanCalculator.formatTillDateRange(todayIso)} · Due day ≤ ${todayIso.takeLast(2).toIntOrNull() ?: ""}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = if (hasUnpaid) CrimsonSoft else EmeraldSoft,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (hasUnpaid) {
                            "${LoanCalculator.formatCurrency(kpis.monthTillDateUnpaid)} Unpaid"
                        } else {
                            "All Paid"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (hasUnpaid) CrimsonOverdue else EmeraldCollected,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            // 3-Column Breakdown: To Receive Till Date | Recorded / Paid | Not Yet Paid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TillDateMetricBox(
                    label = "To Receive",
                    value = LoanCalculator.formatCurrency(kpis.monthTillDateExpected),
                    caption = "${kpis.monthTillDateDueBorrowersCount} due",
                    valueColor = TrustNavy,
                    containerColor = TrustBlueWash,
                    borderColor = TrustBlueBorder,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("till_date_expected"),
                )
                TillDateMetricBox(
                    label = "Recorded",
                    value = LoanCalculator.formatCurrency(kpis.monthTillDateReceived),
                    caption = "${kpis.monthTillDatePaidBorrowersCount} paid",
                    valueColor = EmeraldCollected,
                    containerColor = EmeraldSoft.copy(alpha = 0.6f),
                    borderColor = EmeraldSoft,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("till_date_received"),
                )
                TillDateMetricBox(
                    label = "Not Paid",
                    value = LoanCalculator.formatCurrency(kpis.monthTillDateUnpaid),
                    caption = "${kpis.monthTillDateUnpaidBorrowersCount} unpaid",
                    valueColor = if (hasUnpaid) CrimsonOverdue else TrustNavy,
                    containerColor = if (hasUnpaid) CrimsonSoft else TrustBlueWash,
                    borderColor = if (hasUnpaid) CrimsonBorder else TrustBlueBorder,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("till_date_unpaid"),
                )
            }

            if (kpis.monthTillDateExpected > 0.0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = TrustBlue,
                        trackColor = TrustBlueSoft,
                        strokeCap = StrokeCap.Round,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}% collected of till-date due",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (kpis.monthTotalCollectedThisMonth > kpis.monthTillDateReceived) {
                            Text(
                                text = "Month total: ${LoanCalculator.formatCurrency(kpis.monthTotalCollectedThisMonth)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TrustBlue,
                            )
                        }
                    }
                }
            }

            if (unpaidBorrowers.isNotEmpty()) {
                HorizontalDivider(color = TrustBlueBorder, thickness = 0.75.dp)
                Text(
                    text = "Due Till Date — Not Yet Recorded (${unpaidBorrowers.size})",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CrimsonOverdue,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unpaidBorrowers.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CrimsonSoft.copy(alpha = 0.65f),
                            border = BorderStroke(0.75.dp, CrimsonBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onViewBorrower(item.customer) }
                                .testTag("till_date_unpaid_${item.customer.customerCode}"),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.customer.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = TrustNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Due Day ${item.customer.dueDayOfMonth} · Unpaid: ${LoanCalculator.formatCurrency(item.unpaidAmount)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = CrimsonOverdue,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { onPayBorrower(item.customer) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = TrustBlue,
                                    ) {
                                        Text(
                                            text = "Record Pay",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TillDateMetricBox(
    label: String,
    value: String,
    caption: String,
    valueColor: Color,
    containerColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(0.75.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                ),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    isOverdue: Boolean = false,
) {
    val (bgColor, textColor) = when {
        status == AccountStatus.CLOSED.label -> ClosedSoft to ClosedSlate
        status == AccountStatus.FOLLOW_UP_NEEDED.label -> AmberSoft to AmberFollowUp
        else -> EmeraldSoft to EmeraldCollected
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
        if (isOverdue && status != AccountStatus.CLOSED.label) {
            Surface(
                color = CrimsonSoft,
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Overdue interest",
                        tint = CrimsonOverdue,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = "Overdue",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CrimsonOverdue,
                    )
                }
            }
        }
    }
}

@Composable
fun BorrowerListCard(
    customer: Customer,
    isOverdue: Boolean,
    overdueAmount: Double,
    onClick: () -> Unit,
    onQuickPay: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showOverdueAlert = isOverdue && customer.status != AccountStatus.CLOSED.label
    val avatarBg = when {
        showOverdueAlert -> CrimsonSoft
        customer.status == AccountStatus.CLOSED.label -> ClosedSoft
        else -> TrustBlueSoft
    }
    val avatarTint = when {
        showOverdueAlert -> CrimsonOverdue
        customer.status == AccountStatus.CLOSED.label -> ClosedSlate
        else -> TrustBlue
    }
    val initialChar = customer.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "B"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("borrower_card_${customer.customerCode}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (showOverdueAlert) 1.5.dp else 1.dp,
            color = if (showOverdueAlert) CrimsonBorder else TrustBlueBorder,
        ),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post-style Author Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initialChar,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = avatarTint,
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TrustNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${customer.customerCode} · Due Day ${customer.dueDayOfMonth}" +
                            if (customer.lastPaymentDate.isNotBlank()) " · Paid ${LoanCalculator.formatDateDisplay(customer.lastPaymentDate)}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(status = customer.status, isOverdue = showOverdueAlert)
            }

            // Sleek Two-Column Metrics Strip
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = TrustBlueWash,
                border = BorderStroke(0.75.dp, TrustBlueBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Principal",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = LoanCalculator.formatCurrency(customer.currentActivePrincipal),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = TrustNavy,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "Monthly (${LoanCalculator.formatRate(customer.monthlyInterestRate)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = LoanCalculator.formatCurrency(customer.monthlyInterestDue),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (showOverdueAlert) CrimsonOverdue else EmeraldCollected,
                        )
                    }
                }
            }

            if (showOverdueAlert && overdueAmount > 0.0) {
                Surface(
                    color = CrimsonSoft,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = CrimsonOverdue,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Outstanding Interest Due",
                                style = MaterialTheme.typography.labelLarge,
                                color = CrimsonOverdue,
                            )
                        }
                        Text(
                            text = LoanCalculator.formatCurrency(overdueAmount),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = CrimsonOverdue,
                        )
                    }
                }
            } else if (customer.followUpNotes.isNotBlank()) {
                Text(
                    text = customer.followUpNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                color = TrustBlueBorder,
                thickness = 0.75.dp,
            )

            // Facebook-style Sleek 3-Action Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (customer.status != AccountStatus.CLOSED.label) {
                    CardActionButton(
                        icon = Icons.Filled.Payments,
                        label = "Pay",
                        tint = TrustBlue,
                        onClick = onQuickPay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_pay_${customer.customerCode}"),
                    )
                    VerticalDivider(
                        modifier = Modifier.height(20.dp),
                        color = TrustBlueBorder,
                    )
                }
                CardActionButton(
                    icon = Icons.Filled.Info,
                    label = "Details",
                    tint = TrustNavy,
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(
                    modifier = Modifier.height(20.dp),
                    color = TrustBlueBorder,
                )
                CardActionButton(
                    icon = Icons.Filled.Call,
                    label = "Call",
                    tint = TrustNavy,
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_borrower_${customer.customerCode}"),
                )
            }
        }
    }
}

@Composable
private fun CardActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
        )
    }
}

@Composable
fun TransactionRowCard(
    transaction: LoanTransaction,
    modifier: Modifier = Modifier,
) {
    val type = TransactionType.fromLabel(transaction.transactionType)
    val (accentColor, accentBg, icon) = when (type) {
        TransactionType.INITIAL_DISBURSEMENT ->
            Triple(TrustBlue, TrustBlueSoft, Icons.AutoMirrored.Filled.CallMade)
        TransactionType.MONTHLY_INTEREST_PAYMENT ->
            Triple(EmeraldCollected, EmeraldSoft, Icons.AutoMirrored.Filled.CallReceived)
        TransactionType.TOP_UP ->
            Triple(AmberFollowUp, AmberSoft, Icons.Filled.AddCircleOutline)
        TransactionType.PRINCIPAL_REPAYMENT ->
            Triple(TrustBlue, TrustBlueSoft, Icons.Filled.Payments)
        TransactionType.FULL_CLOSURE ->
            Triple(EmeraldCollected, EmeraldSoft, Icons.Filled.CheckCircle)
    }

    val amountPrefix = when (type) {
        TransactionType.INITIAL_DISBURSEMENT, TransactionType.TOP_UP -> "-"
        TransactionType.MONTHLY_INTEREST_PAYMENT,
        TransactionType.PRINCIPAL_REPAYMENT,
        TransactionType.FULL_CLOSURE -> "+"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("txn_row_${transaction.transactionCode}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, TrustBlueBorder),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.transactionType,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.customerName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TrustNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${transaction.transactionType} · ${transaction.paymentMode}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = accentColor,
                )
                Text(
                    text = "${LoanCalculator.formatDateDisplay(transaction.transactionDate)} · ${transaction.transactionCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (transaction.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$amountPrefix${LoanCalculator.formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (amountPrefix == "+") EmeraldCollected else TrustNavy,
            )
        }
    }
}
