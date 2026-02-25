package com.example.gymmanager.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun MemberListScreen(
    navController: NavController,
    viewModel: MemberViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.fetchMembers()
    }

    val members by viewModel.memberList
    val isLoading by viewModel.isLoading

    // --- NEW: State for our filter (0 means "All") ---
    var selectedPlanFilter by remember { mutableStateOf(0) }
    val filterOptions = listOf(0, 1, 3, 6, 12)

    // Filter the list locally based on the selected chip
    val filteredMembers = if (selectedPlanFilter == 0) {
        members
    } else {
        members.filter { it.planMonths == selectedPlanFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Members") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // --- NEW: FILTER CHIPS ROW ---
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { plan ->
                    val label = if (plan == 0) "All" else "${plan}M Plan"
                    FilterChip(
                        selected = selectedPlanFilter == plan,
                        onClick = { selectedPlanFilter = plan },
                        label = { Text(label) }
                    )
                }
            }

            if (isLoading && members.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredMembers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No members found in this category.",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMembers) { member ->
                        MemberItemCard(member = member)
                    }
                }
            }
        }
    }
}

// Upgraded Card Design
@Composable
fun MemberItemCard(member: Member) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val joinDateString = dateFormat.format(Date(member.joinDate))
    val expiryDateString = dateFormat.format(Date(member.expiryDate))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = member.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                // --- NEW: PLAN BADGE ---
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${member.planMonths} Month",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📞 ${member.phoneNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // --- NEW: DISPLAYING JOIN DATE ---
            Text(text = "Joined: $joinDateString", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            val statusColor = if (member.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Text(
                text = "Expires: $expiryDateString",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}