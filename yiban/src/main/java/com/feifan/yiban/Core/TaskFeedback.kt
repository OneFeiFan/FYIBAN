package com.feifan.yiban.Core

import com.alibaba.fastjson.JSONObject
import com.feifan.yiban.tool.generateRandomPointsInCenter
import okhttp3.Response
import kotlin.text.set

//import kong.unirest.HttpResponse

class TaskFeedback(private val req: BaseReq) {

    // 获取签到任务时间范围
    fun getSignTask(): Map<String, Any> {
        val response = req.get(
            "https://api.uyiban.com/nightAttendance/student/index/signPosition",
            params = mapOf(
                "CSRF" to SchoolBased.csrf()
            ),
            headers = SchoolBased.headers()
        )
        val body = response.body!!.string()

        val json = JSONObject.parseObject(body)

        //在签到后无法获取位置信息，所以返回空值
        if (json.getIntValue("code") == 0) {
            val result = JSONObject()
            val data = json.getJSONObject("data")
            if (data.getString("Msg") == "已签到") {
                result["Position"] = JSONObject()
            } else {
                result["Position"] = data.getJSONArray("Position")[0] as JSONObject
            }
            result["Range"] = data.getJSONObject("Range")
            return result.toMap()
        }
        throw Exception("获取签到任务失败: ${json.getString("msg")}")
    }

    // 提交签到（返回结果消息）
    fun submitSign(): String {
        val sign = getSignTask()
        val timeRange = sign["Range"] as JSONObject
        val location = sign["Position"] as JSONObject
        if (location.isEmpty()) {
            return "已经签到"//使用空值判断签到状态其实不太妥
        }
        val currentTime = System.currentTimeMillis() / 1000
        val point = generateRandomPointsInCenter(location.getJSONArray("Points"))
        val data = JSONObject()
        data["Reason"] = ""
        data["AttachmentFileName"] = ""
        data["LngLat"] = point.toString()
        data["Address"] = location.getString("Address")

        // 时间范围校验
        if (currentTime > timeRange["StartTime"].toString().toLong()
            && currentTime < timeRange["EndTime"].toString().toLong()
        ) {
            val formBody = mapOf(
                "Code" to "",
                "PhoneModel" to "",
                "SignInfo" to data.toJSONString(),
                "OutState" to "1"
            )

            val response = req.post(
                "https://api.uyiban.com/nightAttendance/student/index/signIn",
                params = mapOf("CSRF" to SchoolBased.csrf()),
                headers = SchoolBased.headers(),
                data = formBody
            )
            return parseSignResponse(response)
        }
        return "签到失败：未到签到时间"
    }

    // 解析签到响应
    private fun parseSignResponse(response: Response): String {
        val body = response.body!!.string()
        val json = JSONObject.parseObject(body)
        return if (json.getIntValue("code") == 0 && json.getBoolean("data")) {
            "签到成功"
        } else {
            json.getString("msg")
        }
    }


}
