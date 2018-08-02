package com.tritrio.anabe2y.utils

class DateUtils {
    companion object {
        fun getTime(timestamp: Long): String {
            val timeSinceCreation = ((System.currentTimeMillis() - timestamp) / 1000)
            return when {
                timeSinceCreation < 60 -> "${(timeSinceCreation)}s"
                timeSinceCreation < 3600 -> "${(timeSinceCreation / 60)}min"
                timeSinceCreation < 86400 -> "${(timeSinceCreation / 3600)}h"
                timeSinceCreation < 2629800 -> "${(timeSinceCreation / 86400)}d"
                timeSinceCreation < 31557600 -> "${(timeSinceCreation / 2629800)}mon"
                else -> "${(timeSinceCreation / 31557600).toInt()}y"
            }
        }
    }
}