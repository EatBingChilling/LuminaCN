package com.project.lumina.client.constructors

import com.project.lumina.client.model.Account
import com.project.lumina.relay.LuminaRelay
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class RealmPlayer(
    val uuid: String,
    val name: String?,
    val operator: Boolean,
    val accepted: Boolean,
    val online: Boolean,
    val permission: String
)

@Serializable
data class Realm(
    val id: Int,
    val remoteSubscriptionId: String,
    val owner: String,
    val ownerUUID: String,
    val name: String,
    val motd: String,
    val defaultPermission: String,
    val state: String,
    val daysLeft: Int,
    val expired: Boolean,
    val expiredTrial: Boolean,
    val gracePeriod: Boolean,
    val worldType: String,
    val players: List<RealmPlayer>,
    val maxPlayers: Int,
    val minigameName: String?,
    val minigameId: String?,
    val minigameImage: String?,
    val activeSlot: Int,
    val slots: List<@Contextual Any>,
    val member: Boolean,
    val clubId: Int
)

private const val REALMS_ENDPOINT = "https://pocket.realms.minecraft.net"

class RealmManager(private val account: Account) {

    private val token: String
        get() = "Bearer ${account.refresh()}"


    fun getRealms(): List<Realm>? {
        val request = request(
            "$REALMS_ENDPOINT/api/realms",
            "GET",
            null,
            Headers.Builder().build()
        )
        val response = OkHttpClient().newCall(request).execute()
        if (!response.isSuccessful) {
            println("Error getting realms: ${response.code} ${response.message}")
            return null
        }
        val json = response.body?.string()
        if (json == null) {
            println("Error getting realms: No response body")
            return null
        }
        val realmList = Json.decodeFromString<List<Realm>>(json)
        return realmList
    }

    fun getRealm(id: Int): Realm? {
        val request = request(
            "$REALMS_ENDPOINT /worlds/$id",
            "GET",
            null,
            Headers.Builder().build()
        )
        val response = OkHttpClient().newCall(request).execute()
        if (!response.isSuccessful) {
            println("Error getting realm: ${response.code} ${response.message}")
            return null
        }
        val json = response.body?.string()
        if (json == null) {
            println("Error getting realm: No response body")
            return null
        }
        val realm = Json.decodeFromString<Realm>(json)
        return realm
    }

    fun getRealmAddress(id: Int): String? {
        val request = request(
            "$REALMS_ENDPOINT/server/$id/join",
            "GET",
            null,
            Headers.Builder().build()
        )
        val response = OkHttpClient().newCall(request).execute()
        if (!response.isSuccessful) {
            println("Error getting realm address: ${response.code} ${response.message}")
            return null
        }
        val json = response.body?.string()
        if (json == null) {
            println("Error getting realm address: No response body")
            return null
        }
        val address = Json.decodeFromString<Map<String, String>>(json)["address"]
        return address
    }

    fun acceptRealmInvite(inviteCode: String): Realm? {
        val request = request(
            "$REALMS_ENDPOINT/ /invites/v1/link/accept/$inviteCode",
            "POST",
            null,
            Headers.Builder().build()
        )
        val response = OkHttpClient().newCall(request).execute()
        if (!response.isSuccessful) {
            println("Error accepting realm invite: ${response.code} ${response.message}")
            return null
        }
        val json = response.body?.string()
        if (json == null) {
            println("Error accepting realm invite: No response body")
            return null
        }
        val realm = Json.decodeFromString<Realm>(json)
        return realm
    }


    private fun defaultHeaders(): Headers {
        return Headers.Builder().apply {
            add("authorization", token)
            add("content-type", "application/json")
            add("accept", "*/*")
            add("user-agent", "MCPE/UWP")
            add("client-version", LuminaRelay.DefaultCodec.minecraftVersion)
        }.build()
    }

    private fun request(url: String, method: String, body: String?, headers: Headers): Request {
        return Request.Builder()
            .url(url)
            .method(method, body?.toRequestBody("json".toMediaType()))
            .headers(defaultHeaders())
            .headers(headers)
            .build()
    }
}
