package com.hocalingo.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_packages",
    indices = [Index(value = ["package_id"], unique = true)]
)
data class WordPackageEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_id")
    val packageId: String, // a1_en_tr_v1

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "level")
    val level: String,

    @ColumnInfo(name = "language_pair")
    val languagePair: String, // en_tr

    @ColumnInfo(name = "total_words")
    val totalWords: Int,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "description")
    val description: String? = null
)