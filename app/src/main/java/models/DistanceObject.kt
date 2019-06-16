package models

object DistanceObject {
    data class Distance (
        val meta: LocationObject.meta,
        val documents: ArrayList<LocationObject.documents>
    )
}
