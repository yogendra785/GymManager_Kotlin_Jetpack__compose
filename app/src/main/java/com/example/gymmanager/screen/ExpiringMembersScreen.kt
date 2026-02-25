package com.example.gymmanager.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymmanager.model.Member
import com.example.gymmanager.viewmodel.MemberViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiringMembersScreen(
    navController: NavController,
    viewModel: MemberViewModel = viewModel()
) {

    LaunchedEffect(Unit) {
        viewModel.fetchMembers()
    }
    val liveMembers by viewModel.memberList
    // Read our new filtered list from the ViewModel
    val expiringMembers = viewModel.expiringMembersList

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expiring & Overdue") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer, // Makes the top bar red-ish!
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (expiringMembers.isEmpty()) {
                Text(
                    text = "All good! No one is expiring soon. 🎉",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(expiringMembers) { member ->
                        ExpiringMemberCard(member = member)
                    }
                }
            }
        }
    }
}

@Composable
fun ExpiringMemberCard(member: Member) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val expiryDateString = dateFormat.format(Date(member.expiryDate))

    // Calculate days remaining
    val today = System.currentTimeMillis()
    val diffInMillis = member.expiryDate - today
    val daysRemaining = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

    // Determine color and status text
    val (statusText, statusColor) = when {
        daysRemaining < 0 -> Pair("Expired ${-daysRemaining} days ago", Color.Red)
        daysRemaining == 0 -> Pair("Expires TODAY", Color.Red)
        else -> Pair("Expires in $daysRemaining days", Color(0xFFE65100)) // Orange
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = member.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📞 ${member.phoneNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                fontSize = 16.sp
            )
            Text(text = "Date: $expiryDateString", fontSize = 12.sp, color = Color.Gray)
        }
    }
}