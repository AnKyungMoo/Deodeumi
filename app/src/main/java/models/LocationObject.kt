package models

class LocationObject {
    data class meta (
        val total_count: Int
    )

    data class documents (
        val y: String,
        val x: String
    )
}