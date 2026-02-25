package com.example.gymmanager.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymmanager.model.Member
import com.example.gymmanager.viewmodel.MemberViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen(
    navController: NavController,
    viewModel: MemberViewModel
) {
    // 1. Fetch data when screen opens
    LaunchedEffect(Unit) {
        viewModel.fetchMembers()
    }

    // 2. Observe data
    val members by viewModel.memberList
    val isLoading by viewModel.isLoading

    // 3. Local State for Filtering & Actions
    var selectedPlanFilter by remember { mutableIntStateOf(0) }
    val filterOptions = listOf(0, 1, 3, 6, 12)

    var memberToRenew by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) } // 👈 State for deletion dialog
    var searchQuery by remember { mutableStateOf("") }

    // 4. Filter Logic (Search + Plan Chip)
    val filteredMembers = members.filter { member ->
        val matchesPlan = if (selectedPlanFilter == 0) true else member.planMonths == selectedPlanFilter
        val matchesSearch = member.name.contains(searchQuery, ignoreCase = true) ||
                member.phoneNumber.contains(searchQuery)
        matchesPlan && matchesSearch
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search by name or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // --- FILTER CHIPS ---
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

            // --- MAIN LIST CONTENT ---
            if (isLoading && members.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredMembers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No members found.", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMembers) { member ->
                        MemberItemCard(
                            member = member,
                            navController = navController,
                            onRenewClick = { memberToRenew = member },
                            onDeleteClick = { memberToDelete = member } // 👈 Pass the delete action
                        )
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Renew Dialog
    if (memberToRenew != null) {
        RenewDialog(
            member = memberToRenew!!,
            viewModel = viewModel,
            onDismiss = { memberToRenew = null }
        )
    }

    // 2. Delete Confirmation Dialog
    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Delete Member?") },
            text = { Text("Are you sure you want to permanently delete ${memberToDelete!!.name}? This cannot be undone and their revenue history will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Call the ViewModel to delete from Firebase
                        viewModel.deleteMember(memberToDelete!!.id)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MemberItemCard(
    member: Member,
    navController: NavController,
    onRenewClick: () -> Unit,
    onDeleteClick: () -> Unit // 👈 New Parameter
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val joinDateString = dateFormat.format(Date(member.joinDate))
    val expiryDateString = dateFormat.format(Date(member.expiryDate))
    val isExpired = member.expiryDate < System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("edit_member/${member.id}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name and Plan Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = member.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${member.planMonths} Month",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📞 ${member.phoneNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Dates and Status
            Text(text = "Joined: $joinDateString", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusColor = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(
                    text = "Expires: $expiryDateString",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )

                // Action Buttons Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Delete Button (Icon Only)
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Member",
                            tint = MaterialTheme.colorScheme.error // Red color for danger
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Renew Button
                    Button(
                        onClick = onRenewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Renew")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewDialog(
    member: Member,
    viewModel: MemberViewModel,
    onDismiss: () -> Unit
) {
    var fee by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val planOptions = listOf(1, 3, 6, 12)
    var selectedPlan by remember { mutableIntStateOf(planOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renew ${member.name}") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = "$selectedPlan Month(s)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("New Plan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        planOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text("$selectionOption Month(s)") },
                                onClick = {
                                    selectedPlan = selectionOption
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = fee,
                    onValueChange = { fee = it },
                    label = { Text("Fee Received (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.renewMember(member, fee, selectedPlan) {
                    onDismiss()
                }
            }) {
                Text("Confirm Renewal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}