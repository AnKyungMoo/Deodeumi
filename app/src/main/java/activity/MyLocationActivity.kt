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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_my_location.*
import mapapi.FootfallPaths
import mapapi.MapApiConst
import net.daum.mf.map.api.*
import net.daum.mf.map.n.api.internal.NativeMapLocationManager.getCurrentLocationTrackingMode
import net.daum.mf.map.n.api.internal.NativeMapLocationManager.setCurrentLocationTrackingMode
import service.LocationService


class MyLocationActivity : AppCompatActivity(),MapView.CurrentLocationEventListener
    ,MapReverseGeoCoder.ReverseGeoCodingResultListener,MapView.MapViewEventListener {


    // var : 읽기/쓰기 가능한 일반 변수 val : 읽기만 가능한 final 변수
    private val GPS_ENABLE_REQUEST_CODE: Int = 200
    private val PERMISSIONS_REQUEST_CODE: Int = 100
    private var REQUIRED_PERMISSIONS = arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private lateinit var mapView: MapView
    private lateinit var reverseGeoCoder: MapReverseGeoCoder //? = null
    private lateinit var mapPointGeo : MapPoint.GeoCoordinate
    private lateinit var polyline : MapPolyline
    private var subscription: Disposable? = null //retrofit

    private lateinit var res1 :  Array<String>
    private lateinit var res2 :  Array<String>

    private var pathList = arrayListOf<FootfallPaths>(
        FootfallPaths("ic_launcher_background","덕수궁 운현궁","10걸음"),
        FootfallPaths("ic_launcher_background","덕수궁 운현궁","10걸음")
    ) //경로 리스트

    private var myLocationString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_location)
        val mapViewContainer = findViewById<RelativeLayout>(R.id.mapView)
//        val pathListView = findViewById<ListView>(R.id.list_view_location)
        mapView = MapView(this)
        mapViewContainer.addView(mapView)
        mapView.setCurrentLocationEventListener(this)
        mapView.setMapViewEventListener(this)

//        val dogAdapter = PathAdapter(this, pathList)
//        pathListView.adapter = dogAdapter


        val inflaterView = findViewById<RelativeLayout>(R.id.layout_path)
        var inflater = LayoutInflater.from(this)
        val s = inflater.inflate(R.layout.layout_path_template,inflaterView,false)

        inflaterView.addView(s)


        // 현재 위치 초기화
        myLocationString = intent.getStringExtra("myLocationString")

        if (myLocationString == null || myLocationString.equals("")) {
            txt_my_location.text = "현재 내 위치: "
        } else {
            txt_my_location.text = "현재 내 위치: ".plus(myLocationString)
        }


        if(!checkLocationServiceStatus()){
            showDialogForLocationServiceSetting()
        }else{
            checkRunTimePermission()
        }

        /*
        * 나침반 모드 설정
        * MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeadingWithoutMapMoving
        * -> 이게 3번인거 같은데 왜 되는지 잘..
        * */
        setCurrentLocationTrackingMode(3)

        // 위치 검색으로 이동
        layout_search.setOnClickListener {
            val i = Intent(this, LocationSearchActivity::class.java)
            startActivity(i)
            finish()
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
//        val mapPointGeo : MapPoint.GeoCoordinate = p1!!.mapPointGeoCoord
        mapPointGeo = p1!!.mapPointGeoCoord
//        기존 코드 : mapView.mapCenterPoint(서울시 중구로 셋팅됨)
        var presentPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude)
        Log.i("latitude1111: ",mapPointGeo.latitude.toString())
        Log.i("longitude1111: ",mapPointGeo.longitude.toString())
        reverseGeoCoder = MapReverseGeoCoder(MapApiConst.DAUM_MAPS_ANDROID_APP_API_KEY,presentPoint,this, this)
        reverseGeoCoder.startFindingAddress()
        mapView.setCurrentLocationRadius(50)
        mapView.setCurrentLocationRadiusFillColor(android.graphics.Color.argb(128,255,203,203))
        mapView.setCurrentLocationRadiusStrokeColor(android.graphics.Color.argb(128,255,203,203))
        //Log.i("MyLocationActivity", String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, p2))
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
        val builder = AlertDialog.Builder(this)
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

    // WGS84 -> WTM
    private fun locationConverter(x: String,y: String,input_coord: String,output_coord: String): Array<String?> {

        val res = arrayOfNulls<String>(2)
        var x1 = ""
        var y1 = ""

        subscription = LocationService.distanceRestAPI().distanceConverter(x,y,input_coord,output_coord)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.d("WTM_X", result.documents[0].x)
                    Log.d("WTM_Y", result.documents[0].y)
                    x1 = result.documents[0].x
                    y1 = result.documents[0].y
                },
                { err ->
                    Log.e("Error User",err.toString())
                }
            )

        Log.i("test1", x1+","+y1)

        return res
    }

    // calculator distance
    private fun calculatorDistance(x1: String, y1:String, x2:String, y2:String): Double {

        var x_distance = Math.pow(Math.abs(x1.toInt() - x2.toInt()).toDouble(), 2.0) //Math.abs(x1.toInt() - x2.toInt())
        var y_distance = Math.pow(Math.abs(y1.toInt() - y2.toInt()).toDouble(), 2.0) //Math.abs(y1.toInt() - y2.toInt())

        return Math.sqrt(x_distance+y_distance)
    }

    // MapView.MapViewEventListener
    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {

    }

    override fun onMapViewInitialized(p0: MapView?) {

    }

    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {

    }

    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {

    }

    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {

    }

    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {

    }

    //37.377803802490234, 126.93367767333984
    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {

        mapView.removeAllPolylines()
        polyline = MapPolyline()


        val mapPointGeo : MapPoint.GeoCoordinate = p1!!.mapPointGeoCoord
//        Log.i("latitude2222: ",mapPointGeo.latitude.toString())
//        Log.i("longitude2222: ",mapPointGeo.longitude.toString())

        polyline.tag = 1000
        polyline.lineColor = android.graphics.Color.argb(128,255,51,0)

        polyline.addPoint(MapPoint.mapPointWithGeoCoord(this.mapPointGeo.latitude, this.mapPointGeo.longitude))
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude))


        mapView.addPolyline(polyline)

        //res1 = locationConverter(this.mapPointGeo.longitude.toString(),this.mapPointGeo.latitude.toString(),"WGS84","WTM")
        locationConverter(this.mapPointGeo.longitude.toString(),this.mapPointGeo.latitude.toString(),"WGS84","WTM")
        //res2 = locationConverter(mapPointGeo.longitude.toString(),mapPointGeo.latitude.toString(),"WGS84","WTM")

//        Log.i("test11 ", res1[0]+","+res1[1])
//        Log.i("test22 ", res2[0]+","+res2[1])
//        var distance = calculatorDistance(res1[0], res1[1], res2[0], res2[1])
//        Log.i("거리계산 결과: ", distance.toString())


        val mapPointBounds = MapPointBounds(polyline.mapPoints)
        val padding = 100 // px
        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding))


    }

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {

    }

    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {

    }

}


