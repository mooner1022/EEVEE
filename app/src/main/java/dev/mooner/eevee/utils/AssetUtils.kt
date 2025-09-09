package dev.mooner.eevee.utils

import android.content.Context
import dev.mooner.eevee.GPSLocationList
import dev.mooner.eevee.LocationData
import kotlinx.serialization.json.Json
import java.io.BufferedReader

object AssetUtils {

    private val json by lazy {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

    fun loadGPSLocation(context: Context): List<LocationData> {
        val gpsFileContent = context.assets
            .open("gps_location_of.json")
            .bufferedReader()
            .use(BufferedReader::readText)

        val decoded = CipherUtils.decodeMDMAes(gpsFileContent).let {
            if (it.startsWith("\uFEFF")) // Trim BOM char
                it.substring(1)
            else
                it
        }
        return json.decodeFromString<GPSLocationList>(decoded).locations
    }
}