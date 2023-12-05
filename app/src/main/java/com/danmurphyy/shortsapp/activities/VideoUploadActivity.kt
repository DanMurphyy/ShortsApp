package com.danmurphyy.shortsapp.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.danmurphyy.shortsapp.utils.Constants
import com.danmurphyy.shortsapp.utils.UiUtils
import com.danmurphyy.shortsapp.databinding.ActivityVideoUploadBinding
import com.danmurphyy.shortsapp.model.VideoModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

class VideoUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoUploadBinding
    private var selectedVideoUrl: Uri? = null
    private lateinit var videoLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    selectedVideoUrl = result.data?.data
                    showPostView()
                }

            }
        binding.addIcon.setOnClickListener {
            checkPermissionAndOpenVideoPicker()
        }

        binding.submitPostBtn.setOnClickListener {
            postVideo()
        }

        binding.cancelPostBtn.setOnClickListener {
            finish()
        }

    }

    private fun setInProgress(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBarVideo.visibility = View.VISIBLE
            binding.submitPostBtn.visibility = View.GONE
        } else {
            binding.progressBarVideo.visibility = View.GONE
            binding.submitPostBtn.visibility = View.VISIBLE
        }
    }

    private fun postVideo() {
        if (binding.postCaptionInput.text.toString().isEmpty()) {
            binding.postCaptionInput.error = "Write something"
            return
        }
        setInProgress(true)

        selectedVideoUrl?.apply {
            //store in firebase cloud storage
            val videoRef = FirebaseStorage.getInstance()
                .reference
                .child(Constants.Video + this.lastPathSegment)
            videoRef.putFile(this)
                .addOnSuccessListener {
                    videoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        //video model store in firebase fireStore
                        postToFireStore(downloadUrl.toString())
                    }
                }
        }
    }

    private fun postToFireStore(url: String) {
        val videoModel = VideoModel(
            FirebaseAuth.getInstance().currentUser?.uid!! + "_" + Timestamp.now().toString(),
            binding.postCaptionInput.text.toString(),
            url,
            FirebaseAuth.getInstance().currentUser?.uid!!,
            Timestamp.now()
        )
        Firebase.firestore.collection(Constants.Videos)
            .document(videoModel.videoId)
            .set(videoModel)
            .addOnSuccessListener {
                setInProgress(false)
                UiUtils.showToast(applicationContext, "Video Uploaded")
                finish()
            }
            .addOnFailureListener {
                setInProgress(false)
                UiUtils.showToast(applicationContext, "Video failed to Upload")
            }
    }

    private fun showPostView() {
        selectedVideoUrl?.let {
            binding.uploadView.visibility = View.GONE
            binding.postView.visibility = View.VISIBLE
            Glide
                .with(binding.postThumbnailView)
                .load(it)
                .into(binding.postThumbnailView)
        }

    }

    private fun checkPermissionAndOpenVideoPicker() {
        val readExternalVideo: String
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalVideo = android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            readExternalVideo = android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(
                this,
                readExternalVideo
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //we have permission
            openVideoPicker()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(readExternalVideo), 100)
        }

    }

    @SuppressLint("IntentReset")
    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        videoLauncher.launch(intent)
    }
}