package activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.Toast
import com.km.deodeumi.R
import kotlinx.android.synthetic.main.activity_my_location.*
import mapapi.MapApiConst
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapReverseGeoCoder
import net.daum.mf.map.api.MapView


class MyLocationActivity : AppCompatActivity(),MapView.CurrentLocationEventListener,MapReverseGeoCoder.ReverseGeoCodingResultListener {

    // var : 읽기/쓰기 가능한 일반 변수 val : 읽기만 가능한 final 변수
    private val GPS_ENABLE_REQUEST_CODE: Int = 200
    private val PERMISSIONS_REQUEST_CODE: Int = 100
    private var REQUIRED_PERMISSIONS = arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private lateinit var mapView: MapView
    private lateinit var reverseGeoCoder: MapReverseGeoCoder //? = null


    //private var txt_location = findViewById<TextView>(R.id.txt_my_location)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_location)
        val mapViewContainer = findViewById<RelativeLayout>(R.id.mapView)
        mapView = MapView(this)
        mapViewContainer.addView(mapView)
        mapView.setCurrentLocationEventListener(this)


        txt_my_location.text = "현재 내 위치: "

        if(!checkLocationServiceStatus()){
            showDialogForLocationServiceSetting()
        }else{
            checkRunTimePermission()
        }

    }

    @Override
    override fun onDestroy() {
        super.onDestroy()
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
        mapView.setShowCurrentLocationMarker(false)
    }

    @Override
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size){
            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            var check_result : Boolean = false
            for(index in 0..grantResults.size){
                if(index != PackageManager.PERMISSION_GRANTED){
                    check_result = true
                    break
                }
            }

            if(check_result){
                mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading
            }else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])){
                    Toast.makeText(this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }

        }
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            GPS_ENABLE_REQUEST_CODE -> {
                if(checkLocationServiceStatus()){
                    checkRunTimePermission()
                    return
                }
            }

        }
    }

    // MapView.CurrentLocationEventListener
    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {
        val mapPointGeo : MapPoint.GeoCoordinate = p1!!.mapPointGeoCoord
//        기존 코드 : mapView.mapCenterPoint(서울시 중구로 셋팅됨)
        var presentPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude)
        reverseGeoCoder = MapReverseGeoCoder(MapApiConst.DAUM_MAPS_ANDROID_APP_API_KEY,presentPoint,this, this)
        reverseGeoCoder.startFindingAddress()
        mapView.setCurrentLocationRadius(50)
        mapView.setCurrentLocationRadiusFillColor(android.graphics.Color.argb(128,255,203,203))
        mapView.setCurrentLocationRadiusStrokeColor(android.graphics.Color.argb(128,255,203,203))
        Log.i("MyLocationActivity", String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, p2))
    }

    override fun onCurrentLocationUpdateFailed(p0: MapView?) {

    }

    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {

    }

    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {

    }


    //MapReverseGeoCoder.ReverseGeoCodingResultListener
    override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {

    }

    @SuppressLint("SetTextI18n")
    override fun onReverseGeoCoderFoundAddress(p0: MapReverseGeoCoder?, p1: String?) {
        //txt_my_location.text = "${txt_my_location.text}$p1"
        var short_address: String = p1!!.substring(0,11) //어떤 기준으로 자를 것인지?
        Log.i("현재 주소",p1)
        if (txt_my_location.text.length == 9){
            txt_my_location.text = "${txt_my_location.text}$short_address"
        }
    }


    private fun checkLocationServiceStatus():Boolean {
        var manager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    //GPS 활성화를 위한 메소드
    private fun showDialogForLocationServiceSetting(){
        val builder = AlertDialog.Builder(this@MyLocationActivity)
        builder.setTitle("위치 서비스 비활성화")
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하시겠습니까?")
        builder.setCancelable(true)
        builder.setPositiveButton("설정") { dialog, id ->
            val callGPSSettingIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE)
        }
        builder.setNegativeButton("취소") { dialog, id -> dialog.cancel() }
        builder.create().show()
    }

    //위치 퍼미션을 갖고 있는지 체크
    private fun checkRunTimePermission(){
        // 1. 위치 퍼미션을 가지고 있나
        var hasFineLocationPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        // 2. 이미 퍼미션을 가지고 있다면
        // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)
        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED){
            // 3. 위치값 가져오기
            mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading
        }else{ // 4. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[0])){
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(this,"이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE)
            }else{
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE)
            }

        }
    }

}


