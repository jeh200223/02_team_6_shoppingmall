package com.lion.a02_team_6_shoppingmall.fragment

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lion.a02_team_6_shoppingmall.MainActivity
import com.lion.a02_team_6_shoppingmall.R
import com.lion.a02_team_6_shoppingmall.databinding.FragmentModifyBinding
import com.lion.a02_team_6_shoppingmall.repository.BookRepository
import com.lion.a02_team_6_shoppingmall.util.BookType
import com.lion.a02_team_6_shoppingmall.util.FragmentName
import com.lion.a02_team_6_shoppingmall.viewmodel.BookViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ModifyFragment : Fragment() {
    lateinit var fragmentModifyBinding: FragmentModifyBinding
    lateinit var mainActivity: MainActivity
    lateinit var albumLauncher: ActivityResultLauncher<Intent>
    lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    var selectedUri:Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentModifyBinding = FragmentModifyBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity
        createAlbumLauncher()
        createCameraLauncher()
        settingToolbar()
        settingBasicInfo()
        settingModifyImage()
        return fragmentModifyBinding.root
    }

    fun settingToolbar(){
        fragmentModifyBinding.materialToolbarModify.apply {
            title = "책 정보 수정"
            isTitleCentered = true

            setNavigationIcon(R.drawable.arrow_back_24px)
            setNavigationOnClickListener {
                mainActivity.removeFragment(FragmentName.MODIFY_FRAGMENT)
            }

            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menuComplete -> {
                        modifyDone()
                    }
                }
                true
            }
        }
    }

    fun settingBasicInfo(){
        fragmentModifyBinding.apply {
            val bookIdx = arguments?.getInt("bookIdx")

            CoroutineScope(Dispatchers.Main).launch {
                val work1 = async(Dispatchers.IO){
                    BookRepository.selectBookDataByIdx(mainActivity, bookIdx!!)
                }
                val bookViewModel = work1.await()

                when(bookViewModel.bookType){
                    BookType.BOOK_LITERATURE -> bookGroupModify.check(R.id.buttonLiteratureModify)
                    BookType.BOOK_HUMANITIES -> bookGroupModify.check(R.id.buttonHumanitiesModify)
                    BookType.BOOK_NATURE -> bookGroupModify.check(R.id.buttonNatureModify)
                    BookType.BOOK_ETC -> bookGroupModify.check(R.id.buttonEtcModify)
                    else -> {}
                }

                textFieldTitleModify.editText?.setText(bookViewModel.bookTitle)
                textFieldNameModify.editText?.setText(bookViewModel.bookName)
                textFieldCountModify.editText?.setText(bookViewModel.bookCount.toString())

                // 내부 저장소 URI 설정
                selectedUri = bookViewModel.bookImage.toUri()
                // 저장된 URI가 없으면
                if (selectedUri.toString() == "") {
                    // 기본 이미지 설정
                    imageViewBookModify.setImageResource(R.drawable.empty_file)
                } else {
                    imageViewBookModify.setImageURI(selectedUri)
                }
            }
        }
    }

    // 이미지 변경
    fun settingModifyImage(){
        fragmentModifyBinding.apply {
            buttonImageModifyCamera.setOnClickListener {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraLauncher.launch(cameraIntent)
            }
            buttonImageModifyGallery.setOnClickListener {
                val albumIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                        // 이미지 타입을 설정한다.
                        type = "image/*"
                        // 선택할 파일의 타입을 지정(안드로이드 OS가 사전 작업을 할 수 있도록)
                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
                    }
                // 액티비티 실행
                albumLauncher.launch(albumIntent)
            }
        }
    }

    fun saveImageToGallery(bitmap: Bitmap, context: Context): Uri? {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/MyApp")
        }

        val contentResolver = context.contentResolver
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.apply {
            contentResolver.openOutputStream(this)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
    }

    fun createCameraLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                val bitmap = it.data?.extras?.get("data") as? Bitmap
                bitmap?.let { bmp ->
                    val galleryUri = saveImageToGallery(bmp, mainActivity)
                    galleryUri?.let {
                        selectedUri = copyImageToInternalStorage(it, mainActivity)
                        fragmentModifyBinding.imageViewBookModify.setImageBitmap(bmp)
                    }
                }
            }
        }
    }

    // 런처를 생성하는 메서드
    fun createAlbumLauncher(){
        albumLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if (it.resultCode == AppCompatActivity.RESULT_OK && it.data != null){
                it.data?.data?.let { uri ->
                    // 선택된 이미지 Uri를 저장
                    selectedUri = uri
                    // android 10 버전 이상이라면
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val source = ImageDecoder.createSource(mainActivity.contentResolver, uri)
                        val bitmap: Bitmap = ImageDecoder.decodeBitmap(source)
                        fragmentModifyBinding.imageViewBookModify.setImageBitmap(bitmap)
                    } else {
                        // ContentProvider를 통해 사진 데이터를 가져온다.
                        val cursor = mainActivity.contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()){
                                // 이미지의 경로를 가져온다.
                                val idx = it.getColumnIndex(MediaStore.Images.Media.DATA)
                                val path = it.getString(idx)
                                // 이미지 경로를 Uri로 변환
                                selectedUri = Uri.parse(path)
                                // 이미지를 생성한다.
                                val bitmap = BitmapFactory.decodeFile(path)
                                fragmentModifyBinding.imageViewBookModify.setImageBitmap(bitmap)
                            }
                        }
                    }
                }
            }
        }
    }

    // 외부 저장소의 이미지를 내부 저장소로 복사하는 메서드
    fun copyImageToInternalStorage(uri: Uri, context: Context):Uri?{
        try {
            // 외부 저장소의 파일에 접근
            val contentResolver: ContentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            // 내부 저장소에 이미지를 저장할 파일을 생성합니다.
            val bitmap:Bitmap = BitmapFactory.decodeStream(inputStream)
            val file = File(context.filesDir, "modified_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            // 비트맵을 JPEG 형식으로 압축하여 출력 스트림에 저장
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            // 저장된 파일의 URI를 반환합니다. 이를 통해 내부 저장소에 저장된 파일을 참조할 수 있습니다.
            return Uri.fromFile(file)
        } catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }

    fun modifyDone(){
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(mainActivity)
        materialAlertDialogBuilder.setTitle("책 정보 수정")
        materialAlertDialogBuilder.setMessage("수정하면 이전 정보로 복원 할 수 없습니다.\n" +
                "수정하시겠습니까?")
        materialAlertDialogBuilder.setNeutralButton("취소", null)
        materialAlertDialogBuilder.setPositiveButton("수정"){ dialogInterface: DialogInterface, i: Int ->
            val bookIdx = arguments?.getInt("bookIdx")!!
            val bookType = when (fragmentModifyBinding.bookGroupModify.checkedButtonId) {
                R.id.buttonLiteratureModify -> BookType.BOOK_LITERATURE
                R.id.buttonHumanitiesModify -> BookType.BOOK_HUMANITIES
                R.id.buttonNatureModify -> BookType.BOOK_NATURE
                else -> BookType.BOOK_ETC
            }

            mainActivity.updateFilterState(bookType)
            val bookTitle = fragmentModifyBinding.textFieldTitleModify.editText?.text.toString()
            val bookName = fragmentModifyBinding.textFieldNameModify.editText?.text.toString()
            val bookCount = fragmentModifyBinding.textFieldCountModify.editText?.text.toString().toInt()

            // 내부 저장소에 이미지 복사 및 URI 설정
            val bookImage = selectedUri?.let { uri ->
                copyImageToInternalStorage(uri, mainActivity)?.toString() ?: ""
                // 선택된 이미지가 없으면 빈 문자열 처리
            } ?: ""

            val bookViewModel = BookViewModel(bookIdx, bookType, bookTitle, bookName, bookCount, bookImage)

            CoroutineScope(Dispatchers.Main).launch {
                val work1 = async(Dispatchers.IO){
                    BookRepository.modifyBookData(mainActivity, bookViewModel)
                }
                work1.join()
                mainActivity.removeFragment(FragmentName.MODIFY_FRAGMENT)
            }
        }
        materialAlertDialogBuilder.show()
    }
}