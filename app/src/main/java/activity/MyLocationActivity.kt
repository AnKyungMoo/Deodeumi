package activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.km.deodeumi.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_my_location.*
import kotlinx.android.synthetic.main.layout_path_template.*
import net.daum.mf.map.api.*
import resources.RestAPIKey
import service.LocationService
import java.util.*
import kotlin.math.roundToInt


class MyLocationActivity : AppCompatActivity(),MapView.CurrentLocationEventListener
    ,MapReverseGeoCoder.ReverseGeoCodingResultListener,MapView.MapViewEventListener, TextToSpeech.OnInitListener{

    // var : 읽기/쓰기 가능한 일반 변수 val : 읽기만 가능한 final 변수
    private val AVERAGE_FOOTFALL : Int = 75 //성인 평균 보폭
    private val GPS_ENABLE_REQUEST_CODE: Int = 200
    private val PERMISSIONS_REQUEST_CODE: Int = 100
    private var REQUIRED_PERMISSIONS = arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private lateinit var mapView: MapView
    private lateinit var reverseGeoCoder: MapReverseGeoCoder //? = null
    private lateinit var mapPointGeo : MapPoint.GeoCoordinate
    private lateinit var polyline : MapPolyline
    private var marker: MapPOIItem = MapPOIItem()
    private lateinit var footCount: View
    private lateinit var btnPlay: Button
    private lateinit var tts: TextToSpeech
    private lateinit var broadCastReceiver: BroadcastReceiver
    private lateinit var filter : IntentFilter

    private var subscription: Disposable? = null //retrofit
    private val res = arrayOfNulls<String>(4)

    private var myLocationString: String? = null //현재 내 위치
    private lateinit var x_longitude: String//위도 ?
    private lateinit var y_latitude: String //경도 ?

    private var destinationLongitude: Double? = null
    private var destinationLatitude: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_location)
        val mapViewContainer = findViewById<RelativeLayout>(R.id.mapView)

        var tts_status: Boolean = false
        tts = TextToSpeech(this, this)

        mapView = MapView(this)
        mapViewContainer.addView(mapView)
        mapView.setCurrentLocationEventListener(this)
        mapView.setMapViewEventListener(this)


        val inflaterView = findViewById<RelativeLayout>(R.id.layout_path)
        var inflater = LayoutInflater.from(this)
        footCount = inflater.inflate(R.layout.layout_path_template,inflaterView,false)
        inflaterView.addView(footCount)

        btnPlay = findViewById(R.id.btn_play)

        btnPlay.setOnClickListener {

            if (destinationLatitude != null && destinationLongitude != null) {
                registerReceiver(broadCastReceiver, filter)

                val text: String = calculateAngle() + txt_footfall_count.text.toString()
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

                tts_status = !tts_status
                if(tts_status){ //재생 버튼 눌렸을 때 (true)
                    btnPlay.setBackgroundResource(R.drawable.btn_stop)
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
                }else{
                    btnPlay.setBackgroundResource(R.drawable.btn_play)
                    tts.stop()
                }
            }
        }

        filter = IntentFilter()
        filter.addAction(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED)

        broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {

                when (intent?.action) {
                    TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED -> {
                        tts_status = false
                        btnPlay.setBackgroundResource(R.drawable.btn_play)
                    }
                }
            }
        }



        // 현재 위치 초기화 --> LocationSearchActivity에서 오는 data
        /*
        myLocationString = intent.getStringExtra("myLocationString")
        x_longitude = intent.getStringExtra("longitude")
        y_latitude = intent.getStringExtra("latitude")

        if (myLocationString == null || myLocationString.equals("")) {
            txt_my_location.text = "현재 내 위치: "
        } else {
            txt_my_location.text = "현재 내 위치: ".plus(myLocationString)

            mapPointGeo = MapPoint.GeoCoordinate(y_latitude.toDouble(), x_longitude.toDouble())
            mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(y_latitude.toDouble(),x_longitude.toDouble()),true)
            marker.itemName = myLocationString
            marker.tag = 1
            marker.mapPoint = MapPoint.mapPointWithGeoCoord(y_latitude.toDouble(), x_longitude.toDouble())
            marker.markerType = MapPOIItem.MarkerType.CustomImage
            marker.customImageResourceId = R.drawable.btn_arrow
            marker.setCustomImageAnchor(0.5f,0.5f)
            mapView.addPOIItem(marker)
        }
        */



        if(!checkLocationServiceStatus()){
            showDialogForLocationServiceSetting()
        }else{
            checkRunTimePermission()
        }

        // 위치 검색으로 이동
        layout_search.setOnClickListener {
            val i = Intent(this, LocationSearchActivity::class.java)
            startActivity(i)
            finish()
        }


    }

    @Override
    override fun onStop() {
        super.onStop()
        unregisterReceiver(broadCastReceiver)
    }

    // Text To Speech
    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.KOREA)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                btnPlay.isEnabled = true
                tts.setPitch(0.7f)
                tts.setSpeechRate(1.2f)
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    @Override
    override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
        mapView.setShowCurrentLocationMarker(false)
        super.onDestroy()
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
                //mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
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
        mapView.removePOIItem(marker)
        mapPointGeo = p1!!.mapPointGeoCoord
        marker = MapPOIItem()

//        기존 코드 : mapView.mapCenterPoint(서울시 중구로 셋팅됨)
        var presentPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude)

        reverseGeoCoder = MapReverseGeoCoder(RestAPIKey.kakao,presentPoint,this, this)
        reverseGeoCoder.startFindingAddress()
        mapView.setCurrentLocationRadius(50)
        mapView.setCurrentLocationRadiusFillColor(android.graphics.Color.argb(128,255,203,203))
        mapView.setCurrentLocationRadiusStrokeColor(android.graphics.Color.argb(128,255,203,203))
    }

    override fun onCurrentLocationUpdateFailed(p0: MapView?) {}
    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {}
    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {}

    //MapReverseGeoCoder.ReverseGeoCodingResultListener
    override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {}

    @SuppressLint("SetTextI18n")
    override fun onReverseGeoCoderFoundAddress(p0: MapReverseGeoCoder?, p1: String?) {

        var short_address: String = p1!!.substring(0,11) //어떤 기준으로 자를 것인지?
        Log.i("현재 주소",p1)
        if (txt_my_location.text.length == 9){
            txt_my_location.text = "${txt_my_location.text}$short_address"
        }

        marker.itemName = p1
        marker.tag = 1
        marker.mapPoint = MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude)
        marker.markerType = MapPOIItem.MarkerType.CustomImage
        marker.customImageResourceId = R.drawable.btn_arrow
        marker.setCustomImageAnchor(0.5f,0.5f)
        mapView.addPOIItem(marker)

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
            //mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
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
    private fun locationConverter(x: String,y: String,input_coord: String,output_coord: String) {

        subscription = LocationService.distanceRestAPI().distanceConverter(x,y,input_coord,output_coord)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->

                    if(this.res[0] == null){
                        res[0] = result.documents[0].x
                        res[1] = result.documents[0].y
                    }else if(this.res[2] == null){
                        res[2] = result.documents[0].x
                        res[3] = result.documents[0].y
                        calculatorDistance(res[0].toString(),res[1].toString(),res[2].toString(),res[3].toString())
                        for (item in res.indices){
                            res[item] = null
                        }

                    }

                },
                { err ->
                    Log.e("Error User",err.toString())
                }
            )

    }

    // calculator distance
    private fun calculatorDistance(x1: String, y1:String, x2:String, y2:String): Double {

        //var x_distance = Math.pow(Math.abs(x1.toInt() - x2.toInt()).toDouble(), 2.0) //Math.abs(x1.toInt() - x2.toInt())
        var x_distance = Math.pow(Math.abs(x1.toDouble() - x2.toDouble()), 2.0)
        var y_distance = Math.pow(Math.abs(y1.toDouble() - y2.toDouble()), 2.0) //Math.abs(y1.toInt() - y2.toInt())
        Toast.makeText(this, "거리(m)->"+Math.ceil(Math.sqrt(x_distance+y_distance)), Toast.LENGTH_SHORT).show()
        var meter_value = Math.ceil(Math.sqrt(x_distance+y_distance)) * 100 //m -> cm
        var footfall = (meter_value/ AVERAGE_FOOTFALL).roundToInt()
        footCount.findViewById<TextView>(R.id.txt_footfall_count).text = footfall.toString() + "걸음 후 도착합니다."
        footCount.findViewById<TextView>(R.id.txt_use_path).text = txt_my_location.text

        return Math.ceil(Math.sqrt(x_distance+y_distance))
    }




    // MapView.MapViewEventListener
    override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewInitialized(p0: MapView?) {}
    override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {}
    override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {}

    override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {

        locationConverter(this.mapPointGeo.longitude.toString(),this.mapPointGeo.latitude.toString(),"WGS84","WTM")

        mapView.removeAllPolylines()
        polyline = MapPolyline()

        val mapPointGeo : MapPoint.GeoCoordinate = p1!!.mapPointGeoCoord


        polyline.tag = 1000
        polyline.lineColor = android.graphics.Color.argb(128,255,51,0)

        polyline.addPoint(MapPoint.mapPointWithGeoCoord(this.mapPointGeo.latitude, this.mapPointGeo.longitude))
        polyline.addPoint(MapPoint.mapPointWithGeoCoord(mapPointGeo.latitude, mapPointGeo.longitude))

        mapView.addPolyline(polyline)
        locationConverter(mapPointGeo.longitude.toString(),mapPointGeo.latitude.toString(),"WGS84","WTM")

        destinationLatitude = mapPointGeo.latitude
        destinationLongitude = mapPointGeo.longitude

        val mapPointBounds = MapPointBounds(polyline.mapPoints)
        val padding = 100 // px
        mapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds, padding))


    }

    override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {}
    override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {}

    private fun calculateAngle(): String {
        /* 각도 계산하는 부분 */
        val beginLatitudeRadian: Double = this.mapPointGeo.latitude * (3.141592 / 180)
        val beginLongitudeRadian: Double = this.mapPointGeo.longitude * (3.141592 / 180)
        val destinationLatitudeRadian: Double = destinationLatitude!! * (3.141592 / 180)
        val destinationLongitudeRadian: Double = destinationLongitude!! * (3.141592 / 180)

        val radianDistance: Double = Math.acos(Math.sin(beginLatitudeRadian) * Math.sin(destinationLatitudeRadian) + Math.cos(beginLatitudeRadian) * Math.cos(destinationLatitudeRadian) * Math.cos(beginLongitudeRadian - destinationLongitudeRadian))

        val radianBearing: Double = Math.acos((Math.sin(destinationLatitudeRadian) - Math.sin(beginLatitudeRadian) * Math.cos(radianDistance)) / (Math.cos(beginLatitudeRadian) * Math.sin(radianDistance)))

        var trueBearing: Double = 0.0
        if (Math.sin(destinationLongitudeRadian - beginLongitudeRadian) < 0)
        {
            trueBearing = radianBearing * (180 / 3.141592)
            trueBearing = 360 - trueBearing
        }
        else
        {
            trueBearing = radianBearing * (180 / 3.141592)
        }

        // 각도 계산
        val angle = trueBearing.toInt() - mapView.mapRotationAngle.toInt()

        var result: String
        if (angle >= 0) {
            result = (angle / 30).toString() + "시"

            if ((angle % 30) >= 12) {
                result += " 반 "
            }
        } else {
            result = (12 - (Math.abs(angle) / 30)).toString() + "시"

            if ((angle % 30) >= 12) {
                result += " 반 "
            }
        }

        /* TODO: 0시 일때는 회전하라는 메시지가 안넘어가게 하자
         * TODO: 0시 반일 때는 넘어가야 됨
         * */
        return if (result[0] != '0') {
            result + "방향으로 회전하고 "
        } else {
            "전방으로 "
        }
    }
}


