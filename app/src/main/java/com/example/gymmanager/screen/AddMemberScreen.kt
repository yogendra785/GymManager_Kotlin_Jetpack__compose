package com.example.gymmanager.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.gymmanager.utils.ReceiptGenerator
import com.example.gymmanager.viewmodel.MemberViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    navController: NavController,
    viewModel: MemberViewModel = viewModel()
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var totalFee by remember { mutableStateOf("") }
    var paidAmount by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    val planOptions = listOf(1, 3, 6, 12)
    var selectedPlan by remember { mutableStateOf(planOptions[0]) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val selectedDateString = dateFormat.format(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))

    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    val scrollState = rememberScrollState()

    val total = totalFee.toDoubleOrNull() ?: 0.0
    val paid = paidAmount.toDoubleOrNull() ?: 0.0
    val pending = if (total > 0) (total - paid) else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Member", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- 1. PERSONAL INFORMATION CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Rounded.Call, contentDescription = null, tint = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (e.g. 15 Aug 1998)") },
                        leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null, tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = address, onValueChange = { address = it }, label = { Text("Full Address") },
                        leadingIcon = { Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 2. MEMBERSHIP DETAILS CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Membership Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = selectedDateString, onValueChange = { }, label = { Text("Joining Date") },
                        leadingIcon = { Icon(Icons.Rounded.EditCalendar, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        enabled = false, shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = expanded, onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = "$selectedPlan Month(s)", onValueChange = {}, readOnly = true, label = { Text("Membership Plan") },
                            leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.Gray) },
                            shape = RoundedCornerShape(12.dp), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded, onDismissRequest = { expanded = false }
                        ) {
                            planOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text("$selectionOption Month(s)") },
                                    onClick = { selectedPlan = selectionOption; expanded = false }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. PAYMENT DETAILS CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ShoppingCart, contentDescription = null, tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Payment Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = totalFee, onValueChange = { totalFee = it }, label = { Text("Plan Fee") }, prefix = { Text("₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = paidAmount, onValueChange = { paidAmount = it }, label = { Text("Paid Now") }, prefix = { Text("₹") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (pending > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp), // 👈 Size goes inside the modifier!
                                tint = if (pending > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pending Balance", fontWeight = FontWeight.SemiBold,
                                color = if (pending > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "₹${pending.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                            color = if (pending > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- ACTION BUTTONS ---
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Button(
                        onClick = {
                            val joinDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            val expiryMillis = joinDate + (selectedPlan * 30L * 24 * 60 * 60 * 1000)

                            viewModel.saveMember(
                                name = name, phone = phone, dob = dob, address = address,
                                totalFeeString = totalFee, paidAmountString = paidAmount,
                                joinDateMillis = joinDate, planMonths = selectedPlan
                            ) {
                                val pdfFile = ReceiptGenerator.generatePdf(
                                    context = context, name = name, phone = phone, dob = dob, address = address,
                                    planMonths = selectedPlan, totalFee = total, paidAmount = paid,
                                    pendingBalance = pending, joinDateMillis = joinDate, expiryDateMillis = expiryMillis
                                )

                                if (pdfFile != null) {
                                    val authority = "${context.packageName}.provider"
                                    val uri = FileProvider.getUriForFile(context, authority, pdfFile)
                                    val shareMessage = "Welcome to the gym, $name! 💪 Here is your official payment receipt."

                                    try {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                                            setPackage("com.whatsapp")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        try {
                                            val businessIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                                setPackage("com.whatsapp.w4b")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(businessIntent)
                                        } catch (e2: Exception) {
                                            Toast.makeText(context, "WhatsApp is not installed!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                                navController.popBackStack()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Register & Send Receipt", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            val joinDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                            viewModel.saveMember(
                                name = name, phone = phone, dob = dob, address = address,
                                totalFeeString = totalFee, paidAmountString = paidAmount,
                                joinDateMillis = joinDate, planMonths = selectedPlan
                            ) {
                                Toast.makeText(context, "Member Registered Successfully", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp)
                    ) {
                        Text("Just Register", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}