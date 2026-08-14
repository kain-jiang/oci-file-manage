package com.tiramission.ocisync

import android.app.Application

/** 应用入口(M0 占位;DI 容器在 M4 引入,见 docs/08-implementation-plan.md)。 */
class OciSyncApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
