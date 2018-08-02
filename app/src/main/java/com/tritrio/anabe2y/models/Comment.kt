package com.tritrio.anabe2y.models

data class Comment(
        val author: String = "",
        val content: String = "",
        val timestamp: Long = 0,
        val uid: String = ""
)