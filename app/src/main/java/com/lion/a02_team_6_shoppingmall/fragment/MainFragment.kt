package com.lion.a02_team_6_shoppingmall.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lion.a02_team_6_shoppingmall.MainActivity
import com.lion.a02_team_6_shoppingmall.R
import com.lion.a02_team_6_shoppingmall.databinding.FragmentMainBinding
import com.lion.a02_team_6_shoppingmall.databinding.RowMainBinding
import com.lion.a02_team_6_shoppingmall.repository.BookRepository
import com.lion.a02_team_6_shoppingmall.util.BookType
import com.lion.a02_team_6_shoppingmall.util.FragmentName
import com.lion.a02_team_6_shoppingmall.viewmodel.BookViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainFragment : Fragment() {
    lateinit var fragmentMainBinding: FragmentMainBinding
    lateinit var mainActivity: MainActivity
    var bookList = mutableListOf<BookViewModel>()
    var selectedType: BookType? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentMainBinding = FragmentMainBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity
        settingToolbar()
        settingFabMain()
        setupSegmentButtonFilter()
        settingRecyclerView()
        return fragmentMainBinding.root
    }

    override fun onResume() {
        super.onResume()

        selectedType = when {
            mainActivity.isLiterature -> BookType.BOOK_LITERATURE
            mainActivity.isHumanities -> BookType.BOOK_HUMANITIES
            mainActivity.isNature -> BookType.BOOK_NATURE
            mainActivity.isEtc -> BookType.BOOK_ETC
            else -> BookType.BOOK_ALL
        }

        // 로그 추가
        Log.d("MainFragment", "onResume: selectedType = $selectedType")

        when (selectedType) {
            BookType.BOOK_LITERATURE -> fragmentMainBinding.bookGroupMain.check(R.id.buttonLiteratureMain)
            BookType.BOOK_HUMANITIES -> fragmentMainBinding.bookGroupMain.check(R.id.buttonHumanitiesMain)
            BookType.BOOK_NATURE -> fragmentMainBinding.bookGroupMain.check(R.id.buttonNatureMain)
            BookType.BOOK_ETC -> fragmentMainBinding.bookGroupMain.check(R.id.buttonEtcMain)
            else -> fragmentMainBinding.bookGroupMain.check(R.id.buttonAllMain)
        }

        refreshRecyclerView(selectedType)
    }

    fun settingFabMain(){
        fragmentMainBinding.fabMain.apply {
            setOnClickListener {
                mainActivity.replaceFragment(FragmentName.INPUT_FRAGMENT, true, true, null)
            }
        }
    }

    fun settingToolbar(){
        fragmentMainBinding.materialToolbar.apply {
            title = "책 정보 목록"
            isTitleCentered = true

            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menuLocation -> {
                        mainActivity.replaceFragment(FragmentName.LOCATION_FRAGMENT, true, true, null)
                    }

                    R.id.menuSearch -> {
                        mainActivity.replaceFragment(FragmentName.SEARCH_FRAGMENT, true, true, null)
                    }
                }
                true
            }
        }
    }

    fun setupSegmentButtonFilter() {
        fragmentMainBinding.bookGroupMain.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newSelectedType = when (checkedId) {
                    R.id.buttonAllMain -> BookType.BOOK_ALL
                    R.id.buttonLiteratureMain -> BookType.BOOK_LITERATURE
                    R.id.buttonHumanitiesMain -> BookType.BOOK_HUMANITIES
                    R.id.buttonNatureMain -> BookType.BOOK_NATURE
                    R.id.buttonEtcMain -> BookType.BOOK_ETC
                    else -> BookType.BOOK_ALL // 기본값
                }

                if (selectedType != newSelectedType) {
                    selectedType = newSelectedType
                    mainActivity.updateFilterState(selectedType)
                    Log.d("MainFragment", "setupSegmentButtonFilter: selectedType = $selectedType")
                    refreshRecyclerView(selectedType)
                }
            }
        }
    }

    fun settingRecyclerView() {
        if (fragmentMainBinding.recyclerViewMain.adapter == null) {
            fragmentMainBinding.recyclerViewMain.adapter = RecyclerViewAdapterMain()
        }
        fragmentMainBinding.recyclerViewMain.layoutManager = LinearLayoutManager(mainActivity)
        val deco = DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL)
        fragmentMainBinding.recyclerViewMain.addItemDecoration(deco)
    }

    fun refreshRecyclerView(bookType: BookType?) {
        CoroutineScope(Dispatchers.Main).launch {
            val updatedBookList = async(Dispatchers.IO) {
                if (bookType == BookType.BOOK_ALL || bookType == null) {
                    BookRepository.selectBookDataAll(mainActivity)
                } else {
                    BookRepository.selectBookDataByTypes(mainActivity, bookType)
                }
            }.await()

            Log.d("MainFragment", "refreshRecyclerView: bookList size = ${updatedBookList.size}, bookType = $bookType")

            bookList.clear()
            bookList.addAll(updatedBookList)
            fragmentMainBinding.recyclerViewMain.adapter?.notifyDataSetChanged()
        }
    }


    inner class RecyclerViewAdapterMain(): RecyclerView.Adapter<RecyclerViewAdapterMain.ViewHolderMain>(){
        inner class ViewHolderMain(var rowMainBinding: RowMainBinding) : RecyclerView.ViewHolder(rowMainBinding.root),OnClickListener{

            override fun onClick(v: View?) {
                if (bookList.isNotEmpty()){
                    val databundle = Bundle()
                    databundle.putInt("bookIdx", bookList[adapterPosition].bookIdx)
                    mainActivity.replaceFragment(FragmentName.SHOW_FRAGMENT, true, true, databundle)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMain {
            val rowMainBinding = RowMainBinding.inflate(layoutInflater, parent, false)
            val viewHolderMain = ViewHolderMain(rowMainBinding)

            rowMainBinding.root.setOnClickListener(viewHolderMain)

            return viewHolderMain
        }

        override fun getItemCount(): Int {
            return if (bookList.isNotEmpty()) {
                bookList.size // 데이터가 있을 때는 리스트 크기를 반환
            } else {
                1 // 데이터가 없을 때는 0을 반환
            }
        }

        override fun onBindViewHolder(holder: ViewHolderMain, position: Int) {
            if (bookList.isEmpty()) {
                // 데이터가 없는 경우 기본 메시지를 설정
                holder.rowMainBinding.textViewRowType.text = ""
                holder.rowMainBinding.textViewRowTitle.text = "등록된 책이 없습니다."
                holder.rowMainBinding.textViewRowCount.text = ""
                holder.rowMainBinding.textViewRowNeed.visibility = View.GONE
                // 이미지 아이콘 초기화
                holder.rowMainBinding.textViewRowTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            } else {
                val currentBook = bookList[position]

                // 데이터가 있는 경우 정보를 설정
                holder.rowMainBinding.textViewRowType.text = currentBook.bookType.str
                holder.rowMainBinding.textViewRowTitle.text = currentBook.bookTitle
                holder.rowMainBinding.textViewRowCount.text = "${currentBook.bookCount}권"

                // 이미지가 있는 경우 CompoundDrawable 설정
                if (currentBook.bookImage.isNotEmpty()) {
                    holder.rowMainBinding.textViewRowTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.imagesmode_24px, 0)
                } else {
                    // 이미지가 없는 경우 CompoundDrawable 초기화
                    holder.rowMainBinding.textViewRowTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                }

                // 책 수량에 따라 가시성 설정
                if (currentBook.bookCount <= 10) {
                    holder.rowMainBinding.textViewRowNeed.visibility = View.VISIBLE
                } else {
                    holder.rowMainBinding.textViewRowNeed.visibility = View.GONE
                }
            }
        }
    }
}