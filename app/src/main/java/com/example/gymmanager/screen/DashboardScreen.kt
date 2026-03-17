package com.example.gymmanager.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymmanager.viewmodel.MemberViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: MemberViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.fetchMembers()
    }

    val context = LocalContext.current
    var showRevenueHistory by remember { mutableStateOf(false) }

    // 👇 NEW STATE FOR THE MEMBER BREAKDOWN SHEET
    var showMemberStats by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                val csvData = viewModel.generateCsvData()
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvData.toByteArray())
                    Toast.makeText(context, "Excel Export Saved! 🎉", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save export", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val total = viewModel.totalMembersCount
    val active = viewModel.activeMembersCount
    val activeProgress = if (total > 0) active.toFloat() / total.toFloat() else 0f
    val inactive = total - active // Calculated for the new bottom sheet

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good Morning 👋"
        in 12..16 -> "Good Afternoon 👋"
        in 17..20 -> "Good Evening 👋"
        else -> "Good Night 🌙"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // --- PREMIUM CUSTOM HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Business Overview",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- ANIMATED STAT CARDS GRID ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedStatCard(
                        title = "Total Members", targetValue = total,
                        icon = Icons.Rounded.Person, iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { showMemberStats = true } // 👈 NOW CLICKABLE
                    )
                    AnimatedStatCard(
                        title = "Active Now", targetValue = active,
                        icon = Icons.Rounded.CheckCircle, iconTint = Color(0xFF4CAF50),
                        progress = activeProgress, modifier = Modifier.weight(1f),
                        onClick = { showMemberStats = true } // 👈 NOW CLICKABLE
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedStatCard(
                        title = "Expiring Soon", targetValue = viewModel.expiringSoonCount,
                        icon = Icons.Rounded.Warning, iconTint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("expiring_members") } // 👈 NOW CLICKABLE
                    )
                    AnimatedStatCard(
                        title = "Monthly Revenue", targetValue = viewModel.currentMonthRevenue.toInt(),
                        prefix = "₹", icon = Icons.AutoMirrored.Rounded.TrendingUp, iconTint = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f), onClick = { showRevenueHistory = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CRITICAL ALERT BANNER ---
            if (viewModel.expiringSoonCount > 0) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.98f, targetValue = 1.02f,
                    animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                    label = "scale"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .scale(scale)
                        .clickable { navController.navigate("expiring_members") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Action Required", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("${viewModel.expiringSoonCount} members expiring soon. Tap to collect fees.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // --- CONTROL PANEL GRID ---
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SquareActionCard(
                        title = "Add Member", icon = Icons.Rounded.AddCircle, iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f), onClick = { navController.navigate("add_member") }
                    )
                    SquareActionCard(
                        title = "Directory", icon = Icons.AutoMirrored.Rounded.List, iconTint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f), onClick = { navController.navigate("member_list") }
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SquareActionCard(
                        title = "Export Data", icon = Icons.Rounded.Info, iconTint = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f), onClick = { exportLauncher.launch("Gym_Members_Report.csv") }
                    )
                    SquareActionCard(
                        title = "Settings", icon = Icons.Rounded.Settings, iconTint = Color(0xFF9E9E9E),
                        modifier = Modifier.weight(1f), onClick = { Toast.makeText(context, "Settings Panel Coming Soon!", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- REVENUE HISTORY BOTTOM SHEET ---
    if (showRevenueHistory) {
        ModalBottomSheet(
            onDismissRequest = { showRevenueHistory = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Revenue History", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text("Based on most recent payments", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                val history = viewModel.revenueHistory
                if (history.isEmpty()) {
                    Text("No payment history available yet.", modifier = Modifier.padding(vertical = 32.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {
                        items(history) { (monthYear, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = monthYear, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                Text(text = "₹${amount.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }

    // --- NEW: MEMBER STATS BOTTOM SHEET ---
    if (showMemberStats) {
        ModalBottomSheet(
            onDismissRequest = { showMemberStats = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("Member Breakdown", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Text("Current status of your gym's client base", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))

                // Active Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Active Members", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = "$active", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Inactive Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Gray))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Inactive / Left", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(text = "$inactive", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        showMemberStats = false
                        navController.navigate("member_list")
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Open Member Directory", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- NEW SQUARE ACTION GRID ITEM ---
@Composable
fun SquareActionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .aspectRatio(1f) // 👈 This makes it perfectly square
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- UPGRADED PREMIUM STAT CARD ---
@Composable
fun AnimatedStatCard(
    title: String,
    targetValue: Int,
    prefix: String = "",
    icon: ImageVector,
    iconTint: Color,
    progress: Float? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val animatedValue by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 1200, easing = FastOutLinearInEasing),
        label = "CounterAnimation"
    )

    Card(
        modifier = modifier
            .height(115.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // --- PROGRESS RING CONTAINER ---
                Box(contentAlignment = Alignment.Center) {
                    if (progress != null) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                            label = "ProgressAnimation"
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(44.dp),
                            color = iconTint,
                            trackColor = iconTint.copy(alpha = 0.2f),
                            strokeWidth = 3.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                text = "$prefix$animatedValue",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}