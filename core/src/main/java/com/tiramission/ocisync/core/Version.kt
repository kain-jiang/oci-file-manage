package com.tiramission.ocisync.core

import kotlinx.serialization.Serializable

/** 核心模块冒烟占位(M0):验证 kotlinx-serialization 插件与 JVM 工具链。 */
@Serializable
data class BuildInfo(val name: String, val version: String)

object OciFileManageCore {
    const val VERSION = "0.1.0"
}
