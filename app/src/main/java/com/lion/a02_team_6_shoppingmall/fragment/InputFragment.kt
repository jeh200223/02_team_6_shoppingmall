package com.lion.a02_team_6_shoppingmall.fragment

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
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
import androidx.core.graphics.drawable.toBitmap
import com.lion.a02_team_6_shoppingmall.MainActivity
import com.lion.a02_team_6_shoppingmall.R
import com.lion.a02_team_6_shoppingmall.databinding.FragmentInputBinding
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

class InputFragment : Fragment() {
    lateinit var fragmentInputBinding: FragmentInputBinding
    lateinit var mainActivity: MainActivity
    lateinit var albumLauncher: ActivityResultLauncher<Intent>
    lateinit var basicCameraLauncher: ActivityResultLauncher<Intent>
    var selectedUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentInputBinding = FragmentInputBinding.inflate(layoutInflater)
        mainActivity = activity as MainActivity
        settingToolbar()
        createBasicCameraLauncher()
        createAlbumLauncher()
        settingImage()

        return fragmentInputBinding.root
    }

    fun settingToolbar(){
        fragmentInputBinding.materialToolbarInput.apply {
            title = "책 정보 등록"
            isTitleCentered = true

            setNavigationIcon(R.drawable.arrow_back_24px)
            setNavigationOnClickListener {
                mainActivity.removeFragment(FragmentName.INPUT_FRAGMENT)
            }

            setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.menuComplete -> {
                        inputDone()
                    }
                }
                true
            }
        }
    }

    // 이미지 변경
    fun settingImage(){
        fragmentInputBinding.apply {
            buttonImageCamera.setOnClickListener {
                val basicCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                basicCameraLauncher.launch(basicCameraIntent)
            }

            buttonImageGallery.setOnClickListener {
                val albumIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
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
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            }
        }
        return uri
    }

    fun createBasicCameraLauncher() {
        basicCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK && it.data != null) {
                val bitmap = it.data?.extras?.get("data") as? Bitmap
                bitmap?.let { bmp ->
                    // 갤러리에 저장된 이미지의 URI를 바로 selectedUri에 저장
                    selectedUri = saveImageToGallery(bmp, mainActivity)
                    fragmentInputBinding.imageViewBook.setImageBitmap(bmp) // 이미지뷰에 표시
                }
            }
        }
    }

    // 런처를 생성하는 메서드
    fun createAlbumLauncher(){
        albumLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == AppCompatActivity.RESULT_OK && it.data != null){
                it.data?.data?.let { uri ->
                    // 선택된 이미지 Uri를 저장
                    selectedUri = uri
                    // android 10 버전 이상이라면
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        val source = ImageDecoder.createSource(mainActivity.contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source)
                        fragmentInputBinding.imageViewBook.setImageBitmap(bitmap)
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
                                fragmentInputBinding.imageViewBook.setImageBitmap(bitmap)
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
            val contentResolver:ContentResolver = context.contentResolver
            // 주어진 URI에 해당하는 이미지 파일을 입력 스트림으로 열고 URI를 통해 파일을 열 수 없으면 null을 반환합니다.
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap:Bitmap = BitmapFactory.decodeStream(inputStream)
            // 내부 저장소에 이미지를 저장할 파일을 생성합니다.
            val file = File(context.filesDir, "book_image_${System.currentTimeMillis()}.jpg")
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

    fun inputDone() {
        fragmentInputBinding.apply {
            val bookType = when (bookGroup.checkedButtonId) {
                R.id.buttonLiterature -> BookType.BOOK_LITERATURE
                R.id.buttonHumanities -> BookType.BOOK_HUMANITIES
                R.id.buttonNature -> BookType.BOOK_NATURE
                else -> BookType.BOOK_ETC
            }
            // 데이터 저장 및 Fragment 전환
            val bookTitle = textFieldTitle.editText?.text.toString()
            val bookName = textFieldName.editText?.text.toString()
            val bookCount = textFieldCount.editText?.text.toString().toInt()

            val bookImageUri = selectedUri?.let { uri ->
                copyImageToInternalStorage(uri, mainActivity)?.toString() ?: ""
            } ?: ""

            val bookViewModel = BookViewModel(0, bookType, bookTitle, bookName, bookCount, bookImageUri)

            Log.d("InputFragment", "inputDone() called: $bookTitle, $bookName, $bookCount, $bookImageUri")

            CoroutineScope(Dispatchers.Main).launch {
                val work1 = async(Dispatchers.IO) {
                    BookRepository.insertBookData(mainActivity, bookViewModel)
//                    BookRepository.InsertOrUpdateBookData(mainActivity, bookViewModel)
                }
                work1.await()

                mainActivity.updateFilterState(bookType)
                Log.d("InputFragment", "inputDone: Book registered with type=$bookType")

                // MainFragment로 이동
                mainActivity.removeFragment(FragmentName.INPUT_FRAGMENT)
            }
        }
    }
}