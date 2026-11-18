package dev.imhamid.messages.sms

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimInfo(val subId: Int, val slot: Int, val name: String)

object SimManager {
    @SuppressLint("MissingPermission")
    fun list(ctx: Context): List<SimInfo> {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return runCatching {
            val sm = ctx.getSystemService(SubscriptionManager::class.java)
            sm?.activeSubscriptionInfoList.orEmpty().map {
                val slot = it.simSlotIndex + 1
                val label = (it.displayName ?: it.carrierName)?.toString().orEmpty()
                SimInfo(it.subscriptionId, slot, label.ifBlank { "SIM $slot" })
            }.sortedBy { it.slot }
        }.getOrDefault(emptyList())
    }
}
