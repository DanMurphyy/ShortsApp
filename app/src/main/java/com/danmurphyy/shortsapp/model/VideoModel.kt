package com.danmurphyy.shortsapp.model

import com.google.firebase.Timestamp

data class VideoModel(
    var videoId: String = "",
    var title: String = "",
    var url: String = "",
    var uploadId: String = "",
    var createdTime: Timestamp = Timestamp.now(),
)
