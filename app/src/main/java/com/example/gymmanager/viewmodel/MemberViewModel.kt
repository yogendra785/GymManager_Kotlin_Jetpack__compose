package com.example.gymmanager.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.gymmanager.model.Member
import com.example.gymmanager.repository.MemberRepository
import java.util.Calendar // 👈 Make sure this is imported for the monthly math!

class MemberViewModel : ViewModel() {

    private val _memberList = mutableStateOf<List<Member>>(emptyList())
    val memberList: State<List<Member>> = _memberList
    private val repository = MemberRepository()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
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
        name: String,
        phone: String,
        feeString: String,
        joinDateMillis: Long,
        planMonths: Int,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank() || phone.isBlank() || feeString.isBlank()) {
            _errorMessage.value = "Please fill all fields"
            return
        }

        val fee = feeString.toDoubleOrNull()
        if (fee == null) {
            _errorMessage.value = "Invalid fee amount"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        // 👇 FIX: We actually define todayMillis here now!
        val todayMillis = System.currentTimeMillis()

        val expiryMillis = joinDateMillis + (planMonths * 30L * 24 * 60 * 60 * 1000)

        val newMember = Member(
            name = name,
            phoneNumber = phone,
            joinDate = joinDateMillis,
            expiryDate = expiryMillis,
            feePaid = fee,
            isActive = true,
            planMonths = planMonths,
            lastPaymentDate = todayMillis,
            lastPaymentAmount = fee
        )

        repository.addMember(
            member = newMember,
            onSuccess = {
                _isLoading.value = false
                onSuccess()
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    // 1-Click Renew Logic
    fun renewMember(
        member: Member,
        additionalFeeString: String,
        newPlanMonths: Int,
        onSuccess: () -> Unit
    ) {
        val additionalFee = additionalFeeString.toDoubleOrNull()
        if (additionalFee == null) {
            _errorMessage.value = "Invalid fee amount"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val today = System.currentTimeMillis()
        val baseDate = if (member.expiryDate < today) today else member.expiryDate
        val newExpiry = baseDate + (newPlanMonths * 30L * 24 * 60 * 60 * 1000)
        val newTotalFee = member.feePaid + additionalFee

        val updatedMember = member.copy(
            expiryDate = newExpiry,
            feePaid = newTotalFee,
            planMonths = newPlanMonths,
            isActive = true,
            lastPaymentDate = today,
            lastPaymentAmount = additionalFee
        )

        repository.updateMember(
            member = updatedMember,
            onSuccess = {
                _isLoading.value = false
                onSuccess()
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    // Save the edited details
    fun updateMemberDetails(
        originalMember: Member,
        newName: String,
        newPhone: String,
        newFeeString: String,
        isActive: Boolean,
        onSuccess: () -> Unit
    ) {
        val fee = newFeeString.toDoubleOrNull()
        if (newName.isBlank() || newPhone.isBlank() || fee == null) {
            _errorMessage.value = "Please enter valid details"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        val updatedMember = originalMember.copy(
            name = newName,
            phoneNumber = newPhone,
            feePaid = fee,
            isActive = isActive
        )

        repository.updateMember(
            member = updatedMember,
            onSuccess = {
                _isLoading.value = false
                onSuccess()
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }

    // --- DASHBOARD STATISTICS ---

    val totalMembersCount: Int
        get() = _memberList.value.size

    val activeMembersCount: Int
        get() = _memberList.value.count { it.isActive }

    // 👇 Calculates how many ACTIVE members expire in the next 7 days
    val expiringSoonCount: Int
        get() = _memberList.value.count { member ->
            val today = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
            member.isActive && member.expiryDate <= (today + sevenDaysInMillis)
        }

    // 👇 Get a list of ACTIVE members expiring in the next 7 days OR already expired
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

    // 👇 NEW: Calculates revenue ONLY for the current calendar month!
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

            // 👇 THE FIX: Notice the =\" \" around the phone number!
            builder.append("$safeName,=\"${member.phoneNumber}\",$join,$expiry,${member.feePaid},${member.planMonths},$status\n")
        }

        return builder.toString()
    }

    // Delete a member and instantly refresh the list
    fun deleteMember(memberId: String, onSuccess: () -> Unit = {}) {
        _isLoading.value = true
        _errorMessage.value = null

        repository.deleteMember(
            memberId = memberId,
            onSuccess = {
                _isLoading.value = false
                fetchMembers() // 👈 Re-download the list so the deleted person vanishes
                onSuccess()
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
            }
        )
    }
}