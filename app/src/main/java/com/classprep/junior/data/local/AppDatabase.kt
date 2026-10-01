package com.classprep.junior.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        SubjectEntity::class,
        WeekdayConfigEntity::class,
        LessonSlotEntity::class,
        ReusableItemEntity::class,
        SubjectItemLinkEntity::class,
        WeekdayItemLinkEntity::class,
        DateTaskEntity::class,
        PreparationPlanEntity::class,
        PreparationItemStateEntity::class,
        ReadinessHistoryEntity::class,
        ReadinessHistoryLessonEntity::class,
        ReadinessHistoryItemEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun planDao(): PlanDao
    abstract fun historyDao(): HistoryDao

    companion object {
        const val VERSION = 1
        const val NAME = "classprep.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                // No destructive fallback: an unknown schema change must fail loudly in testing
                // rather than silently erase a family's timetable.
                .build()
    }
}

/**
 * Explicit migrations. Schema JSON for every version is exported to app/schemas/ and committed.
 * When bumping [AppDatabase.VERSION], add a Migration here (e.g. MIGRATION_1_2) and to [ALL].
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf()
}
