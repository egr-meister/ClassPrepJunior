package com.classprep.junior.data.repository

import com.classprep.junior.data.local.DateTaskEntity
import com.classprep.junior.data.local.LessonSlotEntity
import com.classprep.junior.data.local.PreparationItemStateEntity
import com.classprep.junior.data.local.PreparationPlanEntity
import com.classprep.junior.data.local.ReusableItemEntity
import com.classprep.junior.data.local.SubjectEntity
import com.classprep.junior.data.local.SubjectItemLinkEntity
import com.classprep.junior.data.local.WeekdayConfigEntity
import com.classprep.junior.data.local.WeekdayItemLinkEntity
import com.classprep.junior.domain.model.DateTask
import com.classprep.junior.domain.model.ItemCategory
import com.classprep.junior.domain.model.ItemKey
import com.classprep.junior.domain.model.ItemState
import com.classprep.junior.domain.model.LessonSlot
import com.classprep.junior.domain.model.PlanState
import com.classprep.junior.domain.model.ReusableItem
import com.classprep.junior.domain.model.SourceType
import com.classprep.junior.domain.model.Subject
import com.classprep.junior.domain.model.WeekConfig
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

internal fun SubjectEntity.toDomain() = Subject(id, name, iconKey, displayOrder)
internal fun Subject.toEntity() = SubjectEntity(id, name, iconKey, displayOrder)

internal fun ReusableItemEntity.toDomain() = ReusableItem(id, name, note, iconKey, ItemCategory.fromKey(category))
internal fun ReusableItem.toEntity() = ReusableItemEntity(id, name, note, iconKey, category.key)

internal fun LessonSlotEntity.toDomain() = LessonSlot(DayOfWeek.of(weekday), subjectId, position)

internal fun DateTaskEntity.toDomain() =
    DateTask(id, LocalDate.parse(targetDate), name, note, iconKey, ItemCategory.fromKey(category))
internal fun DateTask.toEntity() =
    DateTaskEntity(id, targetDate.toString(), name, note, iconKey, category.key)

internal fun buildConfig(
    subjects: List<SubjectEntity>,
    weekdays: List<WeekdayConfigEntity>,
    lessons: List<LessonSlotEntity>,
    items: List<ReusableItemEntity>,
    subjectLinks: List<SubjectItemLinkEntity>,
    weekdayLinks: List<WeekdayItemLinkEntity>,
): WeekConfig = WeekConfig(
    schoolDays = weekdays.filter { it.isSchoolDay }.map { DayOfWeek.of(it.weekday) }.toSet(),
    subjects = subjects.map { it.toDomain() },
    lessons = lessons.map { it.toDomain() },
    items = items.map { it.toDomain() },
    subjectItems = subjectLinks.groupBy({ it.subjectId }, { it.itemId }).mapValues { it.value.toSet() },
    weekdayItems = weekdayLinks.groupBy({ DayOfWeek.of(it.weekday) }, { it.itemId }).mapValues { it.value.toSet() },
)

internal fun planToDomain(plan: PreparationPlanEntity, states: List<PreparationItemStateEntity>) = PlanState(
    id = plan.id,
    date = LocalDate.parse(plan.targetDate),
    revision = plan.revision,
    signature = plan.contentSignature,
    confirmedRevision = plan.confirmedRevision,
    confirmedAt = plan.confirmedAt?.let(Instant::ofEpochMilli),
    states = states.associate {
        ItemKey(SourceType.fromKey(it.sourceType), it.sourceId) to ItemState(it.ready, Instant.ofEpochMilli(it.updatedAt))
    },
)

internal fun PlanState.toEntity() = PreparationPlanEntity(
    id = id,
    targetDate = date.toString(),
    revision = revision,
    contentSignature = signature,
    confirmedRevision = confirmedRevision,
    confirmedAt = confirmedAt?.toEpochMilli(),
)

internal fun PlanState.stateEntities() = states.map { (k, v) ->
    PreparationItemStateEntity(id, k.sourceType.key, k.sourceId, v.ready, v.updatedAt.toEpochMilli())
}
