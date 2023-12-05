package com.danmurphyy.shortsapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.danmurphyy.shortsapp.activities.SingleVideoPlayerActivity
import com.danmurphyy.shortsapp.databinding.ProfileVideoItemRowBinding
import com.danmurphyy.shortsapp.model.VideoModel
import com.danmurphyy.shortsapp.utils.Constants
import com.danmurphyy.shortsapp.utils.UiUtils
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage

class ProfileVideoAdapter(options: FirestoreRecyclerOptions<VideoModel>) :
    FirestoreRecyclerAdapter<VideoModel, ProfileVideoAdapter.VideoViewHolder>(options) {

    private var onClickListener: OnClickListener? = null

    inner class VideoViewHolder(private val binding: ProfileVideoItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(video: VideoModel) {
            Glide.with(binding.thumbnailImageView)
                .load(video.url)
                .into(binding.thumbnailImageView)

            binding.thumbnailImageView.setOnClickListener {
                val intent = Intent(
                    binding.thumbnailImageView.context,
                    SingleVideoPlayerActivity::class.java
                )
                intent.putExtra(Constants.VideoId, video.videoId)
                binding.thumbnailImageView.context.startActivity(intent)
            }
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid!!
            if (currentUserId == video.uploadId) {
                binding.deleteCurrentVideo.visibility = View.VISIBLE
                binding.deleteCurrentVideo.setOnClickListener {
                    if (onClickListener != null) {
                        onClickListener!!.onClick(video)
                    }
                }
            } else {
                binding.deleteCurrentVideo.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding =
            ProfileVideoItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int, model: VideoModel) {
        holder.bind(model)
    }

    interface OnClickListener {
        fun onClick(videoModel: VideoModel)

    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }
}