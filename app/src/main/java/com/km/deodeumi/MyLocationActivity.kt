package com.km.deodeumi

import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.RelativeLayout
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapReverseGeoCoder
import net.daum.mf.map.api.MapView



class MyLocationActivity : AppCompatActivity(),MapView.CurrentLocationEventListener,MapReverseGeoCoder.ReverseGeoCodingResultListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_location)

        val mapView = MapView(this)
        val mapViewContainer = findViewById<RelativeLayout>(R.id.mapView)
        mapViewContainer.addView(mapView)
        //tracking 모드가 on 이어야 사용자 위치가 업데이트됨
        //mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
        mapView.setCurrentLocationEventListener(this)

    }

    @Override
    override fun onDestroy() {
        super.onDestroy()

    }

    //MapView.CurrentLocationEventListener

    override fun onCurrentLocationUpdate(p0: MapView?, p1: MapPoint?, p2: Float) {

    }

    override fun onCurrentLocationUpdateFailed(p0: MapView?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCurrentLocationUpdateCancelled(p0: MapView?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCurrentLocationDeviceHeadingUpdate(p0: MapView?, p1: Float) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }




    //MapReverseGeoCoder.ReverseGeoCodingResultListener
    override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onReverseGeoCoderFoundAddress(p0: MapReverseGeoCoder?, p1: String?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun checkLocationServiceStatus():Boolean {
        var manager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

}


