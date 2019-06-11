package `interface`

import resources.RestAPIKey
import models.SearchObject
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface SearchInterface {
    // 주소로 장소 검색
    @Headers("Authorization: KakaoAK " + RestAPIKey.kakao)
    @GET("v2/local/search/address.json")
    fun addressSearch(@Query("query") keyword: String): Observable<SearchObject.Address>

    // 키워드로 장소 검색
    @Headers("Authorization: KakaoAK " + RestAPIKey.kakao)
    @GET("v2/local/search/keyword.json")
    fun keywordSearch(@Query("query") keyword: String) : Observable<SearchObject.Keyword>
}