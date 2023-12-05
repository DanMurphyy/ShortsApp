package com.danmurphyy.shortsapp.adapter

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.danmurphyy.shortsapp.R
import com.danmurphyy.shortsapp.activities.ProfileActivity
import com.danmurphyy.shortsapp.databinding.VideoItemRowBinding
import com.danmurphyy.shortsapp.model.UserModel
import com.danmurphyy.shortsapp.model.VideoModel
import com.danmurphyy.shortsapp.utils.Constants
import com.danmurphyy.shortsapp.utils.UiUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

class VideoListAdapter(options: FirestoreRecyclerOptions<VideoModel>) :
    FirestoreRecyclerAdapter<VideoModel, VideoListAdapter.VideoViewHolder>(options) {

    inner class VideoViewHolder(private val binding: VideoItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bindVideo(videoModel: VideoModel) {

            Firebase.firestore.collection(Constants.Users)
                .document(videoModel.uploadId)
                .get().addOnSuccessListener {
                    val userModel = it?.toObject(UserModel::class.java)
                    userModel?.apply {
                        binding.usernameView.text = userName
                        //bind profile pic
                        Glide
                            .with(binding.profileIcon)
                            .load(profilePic)
                            .apply(
                                RequestOptions()
                                    .placeholder(R.drawable.ic_person)
                            )
                            .into(binding.profileIcon)

                        binding.userDetailLayout.setOnClickListener {
                            val intent = Intent(
                                binding.userDetailLayout.context, ProfileActivity::class.java
                            )
                            intent.putExtra(Constants.ProfileUserId, id)
                            binding.userDetailLayout.context.startActivity(intent)
                        }

                    }
                }

            binding.captionView.text = videoModel.title
            binding.progressBarView.visibility = View.VISIBLE

            //bindVideo
            binding.videoView.apply {
                setVideoPath(videoModel.url)
                setOnPreparedListener {
                    binding.progressBarView.visibility = View.GONE
                    it.start()
                    it.isLooping = true
                }
                //play pause
                setOnClickListener {
                    if (isPlaying) {
                        pause()
                        binding.pauseIcon.visibility = View.VISIBLE
                    } else {
                        start()
                        binding.pauseIcon.visibility = View.GONE
                    }
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding =
            VideoItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int, model: VideoModel) {
        holder.bindVideo(model)
    }
}