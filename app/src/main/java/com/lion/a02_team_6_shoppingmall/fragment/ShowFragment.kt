package com.lion.a02_team_6_shoppingmall.fragment

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lion.a02_team_6_shoppingmall.MainActivity
import com.lion.a02_team_6_shoppingmall.R
import com.lion.a02_team_6_shoppingmall.databinding.FragmentShowBinding
import com.lion.a02_team_6_shoppingmall.repository.BookRepository
import com.lion.a02_team_6_shoppingmall.util.FragmentName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ShowFragment : Fragment() {
    lateinit var fragmentShowBinding: FragmentShowBinding
    lateinit var mainActivity: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentShowBinding = FragmentShowBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity
        settingToolbar()
        settingDataInfo()
        return fragmentShowBinding.root
    }

    fun settingToolbar(){
        fragmentShowBinding.materialToolbarShow.apply {
            title = "책 정보 보기"
            isTitleCentered = true

            setNavigationIcon(R.drawable.arrow_back_24px)
            setNavigationOnClickListener {
                mainActivity.removeFragment(FragmentName.SHOW_FRAGMENT)
            }

            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menuDelete -> {
                        deleteDone()
                    }
                    R.id.menuModify -> {
                        val dataBundle = Bundle()
                        dataBundle.putInt("bookIdx", arguments?.getInt("bookIdx")!!)
                        mainActivity.replaceFragment(FragmentName.MODIFY_FRAGMENT, true, true, dataBundle)
                    }
                }
                true
            }
        }
    }

    fun settingDataInfo(){
        fragmentShowBinding.textViewType.editText?.setText("")
        fragmentShowBinding.textViewName.editText?.setText("")
        fragmentShowBinding.textViewCount.editText?.setText("")
        fragmentShowBinding.textViewTitle.editText?.setText("")
        fragmentShowBinding.imageViewShow.setImageURI(null)

        val bookIdx = arguments?.getInt("bookIdx")

        CoroutineScope(Dispatchers.Main).launch {
            val work1 = async(Dispatchers.IO){
                BookRepository.selectBookDataByIdx(mainActivity, bookIdx!!)
            }
            val bookViewModel = work1.await()

            fragmentShowBinding.textViewType.editText?.setText(bookViewModel.bookType.str)
            fragmentShowBinding.textViewName.editText?.setText(bookViewModel.bookName)
            fragmentShowBinding.textViewCount.editText?.setText("${bookViewModel.bookCount}권")
            fragmentShowBinding.textViewTitle.editText?.setText(bookViewModel.bookTitle)
            bookViewModel.bookImage.let { uriString ->
                if (uriString.isNullOrEmpty()){
                    fragmentShowBinding.imageViewShow.setImageResource(R.drawable.empty_file)
                } else {
                    val uri = Uri.parse(uriString)
                    fragmentShowBinding.imageViewShow.setImageURI(uri)
                }
            }
        }
    }

    fun deleteDone(){
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(mainActivity)
        materialAlertDialogBuilder.setTitle("책 정보 삭제")
        materialAlertDialogBuilder.setMessage("삭제를 하실경우 정보 복구가 불가능합니다\n" +
                "삭제 하시겠습니까?")
        materialAlertDialogBuilder.setNeutralButton("취소", null)
        materialAlertDialogBuilder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
            CoroutineScope(Dispatchers.Main).launch {
                val work1 = async(Dispatchers.IO){
                    val bookIdx = arguments?.getInt("bookIdx")
                    BookRepository.deleteBookData(mainActivity, bookIdx!!)
                }
                work1.join()
                mainActivity.removeFragment(FragmentName.SHOW_FRAGMENT)
            }
        }
        materialAlertDialogBuilder.show()
    }
}