package com.hocalingo.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "concepts",
    indices = [Index(value = ["level", "category"])]
)
data class ConceptEntity(
    @PrimaryKey
    val id: Int,

    @ColumnInfo(name = "english")
    val english: String,

    @ColumnInfo(name = "turkish")
    val turkish: String,

    @ColumnInfo(name = "example_en")
    val exampleEn: String? = null,

    @ColumnInfo(name = "example_tr")
    val exampleTr: String? = null,

    @ColumnInfo(name = "pronunciation")
    val pronunciation: String? = null,

    @ColumnInfo(name = "level")
    val level: String, // A1, A2, B1, B2, C1, C2

    @ColumnInfo(name = "category")
    val category: String, // education, food, basic, etc.

    @ColumnInfo(name = "reversible")
    val reversible: Boolean = true,

    @ColumnInfo(name = "user_added")
    val userAdded: Boolean = false,

    @ColumnInfo(name = "package_id")
    val packageId: String, // a1_en_tr_v1

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)