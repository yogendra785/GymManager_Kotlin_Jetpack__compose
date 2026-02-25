package com.example.gymmanager.repository

import com.example.gymmanager.model.Member
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MemberRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // We only save the member under the currently logged-in gym owner's ID
    fun addMember(member: Member, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in!")
            return
        }

        // Navigate to: users -> [Gym Owner ID] -> members
        val memberCollection = firestore.collection("users").document(uid).collection("members")

        // Generate a random unique ID for this new member
        val newMemberRef = memberCollection.document()

        // Make a copy of the member object, but insert the generated ID
        val memberWithId = member.copy(id = newMemberRef.id)

        // Save it to Firestore!
        newMemberRef.set(memberWithId)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Failed to add member") }
    }
    // Fetch members and listen for real-time updates
    fun getMembers(onSuccess: (List<Member>) -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not logged in!")
            return
        }

        firestore.collection("users").document(uid).collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error.message ?: "Failed to fetch members")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Convert the Firestore documents back into our Kotlin Member objects
                    val memberList = snapshot.documents.mapNotNull { document ->
                        document.toObject(Member::class.java)
                    }
                    // Sort them by join date (newest first)
                    onSuccess(memberList.sortedByDescending { it.joinDate })
                }
            }
    }
}