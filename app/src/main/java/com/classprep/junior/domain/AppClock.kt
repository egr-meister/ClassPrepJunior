package com.classprep.junior.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/** Injected clock so date rules are deterministic in tests. Always reads the current device zone. */
interface AppClock {
    fun now(): ZonedDateTime
    fun instant(): Instant = now().toInstant()
    fun today(): LocalDate = now().toLocalDate()
}

object SystemAppClock : AppClock {
    override fun now(): ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
}
