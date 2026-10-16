package com.safemesh.ai.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safemesh.ai.data.sensor.FallPhase
import com.safemesh.ai.data.sensor.VoiceState
import com.safemesh.ai.ui.components.GlassPanel
import com.safemesh.ai.ui.theme.AccentBlue
import com.safemesh.ai.ui.theme.AccentGreen
import com.safemesh.ai.ui.theme.AccentRed
import com.safemesh.ai.ui.theme.AccentYellow
import com.safemesh.ai.ui.theme.BgDark
import com.safemesh.ai.ui.theme.TextMuted
import com.safemesh.ai.ui.theme.TextPrimary
import com.safemesh.ai.ui.viewmodel.EmergencyViewModel

@Composable
fun SafetyScreen(
    viewModel: EmergencyViewModel
) {
    val isMonitoring by viewModel.isSensorMonitoring.collectAsState()
    val liveMagnitude by viewModel.liveMagnitude.collectAsState()
    val liveGyroscopeMagnitude by viewModel.liveGyroscopeMagnitude.collectAsState()
    val hasAccelerometer by viewModel.hasAccelerometer.collectAsState()
    val hasGyroscope by viewModel.hasGyroscope.collectAsState()
    val fallPhase by viewModel.fallPhase.collectAsState()
    val isSimulationOnly by viewModel.isSimulationOnly.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val isVoiceSupported by viewModel.isVoiceSupported.collectAsState()
    val lastDetectedPhrase by viewModel.lastDetectedPhrase.collectAsState()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening(true)
        }
    }

    // Automatically manage sensor and voice lifecycle with Composable lifecycle
    DisposableEffect(Unit) {
        viewModel.toggleSensorMonitoring(true)
        onDispose {
            viewModel.toggleSensorMonitoring(false)
            viewModel.toggleVoiceListening(false)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Motion Guard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Kinematic sensor anomaly detection",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        if (isMonitoring) AccentGreen.copy(alpha = 0.2f) else AccentYellow.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isMonitoring) "SENSORS LIVE" else "PAUSED",
                    color = if (isMonitoring) AccentGreen else AccentYellow,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Safety Setting Banner: Simulation-Only Mode Switch
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (isSimulationOnly) Color(0x33EAB308) else Color(0x33EF4444),
            borderColor = if (isSimulationOnly) AccentYellow.copy(alpha = 0.5f) else AccentRed.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isSimulationOnly) AccentYellow else AccentRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSimulationOnly) "Simulation-Only Mode (Active)" else "LIVE Emergency Mode (Warning)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isSimulationOnly)
                            "Countdown will test UI without making network calls or sending SMS/calls."
                        else
                            "Countdown completion will trigger real backend emergency dispatch.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isSimulationOnly,
                    onCheckedChange = { viewModel.setSimulationOnlyMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentYellow,
                        checkedTrackColor = AccentYellow.copy(alpha = 0.3f),
                        uncheckedThumbColor = AccentRed,
                        uncheckedTrackColor = AccentRed.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fall State Status Card
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Motion Heuristic Status", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Box(
                        modifier = Modifier
                            .background(
                                when (fallPhase) {
                                    FallPhase.IDLE -> AccentGreen
                                    FallPhase.IMPACT_DETECTED -> AccentYellow
                                    FallPhase.STILLNESS_CHECK -> AccentRed
                                    FallPhase.CONFIRMED -> AccentRed
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (fallPhase) {
                                FallPhase.IDLE -> "Monitoring"
                                FallPhase.IMPACT_DETECTED -> "Impact Detected"
                                FallPhase.STILLNESS_CHECK -> "Verifying Stillness"
                                FallPhase.CONFIRMED -> "Fall Triggered"
                            },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = String.format("%.1f m/s²", liveMagnitude),
                            color = if (liveMagnitude > 20f) AccentRed else AccentGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Live Accelerometer Magnitude",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accelerometer Hardware Card
        SafetyModuleCard(
            icon = Icons.Default.Speed,
            title = "Accelerometer Sensor",
            status = if (hasAccelerometer) "Hardware Connected (Active)" else "Hardware Unavailable",
            score = String.format("%.1f m/s²", liveMagnitude),
            color = if (hasAccelerometer) AccentBlue else AccentRed
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Gyroscope Hardware Card
        SafetyModuleCard(
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            title = "Gyroscope Sensor",
            status = if (hasGyroscope) "Hardware Connected (Rotation)" else "Hardware Unavailable",
            score = if (hasGyroscope) String.format("%.2f rad/s", liveGyroscopeMagnitude) else "N/A",
            color = if (hasGyroscope) AccentGreen else AccentYellow
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sensor Active Monitoring Toggle Card
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Motion Detection State", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Enable to process live motion heuristics", color = TextMuted, fontSize = 12.sp)
                }

                Switch(
                    checked = isMonitoring,
                    onCheckedChange = { viewModel.toggleSensorMonitoring(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentGreen,
                        checkedTrackColor = AccentGreen.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Voice Distress Recognition Section
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (voiceState) {
                                        VoiceState.LISTENING -> AccentGreen.copy(alpha = 0.2f)
                                        VoiceState.ERROR_RETRYING -> AccentYellow.copy(alpha = 0.2f)
                                        VoiceState.PERMISSION_REQUIRED -> AccentYellow.copy(alpha = 0.2f)
                                        VoiceState.UNAVAILABLE -> AccentRed.copy(alpha = 0.2f)
                                        VoiceState.OFF -> AccentBlue.copy(alpha = 0.15f)
                                    },
                                    RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = when (voiceState) {
                                    VoiceState.LISTENING -> AccentGreen
                                    VoiceState.ERROR_RETRYING -> AccentYellow
                                    VoiceState.PERMISSION_REQUIRED -> AccentYellow
                                    VoiceState.UNAVAILABLE -> AccentRed
                                    VoiceState.OFF -> TextMuted
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Voice Distress Guard",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Keyword emergency detection",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = (voiceState == VoiceState.LISTENING || voiceState == VoiceState.ERROR_RETRYING),
                        onCheckedChange = { enable ->
                            if (enable) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                viewModel.toggleVoiceListening(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentGreen,
                            checkedTrackColor = AccentGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recognition Status",
                        color = TextMuted,
                        fontSize = 12.sp
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                when (voiceState) {
                                    VoiceState.LISTENING -> AccentGreen.copy(alpha = 0.2f)
                                    VoiceState.ERROR_RETRYING -> AccentYellow.copy(alpha = 0.2f)
                                    VoiceState.PERMISSION_REQUIRED -> AccentYellow.copy(alpha = 0.2f)
                                    VoiceState.UNAVAILABLE -> AccentRed.copy(alpha = 0.2f)
                                    VoiceState.OFF -> Color(0x334B5563)
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (voiceState) {
                                VoiceState.LISTENING -> "LISTENING"
                                VoiceState.ERROR_RETRYING -> "RETRYING"
                                VoiceState.PERMISSION_REQUIRED -> "PERMISSION REQUIRED"
                                VoiceState.UNAVAILABLE -> "UNAVAILABLE"
                                VoiceState.OFF -> "OFF"
                            },
                            color = when (voiceState) {
                                VoiceState.LISTENING -> AccentGreen
                                VoiceState.ERROR_RETRYING -> AccentYellow
                                VoiceState.PERMISSION_REQUIRED -> AccentYellow
                                VoiceState.UNAVAILABLE -> AccentRed
                                VoiceState.OFF -> TextMuted
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (lastDetectedPhrase.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Last Recognized: ",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "\"$lastDetectedPhrase\"",
                                color = AccentBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (voiceState == VoiceState.PERMISSION_REQUIRED) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Grant Microphone Permission",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Explanatory Note & Guidance
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "• Keywords: \"help\", \"help me\", \"bachao\", \"madad\", \"emergency\" (English & Hindi/Hinglish).",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "• Requires device speech recognition support (Google Speech Services / on-device model).",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "• Active only while Safety screen is open. Does not run in the background.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    if (isSimulationOnly) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "• Simulation-Only mode is ACTIVE: Voice triggers will run the 10s countdown without dispatching SOS.",
                            color = AccentYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Manual Test Fall Event Button (simulates sudden impact + stillness)
        OutlinedButton(
            onClick = {
                viewModel.startSosCountdown(reason = "Simulated Fall Event (Manual Test)")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Fall Trigger (Test Countdown)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual Test Voice Distress Button
        OutlinedButton(
            onClick = {
                viewModel.startSosCountdown(reason = "Simulated Voice Distress: 'help me' (Manual Test)")
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentGreen)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simulate Voice Trigger (Test Countdown)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SafetyModuleCard(
    icon: ImageVector,
    title: String,
    status: String,
    score: String,
    color: Color
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = status, color = TextMuted, fontSize = 12.sp)
            }

            Text(text = score, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
