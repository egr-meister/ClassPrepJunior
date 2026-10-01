package com.classprep.junior.domain.planning

object Limits {
    const val MAX_SUBJECTS = 30
    const val MAX_LESSONS_PER_DAY = 12
    const val MAX_ITEMS = 100
    const val MAX_CHECKLIST_ENTRIES = 50
    const val SUBJECT_NAME_MAX = 30
    const val ITEM_NAME_MAX = 60
    const val NOTE_MAX = 120
    const val MAX_TASKS_PER_DATE = 20
    const val HISTORY_KEEP = 60
    const val PLANNING_DAYS_AHEAD = 14L
}

object Validation {
    /** Returns an error message, or null when valid. */
    fun subjectName(raw: String, existingNames: List<String>, editingName: String? = null): String? {
        val name = raw.trim()
        if (name.isEmpty()) return "Enter a subject name."
        if (name.length > Limits.SUBJECT_NAME_MAX) return "Use ${Limits.SUBJECT_NAME_MAX} characters or fewer."
        val clash = existingNames.any { it.trim().equals(name, ignoreCase = true) && !it.equals(editingName, ignoreCase = true) }
        if (clash) return "A subject with this name already exists."
        return null
    }

    fun itemName(raw: String): String? {
        val name = raw.trim()
        if (name.isEmpty()) return "Enter a name."
        if (name.length > Limits.ITEM_NAME_MAX) return "Use ${Limits.ITEM_NAME_MAX} characters or fewer."
        return null
    }

    fun note(raw: String): String? =
        if (raw.trim().length > Limits.NOTE_MAX) "Notes can be up to ${Limits.NOTE_MAX} characters." else null

    fun normalizedNote(raw: String): String? = raw.trim().takeIf { it.isNotEmpty() }
}
