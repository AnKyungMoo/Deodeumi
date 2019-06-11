package activity

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
        getKeywordSearch()
    }

    private fun getAddressSearch() {
        subscription = SearchService.restAPI().addressSearch("강남")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.d("addressResultKM", result.documents[0].toString())
                },
                { err ->
                    Log.e("Error User",err.toString())
                }
            )
    }

    private fun getKeywordSearch() {
        subscription = SearchService.restAPI().keywordSearch("강남역")
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.d("keywordResultKM", result.documents[0].place_name)
                },
                { err ->
                    Log.e("Error User",err.toString())
                }
            )
    }
}