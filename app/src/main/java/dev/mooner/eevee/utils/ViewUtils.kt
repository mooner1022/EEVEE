package dev.mooner.eevee.utils

import android.content.res.Resources
import android.util.DisplayMetrics
import kotlin.math.round

val Int.dp
    get() = round(this / (Resources.getSystem().displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).toInt()