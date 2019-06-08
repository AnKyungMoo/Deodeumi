package activity

import Model.AddressObject
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.km.deodeumi.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import service.SearchService

class LocationSearchActivity : AppCompatActivity() {
    private lateinit var subscription: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_search)

        getAddressSearch()
    }

    private fun getAddressSearch() {
        subscription = SearchService.restAPI().addressSearch("기흥")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.d("resultKM", result.documents.address_name)
                },
                { err ->
                    Log.e("Error User",err.toString())
                }
            )
    }
}