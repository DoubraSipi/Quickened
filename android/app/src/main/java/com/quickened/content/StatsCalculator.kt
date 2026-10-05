package com.quickened.content

// Local-only usage counts. No analytics, no network.
object StatsCalculator {
    data class Stats(val sessionsThisWeek: Int, val totalMinutes: Int)

    fun compute(sessions: List<com.quickened.data.Session>): Stats {
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        var week = 0
        var secs = 0
        sessions.forEach {
            secs += it.durationSeconds
            runCatching {
                if (java.time.Instant.parse(it.timestamp).toEpochMilli() >= weekAgo) week++
            }
        }
        return Stats(week, secs / 60)
    }

    fun encouragement(s: Stats): String =
        if (s.sessionsThisWeek == 0) "A quiet moment with the Lord is waiting for you today."
        else "You spent ${s.totalMinutes} minutes with the Lord this week across ${s.sessionsThisWeek} moments."
}
