package com.safemesh.ai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safemesh.ai.ui.components.GlassPanel
import com.safemesh.ai.ui.theme.AccentBlue
import com.safemesh.ai.ui.theme.AccentRed
import com.safemesh.ai.ui.theme.BgDark
import com.safemesh.ai.ui.theme.CardDark
import com.safemesh.ai.ui.theme.TextMuted
import com.safemesh.ai.ui.theme.TextPrimary

@Composable
fun ProfileScreen(
    userName: String,
    backendUrl: String,
    onNavigateToContacts: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onSaveBackendUrl: (String) -> Unit,
    onSaveUserName: (String) -> Unit
) {
    var editUrl by remember(backendUrl) { mutableStateOf(backendUrl) }
    var showUrlConfig by remember { mutableStateOf(false) }

    var showEditDialog by remember { mutableStateOf(false) }
    var tempName by remember(userName) { mutableStateOf(userName) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar and Profile Header
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(AccentBlue.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(46.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = userName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = "SafeMesh Shield Active", color = TextMuted, fontSize = 13.sp)

        Spacer(modifier = Modifier.height(10.dp))

        // Edit Profile Button
        OutlinedButton(
            onClick = {
                tempName = userName
                errorMessage = null
                showEditDialog = true
            },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
            modifier = Modifier.height(36.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Edit Profile",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Medical Info Card
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Medical Emergency Info", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Blood Group", color = TextMuted, fontSize = 12.sp)
                        Text("O+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("Allergies", color = TextMuted, fontSize = 12.sp)
                        Text("Penicillin", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("Status", color = TextMuted, fontSize = 12.sp)
                        Text("Protected", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Items
        ProfileNavigationItem(
            icon = Icons.Default.Edit,
            title = "Edit Profile Name",
            onClick = {
                tempName = userName
                errorMessage = null
                showEditDialog = true
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        ProfileNavigationItem(
            icon = Icons.Default.Shield,
            title = "Emergency Contacts",
            onClick = onNavigateToContacts
        )

        Spacer(modifier = Modifier.height(10.dp))

        ProfileNavigationItem(
            icon = Icons.Default.Security,
            title = "Permissions & Privacy",
            onClick = onNavigateToPermissions
        )

        Spacer(modifier = Modifier.height(10.dp))

        ProfileNavigationItem(
            icon = Icons.Default.Link,
            title = "Backend Server URL Config",
            onClick = { showUrlConfig = !showUrlConfig }
        )

        if (showUrlConfig) {
            Spacer(modifier = Modifier.height(10.dp))
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Configurable Backend URL",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Emulator: http://10.0.2.2:3001/\nPhysical Phone: http://192.168.x.x:3001/",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            onSaveBackendUrl(editUrl.trim())
                            showUrlConfig = false
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Server URL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    // Edit Profile Name Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                errorMessage = null
            },
            containerColor = CardDark,
            titleContentColor = TextPrimary,
            textContentColor = TextMuted,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Edit Profile Name",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Update your profile name below. This name is used on the dashboard, alerts, and emergency dispatches.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = {
                            tempName = it
                            if (errorMessage != null) {
                                errorMessage = null
                            }
                        },
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g. Shubham Chaubey") },
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = AccentRed,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "Cannot be empty or whitespace only",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentBlue,
                            unfocusedBorderColor = Color(0x44FFFFFF),
                            errorBorderColor = AccentRed,
                            focusedLabelColor = AccentBlue,
                            unfocusedLabelColor = TextMuted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempName.trim()
                        if (trimmed.isEmpty()) {
                            errorMessage = "Name cannot be empty or whitespace only"
                        } else {
                            onSaveUserName(trimmed)
                            showEditDialog = false
                            errorMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showEditDialog = false
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileNavigationItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}
