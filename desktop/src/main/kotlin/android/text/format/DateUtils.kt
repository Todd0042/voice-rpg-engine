package android.text.format

object DateUtils {
    const val MINUTE_IN_MILLIS: Long = 60_000L

    fun getRelativeTimeSpanString(time: Long, now: Long, minResolution: Long): CharSequence {
        val diffSec = ((now - time) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60L -> "just now"
            diffSec < 3600L -> "${diffSec / 60L}m ago"
            diffSec < 86400L -> "${diffSec / 3600L}h ago"
            else -> "${diffSec / 86400L}d ago"
        }
    }
}
