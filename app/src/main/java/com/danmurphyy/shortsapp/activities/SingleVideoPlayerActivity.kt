package com.danmurphyy.shortsapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.danmurphyy.shortsapp.R
import com.danmurphyy.shortsapp.adapter.VideoListAdapter
import com.danmurphyy.shortsapp.databinding.ActivitySingleVideoPlayerBinding
import com.danmurphyy.shortsapp.model.VideoModel
import com.danmurphyy.shortsapp.utils.Constants
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class SingleVideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySingleVideoPlayerBinding
    private lateinit var videoId: String
    private lateinit var adapter: VideoListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoId = intent.getStringExtra(Constants.VideoId)!!
        setupViewPager()
    }

    private fun setupViewPager() {
        val options = FirestoreRecyclerOptions.Builder<VideoModel>()
            .setQuery(
                Firebase.firestore.collection(Constants.Videos)
                    .whereEqualTo(Constants.VideoId, videoId),
                VideoModel::class.java
            ).build()
        adapter = VideoListAdapter(options)
        binding.videoPager.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.stopListening()
    }
}