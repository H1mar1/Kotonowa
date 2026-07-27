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
}
