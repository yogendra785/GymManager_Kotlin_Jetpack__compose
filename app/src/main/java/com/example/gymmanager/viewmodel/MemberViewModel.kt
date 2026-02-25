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

    fun saveMember(name: String, phone: String, feeString: String, onSuccess: () -> Unit) {
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

        // Time calculations using milliseconds
        val todayMillis = System.currentTimeMillis()
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000 // 30 days * 24 hrs * 60 mins * 60 secs * 1000 ms
        val expiryMillis = todayMillis + thirtyDaysInMillis

        // Create the Member object
        val newMember = Member(
            name = name,
            phoneNumber = phone,
            joinDate = todayMillis,
            expiryDate = expiryMillis,
            feePaid = fee,
            isActive = true
        )

        // Send to Repository
        repository.addMember(
            member = newMember,
            onSuccess = {
                _isLoading.value = false
                onSuccess() // Tell the UI it worked!
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
            member.expiryDate in today..(today + sevenDaysInMillis)
        }
}