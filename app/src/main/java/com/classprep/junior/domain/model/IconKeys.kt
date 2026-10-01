package com.classprep.junior.domain.model

/** Stable icon keys stored in the database. Drawables are bundled locally and mapped in ui/theme/AppIcons. */
object IconKeys {
    const val SUBJECT_MATH = "subject_math"
    const val SUBJECT_ENGLISH = "subject_english"
    const val SUBJECT_SCIENCE = "subject_science"
    const val SUBJECT_ART = "subject_art"
    const val SUBJECT_PE = "subject_pe"
    const val SUBJECT_MUSIC = "subject_music"
    const val SUBJECT_HISTORY = "subject_history"
    const val SUBJECT_GENERIC = "subject_generic"

    const val ITEM_NOTEBOOK = "item_notebook"
    const val ITEM_WORKBOOK = "item_workbook"
    const val ITEM_PENCIL_CASE = "item_pencil_case"
    const val ITEM_UNIFORM = "item_uniform"
    const val ITEM_SHOES = "item_shoes"
    const val ITEM_BOTTLE = "item_bottle"
    const val ITEM_LUNCHBOX = "item_lunchbox"
    const val ITEM_FOLDER = "item_folder"
    const val ITEM_SPORTS_CLOTHES = "item_sports_clothes"
    const val ITEM_ART_FOLDER = "item_art_folder"
    const val ITEM_RULER = "item_ruler"
    const val ITEM_BOOK = "item_book"
    const val ITEM_TASK = "item_task"
    const val ITEM_POSTER = "item_poster"
    const val ITEM_BAG = "item_bag"

    data class Option(val key: String, val label: String)

    val subjectOptions = listOf(
        Option(SUBJECT_MATH, "Mathematics"),
        Option(SUBJECT_ENGLISH, "English"),
        Option(SUBJECT_SCIENCE, "Science"),
        Option(SUBJECT_ART, "Art"),
        Option(SUBJECT_PE, "Physical Education"),
        Option(SUBJECT_MUSIC, "Music"),
        Option(SUBJECT_HISTORY, "History"),
        Option(SUBJECT_GENERIC, "Other subject"),
    )

    val itemOptions = listOf(
        Option(ITEM_NOTEBOOK, "Notebook"),
        Option(ITEM_WORKBOOK, "Workbook"),
        Option(ITEM_PENCIL_CASE, "Pencil case"),
        Option(ITEM_UNIFORM, "Uniform"),
        Option(ITEM_SHOES, "Shoes"),
        Option(ITEM_BOTTLE, "Bottle"),
        Option(ITEM_LUNCHBOX, "Lunchbox"),
        Option(ITEM_FOLDER, "Folder"),
        Option(ITEM_SPORTS_CLOTHES, "Sports clothes"),
        Option(ITEM_ART_FOLDER, "Art folder"),
        Option(ITEM_RULER, "Ruler"),
        Option(ITEM_BOOK, "Book"),
        Option(ITEM_TASK, "Task"),
        Option(ITEM_POSTER, "Poster"),
        Option(ITEM_BAG, "Bag"),
    )

    /** Picks a sensible icon for a subject name typed by a parent. */
    fun suggestSubjectIcon(name: String): String {
        val n = name.trim().lowercase()
        return when {
            listOf("math", "algebra", "geometry", "number").any { it in n } -> SUBJECT_MATH
            listOf("english", "reading", "writing", "literacy", "language", "spelling").any { it in n } -> SUBJECT_ENGLISH
            listOf("science", "biology", "chemistry", "physics").any { it in n } -> SUBJECT_SCIENCE
            listOf("art", "drawing", "craft", "design").any { it in n } -> SUBJECT_ART
            "pe" in n.split(Regex("[^a-z]+")) || listOf("p.e", "sport", "physical", "gym", "swim").any { it in n } -> SUBJECT_PE
            listOf("music", "choir", "singing").any { it in n } -> SUBJECT_MUSIC
            listOf("history", "geography", "social").any { it in n } -> SUBJECT_HISTORY
            else -> SUBJECT_GENERIC
        }
    }
}
