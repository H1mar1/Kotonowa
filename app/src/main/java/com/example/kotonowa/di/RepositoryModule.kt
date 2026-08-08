package com.example.kotonowa.di

import com.example.kotonowa.data.repository.AuthRepositoryImpl
import com.example.kotonowa.data.repository.ScheduleRepositoryImpl
import com.example.kotonowa.domain.repository.AuthRepository
import com.example.kotonowa.domain.repository.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 「domain 層の interface を頼まれたら、対応する data 層の実装を渡す」という対応表。
 *
 * ViewModel は interface（お品書き）しか知らないので、
 * どの実装が担当するかをここで一箇所にまとめて決めている。
 *
 * こう分けておくと、テストのときは偽物の実装に差し替えられる。
 * 新しい Repository を作ったら、ここに 1 つ足すのを忘れないこと
 * （足し忘れると `[Dagger/MissingBinding]` でビルドが通らない）。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository
}
