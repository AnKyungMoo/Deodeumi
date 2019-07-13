package `interface`

import io.reactivex.Observable
import models.DistanceObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DistanceInterface {
    // 좌표 변환
    //@Headers("Authorization: KakaoAK " + APIKey.TMAP)
    @GET("v2/local/geo/transcoord.json")
    fun distanceConverter(
        @Query("x") x:String,
        @Query("y") y:String,
        @Query("input_coord") input_coord:String,
        @Query("output_coord") output_coord:String
        ) : Observable<DistanceObject.Distance>


    //@Headers("Authorization: KakaoAK " + APIKey.TMAP)
    @GET("v2/local/geo/transcoord.json")
    fun testDistance(
        @Query("x") x:String,
        @Query("y") y:String,
        @Query("input_coord") input_coord:String,
        @Query("output_coord") output_coord:String
    ) : Call<DistanceObject.Distance>

}