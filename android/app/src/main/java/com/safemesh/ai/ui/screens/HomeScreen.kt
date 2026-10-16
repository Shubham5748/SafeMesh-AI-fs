package com.safemesh.ai.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safemesh.ai.ui.components.GlassPanel
import com.safemesh.ai.ui.theme.AccentBlue
import com.safemesh.ai.ui.theme.AccentGreen
import com.safemesh.ai.ui.theme.AccentRed
import com.safemesh.ai.ui.theme.BgDark
import com.safemesh.ai.ui.theme.TextMuted
import com.safemesh.ai.ui.theme.TextPrimary

@Composable
fun HomeScreen(
    userName: String,
    onSosClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse1"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, $userName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Safe & Protected",
                    fontSize = 14.sp,
                    color = AccentGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(AccentBlue.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.toString() ?: "U",
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                icon = Icons.Default.LocationOn,
                label = "GPS",
                value = "Active",
                statusColor = AccentGreen,
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                icon = Icons.Default.Wifi,
                label = "Network",
                value = "Connected",
                statusColor = AccentGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                icon = Icons.Default.Bluetooth,
                label = "BLE Mesh",
                value = "Standby",
                statusColor = AccentBlue,
                modifier = Modifier.weight(1f)
            )
            StatusCard(
                icon = Icons.Default.Security,
                label = "AI Guard",
                value = "Monitoring",
                statusColor = AccentGreen,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Big SOS Button
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulse Ring
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulse1)
                    .background(AccentRed.copy(alpha = 0.15f), CircleShape)
            )

            // Outer red glow ring
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(AccentRed.copy(alpha = 0.35f), CircleShape)
            )

            // Inner SOS solid button
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(AccentRed, CircleShape)
                    .clickable { onSosClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS Alert",
                        tint = TextPrimary,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "SOS",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Quick Info Banner
        GlassPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SafeMesh Guard Active", color = TextPrimary, fontSize = 14.sp)
                }
                Text("Ready", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatusCard(
    icon: ImageVector,
    label: String,
    value: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier) {
        Column {
            Icon(imageVector = icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, color = TextMuted, fontSize = 12.sp)
            Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
