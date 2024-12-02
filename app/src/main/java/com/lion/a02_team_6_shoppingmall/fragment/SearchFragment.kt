package com.lion.a02_team_6_shoppingmall.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lion.a02_team_6_shoppingmall.MainActivity
import com.lion.a02_team_6_shoppingmall.R
import com.lion.a02_team_6_shoppingmall.databinding.FragmentSearchBinding
import com.lion.a02_team_6_shoppingmall.databinding.RowMainBinding
import com.lion.a02_team_6_shoppingmall.repository.BookRepository
import com.lion.a02_team_6_shoppingmall.util.BookType
import com.lion.a02_team_6_shoppingmall.util.FragmentName
import com.lion.a02_team_6_shoppingmall.viewmodel.BookViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    lateinit var fragmentSearchBinding: FragmentSearchBinding
    lateinit var mainActivity: MainActivity
    var searchBookList = mutableListOf<BookViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentSearchBinding = FragmentSearchBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity
        settingToolbar()
        settingRecyclerView()
        settingTextField()
        return fragmentSearchBinding.root
    }

    fun settingToolbar() {
        fragmentSearchBinding.materialToolbarSearch.apply {
            setNavigationIcon(R.drawable.arrow_back_24px)
            setNavigationOnClickListener {
                mainActivity.removeFragment(FragmentName.SEARCH_FRAGMENT)
            }
        }
    }

    fun settingRecyclerView() {
        fragmentSearchBinding.apply {
            recyclerSearch.adapter = RecyclerViewAdapterSearch()
            recyclerSearch.layoutManager = LinearLayoutManager(mainActivity)
            val deco = DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL)
            recyclerSearch.addItemDecoration(deco)
        }
    }

    inner class RecyclerViewAdapterSearch() : RecyclerView.Adapter<RecyclerViewAdapterSearch.ViewHolderSearch>() {
        inner class ViewHolderSearch(var rowMainBinding: RowMainBinding) : RecyclerView.ViewHolder(rowMainBinding.root), OnClickListener, OnLongClickListener {
            var isLongClick = false

            override fun onClick(v: View?) {
                isLongClick = false
                val databundle = Bundle()
                databundle.putInt("bookIdx", searchBookList[adapterPosition].bookIdx)
                mainActivity.replaceFragment(FragmentName.SHOW_FRAGMENT, true, true, databundle)
            }

            override fun onLongClick(v: View?): Boolean {
                if (isLongClick == false) {
                    isLongClick = true
                    CoroutineScope(Dispatchers.Main).launch {
                        val work1 = async(Dispatchers.IO) {
                            BookRepository.selectBookDataByIdx(
                                mainActivity,
                                searchBookList[adapterPosition].bookIdx
                            )
                        }
                        val bookViewModel = work1.await()

                        bookViewModel.bookImage?.let { uriString ->
                            val uri = android.net.Uri.parse(uriString)
                            rowMainBinding.imageViewRow.setImageURI(uri)
                        }
                    }
                } else {
                    isLongClick = false
                    rowMainBinding.imageViewRow.setImageResource(android.R.color.transparent)
                }
                return true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSearch {
            val rowMainBinding = RowMainBinding.inflate(layoutInflater, parent, false)
            val viewHolderMain = ViewHolderSearch(rowMainBinding)

            rowMainBinding.root.setOnClickListener(viewHolderMain)
            rowMainBinding.root.setOnLongClickListener(viewHolderMain)

            return viewHolderMain
        }

        override fun getItemCount(): Int {
            return searchBookList.size
        }

        override fun onBindViewHolder(holder: ViewHolderSearch, position: Int) {
            holder.rowMainBinding.textViewRowType.text = searchBookList[position].bookType.str
            holder.rowMainBinding.textViewRowTitle.text = searchBookList[position].bookTitle
            holder.rowMainBinding.textViewRowCount.text = "${searchBookList[position].bookCount}권"
            if (searchBookList[position].bookCount <= 10){
                holder.rowMainBinding.textViewRowNeed.visibility = View.VISIBLE
            } else {
                holder.rowMainBinding.textViewRowNeed.visibility = View.GONE
            }
        }
    }

    // 입력 요소 설정
    fun settingTextField(){
        fragmentSearchBinding.apply {
            // 검색창에 포커스를 준다.
            mainActivity.showSoftInput(textFieldSearch.editText!!)

            // 키보드의 엔터를 누르면 동작하는 리스너
            textFieldSearch.editText?.setOnEditorActionListener { v, actionId, event ->
                // 검색 데이터를 가져와 보여준다.
                CoroutineScope(Dispatchers.Main).launch {
                    val work1 = async(Dispatchers.IO){
                        val searchBookTitle = textFieldSearch.editText?.text.toString()
                        BookRepository.selectBookDataByTitle(mainActivity, searchBookTitle)
                    }
                    searchBookList = work1.await()
                    recyclerSearch.adapter?.notifyDataSetChanged()
                }
                mainActivity.hideSoftInput()
                true
            }
        }
    }
}