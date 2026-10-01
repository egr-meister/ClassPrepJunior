package com.classprep.junior.data.repository

import com.classprep.junior.domain.model.GeneratedPlan
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.PlanState
import com.classprep.junior.domain.model.WeekConfig
import com.classprep.junior.domain.planning.ConfirmResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class PlanData(
    val config: WeekConfig,
    val generated: GeneratedPlan,
    val plan: PlanState?,
)

/** What the planner and review screens need. Implemented by the real repository and the in-memory example. */
interface PlanSource {
    val isExample: Boolean
    fun observeConfig(): Flow<WeekConfig>
    fun observePlan(date: LocalDate): Flow<PlanData>
    suspend fun preparePlan(date: LocalDate)
    suspend fun setReady(date: LocalDate, key: ItemKey, ready: Boolean)
    suspend fun undoReady(date: LocalDate)
    suspend fun confirm(date: LocalDate, expectedRevision: Int): ConfirmResult

    /** Returns an error message or null on success. */
    suspend fun addTask(date: LocalDate, name: String, note: String?, category: ItemCategory, iconKey: String): String?
}
