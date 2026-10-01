package com.classprep.junior.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {
    // Subjects
    @Query("SELECT * FROM subjects ORDER BY displayOrder, id")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY displayOrder, id")
    suspend fun subjects(): List<SubjectEntity>

    @Insert
    suspend fun insertSubject(entity: SubjectEntity): Long

    @Upsert
    suspend fun upsertSubject(entity: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    @Query("SELECT COALESCE(MAX(displayOrder), -1) + 1 FROM subjects")
    suspend fun nextSubjectOrder(): Int

    // Weekdays
    @Query("SELECT * FROM weekday_configs")
    fun observeWeekdays(): Flow<List<WeekdayConfigEntity>>

    @Query("SELECT * FROM weekday_configs")
    suspend fun weekdays(): List<WeekdayConfigEntity>

    @Upsert
    suspend fun upsertWeekday(entity: WeekdayConfigEntity)

    // Lessons
    @Query("SELECT * FROM lesson_slots ORDER BY weekday, position")
    fun observeLessons(): Flow<List<LessonSlotEntity>>

    @Query("SELECT * FROM lesson_slots ORDER BY weekday, position")
    suspend fun lessons(): List<LessonSlotEntity>

    @Query("DELETE FROM lesson_slots WHERE weekday = :weekday")
    suspend fun deleteLessonsFor(weekday: Int)

    @Insert
    suspend fun insertLessons(lessons: List<LessonSlotEntity>)

    // Items
    @Query("SELECT * FROM reusable_items ORDER BY name COLLATE NOCASE, id")
    fun observeItems(): Flow<List<ReusableItemEntity>>

    @Query("SELECT * FROM reusable_items ORDER BY name COLLATE NOCASE, id")
    suspend fun items(): List<ReusableItemEntity>

    @Insert
    suspend fun insertItem(entity: ReusableItemEntity): Long

    @Upsert
    suspend fun upsertItem(entity: ReusableItemEntity)

    @Query("DELETE FROM reusable_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("SELECT COUNT(*) FROM reusable_items")
    suspend fun itemCount(): Int

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun subjectCount(): Int

    // Links
    @Query("SELECT * FROM subject_item_links")
    fun observeSubjectLinks(): Flow<List<SubjectItemLinkEntity>>

    @Query("SELECT * FROM subject_item_links")
    suspend fun subjectLinks(): List<SubjectItemLinkEntity>

    @Query("DELETE FROM subject_item_links WHERE subjectId = :subjectId")
    suspend fun deleteSubjectLinks(subjectId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubjectLinks(links: List<SubjectItemLinkEntity>)

    @Query("SELECT * FROM weekday_item_links")
    fun observeWeekdayLinks(): Flow<List<WeekdayItemLinkEntity>>

    @Query("SELECT * FROM weekday_item_links")
    suspend fun weekdayLinks(): List<WeekdayItemLinkEntity>

    @Query("DELETE FROM weekday_item_links WHERE weekday = :weekday")
    suspend fun deleteWeekdayLinks(weekday: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeekdayLinks(links: List<WeekdayItemLinkEntity>)

    // Date tasks
    @Query("SELECT * FROM date_tasks WHERE targetDate = :date ORDER BY id")
    fun observeTasks(date: String): Flow<List<DateTaskEntity>>

    @Query("SELECT * FROM date_tasks WHERE targetDate = :date ORDER BY id")
    suspend fun tasks(date: String): List<DateTaskEntity>

    @Query("SELECT * FROM date_tasks WHERE targetDate >= :from ORDER BY targetDate, id")
    fun observeTasksFrom(from: String): Flow<List<DateTaskEntity>>

    @Query("SELECT * FROM date_tasks WHERE id = :id")
    suspend fun task(id: Long): DateTaskEntity?

    @Insert
    suspend fun insertTask(entity: DateTaskEntity): Long

    @Upsert
    suspend fun upsertTask(entity: DateTaskEntity)

    @Query("DELETE FROM date_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT COUNT(*) FROM date_tasks WHERE targetDate = :date")
    suspend fun taskCount(date: String): Int
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM preparation_plans WHERE targetDate = :date")
    suspend fun plan(date: String): PreparationPlanEntity?

    @Query("SELECT * FROM preparation_plans WHERE targetDate = :date")
    fun observePlan(date: String): Flow<PreparationPlanEntity?>

    @Query("SELECT * FROM preparation_plans WHERE targetDate >= :from")
    suspend fun plansFrom(from: String): List<PreparationPlanEntity>

    @Insert
    suspend fun insertPlan(entity: PreparationPlanEntity): Long

    @Upsert
    suspend fun upsertPlan(entity: PreparationPlanEntity)

    @Query("DELETE FROM preparation_plans WHERE targetDate < :before")
    suspend fun deletePlansBefore(before: String)

    @Query("SELECT * FROM preparation_item_states WHERE planId = :planId")
    suspend fun states(planId: Long): List<PreparationItemStateEntity>

    @Query("SELECT s.* FROM preparation_item_states s JOIN preparation_plans p ON p.id = s.planId WHERE p.targetDate = :date")
    fun observeStates(date: String): Flow<List<PreparationItemStateEntity>>

    @Query("DELETE FROM preparation_item_states WHERE planId = :planId")
    suspend fun deleteStates(planId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStates(states: List<PreparationItemStateEntity>)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM readiness_history ORDER BY confirmedAt DESC, id DESC")
    fun observeHistory(): Flow<List<ReadinessHistoryEntity>>

    @Query("SELECT * FROM readiness_history WHERE id = :id")
    suspend fun entry(id: Long): ReadinessHistoryEntity?

    @Query("SELECT * FROM readiness_history_lessons WHERE historyId = :id ORDER BY position")
    suspend fun lessons(id: Long): List<ReadinessHistoryLessonEntity>

    @Query("SELECT * FROM readiness_history_items WHERE historyId = :id ORDER BY sortOrder, id")
    suspend fun items(id: Long): List<ReadinessHistoryItemEntity>

    @Insert
    suspend fun insertEntry(entity: ReadinessHistoryEntity): Long

    @Insert
    suspend fun insertLessons(lessons: List<ReadinessHistoryLessonEntity>)

    @Insert
    suspend fun insertItems(items: List<ReadinessHistoryItemEntity>)

    @Query("DELETE FROM readiness_history WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM readiness_history")
    suspend fun clear()

    @Query(
        "DELETE FROM readiness_history WHERE id NOT IN " +
            "(SELECT id FROM readiness_history ORDER BY confirmedAt DESC, id DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)
}
