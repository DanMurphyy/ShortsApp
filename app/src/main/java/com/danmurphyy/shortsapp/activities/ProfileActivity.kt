package com.danmurphyy.shortsapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.danmurphyy.shortsapp.R
import com.danmurphyy.shortsapp.adapter.ProfileVideoAdapter
import com.danmurphyy.shortsapp.databinding.ActivityProfileBinding
import com.danmurphyy.shortsapp.log.LogActivity
import com.danmurphyy.shortsapp.model.UserModel
import com.danmurphyy.shortsapp.model.VideoModel
import com.danmurphyy.shortsapp.utils.Constants
import com.danmurphyy.shortsapp.utils.UiUtils
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var profileUserId: String
    private lateinit var currentUserId: String
    private lateinit var profileUserModel: UserModel
    private var selectedPhotoUrl: Uri? = null
    private lateinit var photoLauncher: ActivityResultLauncher<Intent>
    private lateinit var adapter: ProfileVideoAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        profileUserId = intent.getStringExtra(Constants.ProfileUserId)!!
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid!!

        photoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    selectedPhotoUrl = result.data?.data
                    uploadPhoto(selectedPhotoUrl)
                }

            }


        if (currentUserId == profileUserId) {
            //current user profile
            binding.profileBtn.text = getString(R.string.logout)
            binding.profileBtn.setOnClickListener {
                logout()
            }
            binding.profilePic.setOnClickListener {
                checkPermissionAndOpenVideoPicker()
            }

        } else {
            // other user profile
            binding.profileBtn.text = getString(R.string.follow)
            binding.profileBtn.setOnClickListener {
                followUnfollow()
            }
        }

        getProfileDataFromFirebase()
        setupRecyclerView()

    }

    private fun setupRecyclerView() {
        val options = FirestoreRecyclerOptions.Builder<VideoModel>()
            .setQuery(
                Firebase.firestore.collection(Constants.Videos)
                    .whereEqualTo(Constants.UploadId, profileUserId)
                    .orderBy(Constants.CreatedTime, Query.Direction.DESCENDING),
                VideoModel::class.java
            ).build()
        adapter = ProfileVideoAdapter(options)
        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerView.adapter = adapter
        adapter.setOnClickListener(object : ProfileVideoAdapter.OnClickListener {
            override fun onClick(videoModel: VideoModel) {
                deleteVideo(videoModel)
            }

        })
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.stopListening()
    }

    private fun followUnfollow() {
        Firebase.firestore.collection(Constants.Users)
            .document(currentUserId)
            .get()
            .addOnSuccessListener {
                val currentUserModel = it.toObject(UserModel::class.java)!!
                if (profileUserModel.followerList.contains(currentUserId)) {
                    //unfollow user
                    profileUserModel.followerList.remove(currentUserId)
                    currentUserModel.followingList.remove(profileUserId)
                    binding.profileBtn.text = getString(R.string.follow)
                } else {
                    //follow user
                    profileUserModel.followerList.add(currentUserId)
                    currentUserModel.followingList.add(profileUserId)
                    binding.profileBtn.text = getString(R.string.unfollow)
                }
                updateUserData(profileUserModel)
                updateUserData(currentUserModel)
            }
    }

    private fun updateUserData(model: UserModel) {
        Firebase.firestore.collection(Constants.Users)
            .document(model.id)
            .set(model)
            .addOnSuccessListener {
                getProfileDataFromFirebase()
            }
    }

    private fun uploadPhoto(photoUrl: Uri?) {
        val photoRef = FirebaseStorage.getInstance()
            .reference
            .child(Constants.Photo + currentUserId)
        photoRef.putFile(photoUrl!!)
            .addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    //video model store in firebase fireStore
                    uploadToFireStore(downloadUrl.toString())
                }
            }
    }

    private fun uploadToFireStore(url: String) {
        binding.progressBarProfile.visibility = View.VISIBLE

        Firebase.firestore.collection(Constants.Users)
            .document(currentUserId)
            .update(Constants.ProfilePic, url)
            .addOnSuccessListener {
                getProfileDataFromFirebase()
                UiUtils.showToast(applicationContext, "Photo Uploaded")
            }
            .addOnFailureListener {
                UiUtils.showToast(applicationContext, "Photo failed to Upload")
            }
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

    @SuppressLint("SetTextI18n")
    private fun setUI() {
        profileUserModel.apply {
            Glide.with(binding.profilePic)
                .load(profilePic)
                .circleCrop()
                .apply(RequestOptions().placeholder(R.drawable.ic_account_circle))
                .into(binding.profilePic)
            binding.profileUsername.text = "@$userName"
            if (profileUserModel.followerList.contains(currentUserId)) {
                binding.profileBtn.text = getString(R.string.unfollow)
            }
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

    private fun deleteVideo(videoModel: VideoModel) {
        val videoRef =
            Firebase.firestore.collection(Constants.Videos).document(videoModel.videoId)

        // Delete the document from FireStore
        videoRef.delete()
            .addOnSuccessListener {
                // The document has been successfully deleted from FireStore

                UiUtils.showToast(applicationContext, "Video deleted successfully")
                recreate()
                // Now, delete the video file from Firebase Storage
//                val storageRef = FirebaseStorage.getInstance()
//                    .reference
//                    .child(videoModel.url)
//                storageRef.delete()
//                    .addOnSuccessListener {
//                        // The video file has been successfully deleted from Storage
//                    }
//                    .addOnFailureListener { e ->
//                        // Handle the failure to delete the video file from Storage
//                        UiUtils.showToast(
//                            applicationContext,
//                            "Failed to delete video file: ${e.message}"
//                        )
//                    }
            }
            .addOnFailureListener { e ->
                // Handle the failure to delete the document from FireStore
                UiUtils.showToast(applicationContext, "Failed to delete video: ${e.message}")
            }
    }

    private fun checkPermissionAndOpenVideoPicker() {
        val readExternalPhoto: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalPhoto = android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            readExternalPhoto = android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(
                this,
                readExternalPhoto
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //we have permission
            openPhotoPicker()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(readExternalPhoto), 100)
        }
    }

    @SuppressLint("IntentReset")
    private fun openPhotoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        photoLauncher.launch(intent)
    }
}