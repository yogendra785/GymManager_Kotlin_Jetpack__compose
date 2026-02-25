package com.example.gymmanager.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.gymmanager.viewmodel.MemberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberScreen(
    navController: NavController,
    memberId: String,
    viewModel: MemberViewModel
) {
    val member = viewModel.getMemberById(memberId)

    if (member == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Member not found.")
        }
        return
    }

    var name by remember { mutableStateOf(member.name) }
    var phone by remember { mutableStateOf(member.phoneNumber) }
    var fee by remember { mutableStateOf(member.feePaid.toInt().toString()) }

    // 👇 NEW: State to hold their active status
    var isActive by remember { mutableStateOf(member.isActive) }

    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Member Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fee, onValueChange = { fee = it }, label = { Text("Total Fee Paid (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- NEW: DEACTIVATE TOGGLE SWITCH ---
            Surface(
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isActive) "Active Member" else "Inactive / Archived",
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (isActive) "Turn off if they left the gym." else "They will not trigger expiry alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        // 👇 Pass the isActive state to the ViewModel!
                        viewModel.updateMemberDetails(member, name, phone, fee, isActive) {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp)
                ) {
                    Text("Update Member")
                }
            }
        }
    }
}