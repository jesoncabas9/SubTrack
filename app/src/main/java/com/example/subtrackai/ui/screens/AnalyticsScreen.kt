package com.example.subtrackai.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subtrackai.ui.theme.DeepBlue
import com.example.subtrackai.ui.theme.DeepPurple
import com.example.subtrackai.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
    isDarkMode: Boolean
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val totalSpend by viewModel.totalMonthlySpend.collectAsState()

    Scaffold(
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Monthly Distribution", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Simple Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxPrice = subscriptions.maxOfOrNull { it.price } ?: 1.0
                    subscriptions.take(5).forEach { sub ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val ratio = if (maxPrice > 0) (sub.price / maxPrice).toFloat() else 0.1f
                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .fillMaxHeight(ratio.coerceIn(0.1f, 1f))
                                    .background(DeepPurple, RoundedCornerShape(8.dp, 8.dp, 0.dp, 0.dp))
                            )
                            Text(sub.name.take(3), fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Insights", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Your monthly spend is $${"%.2f".format(totalSpend)}")
                    Text("• Yearly, this accounts for $${"%.2f".format(totalSpend * 12)}")
                    if (subscriptions.any { it.billingCycle == "Monthly" }) {
                        Text("• Tip: Some services offer discounts for yearly billing.")
                    }
                }
            }
        }
    }
}
