package com.lion.a02_team_6_shoppingmall.util

enum class FragmentName(var number:Int, var str:String) {
    MAIN_FRAGMENT(1, "MainFragment"),
    INPUT_FRAGMENT(2, "InputFragment"),
    SHOW_FRAGMENT(3, "ShowFragment"),
    MODIFY_FRAGMENT(4, "ModifyFragment"),
    LOCATION_FRAGMENT(5, "LocationFragment"),
    SEARCH_FRAGMENT(6, "SearchFragment"),
}

enum class BookType(var number:Int, var str:String) {
    BOOK_LITERATURE(1, "문학"),
    BOOK_HUMANITIES(2, "인문"),
    BOOK_NATURE(3, "자연"),
    BOOK_ETC(4, "기타"),
    BOOK_ALL(5, "전체"),
}