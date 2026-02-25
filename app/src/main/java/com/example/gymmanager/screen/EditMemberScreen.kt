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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymmanager.viewmodel.MemberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMemberScreen(
    navController: NavController,
    memberId: String, // 👈 We receive the ID from navigation
    viewModel: MemberViewModel = viewModel()
) {
    // Find the member using the ID
    val member = viewModel.getMemberById(memberId)

    // If something goes wrong and we can't find them, show an error
    if (member == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Member not found.")
        }
        return
    }

    // Pre-fill the text fields with their current data!
    var name by remember { mutableStateOf(member.name) }
    var phone by remember { mutableStateOf(member.phoneNumber) }
    var fee by remember { mutableStateOf(member.feePaid.toInt().toString()) }

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
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fee,
                onValueChange = { fee = it },
                label = { Text("Fee Paid (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        // Tell ViewModel to save the changes!
                        viewModel.updateMemberDetails(member, name, phone, fee) {
                            navController.popBackStack() // Go back to the list when done
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