package com.example.kotonowa.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase の SDK が用意しているオブジェクトを Hilt に登録するモジュール。
 *
 * FirebaseAuth などは自分で `new` できず `getInstance()` で取り出す作りになっているため、
 * 「取り出し方」をここで教えておく。
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // プッシュ通知の受付（Step 41）。端末の宛先（FCM トークン）をもらうのに使う
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance()

    // サーバーの関数の呼び出し口（Step 46）。関数を置いた場所（東京）を指定する。
    // 指定しないと既定の us-central1 に電話してしまい、「そんな関数は無い」（NOT_FOUND）になる
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("asia-northeast1")
}
