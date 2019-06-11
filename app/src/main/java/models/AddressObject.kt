package models

class AddressObject {
    data class meta (
        val total_count: Int,
        val pageable_count: Int,
        val is_end: Boolean
    )

    data class documents (
        val address_name: String,
        val y: String,
        val x: String,
        val address_type: String,
        val address: AddressModel,
        val road_address: RoadAddressModel
    )
}