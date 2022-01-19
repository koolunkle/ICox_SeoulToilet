package com.icox.seoultoilet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

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

    // 서울 열린 데이터 광장에서 발급받은 API 키를 입력
    val API_KEY = "45546b6f776a756e36355271475871"

    // 앱이 비활성화될 때 백그라운드 작업도 취소하기 위한 변수 선언
    var task: ToiletReadTask? = null

    // 서울시 화장실 정보 집합을 저장할 Array 변수 -> 검색을 위해 저장
    var toilets = JSONArray()

    // 화장실 이미지로 사용할 Bitmap
    val bitmap by lazy {
        val drawable = resources.getDrawable(R.drawable.restroom_sign) as BitmapDrawable
        Bitmap.createScaledBitmap(drawable.bitmap, 64, 64, false)
    }

    // JSONArray 를 병합하기 위한 확장 함수
    fun JSONArray.merge(anotherArray: JSONArray) {
        for (i in 0 until anotherArray.length()) {
            this.put(anotherArray.get(i))
        }
    }

    // 화장실 정보를 읽어와 JSONObject 로 반환하는 함수
    fun readData(startIndex: Int, lastIndex: Int): JSONObject {
        val url =
            URL("http://openAPI.seoul.go.kr:8088/${API_KEY}/json/SearchPublicToiletPOIService/${startIndex}/${lastIndex}/")
        val connection = url.openConnection()
        val data = connection.getInputStream().readBytes().toString(charset("UTF-8"))
        return JSONObject(data)
    }

    // 화장실 데이터를 읽어오는 AsyncTask
    inner class ToiletReadTask : AsyncTask<Void, JSONArray, String>() {
        // 데이터를 읽기 전에 기존 데이터 초기화
        override fun onPreExecute() {
//        Google Maps Marker 초기화
            googleMap?.clear()
//        화장실 정보 초기화
            toilets = JSONArray()
        }

        override fun doInBackground(vararg params: Void?): String {
//            서울시 데이터는 최대 1000개씩 가져올 수 있다
//            step 만큼 startIndex 와 lastIndex 값을 변경하며 여러번 호출해야 함
            val step = 1000
            var startIndex = 1
            var lastIndex = step
            var totalCount = 0

            do {
//                백그라운드 작업이 취소된 경우 루프를 빠져나간다
                if (isCancelled) break

//                totalCount 가 0이 아닌 경우 최초 실행이 아니므로 step 만큼 startIndex, lastIndex 를 증가
                if (totalCount != 0) {
                    startIndex += step
                    lastIndex += step
                }

//                startIndex, lastIndex 로 데이터 조회
                val jsonObject = readData(startIndex, lastIndex)

//                totalCount 를 가져온다
                totalCount = jsonObject.getJSONObject("SearchPublicToiletPOIService")
                    .getInt("list_total_count")

//                화장실 정보 데이터 집합을 가져온다
                val rows =
                    jsonObject.getJSONObject("SearchPublicToiletPOIService").getJSONArray("row")

//                기존에 읽은 데이터와 병합
                toilets.merge(rows)

//                UI 업데이트를 위해 progress 발행
                publishProgress(rows)
            }

//            lastIndex 가 총 개수보다 적으면 반복한다
            while (lastIndex < totalCount)
            return "complete"
        }

        // 데이터를 읽어올 때마다 중간중간 실행
        override fun onProgressUpdate(vararg values: JSONArray?) {
//            vararg 는 JSONArray 파라미터를 가변적으로 전달하도록 하는 키워드
//            Index 0의 데이터를 사용
            val array = values[0]
            array?.let {
                for (i in 0 until array.length()) {
//                    Marker 추가
                    addMarkers(array.getJSONObject(i))
                }
            }
        }
    }

    // 앱이 활성화될 때 서울시 데이터를 읽어옴
    override fun onStart() {
        super.onStart()
        task?.cancel(true)
        task = ToiletReadTask()
        task?.execute()
    }

    // 앱이 비활성화될 때 백그라운드 작업 취소
    override fun onStop() {
        super.onStop()
        task?.cancel(true)
        task = null
    }

    // Marker 를 추가하는 함수
    fun addMarkers(toilet: JSONObject) {
        googleMap?.addMarker(
            MarkerOptions().position(
                LatLng(
                    toilet.getDouble("Y_WGS84"),
                    toilet.getDouble("X_WGS84")
                )
            ).title(toilet.getString("FNAME")).snippet(toilet.getString("ANAME"))
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        )
    }

}
