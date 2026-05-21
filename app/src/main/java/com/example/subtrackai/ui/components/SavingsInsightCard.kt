package com.example.subtrackai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.model.Subscription

@Composable
fun SavingsInsightCard(
    potentialSavings: Double,
    upcomingTrials: List<Subscription>,
    showInsights: Boolean
) {
    if (!showInsights || (potentialSavings <= 0 && upcomingTrials.isEmpty())) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Smart Insights",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (potentialSavings > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You could save up to $${String.format("%.2f", potentialSavings)} / year by switching some monthly plans to annual billing.",
                    fontSize = 14.sp
                )
            }

            if (upcomingTrials.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF44336)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Trial Warnings",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF44336)
                    )
                }
                upcomingTrials.forEach { trial ->
                    Text(
                        "• ${trial.name} trial ends on ${trial.renewalDate}",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}
