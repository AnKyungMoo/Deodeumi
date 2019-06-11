package Model

object SearchObject {
    data class Address (
        val meta: AddressObject.meta,
        val documents: ArrayList<AddressObject.documents>
    )

    data class Keyword (
        val first: String,
        val second: String
    )
}