package com.nalansitan.chinesepoetry

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口
 */
@HiltAndroidApp
class PoetryApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 应用初始化
    }
}
