package com.example.kotonowa

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.kotonowa.data.local.createNotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * アプリ全体で1つだけ作られるクラス。どの画面よりも先に生成されるため、
 * Hilt が「材料を配る仕組み」を組み立てる起点になる。
 *
 * [Configuration.Provider] を実装しているのは、WorkManager に
 * 「Worker は Hilt 経由で作ってください」と教えるため（grammar §8-(104)）。
 * Worker は OS が作るクラスなので、普通の @Inject では材料を受け取れない。
 */
@HiltAndroidApp
class KotonowaApplication : Application(), Configuration.Provider {

    /**
     * Worker の作り方を知っている工場。Hilt が用意して差し込む。
     * 生成時にはまだ入っていないので lateinit var で受ける。
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** WorkManager がこれを読んで、上の工場を使って Worker を作る。 */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
    }
}
