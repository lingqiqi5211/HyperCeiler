package com.sevtinge.hyperceiler.hook.module.base.manager

import com.sevtinge.hyperceiler.hook.utils.log.AndroidLogUtils
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

object ServiceManager {

    private const val TAG = "HyperCeiler/ServiceManager"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val connectionState = _connectionState.asStateFlow()

    private val isInitialized = AtomicBoolean(false)

    @JvmStatic
    val service: XposedService?
        get() = (connectionState.value as? ConnectionState.Connected)?.service

    @JvmStatic
    val isModuleActivated: Boolean
        get() = connectionState.value is ConnectionState.Connected

    fun init() {
        if (!isInitialized.compareAndSet(false, true)) {
            return
        }

        val listener = object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(boundService: XposedService) {
                AndroidLogUtils.logI(TAG, "LSPosed service connected: ${boundService.frameworkName} v${boundService.frameworkVersion}")
                _connectionState.value = ConnectionState.Connected(boundService)
            }

            override fun onServiceDied(deadService: XposedService) {
                if (service == deadService) {
                    AndroidLogUtils.logE(TAG, "LSPosed service died.")
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }

        XposedServiceHelper.registerListener(listener)
        AndroidLogUtils.logI(TAG, "ServiceManager initialized and listener registered.")
    }
}

sealed interface ConnectionState {
    data object Connecting : ConnectionState
    data class Connected(val service: XposedService) : ConnectionState
    data object Disconnected : ConnectionState
}
