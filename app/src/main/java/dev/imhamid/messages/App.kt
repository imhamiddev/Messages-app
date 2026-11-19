package dev.imhamid.messages

import android.app.Application
import android.content.Context
import dev.imhamid.messages.data.AppDatabase
import dev.imhamid.messages.data.ContactsRepo
import dev.imhamid.messages.data.Repository
import dev.imhamid.messages.sms.Notifier

class App : Application() {
    lateinit var settings: SettingsStore
        private set
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        repo = Repository(this, AppDatabase.build(this), ContactsRepo(this), settings)
        Notifier.ensureChannel(this)
    }
}

val Context.app: App get() = applicationContext as App
