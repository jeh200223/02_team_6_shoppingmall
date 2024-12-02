package com.lion.a02_team_6_shoppingmall.viewmodel

import com.lion.a02_team_6_shoppingmall.util.BookType
//import com.lion.a02_team_6_shoppingmall.util.BookTypeMain

data class BookViewModel(
    var bookIdx: Int,
    var bookType: BookType,
    var bookTitle: String,
    var bookName: String,
    var bookCount: Int,
    var bookImage: String
)