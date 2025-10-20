package com.feifan.yiban.Core

import com.alibaba.fastjson.JSONObject
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.crypto.Cipher


class SchoolBasedAuth(private val req: BaseReq) {
    @Throws(Exception::class)
    fun auth(mobile: String, password: String) {
        val html = req.get(
            url = "https://oauth.yiban.cn/code/html",
            params = mapOf(
                "client_id" to "95626fa3080300ea",
                "redirect_uri" to "https://f.yiban.cn/iapp7463"
            )
        ).body!!.string()
        val doc: Document = Jsoup.parse(html)
        // 提取key
        val keyElement: Element? = doc.selectFirst("input#key")
        val key: String = keyElement!!.attr("value")
        // 提取page_use
        val pattern: Pattern = Pattern.compile("var page_use = '([^']+)'")
        val script: Element = doc.select("script").stream()
            .filter { e -> e.html().contains("var page_use") }
            .findFirst()
            .orElse(null)

        val matcher: Matcher = pattern.matcher(script.html())
        val pageUse: String = if (matcher.find()) matcher.group(1) else ""
        val encrypted = encryptWithRsa(password, key)

        val loginData = req.post(
            url = "https://oauth.yiban.cn/code/usersure",
            params = mapOf("ajax_sign" to pageUse),
            data = mapOf(
                "oauth_uname" to mobile,
                "oauth_upwd" to encrypted,
                "client_id" to "95626fa3080300ea",
                "redirect_uri" to "https://f.yiban.cn/iapp7463",
                "state" to "",
                "scope" to "",
                "display" to "authorize"
            )
        ).parseJson()

        if (loginData["code"] != "s200") {
            throw Exception("登录失败: ${loginData["msgCN"]}")
        }
        val redirectResponse = req.get(
            url = "https://f.yiban.cn/iframe/index",
            params = mapOf("act" to "iapp7463")
        )

        val location = redirectResponse.headers["Location"]
            ?: throw Exception("缺少重定向Location头")

        val verifyRequest = Regex("verify_request=(.*?)&")
            .find(location)?.groupValues?.get(1)
            ?: throw Exception("无法提取verify_request")

        val authResponse = req.get(
            url = "https://api.uyiban.com/base/c/auth/yiban",
            params = mapOf(
                "verifyRequest" to verifyRequest,
                "CSRF" to SchoolBased.csrf()
            ),
        ).parseJson()

        if (authResponse["code"] != 0) {
            throw Exception("最终认证失败: ${authResponse["msg"]}")
        }
    }

    private fun encryptWithRsa(data: String, publicKey: String): String {
        val publicKeyContent = publicKey
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyContent))

        val key = KeyFactory.getInstance("RSA")
            .generatePublic(keySpec)

        return Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.ENCRYPT_MODE, key)
            Base64.getEncoder().encodeToString(doFinal(data.toByteArray(Charsets.UTF_8)))
        }
    }

//    private fun Response<String?>.parseJson(): Map<String, Any> {
//        val responseBody = body
//        return JSONObject.parseObject(responseBody)
//    }
}

private fun Response.parseJson(): JSONObject {
            val responseBody = body!!.string()
        return JSONObject.parseObject(responseBody)
}
