package com.example.kotonowa.di

import com.example.kotonowa.data.repository.AuthRepositoryImpl
import com.example.kotonowa.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 「`AuthRepository` を頼まれたら `AuthRepositoryImpl` を渡す」という対応表。
 *
 * ViewModel は interface である `AuthRepository` しか知らないので、
 * どの実装を使うかをここで一箇所にまとめて決めている。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    // TODO: 「ScheduleRepository を頼まれたら ScheduleRepositoryImpl を渡す」対応を 1 つ足す
    //   上の bindAuthRepository とまったく同じ形。3 か所を Schedule に置き換えるだけ。
    //   - 関数名（自分で決める名前。小文字始まり。§1-⑧）
    //   - 引数の型 … 実際の担当者（data 層の実装クラス）
    //   - 戻り値の型 … お品書き（domain 層の interface）
    //   import も 2 行必要になる（Alt + Enter で入る）
}
