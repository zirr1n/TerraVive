package com.example.terravive

import com.google.firebase.auth.FirebaseAuth

data class ActivityItemModel(
    var id: String = "",
    var title: String = "",
    var date: String = "",
    var time: String = "",
    var location: String = "",
    var description: String = "",
    var userId: String = "",
    var isApproved: Boolean = false,
    var status: String = ""  // if used for 'approved' / 'denied'
) {
    // Method to check if the current activity belongs to the logged-in user
    fun isOwner(): Boolean {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        return userId == currentUserId
    }
}
