package com.kipia.management.mobile.presentation.navigation

sealed class Screen(val route: String) {
    object DeviceList : Screen("device_list")
    object DeviceDetail : Screen("device_detail/{deviceId}") {
        fun createRoute(deviceId: Int) = "device_detail/$deviceId"
    }
    object DeviceEdit : Screen("device_edit/{deviceId}") {
        fun createRoute(deviceId: Int = -1) = "device_edit/$deviceId" // -1 = новый прибор
    }
    object Schemes : Screen("schemes")
    object Photos : Screen("photos")
    object Settings : Screen("settings")
}