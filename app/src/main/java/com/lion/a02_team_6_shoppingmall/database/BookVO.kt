package com.lion.a02_team_6_shoppingmall.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "BookTable")
data class BookVO(
    @PrimaryKey(autoGenerate = true)
    var bookIdx: Int = 0,
    var bookType: Int = 0,
    var bookTitle: String = "",
    var bookName: String = "",
    var bookCount: Int = 0,
    var bookImage: String = ""
)