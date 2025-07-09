/*
 * © Project Lumina 2025 — Licensed under GNU GPLv3
 * You are free to use, modify, and redistribute this code under the terms
 * of the GNU General Public License v3. See the LICENSE file for details.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * This is open source — not open credit.
 *
 * If you're here to build, welcome. If you're here to repaint and reupload
 * with your tag slapped on it… you're not fooling anyone.
 *
 * Changing colors and class names doesn't make you a developer.
 * Copy-pasting isn't contribution.
 *
 * You have legal permission to fork. But ask yourself — are you improving,
 * or are you just recycling someone else's work to feed your ego?
 *
 * Open source isn't about low-effort clones or chasing clout.
 * It's about making things better. Sharper. Cleaner. Smarter.
 *
 * So go ahead, fork it — but bring something new to the table,
 * or don’t bother pretending.
 *
 * This message is philosophical. It does not override your legal rights under GPLv3.
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * GPLv3 Summary:
 * - You have the freedom to run, study, share, and modify this software.
 * - If you distribute modified versions, you must also share the source code.
 * - You must keep this license and copyright intact.
 * - You cannot apply further restrictions — the freedom stays with everyone.
 * - This license is irrevocable, and applies to all future redistributions.
 *
 * Full text: https://www.gnu.org/licenses/gpl-3.0.html
 */


package com.project.lumina.client.application

/*
object RealmDefaults {
    const val HOST_URL = "https://pocket.realms.minecraft.net/"

    val DEFAULT_HEADERS = mapOf(
        "Accept" to "*\/*",
        "charset" to "utf-8",
        "client-ref" to "970b2886590b1aa2798004328a8fc7b1d7108011",
        "client-version" to "1.21.72",
        "content-type" to "application/json",
        "user-agent" to "MCPE/UWP",
        "x-clientplatform" to "Windows",
        "x-networkprotocolversion" to "786",
        "Accept-Language" to "en-CA",
        "Host" to "pocket.realms.minecraft.net"
    )
}
@Serializable
data class RealmListResponse(
    val servers: List<Realm>
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
    @Contextual
    val players: Any?,
    val maxPlayers: Int,
    val minigameName: Any?,
    val minigameId: Any?,
    val minigameImage: Any?,
    val activeSlot: Int,
    val slots: Any?,
    val member: Boolean,
    val clubId: Int
)

@Serializable
data class ServerAddress(
    val networkProtocol: String,
    val address: String,
    val pendingUpdate: Boolean,
    val sessionRegionData: SessionRegionData
)
@Serializable
data class SessionRegionData(
    val regionName: String,
    val serviceQuality: Int
)

class RealmManager(private val xboxToken: String) {
    private val client = OkHttpClient()
    private fun makeRequest(
        path: String,
        method: String = "GET",
        headers: Map<String, String> = RealmDefaults.DEFAULT_HEADERS,
        body: String? = null
    ): Response {
        val call = client.newCall(
            Request.Builder()
                .method(method, body?.toRequestBody())
                .url(RealmDefaults.HOST_URL + path)
                .headers(Headers.headersOf(*headers.values.toTypedArray()))
                .addHeader("Authorization", xboxToken)
                .build()
        )
        val res = call.execute()
        return res
    }

    fun getRealms(): Result<List<Realm>> {
        val res = makeRequest("worlds")
        if (res.isSuccessful) {
            val body = res.body?.string()
            if (body == null) {
                return Result.failure(Exception("Realm list response body is null"))
            }
            val list = json.decodeFromString(RealmListResponse.serializer(), body)
            return Result.success(list.servers)
        } else {
            return Result.failure(Exception("Failed to get realms list: ${res.code} ${res.message}"))
        }
    }
    fun getRealm(realmId: Int): Result<Realm> {
        val res = makeRequest("worlds/$realmId")
        if (res.isSuccessful) {
            val body = res.body?.string()
            if (body == null) {
                return Result.failure(Exception("Realm response body is null"))
            }
            val realm = json.decodeFromString(Realm.serializer(), body)
            return Result.success(realm)
        } else {
            return Result.failure(Exception("Failed to get realm: ${res.code} ${res.message}"))
        }
    }
    fun getAddress(realmId: Int): Result<ServerAddress> {
        val res = makeRequest("worlds/$realmId/address")
        if (res.isSuccessful) {
            val body = res.body?.string()
            if (body == null) {
                return Result.failure(Exception("Realm address response body is null"))
            }
            return Result.success(json.decodeFromString(ServerAddress.serializer(), body))
        } else {
            return Result.failure(Exception("Failed to get realm address: ${res.code} ${res.message}"))
        }
    }
}
*/