package com.danmurphyy.shortsapp.model

data class UserModel(
    var id: String = "",
    var email: String = "",
    var userName: String = "",
    var profilePic: String = "",
    var followerList: MutableList<String> = mutableListOf(),
    var followingList: MutableList<String> = mutableListOf(),
)
