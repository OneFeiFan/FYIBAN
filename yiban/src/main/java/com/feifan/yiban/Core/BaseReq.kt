package com.feifan.yiban.Core

import android.content.Context
import com.feifan.yiban.Core.SchoolBased.csrf
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit


class BaseReq(context: Context) {
    private val baseHeaders: HashMap<String, String> =
        SchoolBased.headers() as HashMap<String, String>
    private val client: OkHttpClient

    init {
        val defaultCookie = Cookie.Builder()
            .name("csrf_token")
            .value(csrf())
            .domain("uyiban.com")
            .build()

        val cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context));


        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .followRedirects(false)
            .retryOnConnectionFailure(true)
            .build()

        // 添加默认cookie
        val dummyUrl = "https://uyiban.com".toHttpUrl()
        cookieJar.saveFromResponse(dummyUrl, listOf(defaultCookie))
    }

    fun request(
        method: String?,
        url: String?,
        params: Map<String, String>?,
        headers: Map<String, String>,
        data: Map<String, Any>?
    ): Response {
        val mergedHeaders = baseHeaders.toMutableMap()
        mergedHeaders.putAll(headers)

        val httpUrl = url?.toHttpUrl()?.newBuilder()?.apply {
            params?.forEach { (name, value) ->
                addQueryParameter(name, value)
            }
        }?.build() ?: throw IllegalArgumentException("Invalid URL")

        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .headers(Headers.Builder().apply {
                mergedHeaders.forEach { (name, value) ->
                    add(name, value)
                }
            }.build())

        when (method?.uppercase()) {
            "POST" -> {
                val formBody = FormBody.Builder().apply {
                    data?.forEach { (name, value) ->
                        add(name, value.toString())
                    }
                }.build()
                requestBuilder.post(formBody)
            }

            else -> {
                requestBuilder.get()
            }
        }

        return client.newCall(requestBuilder.build()).execute()
    }

    fun get(
        url: String,
        params: Map<String, String> = mapOf(),
        headers: Map<String, String> = mapOf()
    ): Response {
        return request(
            "GET",
            url,
            params,
            headers,
            null
        )
    }

    fun post(
        url: String,
        params: Map<String, String> = mapOf(),
        headers: Map<String, String> = mapOf(),
        data: Map<String, Any> = mapOf()
    ): Response {
        return request(
            "POST",
            url,
            params,
            headers,
            data
        )
    }
}
