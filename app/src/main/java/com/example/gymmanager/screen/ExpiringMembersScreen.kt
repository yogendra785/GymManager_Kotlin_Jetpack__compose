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
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import java.net.URLEncoder
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

    val today = System.currentTimeMillis()
    val diffInMillis = member.expiryDate - today
    val daysRemaining = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

    val (statusText, statusColor) = when {
        daysRemaining < 0 -> Pair("Expired ${-daysRemaining} days ago", Color.Red)
        daysRemaining == 0 -> Pair("Expires TODAY", Color.Red)
        else -> Pair("Expires in $daysRemaining days", Color(0xFFE65100))
    }

    // 👇 This gets the Android Context, which we need to launch other apps
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(text = member.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "📞 ${member.phoneNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // 👇 THE WHATSAPP BUTTON 👇
                Button(
                    onClick = {
                        // 1. Clean the phone number (remove spaces)
                        val cleanPhone = member.phoneNumber.filter { it.isDigit() }
                        // Assuming Indian numbers, add 91 if it's 10 digits
                        val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

                        // 2. Draft the message
                        val message = "Hi ${member.name}, your gym membership expired on $expiryDateString. Please renew it soon to continue your workouts!"
                        val encodedMessage = URLEncoder.encode(message, "UTF-8")

                        // 3. Create the Intent to open WhatsApp
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/$formattedPhone?text=$encodedMessage")
                        }

                        // 4. Try to open it! (Use a try-catch just in case they don't have WhatsApp installed)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)) // WhatsApp Green!
                ) {
                    Text("Remind", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
