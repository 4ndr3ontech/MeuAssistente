package com.example.meuassistente

import android.app.Application
import com.example.meuassistente.di.AppContainer

class MeuAssistenteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
