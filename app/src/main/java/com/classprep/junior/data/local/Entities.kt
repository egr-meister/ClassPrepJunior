package com.classprep.junior.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Weekdays are stored as ISO numbers (1 = Monday … 7 = Sunday).
// Target dates are ISO local-date strings (yyyy-MM-dd). Instants are epoch milliseconds (UTC).

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val displayOrder: Int,
)

@Entity(tableName = "weekday_configs")
data class WeekdayConfigEntity(
    @PrimaryKey val weekday: Int,
    val isSchoolDay: Boolean,
)

@Entity(
    tableName = "lesson_slots",
    foreignKeys = [ForeignKey(SubjectEntity::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subjectId"), Index(value = ["weekday", "position"], unique = true)],
)
data class LessonSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekday: Int,
    val subjectId: Long,
    val position: Int,
)

@Entity(tableName = "reusable_items")
data class ReusableItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String?,
    val iconKey: String,
    val category: String,
)

@Entity(
    tableName = "subject_item_links",
    primaryKeys = ["subjectId", "itemId"],
    foreignKeys = [
        ForeignKey(SubjectEntity::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(ReusableItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("itemId")],
)
data class SubjectItemLinkEntity(val subjectId: Long, val itemId: Long)

@Entity(
    tableName = "weekday_item_links",
    primaryKeys = ["weekday", "itemId"],
    foreignKeys = [ForeignKey(ReusableItemEntity::class, ["id"], ["itemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("itemId")],
)
data class WeekdayItemLinkEntity(val weekday: Int, val itemId: Long)

@Entity(tableName = "date_tasks", indices = [Index("targetDate")])
data class DateTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetDate: String,
    val name: String,
    val note: String?,
    val iconKey: String,
    val category: String,
)

@Entity(tableName = "preparation_plans", indices = [Index(value = ["targetDate"], unique = true)])
data class PreparationPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetDate: String,
    val revision: Int,
    val contentSignature: String,
    val confirmedRevision: Int?,
    val confirmedAt: Long?,
)

@Entity(
    tableName = "preparation_item_states",
    primaryKeys = ["planId", "sourceType", "sourceId"],
    foreignKeys = [ForeignKey(PreparationPlanEntity::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)],
)
data class PreparationItemStateEntity(
    val planId: Long,
    val sourceType: String,
    val sourceId: Long,
    val ready: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "readiness_history", indices = [Index("confirmedAt")])
data class ReadinessHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetDate: String,
    val confirmedAt: Long,
    val itemCount: Int,
    val planRevision: Int,
)

@Entity(
    tableName = "readiness_history_lessons",
    primaryKeys = ["historyId", "position"],
    foreignKeys = [ForeignKey(ReadinessHistoryEntity::class, ["id"], ["historyId"], onDelete = ForeignKey.CASCADE)],
)
data class ReadinessHistoryLessonEntity(
    val historyId: Long,
    val position: Int,
    val subjectNameSnapshot: String,
    val iconKeySnapshot: String,
)

@Entity(
    tableName = "readiness_history_items",
    foreignKeys = [ForeignKey(ReadinessHistoryEntity::class, ["id"], ["historyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("historyId")],
)
data class ReadinessHistoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val historyId: Long,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    val nameSnapshot: String,
    val noteSnapshot: String?,
    val categorySnapshot: String,
    val iconKeySnapshot: String,
    val subjectLabelsSnapshot: String,
)
