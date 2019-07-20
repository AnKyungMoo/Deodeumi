package activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
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
import org.json.JSONArray
import org.json.JSONException
import resources.APIKey
import java.util.*

class MapActivity : AppCompatActivity(), TMapGpsManager.onLocationChangedCallback, TMapView.OnClickListenerCallback{

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

    private var subwayCount: Int = 0
    private var busCount: Int = 0
    private var walkCount: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

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


        tMapView.mapType = TMapView.MAPTYPE_STANDARD
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN)
        tMapView.zoomLevel = 20

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

        btn_count_foot.setOnClickListener{
            val intent = Intent(this, StrideActivity::class.java)
            startActivityForResult(intent, 200)
        }

        btn_search_location.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            startActivityForResult(intent, LOCATION_ACTIVITY_CODE)
        }

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
            tMapView.removeAllTMapCircle()
            tMapView.zoomLevel = 20
            tMapPoint = TMapPoint(tMapView.longitude, tMapView.latitude)

            tMapData.convertGpsToAddress(tMapView.latitude, tMapView.longitude) {
                var location: String = it.substring(0,13)
                Log.i("location", location)
                locationText.text = "출발: ".plus(it)
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
        when(requestCode){
            GPS_ENABLE_REQUEST_CODE -> {
                if(checkLocationServiceStatus()){
                    checkRunTimePermission()
                    return
                }
            }

            LOCATION_ACTIVITY_CODE -> {
                if(resultCode == 0){
                    des_text = data!!.getStringExtra("myLocationString").toString()
                    des_longitude = data.getStringExtra("longitude").toDouble()
                    des_latitude = data.getStringExtra("latitude").toDouble()
                    desMapPoint = TMapPoint(des_latitude!!, des_longitude!!)

                    tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH,tMapView.locationPoint, desMapPoint) {
                        it.lineColor = Color.BLUE
                        it.passPoint.forEach{
                            Log.d("CheckPointKM", it.latitude.toString() + " " + it.longitude)
                        }
                        tMapView.addTMapPath(it)
                    }

                    var callbackListener = object: OnResultCallbackListener {
                        override fun onSuccess(odsayData: ODsayData?, api: API?) {
                            try {
                                if(api== API.SEARCH_PUB_TRANS_PATH){ //대중교통 길찾기
                                    //최초 출발역
                                    val jArray: JSONArray = odsayData!!.json.getJSONObject("result").getJSONArray("path")
                                    Log.i("??",jArray.toString())

                                    for (i in 0..jArray.length()-1) {
                                        val jObject = jArray.getJSONObject(i)
//                                        if(jObject.getInt("pathType") == 3){


                                            val jSubPath = jObject.getJSONArray("subPath")
                                            for(j in 0..jSubPath.length()-1){
                                                val subPath = jSubPath.getJSONObject(j)
                                                if (subPath.getInt("trafficType") == 1) { //1:지하철 , 2:버스, 3:도보
                                                    Log.i("subwayname: ", subPath.getString("startName"))
                                                    subwayCount += 1
                                                } else if(subPath.getInt("trafficType") ==2 ){
                                                    busCount += 1
                                                } else {
                                                    Log.i("이동거리: ", subPath.getString("distance").toString())
                                                    Log.i("소요시간: ", subPath.getString("sectionTime").toString())
                                                    walkCount += 1
                                                }
                                            }
//                                        }
//                                        val jInfo = jObject.getJSONObject("info")
//                                        val jSubPath = jObject.getJSONArray("subPath")

//                                        for (j in 0..jSubPath.length()-1) {
//                                            val subPath = jSubPath.getJSONObject(j)
//                                            if (subPath.getInt("trafficType") == 1) { //1:지하철 , 2:버스, 3:도보
//                                                Log.i("subwayname: ", subPath.getString("startName"))
//                                                subwayCount += 1
//                                            } else if(subPath.getInt("trafficType") ==2 ){
//                                                busCount += 1
//                                            } else {
//                                                Log.i("이동거리: ", subPath.getString("distance").toString())
//                                                Log.i("소요시간: ", subPath.getString("sectionTime").toString())
//                                                walkCount += 1
//                                            }
//                                        }
                                    }
                                }
                            }catch (e: JSONException){
                                    e.printStackTrace()
                            }

                            Log.i("subway: ", subwayCount.toString())
                            Log.i("bus: ", busCount.toString())
                            Log.i("walk: ", walkCount.toString())
                        }

                        override fun onError(p0: Int, p1: String?, p2: API?) {
                            if(p2== API.SEARCH_PUB_TRANS_PATH){}
                        }
                    }
                    //경도 : long 위도: lati
                    oDsayService.requestSearchPubTransPath(tMapView.longitude.toString(), tMapView.latitude.toString()
                        ,des_longitude.toString(), des_latitude.toString(),"0","0","0",callbackListener)
                }
            }
        }

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
