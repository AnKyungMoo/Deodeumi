package Model

object SearchObject {
    data class Address (
        val meta: AddressObject.meta,
        val documents: ArrayList<AddressObject.documents>
    )

    data class Keyword (
        val meta: KeywordObject.meta,
        val documents: ArrayList<KeywordObject.documents>
    )
}