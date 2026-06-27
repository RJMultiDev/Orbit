package com.qx.orbit.bili.data.api
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
fun test() {
    val b = "{}".toRequestBody("application/json".toMediaTypeOrNull())
}
