package com.example.gymmanager.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.gymmanager.model.Member
import com.example.gymmanager.repository.MemberRepository

class MemberViewModel : ViewModel() {


    private val _memberList = mutableStateOf<List<Member>>(emptyList())
    val memberList: State<List<Member>> = _memberList
    private val repository = MemberRepository()

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    //function fetch member
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
        joinDateMillis: Long, // 👈 Now accepts a custom date
        planMonths: Int,      // 👈 Now accepts the plan duration
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

        // Calculate expiry based on the selected join date and the plan they chose!
        // planMonths * 30 days * 24 hrs * 60 mins * 60 secs * 1000 ms
        val expiryMillis = joinDateMillis + (planMonths * 30L * 24 * 60 * 60 * 1000)

        val newMember = Member(
            name = name,
            phoneNumber = phone,
            joinDate = joinDateMillis,
            expiryDate = expiryMillis,
            feePaid = fee,
            isActive = true,
            planMonths = planMonths
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
    // --- DASHBOARD STATISTICS ---

    // Total members is just the size of the list
    val totalMembersCount: Int
        get() = _memberList.value.size

    // Active members are the ones where isActive == true
    val activeMembersCount: Int
        get() = _memberList.value.count { it.isActive }

    // Calculate total revenue collected
    val totalRevenue: Double
        get() = _memberList.value.sumOf { it.feePaid }

    // Calculate how many members expire in the next 7 days
    val expiringSoonCount: Int
        get() = _memberList.value.count { member ->
            val today = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
            // Check if the expiry date falls between today and 7 days from now
           member.isActive &&  member.expiryDate <= (today + sevenDaysInMillis)
        }

    // Get a list of members expiring in the next 7 days OR already expired
    val expiringMembersList: List<Member>
        get() {
            val today = System.currentTimeMillis()
            val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000

            return _memberList.value.filter { member ->
                // Keep them if their expiry date is before (today + 7 days)
               member.isActive && member.expiryDate <= (today + sevenDaysInMillis)
            }.sortedBy { it.expiryDate } // Sort them so the oldest dates show up first
        }

    // Find a single member from our downloaded list
    fun getMemberById(id: String): Member? {
        return _memberList.value.find { it.id == id }
    }

    //renew membership function
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

        // Calculate the new expiry date
        val newExpiry = baseDate + (newPlanMonths * 30L * 24 * 60 * 60 * 1000)

        // Add the new money to their lifetime total fee paid
        val newTotalFee = member.feePaid + additionalFee

        // Make a copy of the member with the upgraded details
        val updatedMember = member.copy(
            expiryDate = newExpiry,
            feePaid = newTotalFee,
            planMonths = newPlanMonths,
            isActive = true // Reactivate them just in case!
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

        // Make a copy of the original member, but swap in the new text
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
}