package dev.mooner.eevee

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GPSLocationList(
    @SerialName("location")
    val locations: List<LocationData>
)

@Serializable
data class LocationData(
    @SerialName("Location")
    val name      : String,
    @SerialName("Latitude")
    val latitude  : Double,
    @SerialName("Longitude")
    val longitude : Double,
    @SerialName("Radius")
    val radius    : Int,
) {

    fun longHashCode(): Long {
        var result = name.hashCode().toLong()
        result = 31 * result + latitude.toBits()
        result = 31 * result + longitude.toBits()
        result = 31 * result + radius.toLong()
        return result
    }
}

data class LocationWithDistance(val location: LocationData, val distance: Double) : Comparable<LocationWithDistance> {
    // 최대 힙을 위해 거리가 큰 것이 우선순위가 높도록 구현
    override fun compareTo(other: LocationWithDistance): Int {
        return other.distance.compareTo(this.distance)
    }
}