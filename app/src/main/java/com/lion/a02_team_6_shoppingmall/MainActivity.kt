package com.lion.a02_team_6_shoppingmall

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialSharedAxis
import com.lion.a02_team_6_shoppingmall.databinding.ActivityMainBinding
import com.lion.a02_team_6_shoppingmall.fragment.InputFragment
import com.lion.a02_team_6_shoppingmall.fragment.LocationFragment
import com.lion.a02_team_6_shoppingmall.fragment.MainFragment
import com.lion.a02_team_6_shoppingmall.fragment.ModifyFragment
import com.lion.a02_team_6_shoppingmall.fragment.SearchFragment
import com.lion.a02_team_6_shoppingmall.fragment.ShowFragment
import com.lion.a02_team_6_shoppingmall.util.BookType
import com.lion.a02_team_6_shoppingmall.util.FragmentName
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    lateinit var activityMainBinding: ActivityMainBinding

    // 권한 확인을 위한 런처
    lateinit var permissionCheckLauncher: ActivityResultLauncher<Array<String>>
    val permissionList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    } else {
        arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    var isAll = false
    var isLiterature = false
    var isHumanities = false
    var isNature = false
    var isEtc = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createPermissionCheckLauncher()
        permissionCheckLauncher.launch(permissionList)
        replaceFragment(FragmentName.MAIN_FRAGMENT, false, false, null)
    }

    // 권한 확인을 위해 사용할 런처 생성
    fun createPermissionCheckLauncher(){
        // 런처를 등록한다.
        permissionCheckLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            // Snackbar.make(activityMainBinding.root, "권한 확인 완료", Snackbar.LENGTH_LONG).show()
            // 모든 권한에 대해 확인한다.
            permissionList.forEach { permissionName ->
                // 현재 권한이 허용되어 있지 않다면 다이얼로그를 띄운다.
                if(it[permissionName] == false){
                    // 설정 화면을 띄우는 메서드를 호출한다.
                    startSettingActivity()
                    // 함수 종료
                    return@registerForActivityResult
                }
            }
        }
    }

    // 애플리케이션의 설정화면을 실행시키는 메서드
    fun startSettingActivity(){
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this@MainActivity)
        materialAlertDialogBuilder.setTitle("권한 확인 요청")
        materialAlertDialogBuilder.setMessage("권한을 모두 허용해줘야 정상적인 서비스 이용이 가능합니다")
        materialAlertDialogBuilder.setPositiveButton("권한 설정 하기"){ dialogInterface: DialogInterface, i: Int ->
            val uri = Uri.fromParts("package", packageName, null)
            val permissionIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
            permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(permissionIntent)
        }
        materialAlertDialogBuilder.show()
    }

    fun checkPermissions(): Boolean {
        // 권한 상태 확인
        val isPermissionGranted = permissionList.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (!isPermissionGranted) {
            // 권한이 하나라도 부족하면 재요청
            permissionCheckLauncher.launch(permissionList)
        }

        return isPermissionGranted
    }

    // 키보드 올리는 메서드
    fun showSoftInput(view: View){
        // 입력을 관리하는 매니저
        val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        // 포커스를 준다.
        view.requestFocus()

        thread {
            SystemClock.sleep(1000)
            // 키보드를 올린다.
            inputManager.showSoftInput(view, 0)
        }
    }

    // 키보드를 내리는 메서드
    fun hideSoftInput(){
        // 포커스가 있는 뷰가 있다면
        if(currentFocus != null){
            // 입력을 관리하는 매니저
            val inputManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            // 키보드를 내린다.
            inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
            // 포커스를 해제한다.
            currentFocus?.clearFocus()
        }
    }

    fun updateFilterState(newType: BookType?) {
        isLiterature = newType == BookType.BOOK_LITERATURE
        isHumanities = newType == BookType.BOOK_HUMANITIES
        isNature = newType == BookType.BOOK_NATURE
        isEtc = newType == BookType.BOOK_ETC
        isAll = newType == BookType.BOOK_ALL
    }

    fun replaceFragment(fragmentName: FragmentName, isAddToBackStack:Boolean, animate:Boolean, dataBundle: Bundle?){
        if (!checkPermissions()) return

        val newFragment = when(fragmentName){
            FragmentName.MAIN_FRAGMENT -> MainFragment()
            FragmentName.INPUT_FRAGMENT -> InputFragment()
            FragmentName.SHOW_FRAGMENT -> ShowFragment()
            FragmentName.MODIFY_FRAGMENT -> ModifyFragment()
            FragmentName.LOCATION_FRAGMENT -> LocationFragment()
            FragmentName.SEARCH_FRAGMENT -> SearchFragment()
        }

        if (dataBundle != null) {
            newFragment.arguments = dataBundle
        }

        supportFragmentManager.commit {
            newFragment.exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
            newFragment.reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)
            newFragment.enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ true)
            newFragment.returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, /* forward= */ false)

            replace(R.id.fragmentViewMain, newFragment)
            if (isAddToBackStack){
                addToBackStack(fragmentName.str)
            }
        }
    }

    fun removeFragment(fragmentName: FragmentName){
        supportFragmentManager.popBackStack(fragmentName.str, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}