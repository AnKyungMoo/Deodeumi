package activity

import adapter.SearchAdapter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.km.deodeumi.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_location_search.*
import service.SearchService

class LocationSearchActivity : AppCompatActivity() {
    private lateinit var subscription: Disposable

    private lateinit var searchAdapter: SearchAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_search)

        searchAdapter = SearchAdapter(this)
        search_recycler_view.adapter = searchAdapter

        val layoutManager = LinearLayoutManager(this)
        search_recycler_view.layoutManager = layoutManager
        search_recycler_view.setHasFixedSize(true)

        getAddressSearch("강남")
        getKeywordSearch("강남역")
    }

    private fun getAddressSearch(keyword: String) {
        subscription = SearchService.restAPI().addressSearch(keyword)
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

    private fun getKeywordSearch(keyword: String) {
        subscription = SearchService.restAPI().keywordSearch(keyword)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { result ->
                    Log.d("keywordResultKM", result.documents[0].place_name)

                    result.documents.forEach{
                        searchAdapter.addItem(it)
                    }
                },
                { err ->
                    Log.e("Error User",err.toString())
                }
            )
    }
}