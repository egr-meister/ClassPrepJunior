package com.classprep.junior.ui.theme

import androidx.annotation.DrawableRes
import com.classprep.junior.R
import com.classprep.junior.domain.model.IconKeys

/** Maps stored icon keys to bundled vector drawables. Unknown keys fall back to a generic icon. */
object AppIcons {
    @DrawableRes
    fun forKey(key: String): Int = when (key) {
        IconKeys.SUBJECT_MATH -> R.drawable.ic_subject_math
        IconKeys.SUBJECT_ENGLISH -> R.drawable.ic_subject_english
        IconKeys.SUBJECT_SCIENCE -> R.drawable.ic_subject_science
        IconKeys.SUBJECT_ART -> R.drawable.ic_subject_art
        IconKeys.SUBJECT_PE -> R.drawable.ic_subject_pe
        IconKeys.SUBJECT_MUSIC -> R.drawable.ic_subject_music
        IconKeys.SUBJECT_HISTORY -> R.drawable.ic_subject_history
        IconKeys.SUBJECT_GENERIC -> R.drawable.ic_subject_generic
        IconKeys.ITEM_NOTEBOOK -> R.drawable.ic_item_notebook
        IconKeys.ITEM_WORKBOOK -> R.drawable.ic_item_workbook
        IconKeys.ITEM_PENCIL_CASE -> R.drawable.ic_item_pencil_case
        IconKeys.ITEM_UNIFORM -> R.drawable.ic_item_uniform
        IconKeys.ITEM_SHOES -> R.drawable.ic_item_shoes
        IconKeys.ITEM_BOTTLE -> R.drawable.ic_item_bottle
        IconKeys.ITEM_LUNCHBOX -> R.drawable.ic_item_lunchbox
        IconKeys.ITEM_FOLDER -> R.drawable.ic_item_folder
        IconKeys.ITEM_SPORTS_CLOTHES -> R.drawable.ic_item_sports_clothes
        IconKeys.ITEM_ART_FOLDER -> R.drawable.ic_item_art_folder
        IconKeys.ITEM_RULER -> R.drawable.ic_item_ruler
        IconKeys.ITEM_BOOK -> R.drawable.ic_item_book
        IconKeys.ITEM_TASK -> R.drawable.ic_item_task
        IconKeys.ITEM_POSTER -> R.drawable.ic_item_poster
        IconKeys.ITEM_BAG -> R.drawable.ic_item_bag
        else -> if (key.startsWith("subject")) R.drawable.ic_subject_generic else R.drawable.ic_item_bag
    }
}
