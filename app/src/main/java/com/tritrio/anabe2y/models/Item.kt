package com.tritrio.anabe2y.models

data class Item(
        val title: String? = null,
        val link: String? = null,
        val description: String? = null,
        var score: Long = 0,
        val num_comments: Long = 0,
        val uid: String = "",
        val author: String = "",
        val timestamp: Long = 0,
        val thumbnail: String? = null,
        val aspectRatio: Float? = null,
        val youtube: Boolean = false,
        val path: String? = null
) {
    var likes: Boolean? = null
    var id: String? = null
}