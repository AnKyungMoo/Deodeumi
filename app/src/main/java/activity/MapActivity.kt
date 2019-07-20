package activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.km.deodeumi.R
import com.skt.Tmap.*
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.android.synthetic.main.custom_dialog.view.*
import resources.APIKey
import java.util.*

class MapActivity : AppCompatActivity(), TMapGpsManager.onLocationChangedCallback, TMapView.OnClickListenerCallback{

    private val GPS_ENABLE_REQUEST_CODE: Int = 200
    private val PERMISSIONS_REQUEST_CODE: Int = 100
    private var REQUIRED_PERMISSIONS = arrayOf<String>(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private lateinit var callBtn: Button
    private lateinit var footCountBtn: Button
    private lateinit var locationText: TextView
    private lateinit var tMapView: TMapView
    private lateinit var tMapGpsManager: TMapGpsManager
    private lateinit var tMapPoint: TMapPoint
    private lateinit var tMapData: TMapData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        tMapView = TMapView(this)
        map_layout.addView(tMapView)
        tMapView.setSKTMapApiKey(APIKey.TMAP)
        tMapView.setCompassMode(true)
        tMapView.setIconVisibility(true)


//        if(!checkLocationServiceStatus()){
//            showDialogForLocationServiceSetting()
//        }
//        checkRunTimePermission()


        tMapView.mapType = TMapView.MAPTYPE_STANDARD
        tMapView.setLanguage(TMapView.LANGUAGE_KOREAN)

        tMapGpsManager = TMapGpsManager(this)
        tMapGpsManager.minTime = 1000
        tMapGpsManager.minDistance = 5F
        tMapGpsManager.provider = TMapGpsManager.GPS_PROVIDER
        tMapGpsManager.OpenGps()

        tMapView.setTrackingMode(true)
        tMapView.setSightVisible(true)

        tMapData = TMapData()

        callBtn = findViewById(R.id.btn_call_center)
        footCountBtn = findViewById(R.id.btn_count_foot)
        locationText = findViewById(R.id.txt_my_location)

        callBtn.setOnClickListener {
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

        footCountBtn.setOnClickListener{
            val intent = Intent(this, StrideActivity::class.java)
            startActivityForResult(intent, 200)
        }

        btn_search_location.setOnClickListener {
            val intent = Intent(this, LocationSearchActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onLocationChange(p0: Location?) {

        if (p0 != null) {

            tMapView.setLocationPoint(p0.longitude, p0.latitude)
            tMapView.setCenterPoint(p0.longitude, p0.latitude)
            tMapView.removeAllTMapCircle()
            tMapView.zoomLevel = 15
            tMapPoint = TMapPoint(tMapView.longitude, tMapView.latitude)

            tMapData.convertGpsToAddress(tMapView.latitude, tMapView.longitude) {
                Log.i("it---->", it)
                var location: String = it.substring(0,13)
                Log.i("location", location)
                //${txt_my_location.text}$short_address
                locationText.text = "$location"
            }

//            var circle = TMapCircle()
//            circle.radius = 50.0
//            circle.areaAlpha = 50
//            circle.areaColor = Color.argb(128,255,203,203)
//            circle.lineColor = Color.argb(128,255,203,203)
//            circle.centerPoint = tMapPoint
//            tMapView.addTMapCircle("circle_test", circle)
        }
    }

    override fun onPressEvent(
        p0: ArrayList<TMapMarkerItem>?,
        p1: ArrayList<TMapPOIItem>?,
        p2: TMapPoint?,
        p3: PointF?
    ): Boolean {
        if (p2 != null) {
//            tMapView.removeTMapPolyLine("line_test")
//            var polyLine = TMapPolyLine()
//            polyLine.lineColor = Color.RED
//            polyLine.outLineColor = Color.RED
//            polyLine.lineWidth = 4F
//            polyLine.addLinePoint(tMapPoint)
//            polyLine.addLinePoint(TMapPoint(p2.longitude, p2.latitude))
//            tMapView.addTMapPolyLine("line_test", polyLine)
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
