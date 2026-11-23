package com.ocalix.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow

class OcalixApp : Application() {
    lateinit var store: Store
    lateinit var player: PlayerManager
    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 16)

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        Api.base = Api.normalize(store.server.value)
        player = PlayerManager(this, store, toasts)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                player.onAppBackground()
            }
        })
    }
}
