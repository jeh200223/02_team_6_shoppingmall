package com.lion.a02_team_6_shoppingmall.repository

import android.content.Context
import com.lion.a02_team_6_shoppingmall.database.BookDatabase
import com.lion.a02_team_6_shoppingmall.database.BookVO
import com.lion.a02_team_6_shoppingmall.util.BookType
import com.lion.a02_team_6_shoppingmall.viewmodel.BookViewModel

class BookRepository {
    companion object{
        fun insertBookData(context: Context, bookViewModel: BookViewModel){
            val bookDatabase = BookDatabase.getInstance(context)
            val bookType = bookViewModel.bookType.number
            val bookTitle = bookViewModel.bookTitle
            val bookName = bookViewModel.bookName
            val bookCount = bookViewModel.bookCount
            val bookImage = bookViewModel.bookImage

            val bookVO = BookVO(bookType = bookType, bookTitle = bookTitle, bookName = bookName, bookCount = bookCount, bookImage = bookImage)

            bookDatabase?.bookDAO()?.insertBookData(bookVO)
        }

//        fun InsertOrUpdateBookData(context: Context, bookViewModel: BookViewModel) {
//            val bookDatabase = BookDatabase.getInstance(context)
//            val bookDAO = bookDatabase?.bookDAO()
//            val bookType = bookViewModel.bookType.number
//            val bookName = bookViewModel.bookName
//            val bookCount = bookViewModel.bookCount
//            val bookTitle = bookViewModel.bookTitle
//            val bookImage = bookViewModel.bookImage
//
//            // 기존 데이터 검색
//            val existingBook = bookDAO?.selectBookByTitleTypeAndName(bookTitle, bookType, bookName)
//
//            if (existingBook != null) {
//                // 기존 데이터가 있으면 재고량 업데이트
//                bookDAO.updateBookCount(bookTitle, bookType, bookName, bookCount)
//            } else {
//                // 기존 데이터가 없으면 새로 삽입
//                val bookVO = BookVO(
//                    bookIdx = 0,
//                    bookType = bookType,
//                    bookTitle = bookTitle,
//                    bookName = bookName,
//                    bookCount = bookCount,
//                    bookImage = bookImage
//                )
//                bookDAO?.insertBookData(bookVO)
//            }
//        }

        fun selectBookDataAll(context: Context): MutableList<BookViewModel>{
            val bookDatabase = BookDatabase.getInstance(context)
            val bookVoList = bookDatabase?.bookDAO()?.selectBookDataAll()
            val bookVewModelList = mutableListOf<BookViewModel>()

            bookVoList?.forEach {
                val bookType = when(it.bookType){
                    BookType.BOOK_LITERATURE.number -> BookType.BOOK_LITERATURE
                    BookType.BOOK_HUMANITIES.number -> BookType.BOOK_HUMANITIES
                    BookType.BOOK_NATURE.number -> BookType.BOOK_NATURE
                    BookType.BOOK_ETC.number -> BookType.BOOK_ETC
                    else -> BookType.BOOK_ALL
                }
                val bookTitle = it.bookTitle
                val bookIdx = it.bookIdx
                val bookName = it.bookName
                val bookCount = it.bookCount
                val bookImage = it.bookImage
                val bookViewModel = BookViewModel(bookIdx, bookType, bookTitle, bookName, bookCount, bookImage)
                bookVewModelList.add(bookViewModel)
            }

            return bookVewModelList
        }

        fun selectBookDataByIdx(context: Context, bookIdx: Int): BookViewModel {
            val bookDatabase = BookDatabase.getInstance(context)
            val bookVO = bookDatabase?.bookDAO()?.selectBookDataByBookIdx(bookIdx)

            val bookType = when(bookVO?.bookType){
                BookType.BOOK_LITERATURE.number -> BookType.BOOK_LITERATURE
                BookType.BOOK_HUMANITIES.number -> BookType.BOOK_HUMANITIES
                BookType.BOOK_NATURE.number -> BookType.BOOK_NATURE
                BookType.BOOK_ETC.number -> BookType.BOOK_ETC
                else -> BookType.BOOK_ALL
            }

            val bookTitle = bookVO?.bookTitle
            val bookName = bookVO?.bookName
            val bookCount = bookVO?.bookCount
            val bookImage = bookVO?.bookImage

            val bookViewModel = BookViewModel(bookIdx, bookType, bookTitle!!, bookName!!, bookCount!!, bookImage!!)

            return bookViewModel
        }

        fun selectBookDataByTypes(context: Context, bookType: BookType): MutableList<BookViewModel> {
            val bookDatabase = BookDatabase.getInstance(context)

            // BookType 리스트를 Int 리스트로 변환
            val bookTypeNumber = bookType.number

            // DAO에서 selectBookDataByTypes 호출
            val bookVoList = bookDatabase?.bookDAO()?.selectBookDataByTypes(bookTypeNumber)

            val bookViewModelList = mutableListOf<BookViewModel>()
            bookVoList?.forEach {
                val bookType = when (it.bookType) {
                    BookType.BOOK_LITERATURE.number -> BookType.BOOK_LITERATURE
                    BookType.BOOK_HUMANITIES.number -> BookType.BOOK_HUMANITIES
                    BookType.BOOK_NATURE.number -> BookType.BOOK_NATURE
                    BookType.BOOK_ETC.number -> BookType.BOOK_ETC
                    else -> BookType.BOOK_ALL
                }
                val bookViewModel = BookViewModel(it.bookIdx, bookType, it.bookTitle, it.bookName, it.bookCount, it.bookImage)
                bookViewModelList.add(bookViewModel)
            }

            return bookViewModelList
        }

        fun selectBookDataByTitle(context: Context, bookTitle: String): MutableList<BookViewModel> {
            val bookDatabase = BookDatabase.getInstance(context)
            val bookVoList = bookDatabase?.bookDAO()?.selectBookDataByBookTitle(bookTitle)

            val bookViewModelList = mutableListOf<BookViewModel>()
            bookVoList?.forEach {
                val bookType = when (it.bookType) {
                    BookType.BOOK_LITERATURE.number -> BookType.BOOK_LITERATURE
                    BookType.BOOK_HUMANITIES.number -> BookType.BOOK_HUMANITIES
                    BookType.BOOK_NATURE.number -> BookType.BOOK_NATURE
                    BookType.BOOK_ETC.number -> BookType.BOOK_ETC
                    else -> BookType.BOOK_ALL
                }
                val bookViewModel = BookViewModel(it.bookIdx, bookType, it.bookTitle, it.bookName, it.bookCount, it.bookImage)
                bookViewModelList.add(bookViewModel)
            }

            return bookViewModelList
        }

        fun deleteBookData(context: Context, bookIdx: Int){
            val bookDatabase = BookDatabase.getInstance(context)
            val bookVO = BookVO(bookIdx = bookIdx)

            bookDatabase?.bookDAO()?.deleteBookData(bookVO)
        }

        fun modifyBookData(context: Context, bookViewModel: BookViewModel){
            val bookDatabase = BookDatabase.getInstance(context)

            val bookIdx = bookViewModel.bookIdx
            val bookType = bookViewModel.bookType.number
            val bookTitle = bookViewModel.bookTitle
            val bookName = bookViewModel.bookName
            val bookCount = bookViewModel.bookCount
            val bookImage = bookViewModel.bookImage

            val bookVO = BookVO(bookIdx, bookType, bookTitle, bookName, bookCount, bookImage)
            bookDatabase?.bookDAO()?.modifyBookData(bookVO)
        }
    }
}