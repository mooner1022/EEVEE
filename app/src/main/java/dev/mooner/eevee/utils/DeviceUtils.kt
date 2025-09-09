package dev.mooner.eevee.utils

import android.os.Build

fun getAndroidVersionCode(): Int =
    Build.VERSION.SDK_INT

fun getAndroidVersionName(): String =
    Build.VERSION.RELEASE

fun getPhoneBrand(): String =
    Build.PRODUCT

fun getPhoneManufacturer(): String =
    Build.MANUFACTURER

fun getPhoneModel(): String =
    Build.MODEL