package com.chetna.frostexpress

import android.app.Activity
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange

/** Cloud save + sign-in. All callbacks run on the main thread. */
interface CloudSaveProvider {
    fun init()
    fun checkSignedIn(cb: (Boolean) -> Unit)
    fun signIn(cb: (Boolean) -> Unit)
    fun load(cb: (String?) -> Unit)
    fun save(json: String)
}

/** Used while Balance.PLAY_GAMES_ENABLED is false. */
object NoCloud : CloudSaveProvider {
    override fun init() {}
    override fun checkSignedIn(cb: (Boolean) -> Unit) = cb(false)
    override fun signIn(cb: (Boolean) -> Unit) = cb(false)
    override fun load(cb: (String?) -> Unit) = cb(null)
    override fun save(json: String) {}
}

/** Google Play Games Services: sign-in plus Saved Games. Needs the setup described in the README. */
class PlayGamesCloud(private val activity: Activity) : CloudSaveProvider {

    private val saveName = "frost_express_progress"

    override fun init() {
        PlayGamesSdk.initialize(activity.applicationContext)
    }

    override fun checkSignedIn(cb: (Boolean) -> Unit) {
        PlayGames.getGamesSignInClient(activity).isAuthenticated().addOnCompleteListener { task ->
            cb(task.isSuccessful && task.result.isAuthenticated)
        }
    }

    override fun signIn(cb: (Boolean) -> Unit) {
        PlayGames.getGamesSignInClient(activity).signIn().addOnCompleteListener { task ->
            cb(task.isSuccessful && task.result.isAuthenticated)
        }
    }

    override fun load(cb: (String?) -> Unit) {
        val client = PlayGames.getSnapshotsClient(activity)
        client.open(saveName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnCompleteListener { task ->
                val snapshot = if (task.isSuccessful) task.result.data else null
                if (snapshot == null) {
                    cb(null)
                } else {
                    val bytes = try {
                        snapshot.snapshotContents.readFully()
                    } catch (e: Exception) {
                        ByteArray(0)
                    }
                    client.discardAndClose(snapshot)
                    cb(if (bytes.isEmpty()) null else String(bytes, Charsets.UTF_8))
                }
            }
    }

    override fun save(json: String) {
        val client = PlayGames.getSnapshotsClient(activity)
        client.open(saveName, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .addOnCompleteListener { task ->
                val snapshot = if (task.isSuccessful) task.result.data else null
                if (snapshot != null) {
                    snapshot.snapshotContents.writeBytes(json.toByteArray(Charsets.UTF_8))
                    client.commitAndClose(
                        snapshot,
                        SnapshotMetadataChange.Builder().setDescription("Little Express progress").build(),
                    )
                }
            }
    }
}
