package com.danmurphyy.shortsapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.danmurphyy.shortsapp.R
import com.danmurphyy.shortsapp.databinding.ActivityProfileBinding
import com.danmurphyy.shortsapp.log.LogActivity
import com.danmurphyy.shortsapp.model.UserModel
import com.danmurphyy.shortsapp.utils.Constants
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileUserId: String
    private lateinit var currentUserId: String
    private lateinit var profileUserModel: UserModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        profileUserId = intent.getStringExtra(Constants.ProfileUserId)!!
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid!!

        if (currentUserId == profileUserId) {
            //current user profile
            binding.profileBtn.text = getString(R.string.logout)
            binding.profileBtn.setOnClickListener {
                logout()
            }
        } else {
            // other user profile
        }

        getProfileDataFromFirebase()

    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LogActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun getProfileDataFromFirebase() {
        Firebase.firestore.collection(Constants.Users)
            .document(profileUserId)
            .get()
            .addOnSuccessListener {
                profileUserModel = it.toObject(UserModel::class.java)!!
                setUI()
            }
    }

    private fun setUI() {
        profileUserModel.apply {
            Glide.with(binding.profilePic)
                .load(profilePic)
                .apply(RequestOptions().placeholder(R.drawable.ic_account_circle))
                .into(binding.profilePic)
            binding.profileUsername.text = "@$userName"
            binding.progressBarProfile.visibility = View.INVISIBLE
            binding.followerCount.text = followerList.size.toString()
            binding.followingCount.text = followingList.size.toString()
            Firebase.firestore.collection(Constants.Videos)
                .whereEqualTo(Constants.UploadId, profileUserId)
                .get().addOnSuccessListener {
                    binding.postCount.text = it.size().toString()
                }
        }
    }
}