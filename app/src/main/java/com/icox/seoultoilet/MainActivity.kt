package com.icox.seoultoilet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    // 런타임에서 권한이 필요한 Permission 목록
    val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Permission 승인 요청 시 사용하는 요청 코드
    val REQUEST_PERMISSION_CODE = 1

    // 기본 Google Maps Zoom Level
    val DEFAULT_ZOOM_LEVEL = 17f

    // 현재 위치를 가져올 수 없는 경우 서울시청 위치로 지도를 보여준다
    // LatLng 클래스는 위도/경도 좌표를 정의
    val CITY_HALL = LatLng(37.5662952, 126.97794509999994)

    // Google Maps 객체를 참조할 멤버 변수
    var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        MapView 에 onCreate 함수 호출
        mapView.onCreate(savedInstanceState)

//        앱이 실행될 때 런타임에서 위치 서비스 관련 권한 체크
        if (hasPermissions()) {
//            권한이 있는 경우 Map 초기화
            initMap()
        } else {
//            권한 요청
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_CODE)
        }

//        현재 위치 버튼 클릭 이벤트 Listener 설정
        myLocationButton.setOnClickListener {
            onMyLocationButtonClick()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//            Map 초기화
        initMap()
    }

    // 앱에서 사용하는 권한이 있는지 체크하는 함수
    fun hasPermissions(): Boolean {
//            Permission 목록 중 하나라도 권한이 없으면 false 변환
        for (permission in PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    // Map 초기화하는 함수
    @SuppressLint("MissingPermission")
    fun initMap() {
//            MapView 에서 Google Maps 불러오는 함수 -> 콜백 함수에서 Google Maps 객체가 전달됨
        mapView.getMapAsync {
//                Google Maps 멤버 변수에 Google Maps 객체 저장
            googleMap = it
//                현재 위치로 이동 버튼 비활성화
            it.uiSettings.isMyLocationButtonEnabled = false
//                위치 사용 권한이 있는 경우
            when {
                hasPermissions() -> {
//                        현재 위치 표시 활성화
                    it.isMyLocationEnabled = true
//                        현재 위치로 카메라 이동
                    it.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            getMyLocation(),
                            DEFAULT_ZOOM_LEVEL
                        )
                    )
                }
                else -> {
//                        권한이 없으면 서울시청 위치로 이동
                    it.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            CITY_HALL,
                            DEFAULT_ZOOM_LEVEL
                        )
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getMyLocation(): LatLng {
//            위치를 측정하는 Provider 를 GPS 센서로 지정
        val locationProvider: String = LocationManager.GPS_PROVIDER

//            위치 서비스 객체를 불러옴
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

//            마지막으로 업데이트 된 위치를 가져옴
        val lastKnownLocation: Location? =
            locationManager.getLastKnownLocation(locationProvider)

        if (lastKnownLocation != null) {
//                위도/경도 객체로 반환
            return LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
        } else {
//                위치를 구하지 못한 경우 기본 값 반환
            return CITY_HALL
        }
    }

    // 현재 위치 버튼 클릭한 경우
    fun onMyLocationButtonClick() {
        when {
            hasPermissions() -> googleMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    getMyLocation(),
                    DEFAULT_ZOOM_LEVEL
                )
            )
            else -> Toast.makeText(
                applicationContext,
                "위치 사용 권한 설정에 동의해주세요",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 하단부터 MapView 의 Lifecycle 함수 호출을 위한 코드
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}
