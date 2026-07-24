package com.example.kotonowa

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * アプリ全体で1つだけ作られるクラス。どの画面よりも先に生成されるため、
 * Hilt が「材料を配る仕組み」を組み立てる起点になる。
 */
@HiltAndroidApp
class KotonowaApplication : Application()
