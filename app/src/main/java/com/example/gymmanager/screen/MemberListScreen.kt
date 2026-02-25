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
    LaunchedEffect(Unit) {
        viewModel.fetchMembers()
    }

    val members by viewModel.memberList
    val isLoading by viewModel.isLoading

    var selectedPlanFilter by remember { mutableStateOf(0) }
    val filterOptions = listOf(0, 1, 3, 6, 12)

    var memberToRenew by remember { mutableStateOf<Member?>(null) }

    // 👇 NEW: State to hold what the user is typing in the search bar
    var searchQuery by remember { mutableStateOf("") }

    // 👇 UPGRADED: Filter by BOTH the Plan Chip AND the Search Query!
    val filteredMembers = members.filter { member ->
        // 1. Check if they match the selected plan
        val matchesPlan = if (selectedPlanFilter == 0) true else member.planMonths == selectedPlanFilter

        // 2. Check if their name OR phone number contains the search text (ignoring uppercase/lowercase)
        val matchesSearch = member.name.contains(searchQuery, ignoreCase = true) ||
                member.phoneNumber.contains(searchQuery)

        // 3. Keep them in the list only if they match BOTH filters
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // --- NEW: SMART SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                placeholder = { Text("Search by name or phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    // Add a little 'X' button to instantly clear the search text
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // --- FILTER CHIPS ROW ---
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                            onRenewClick = { memberToRenew = member }
                        )
                    }
                }
            }
        }
    }

    if (memberToRenew != null) {
        RenewDialog(
            member = memberToRenew!!,
            viewModel = viewModel,
            onDismiss = { memberToRenew = null }
        )
    }
}

// ... Keep your MemberItemCard and RenewDialog functions exactly the same down here!
// (I left them out to keep the code block short, just paste this top part over your existing file)

@Composable
fun MemberItemCard(
    member: Member,
    navController: NavController,
    onRenewClick: () -> Unit // 👈 Added trigger for the Renew button
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val joinDateString = dateFormat.format(Date(member.joinDate))
    val expiryDateString = dateFormat.format(Date(member.expiryDate))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("edit_member/${member.id}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(text = member.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text("${member.planMonths} Month", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📞 ${member.phoneNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Joined: $joinDateString", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if (member.expiryDate < System.currentTimeMillis()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(text = "Expires: $expiryDateString", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = statusColor)

                // --- NEW: RENEW BUTTON ---
                Button(onClick = onRenewClick, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Renew")
                }
            }
        }
    }
}

// --- NEW COMPOSABLE: The Pop-Up Dialog ---
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
    var selectedPlan by remember { mutableStateOf(planOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renew ${member.name}") },
        text = {
            Column {
                // Dropdown for months
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = "$selectedPlan Month(s)",
                        onValueChange = {}, readOnly = true, label = { Text("New Plan") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
                // Fee Input
                OutlinedTextField(
                    value = fee, onValueChange = { fee = it }, label = { Text("Fee Received (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.renewMember(member, fee, selectedPlan) {
                    onDismiss() // Close the dialog on success
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