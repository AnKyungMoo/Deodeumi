package activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.km.deodeumi.R
import com.odsay.odsayandroidsdk.API
import com.odsay.odsayandroidsdk.ODsayData
import com.odsay.odsayandroidsdk.ODsayService
import com.odsay.odsayandroidsdk.OnResultCallbackListener
import com.skt.Tmap.*
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.custom_dialog.view.*
import models.CheckPointModel
import org.json.JSONArray
import org.json.JSONException
import resources.APIKey
import java.util.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin



class MapActivity : AppCompatActivity(), TMapGpsManager.onLocationChangedCallback, TMapView.OnClickListenerCallback, TextToSpeech.OnInitListener{

    private val GPS_ENABLE_REQUEST_CODE: Int = 200
    private val PERMISSIONS_REQUEST_CODE: Int = 100
    private val LOCATION_ACTIVITY_CODE: Int = 300
    private var REQUIRED_PERMISSIONS = arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private lateinit var locationText: TextView
    private lateinit var tMapView: TMapView
    private lateinit var tMapGpsManager: TMapGpsManager
    private lateinit var tMapPoint: TMapPoint
    private lateinit var desMapPoint: TMapPoint
    private lateinit var tMapData: TMapData
    private lateinit var oDsayService: ODsayService

    private var des_text: String? = null
    private var des_longitude: Double? = null
    private var des_latitude: Double? = null

    private var checkPointList = arrayOf<CheckPointModel>()
    private var currentIndex: Int = 0

    private var stepCount: Int = 0

    private lateinit var tts: TextToSpeech
    private lateinit var broadCastReceiver: BroadcastReceiver
    private lateinit var filter : IntentFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        var tts_status: Boolean = false
        tMapView = TMapView(this)
        map_layout.addView(tMapView)
        tMapView.setSKTMapApiKey(APIKey.TMAP)
        tMapView.setCompassMode(true)
        tMapView.setIconVisibility(true)
        settingOdsay()

//        if(!checkLocationServiceStatus()){
//            showDialogForLocationServiceSetting()
//        }
//        checkRunTimePermission()

        tts = TextToSpeech(this, this)
        tMapView.mapType = TMapView.MAPTYPE_STANDARD
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN)
//        tMapView.zoomLevel = 20

        tMapGpsManager = TMapGpsManager(this)
        tMapGpsManager.minTime = 1000
        tMapGpsManager.minDistance = 5F
        tMapGpsManager.provider = TMapGpsManager.GPS_PROVIDER
        tMapGpsManager.OpenGps()

        tMapView.setTrackingMode(true)
        tMapView.setSightVisible(true)

        tMapData = TMapData()

        locationText = findViewById(R.id.txt_my_location)

        btn_call_center.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.custom_dialog, null)
            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
            val mAlertDialog = builder.show()
            dialogView.btn_call.setOnClickListener{
                mAlertDialog.dismiss()
                /* TODO: 전화번호를 여러 데이터를 받아오면 상황에 맞춰 사용하자 */
                startActivity(Intent("android.intent.action.CALL", Uri.parse("tel:0220920000")))
            }

        }

        /* TODO: 임시로 거리 확인중임 완료되면 tts 연결로 바꾸자 */
        btn_play.setOnClickListener {
            if(checkPointList.isNotEmpty()){
                registerReceiver(broadCastReceiver, filter)
                val text: String = calculateAngle() + stepCount + "걸음 전진하세요"
                Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

                tts_status = !tts_status
                if(tts_status){ //재생 버튼 눌렸을 때 (true)
                    btn_play.setBackgroundResource(R.drawable.btn_stop)
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
                }else{
                    btn_play.setBackgroundResource(R.drawable.btn_play)
                    tts.stop()
                }
            }
        }

        btn_count_foot.setOnClickListener{
            val intent = Intent(this, StrideActivity::class.java)
            startActivityForResult(intent, 200)
        }

        btn_search_location.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            startActivityForResult(intent, LOCATION_ACTIVITY_CODE)
        }

        filter = IntentFilter()
        filter.addAction(TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED)

        broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {

                when (intent?.action) {
                    TextToSpeech.ACTION_TTS_QUEUE_PROCESSING_COMPLETED -> {
                        tts_status = false
                        btn_play.setBackgroundResource(R.drawable.btn_play)
                    }
                }
            }
        }

    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.KOREA)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                btn_play.isEnabled = true
                tts.setPitch(0.7f)
                tts.setSpeechRate(1.2f)
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    override fun onStop() {
        super.onStop()
        if(checkPointList.isNotEmpty()){
            unregisterReceiver(broadCastReceiver)
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }

        super.onDestroy()
    }

    fun settingOdsay(){
        oDsayService = ODsayService.init(this, APIKey.ODsay)
        oDsayService.setConnectionTimeout(5000)
        oDsayService.setConnectionTimeout(5000)
    }

    override fun onLocationChange(p0: Location?) {

        if (p0 != null) {

            tMapView.setLocationPoint(p0.longitude, p0.latitude)
            tMapView.setCenterPoint(p0.longitude, p0.latitude)
            /* TODO: 지도 움직이면 회전을 안하는 버그를 처리하는 방법을 알게되면 고치자 */
            tMapView.setCompassMode(true)
//            tMapView.removeAllTMapCircle()
//            tMapView.zoxomLevel = 20
            tMapPoint = TMapPoint(p0.latitude, p0.longitude)

            tMapData.convertGpsToAddress(p0.latitude, p0.longitude) {
                var location: String = it.substring(0,13)
                Log.i("location", location)
                locationText.text = "출발: ".plus(it)
            }

            if (checkPointList.isNotEmpty()) {
                if (distance(p0.latitude, p0.longitude,
                        checkPointList[currentIndex].latitude, checkPointList[currentIndex].longitude) <= 3) {
                    checkPointList[currentIndex].isVisit = true
                    currentIndex++
                    Toast.makeText(this, "도착!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onPressEvent(
        p0: ArrayList<TMapMarkerItem>?,
        p1: ArrayList<TMapPOIItem>?,
        p2: TMapPoint?,
        p3: PointF?
    ): Boolean {
        if (p2 != null) {

        }

        return true
    }

    override fun onPressUpEvent(
        p0: ArrayList<TMapMarkerItem>?,
        p1: ArrayList<TMapPOIItem>?,
        p2: TMapPoint?,
        p3: PointF?
    ): Boolean {
        return true
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            GPS_ENABLE_REQUEST_CODE -> {
                if (checkLocationServiceStatus()) {
                    checkRunTimePermission()
                    return
                }
            }

            LOCATION_ACTIVITY_CODE -> {
                if (resultCode == 0) {
                    des_text = data!!.getStringExtra("myLocationString").toString()
                    des_longitude = data.getStringExtra("longitude").toDouble()
                    des_latitude = data.getStringExtra("latitude").toDouble()

                    desMapPoint = TMapPoint(des_latitude!!, des_longitude!!)

                    tMapData.findPathDataWithType(
                        TMapData.TMapPathType.PEDESTRIAN_PATH,
                        tMapView.locationPoint,
                        desMapPoint
                    ) { polyLine ->
                        polyLine.lineColor = Color.BLUE

                        var index = 0
                        checkPointList = arrayOf()
                        polyLine.linePoint.forEach { item ->
                            val checkPointModel = CheckPointModel(false, item.latitude, item.longitude)
                            checkPointList += checkPointModel

                            val point = TMapPoint(item.latitude, item.longitude)
                            val tMapCircle = TMapCircle()
                            tMapCircle.centerPoint = point
                            tMapCircle.radius = 1.0
                            tMapCircle.circleWidth = 1f
                            tMapCircle.lineColor = Color.RED
                            tMapCircle.areaColor = Color.RED
                            tMapCircle.areaAlpha = 100
                            tMapView.addTMapCircle("circle$index", tMapCircle)
                            index++
                        }

                        tMapView.addTMapPath(polyLine)
                    }

                    var callbackListener = object: OnResultCallbackListener {
                        override fun onSuccess(odsayData: ODsayData?, api: API?) {
                            try {
                                if(api== API.SEARCH_PUB_TRANS_PATH){ //대중교통 길찾기
                                    //최초 출발역
                                    val jArray: JSONArray = odsayData!!.json.getJSONObject("result").getJSONArray("path")
                                    for(i in 0..jArray.length()-1){
                                        val jObject = jArray.getJSONObject(i)
                                        val jInfo = jObject.getJSONObject("info")
                                        val jSubPath = jObject.getJSONArray("subPath")

                                        for (j in 0..jSubPath.length()-1) {
                                            val subPath = jSubPath.getJSONObject(j)
                                            if (subPath.getInt("trafficType") != 3) { // 도보
                                                var passList = subPath.getJSONObject("passStopList")
                                                var stations = passList.getJSONArray("stations")
                                                stations


                                            }

                                        }
                                    }

//                                    for (i in 0..jArray.length()-1) {
//                                        val jObject = jArray.getJSONObject(i)
//                                        val jInfo = jObject.getJSONObject("info")
//                                        val jSubPath = jObject.getJSONArray("subPath")
//
//
//                                        Log.i("count", jSubPath.length().toString())
//                                        for (j in 0..jSubPath.length()-1) {
//                                            val subPath = jSubPath.getJSONObject(j)
//                                            if (subPath.getInt("trafficType") != 3) {
//                                                var first = jInfo.getString("firstStartStation")
//                                                Log.i("first", first)
//                                                break
//                                            }
//                                        }
//
//                                    }
                                }
                            }catch (e: JSONException){
                                e.printStackTrace()
                            }
                        }

                        override fun onError(p0: Int, p1: String?, p2: API?) {
                            if(p2== API.SEARCH_PUB_TRANS_PATH){}
                        }
                    }

                    oDsayService.requestSearchPubTransPath(tMapView.longitude.toString(), tMapView.latitude.toString()
                        , des_longitude.toString(), des_latitude.toString(),"0","0","0",callbackListener)
                }
            }
        }
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val theta = lon1 - lon2
        var dist = sin(deg2rad(lat1)) * sin(deg2rad(lat2)) + cos(deg2rad(lat1)) * cos(deg2rad(lat2)) * cos(deg2rad(theta))

        dist = acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515

        dist *= 1609.344
        val cm = Math.ceil(dist)*100 //cm
        val step = getSharedPreferences("step", MODE_PRIVATE)
        val stride = step.getInt("distance", 75) //보폭
        stepCount = (cm/stride).roundToInt()
        Log.i("걸음수: ", stepCount.toString()+" "+cm)

        return dist.toInt()
    } // m -> cm -> 걸음 수 환산(shared에서 가져오기) -> 걸음!

    private fun calculateAngle(): String {
        /* 각도 계산하는 부분 */
        val beginLatitudeRadian: Double = tMapPoint.latitude * (3.141592 / 180)
        val beginLongitudeRadian: Double = tMapPoint.longitude * (3.141592 / 180)
        val destinationLatitudeRadian: Double = checkPointList[currentIndex].latitude * (3.141592 / 180)
        val destinationLongitudeRadian: Double = checkPointList[currentIndex].longitude * (3.141592 / 180)

        val radianDistance: Double = acos(sin(beginLatitudeRadian) * sin(destinationLatitudeRadian) + cos(beginLatitudeRadian) * cos(destinationLatitudeRadian) * cos(beginLongitudeRadian - destinationLongitudeRadian))
        val radianBearing: Double = acos((sin(destinationLatitudeRadian) - sin(beginLatitudeRadian) * cos(radianDistance)) / (cos(beginLatitudeRadian) * sin(radianDistance)))

        var trueBearing: Double = 0.0
        if (sin(destinationLongitudeRadian - beginLongitudeRadian) < 0)
        {
            trueBearing = radianBearing * (180 / 3.141592)
            trueBearing = 360 - trueBearing
        } //trueBearing: 도착점 각도
        else
        {
            trueBearing = radianBearing * (180 / 3.141592)
        }


        var rotate = if(tMapView.rotate.toInt() > 0){
            360 - tMapView.rotate.toInt()
        }else{
            tMapView.rotate.toInt()*-1
        }

        // 각도 계산
        val angle = trueBearing.toInt() - rotate
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

    // This function converts decimal degrees to radians
    private fun deg2rad(deg: Double): Double {
        return (deg * Math.PI / 180.0)
    }

    // This function converts radians to decimal degrees
    private fun rad2deg(rad: Double): Double {
        return (rad * 180 / Math.PI)
    }

    private fun checkLocationServiceStatus():Boolean {
        var manager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkRunTimePermission(){

        var hasFineLocationPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED){

        }else{
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[0])){
                Toast.makeText(this,"이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE)
            }else{
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE)
            }

        }
    }

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


}
