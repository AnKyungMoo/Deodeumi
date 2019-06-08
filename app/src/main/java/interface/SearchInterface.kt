package `interface`

import Model.AddressObject
import Model.SearchObject
import com.google.gson.JsonObject
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface SearchInterface {
    // 주소로 장소 검색
    @Headers("Authorization: KakaoAK 2168d4f5bb0f2f4ce3c12ab1a65ac724")
    @GET("v2/local/search/address.json")
    fun addressSearch(@Query("query") keyword: String): Observable<SearchObject.Address>

    // 키워드로 장소 검색
    @GET("v2/local/search/keyword.json")
    fun keywordSearch(@Header("Authorization") apiKey: String, @Query("query") keyword: String) : Observable<JsonObject>
}