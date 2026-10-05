package com.quickened.content

// Local-only usage counts. No analytics, no network.
object StatsCalculator {
    data class Stats(val sessionsThisWeek: Int, val totalMinutes: Int, val streakDays: Int, val totalSessions: Int)

    fun compute(sessions: List<com.quickened.data.Session>): Stats {
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        var week = 0
        var secs = 0
        val days = mutableSetOf<String>()
        sessions.forEach {
            secs += it.durationSeconds
            runCatching {
                val instant = java.time.Instant.parse(it.timestamp)
                if (instant.toEpochMilli() >= weekAgo) week++
                days.add(instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString())
            }
        }
        var streak = 0
        var day = java.time.LocalDate.now()
        while (days.contains(day.toString())) {
            streak++
            day = day.minusDays(1)
        }
        return Stats(week, secs / 60, streak, sessions.size)
    }

    fun encouragement(s: Stats): String =
        if (s.totalSessions == 0) "Begin your first moment with God today."
        else if (s.streakDays >= 2) "${s.streakDays}-day walk with God — keep going. ${s.totalMinutes} minutes this week."
        else "You spent ${s.totalMinutes} minutes with the Lord this week."
}
