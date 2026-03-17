package com.example.gymmanager.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.gymmanager.model.Member
import com.example.gymmanager.model.PaymentRecord
import com.example.gymmanager.repository.MemberRepository
import java.util.Calendar

class MemberViewModel : ViewModel() {

    private val _memberList = mutableStateOf<List<Member>>(emptyList())
    val memberList: State<List<Member>> = _memberList
    private val repository = MemberRepository()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)

    private val _isSubscriptionLocked = mutableStateOf(false)
    val isSubscriptionLocked: State<Boolean> = _isSubscriptionLocked

    // Trigger the repository check
    fun verifySubscription() {
        repository.checkSubscriptionStatus { isLocked, _ ->
            _isSubscriptionLocked.value = isLocked
        }
    }
    val errorMessage: State<String?> = _errorMessage

    // Call this when the screen opens
    fun fetchMembers() {
        _isLoading.value = true
        repository.getMembers(
            onSuccess = { members ->
                _memberList.value = members
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = error
                _isLoading.value = false
            }
        )
    }

    fun saveMember(
        name: String, phone: String, dob: String, address: String,
        totalFeeString: String, paidAmountString: String,
        joinDateMillis: Long, planMonths: Int, onSuccess: () -> Unit
    ) {
        if (name.isBlank() || phone.isBlank() || totalFeeString.isBlank() || paidAmountString.isBlank()) {
            _errorMessage.value = "Please fill all required fields"
            return
        }

        val totalFee = totalFeeString.toDoubleOrNull()
        val paidAmount = paidAmountString.toDoubleOrNull()

        if (totalFee == null || paidAmount == null) {
            _errorMessage.value = "Invalid fee amount"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val todayMillis = System.currentTimeMillis()
        val expiryMillis = joinDateMillis + (planMonths * 30L * 24 * 60 * 60 * 1000)
        val calculatedPending = totalFee - paidAmount

        // 👇 LEDGER UPDATE: Create the very first record
        val initialPayment = PaymentRecord(
            amount = paidAmount,
            date = todayMillis,
            description = "Initial Registration (${planMonths}M Plan)"
        )

        val newMember = Member(
            name = name, phoneNumber = phone, dob = dob, address = address,
            joinDate = joinDateMillis, expiryDate = expiryMillis,
            totalPlanFee = totalFee, feePaid = paidAmount, pendingBalance = calculatedPending,
            isActive = true, planMonths = planMonths,
            lastPaymentDate = todayMillis, lastPaymentAmount = paidAmount,
            paymentHistory = listOf(initialPayment) // 👈 Save the ledger to DB
        )

        repository.addMember(
            member = newMember,
            onSuccess = {
                _isLoading.value = false
                fetchMembers() // 👈 INSTANT REFRESH FIX
                onSuccess()
            },
            onError = { _isLoading.value = false; _errorMessage.value = it }
        )
    }

    fun renewMember(
        member: Member,
        newPlanTotalFeeString: String,
        newPlanPaidAmountString: String,
        newPlanMonths: Int,
        onSuccess: () -> Unit
    ) {
        val newTotalFee = newPlanTotalFeeString.toDoubleOrNull()
        val newPaidAmount = newPlanPaidAmountString.toDoubleOrNull()

        if (newTotalFee == null || newPaidAmount == null) {
            _errorMessage.value = "Invalid fee amount"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val today = System.currentTimeMillis()
        val baseDate = if (member.expiryDate < today) today else member.expiryDate
        val newExpiry = baseDate + (newPlanMonths * 30L * 24 * 60 * 60 * 1000)

        val accumulatedTotalFee = member.totalPlanFee + newTotalFee
        val accumulatedFeePaid = member.feePaid + newPaidAmount
        val newPendingBalance = accumulatedTotalFee - accumulatedFeePaid

        // 👇 LEDGER UPDATE: Add the renewal to the ledger
        val updatedHistory = member.paymentHistory.toMutableList()
        updatedHistory.add(
            PaymentRecord(
                amount = newPaidAmount,
                date = today,
                description = "Plan Renewal (${newPlanMonths}M Plan)"
            )
        )

        val updatedMember = member.copy(
            expiryDate = newExpiry,
            totalPlanFee = accumulatedTotalFee,
            feePaid = accumulatedFeePaid,
            pendingBalance = newPendingBalance,
            planMonths = newPlanMonths,
            isActive = true,
            lastPaymentDate = today,
            lastPaymentAmount = newPaidAmount,
            paymentHistory = updatedHistory // 👈 Save the ledger
        )

        repository.updateMember(
            member = updatedMember,
            onSuccess = {
                _isLoading.value = false
                fetchMembers() // 👈 INSTANT REFRESH FIX
                onSuccess()
            },
            onError = { error -> _isLoading.value = false; _errorMessage.value = error }
        )
    }

    // Save the edited details (With fixed Cumulative Monthly Math & Ledger!)
    fun updateMemberDetails(
        originalMember: Member,
        newName: String,
        newPhone: String,
        newDob: String,
        newAddress: String,
        newTotalFeeString: String,
        newPaidAmountString: String,
        isActive: Boolean,
        onSuccess: () -> Unit
    ) {
        val totalFee = newTotalFeeString.toDoubleOrNull()
        val paidAmount = newPaidAmountString.toDoubleOrNull()

        if (newName.isBlank() || newPhone.isBlank() || totalFee == null || paidAmount == null) {
            _errorMessage.value = "Please enter valid details"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val calculatedPending = totalFee - paidAmount
        val newlyCollectedCash = paidAmount - originalMember.feePaid
        val todayMillis = System.currentTimeMillis()

        val calendarNow = Calendar.getInstance()
        val calendarLast = Calendar.getInstance().apply { timeInMillis = originalMember.lastPaymentDate }

        val isSameMonth = calendarNow.get(Calendar.MONTH) == calendarLast.get(Calendar.MONTH) &&
                calendarNow.get(Calendar.YEAR) == calendarLast.get(Calendar.YEAR)

        val finalPaymentAmount = if (newlyCollectedCash > 0) {
            if (isSameMonth) {
                originalMember.lastPaymentAmount + newlyCollectedCash
            } else {
                newlyCollectedCash
            }
        } else {
            originalMember.lastPaymentAmount
        }

        val finalPaymentDate = if (newlyCollectedCash > 0) todayMillis else originalMember.lastPaymentDate

        // 👇 LEDGER UPDATE: If they paid more money, add a new line to the ledger!
        val updatedHistory = originalMember.paymentHistory.toMutableList()
        if (newlyCollectedCash > 0) {
            updatedHistory.add(
                PaymentRecord(
                    amount = newlyCollectedCash,
                    date = todayMillis,
                    description = "Cleared Pending Dues"
                )
            )
        }

        val updatedMember = originalMember.copy(
            name = newName,
            phoneNumber = newPhone,
            dob = newDob,
            address = newAddress,
            totalPlanFee = totalFee,
            feePaid = paidAmount,
            pendingBalance = calculatedPending,
            isActive = isActive,
            lastPaymentDate = finalPaymentDate,
            lastPaymentAmount = finalPaymentAmount,
            paymentHistory = updatedHistory // 👈 Save the ledger
        )

        repository.updateMember(
            member = updatedMember,
            onSuccess = {
                _isLoading.value = false
                fetchMembers() // 👈 INSTANT REFRESH FIX
                onSuccess()
            },
            onError = { error -> _isLoading.value = false; _errorMessage.value = error }
        )
    }

    // --- DASHBOARD STATISTICS ---

    val totalMembersCount: Int
        get() = _memberList.value.size

    val activeMembersCount: Int
        get() = _memberList.value.count { it.isActive }

    val expiringSoonCount: Int
        get() = _memberList.value.count { member ->
            val today = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
            member.isActive && member.expiryDate <= (today + sevenDaysInMillis)
        }

    val expiringMembersList: List<Member>
        get() {
            val today = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000

            return _memberList.value.filter { member ->
                member.isActive && member.expiryDate <= (today + sevenDaysInMillis)
            }.sortedBy { it.expiryDate }
        }

    fun getMemberById(id: String): Member? {
        return _memberList.value.find { it.id == id }
    }

    val currentMonthRevenue: Double
        get() {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            return _memberList.value.sumOf { member ->
                val paymentCalendar = Calendar.getInstance().apply { timeInMillis = member.lastPaymentDate }

                if (paymentCalendar.get(Calendar.MONTH) == currentMonth &&
                    paymentCalendar.get(Calendar.YEAR) == currentYear) {
                    member.lastPaymentAmount
                } else {
                    0.0
                }
            }
        }

    // --- EXCEL (CSV) EXPORT LOGIC ---
    fun generateCsvData(): String {
        val builder = java.lang.StringBuilder()

        builder.append("Name,Phone Number,Join Date,Expiry Date,Fee Paid (Rs),Plan (Months),Status\n")

        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())

        _memberList.value.forEach { member ->
            val join = dateFormat.format(java.util.Date(member.joinDate))
            val expiry = dateFormat.format(java.util.Date(member.expiryDate))
            val status = if (member.isActive) "Active" else "Inactive"

            val safeName = member.name.replace(",", " ")

            builder.append("$safeName,=\"${member.phoneNumber}\",$join,$expiry,${member.feePaid},${member.planMonths},$status\n")
        }

        return builder.toString()
    }

    fun deleteMember(memberId: String, onSuccess: () -> Unit = {}) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.deleteMember(
            memberId = memberId,
            onSuccess = {
                _isLoading.value = false
                fetchMembers()
                onSuccess()
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    val revenueHistory: List<Pair<String, Double>>
        get() {
            val calendar = Calendar.getInstance()

            val grouped = _memberList.value
                .filter { it.lastPaymentAmount > 0.0 }
                .groupBy {
                    calendar.timeInMillis = it.lastPaymentDate
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    Pair(year, month)
                }
                .mapValues { entry -> entry.value.sumOf { it.lastPaymentAmount } }
                .toList()
                .sortedWith(compareByDescending<Pair<Pair<Int, Int>, Double>> { it.first.first }.thenByDescending { it.first.second })

            val displayFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())

            return grouped.map {
                calendar.set(Calendar.YEAR, it.first.first)
                calendar.set(Calendar.MONTH, it.first.second)
                Pair(displayFormat.format(calendar.time), it.second)
            }
        }
}