package com.example.motogokmp

import com.example.motogokmp.network.ParkingApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ParkingApiTest {

    @Test
    fun testFetchTaipeiParking() = runTest {
        val api = ParkingApi()
        val result = api.fetchTaipeiParking()

        // 驗證 API 回傳的列表中包含資料
        assertTrue(result.isNotEmpty(), "停車場列表不應為空")

        // 驗證第一筆資料的 ID 與名稱不為空字串
        val firstItem = result.first()
        assertTrue(firstItem.id.isNotEmpty(), "停車場 ID 應該要有值")
        println("成功抓取停車場：${firstItem.name}，剩餘車位：${firstItem.availableSpaces}")
    }
}