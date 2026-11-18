package dev.imhamid.messages

import android.app.Application
import android.content.Context
import dev.imhamid.messages.data.AppDatabase
import dev.imhamid.messages.data.ContactsRepo
import dev.imhamid.messages.data.Repository

class App : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        repo = Repository(this, AppDatabase.build(this), ContactsRepo(this), settings)
    }
}

val Context.app: App get() = applicationContext as App
