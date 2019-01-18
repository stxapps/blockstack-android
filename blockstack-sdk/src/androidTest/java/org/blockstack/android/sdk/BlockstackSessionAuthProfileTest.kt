package org.blockstack.android.sdk;

import android.content.Context
import android.preference.PreferenceManager
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import kotlinx.coroutines.experimental.runBlocking
import org.blockstack.android.sdk.model.Profile
import org.blockstack.android.sdk.model.Proof
import org.blockstack.android.sdk.model.toBlockstackConfig
import org.blockstack.android.sdk.test.TestActivity
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.notNullValue
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

val A_VALID_BLOCKSTACK_SESSION_JSON = "{\"transitKey\":\"000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f\",\"userData\":{\"username\":null,\"profile\":{\"@type\":\"Person\",\"@context\":\"http:\\/\\/schema.org\",\"apps\":{\"https:\\/\\/app.graphitedocs.com\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1Fuzd6sJXqtXhgoFpb8W19Ao4BCG3EHQGf\\/\",\"https:\\/\\/app.misthos.io\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/1CkGhNN71a1dpgXQjncunYZpXakJvwrpmc\\/\"},\"image\":[{\"@type\":\"ImageObject\",\"name\":\"avatar\",\"contentUrl\":\"https:\\/\\/gaia.blockstack.org\\/hub\\/12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\\/\\/avatar-0\"}],\"name\":\"fm\"},\"decentralizedID\":\"did:btc-addr:12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"identityAddress\":\"12Gpr9kYXJJn4fWec4nRVwHLSrrTieeywt\",\"appPrivateKey\":\"89f92476f13f5b173e53926ad7d6e22baf78c6b1dcdf200c38dc73d2bf47d43b\",\"coreSessionToken\":null,\"authResponseToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiI3MWI3NWY1Yi0xNDJkLTQxMzQtOGZkYy0zYWUyMWRmZTkzNjAiLCJpYXQiOjE1MzcxNjkxODYsImV4cCI6MTUzOTc2MTE4NSwiaXNzIjoiZGlkOmJ0Yy1hZGRyOjEyR3ByOWtZWEpKbjRmV2VjNG5SVndITFNyclRpZWV5d3QiLCJwcml2YXRlX2tleSI6IjdiMjI2OTc2MjIzYTIyMzEzOTMzMzM2NTM5MzYzNTY2NjMzNzM5NjM2MjYyMzAzNjY0NjMzMjM0MzIzNzY2NjY2MjYzMzczMjMyMzgzOTIyMmMyMjY1NzA2ODY1NmQ2NTcyNjE2YzUwNGIyMjNhMjIzMDMyMzQzMTY1MzY2NjYzMzEzODM0MzAzMDMyMzQzMTY0NjYzNzMzMzMzNTY1NjM2MjY2MzQzMzM0NjY2NDM5MzU2MjY0NjUzMjY2MzIzNTMxNjU2MzM3MzczNjMxMzQzNDM0MzAzMjY2NjUzNTM3NjE2MzYzMzA2MjY2MzQzNjYxMzQyMjJjMjI2MzY5NzA2ODY1NzI1NDY1Nzg3NDIyM2EyMjM5MzkzMTYxMzM2MzM1NjYzOTY2NjI2MzM2MzYzMzY2MzMzMDY0MzgzNzMxMzgzNzMyNjIzODM5MzMzMTYxMzMzMTMyNjUzMjM0NjM2MjM4MzAzNzM1NjQzODY0MzkzNzM1Mzg2MjM3NjEzMDY1NjMzMTM0Mzg2MTM2MzEzMjM2MzIzMjY1MzYzMjYyMzQ2NjY1NjYzNjMzNjMzMTYzNjYzMjMzNjEzMTM3MzM2NTM0NjUzOTMwMzIzMzM5MzQ2NDY1NjEzMTMxMzQzOTYzNjE2NTYxMzMzMzY2MzMzODY2NjIzMTMwMzczNTM0MzU2NDM1MzUzNDM3NjYzNjY2NjMzODMwMzg2MTM2MzIzNjM2MzAzMDMzNjMzNzM5MzMzNzM4MzYzMzM4MzMzNDMyNjEzNjYzMzMzOTM4MzM2MjM5MjIyYzIyNmQ2MTYzMjIzYTIyNjYzODMwMzQ2MjMzMzY2MjY0MzgzMjM2NjQzNTM3MzIzMzYxMzQzOTM4MzczOTYzMzAzMDYzNjQ2MzMzNjYzMDYxNjQzNzY0NjE2MTYyNjE2NDM0MzY2NDMzMzczNjY0MzUzMTYxNjI2NjYzNjYzMTM4MzM2MTM4MzAzNDMwMzgyMjJjMjI3NzYxNzM1Mzc0NzI2OTZlNjcyMjNhNzQ3Mjc1NjU3ZCIsInB1YmxpY19rZXlzIjpbIjAyODcyNmQyMGZhMTI4Yjc1OWViOGIwOWZjY2Y2ODU2ZTQzMjM1YmI5OTkxNjI0OWNmNjNhM2NiMjA0MTY0Y2I4ZSJdLCJwcm9maWxlIjpudWxsLCJ1c2VybmFtZSI6bnVsbCwiY29yZV90b2tlbiI6bnVsbCwiZW1haWwiOiJmcmllZGdlckBnbWFpbC5jb20iLCJwcm9maWxlX3VybCI6Imh0dHBzOi8vZ2FpYS5ibG9ja3N0YWNrLm9yZy9odWIvMTJHcHI5a1lYSkpuNGZXZWM0blJWd0hMU3JyVGllZXl3dC9wcm9maWxlLmpzb24iLCJodWJVcmwiOiJodHRwczovL2h1Yi5ibG9ja3N0YWNrLm9yZyIsInZlcnNpb24iOiIxLjIuMCJ9.QM8wx-EDjZrHk1ubK99divaejN5XAcCn_OkYAiPXQUNtzENUy8ogyrHzB4xC7mwfMckzFsMxEOdzwVW_Xcjqqg\",\"hubUrl\":\"https:\\/\\/hub.blockstack.org\"}}"

private val PRIVATE_KEY = "a5c61c6ca7b3e7e55edee68566aeab22e4da26baa285c7bd10e8d2218aa3b229"
private val PUBLIC_KEY = "027d28f9951ce46538951e3697c62588a87f1f1f295de4a14fdd4c780fc52cfe69"
private val DECENTRALIZED_ID = "did:btc-addr:1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
private val BITCOIN_ADDRESS = "1NZNxhoxobqwsNvTb16pdeiqvFvce3Yg8U"
private val TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJqdGkiOiJhMzU4MzdmNS1jOGNmLTRiYTMtYjk4ZS03OTY5YTUzZmVjNDgiLCJpYXQiOiIyMDE3LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJleHAiOiIyMDE4LTAzLTA0VDE2OjEzOjA2Ljc5MFoiLCJzdWJqZWN0Ijp7InB1YmxpY0tleSI6IjAzZTdhNGQ3OTgzMzY5ZDMzZWQxMzAyMDg4NTk4NWQ2OGY4YjA1ZGVlNjE2OGY3NWY5ZDk3ZTFhMDcyY2RmY2RjNSJ9LCJpc3N1ZXIiOnsicHVibGljS2V5IjoiMDNlN2E0ZDc5ODMzNjlkMzNlZDEzMDIwODg1OTg1ZDY4ZjhiMDVkZWU2MTY4Zjc1ZjlkOTdlMWEwNzJjZGZjZGM1In0sImNsYWltIjp7IkBjb250ZXh0IjoiaHR0cDovL3NjaGVtYS5vcmcvIiwiQHR5cGUiOiJDcmVhdGl2ZVdvcmsiLCJuYW1lIjoiQmFsbG9vbiBEb2ciLCJjcmVhdG9yIjpbeyJAdHlwZSI6IlBlcnNvbiIsIkBpZCI6InRoZXJlYWxqZWZma29vbnMuaWQiLCJuYW1lIjoiSmVmZiBLb29ucyJ9XSwiZGF0ZUNyZWF0ZWQiOiIxOTk0LTA1LTA5VDAwOjAwOjAwLTA0MDAiLCJkYXRlUHVibGlzaGVkIjoiMjAxNS0xMi0xMFQxNDo0NDoyNi0wNTAwIn19.MF7ru91rk8IIKNEEqo9wjLHkvW3jSlcDJmeZZeOVSj9KlXApBp67q_3ke0-LzSO_YyYsUnGOplMYiNxY1XynAA"
private val TOKEN_PUBLIC_KEY = "03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5"
private val DECODED_TOKEN = "{\"header\":{\"typ\":\"JWT\",\"alg\":\"ES256K\"},\"payload\":{\"jti\":\"a35837f5-c8cf-4ba3-b98e-7969a53fec48\",\"iat\":\"2017-03-04T16:13:06.790Z\",\"exp\":\"2018-03-04T16:13:06.790Z\",\"subject\":{\"publicKey\":\"03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5\"},\"issuer\":{\"publicKey\":\"03e7a4d7983369d33ed13020885985d68f8b05dee6168f75f9d97e1a072cdfcdc5\"},\"claim\":{\"@context\":\"http:\\/\\/schema.org\\/\",\"@type\":\"CreativeWork\",\"name\":\"Balloon Dog\",\"creator\":[{\"@type\":\"Person\",\"@id\":\"therealjeffkoons.id\",\"name\":\"Jeff Koons\"}],\"dateCreated\":\"1994-05-09T00:00:00-0400\",\"datePublished\":\"2015-12-10T14:44:26-0500\"}},\"signature\":\"MF7ru91rk8IIKNEEqo9wjLHkvW3jSlcDJmeZZeOVSj9KlXApBp67q_3ke0-LzSO_YyYsUnGOplMYiNxY1XynAA\"}"

@RunWith(AndroidJUnit4::class)
class BlockstackSessionAuthProfileTest {
    @get:Rule
    val rule = ActivityTestRule(TestActivity::class.java)

    private lateinit var session: BlockstackSession

    @Before
    fun setup() {
        session = BlockstackSession(rule.activity,
                "https://flamboyant-darwin-d11c17.netlify.com".toBlockstackConfig(emptyArray()),
                sessionStore = sessionStoreforIntegrationTests(rule),
                executor = IntegrationTestExecutor(rule))
    }

    @After
    fun teardown() {
        session.release()
    }

    @Test
    fun loadedIsTrueAfterSessionCreated() {
        Assert.assertThat(session.loaded, Matchers.`is`(true))
    }

    @Test
    fun userIsSignedInAfterSessionCreated() {
        Assert.assertThat(session.isUserSignedIn(), Matchers.`is`(true))
    }

    @Test
    fun userDataCanBeLoadedAfterSessionCreated() {
        Assert.assertThat(session.loadUserData(), Matchers.`is`(notNullValue()))
    }

    @Test
    fun testHandlingPendingSignInWithInvalidToken() {
        val latch = CountDownLatch(1)
        var error: String? = null
        session.handlePendingSignIn("authResponse") {
            latch.countDown()
            error = it.error
        }
        latch.await()
        Assert.assertThat(error, Matchers.`is`("SyntaxError: Unexpected token j in JSON at position 0"))
    }

    @Test
    fun loadUserDataIsNullAfterSignOut() {
        session.signUserOut()
        assertThat(session.loadUserData(), `is`(nullValue()))
    }

    @Test
    fun isUserSignedInIsFalseAfterSignOut() {
        assertThat(session.isUserSignedIn(), `is`(true))
        session.signUserOut()
        assertThat(session.isUserSignedIn(), `is`(false))
    }

    @Test
    fun verifyProofsReturnsEmptyListForEmptyProfile() {
        val latch = CountDownLatch(1)
        var proofList: ArrayList<Proof>? = null

        session.validateProofs(Profile(JSONObject("{}")), BITCOIN_ADDRESS, null) { proofs ->
            if (proofs.hasValue) {
                proofList = proofs.value
                latch.countDown()
            } else {
                latch.countDown()
            }
        }

        latch.await()
        assertThat(proofList, notNullValue())
        assertThat(proofList!!.size, `is`(0))
    }

    @Test
    fun verifyProofsReturnsAllProofsForFriedger() {
        val latch = CountDownLatch(1)
        var proofList: ArrayList<Proof>? = null
        val profile = "{\"@type\":\"Person\",\"@context\":\"http://schema.org\",\"name\":\"Friedger Müffke\",\"description\":\"Entredeveloper in Europe\",\"image\":[{\"@type\":\"ImageObject\",\"name\":\"avatar\",\"contentUrl\":\"https://gaia.blockstack.org/hub/1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v/0/avatar-0\"}],\"account\":[{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"twitter\",\"identifier\":\"fmdroid\",\"proofType\":\"http\",\"proofUrl\":\"https://twitter.com/fmdroid/status/927285474854670338\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"facebook\",\"identifier\":\"friedger.mueffke\",\"proofType\":\"http\",\"proofUrl\":\"https://www.facebook.com/friedger.mueffke/posts/10155370909214191\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"github\",\"identifier\":\"friedger\",\"proofType\":\"http\",\"proofUrl\":\"https://gist.github.com/friedger/d789f7afd1aa0f23dd3f87eb40c2673e\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"bitcoin\",\"identifier\":\"1MATdc1Xjen4GUYMhZW5nPxbou24bnWY1v\",\"proofType\":\"http\",\"proofUrl\":\"\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"pgp\",\"identifier\":\"5371148B3FC6B5542CADE04F279B3081B173CFD0\",\"proofType\":\"http\",\"proofUrl\":\"\"},{\"@type\":\"Account\",\"placeholder\":false,\"service\":\"ethereum\",\"identifier\":\"0x73274c046ae899b9e92EaAA1b145F0b5f497dd9a\",\"proofType\":\"http\",\"proofUrl\":\"\"}],\"apps\":{\"https://app.graphitedocs.com\":\"https://gaia.blockstack.org/hub/17Qhy4ob8EyvScU6yiP6sBdkS2cvWT9FqE/\",\"https://www.stealthy.im\":\"https://gaia.blockstack.org/hub/1KyYJihfZUjYyevfPYJtCEB8UydxqQS67E/\",\"https://www.chat.hihermes.co\":\"https://gaia.blockstack.org/hub/1DbpoUCdEpyTaND5KbZTMU13nhNeDfVScD/\",\"https://app.travelstack.club\":\"https://gaia.blockstack.org/hub/1QK5n11Xn1p5aP74xy14NCcYPndHxnwN5y/\"}}"

        session.validateProofs(Profile(JSONObject(profile)), "1Maw8BjWgj6MWrBCfupqQuWANthMhefb2v", "friedger.id") { proofs ->
            if (proofs.hasValue) {
                proofList = proofs.value
                latch.countDown()
            } else {
                latch.countDown()
            }
        }

        latch.await()
        assertThat(proofList, notNullValue())
        assertThat(proofList!!.size, `is`(3))
        assertThat(proofList!![0].service, `is`("twitter"))
        assertThat(proofList!![0].valid, `is`(true))
        assertThat(proofList!![1].service, `is`("facebook"))
        assertThat(proofList!![1].valid, `is`(false)) // facebook friedger.mueffke is indeed invalid
        assertThat(proofList!![2].service, `is`("github"))
        assertThat(proofList!![2].valid, `is`(true))
    }

    @Test
    fun verifyProfileTokenReturnsToken() {
        val profileToken = session.verifyProfileToken(TOKEN, TOKEN_PUBLIC_KEY)
        assertThat(profileToken.json.toString(), `is`(DECODED_TOKEN))
    }
}

class IntegrationTestExecutor(var rule: ActivityTestRule<*>) : Executor {
    override fun onMainThread(function: (Context) -> Unit) {
        function(rule.activity)
    }

    override fun onNetworkThread(function: suspend () -> Unit) {
        runBlocking {
            function()
        }
    }

    override fun onV8Thread(function: () -> Unit) {
        function()
    }
}

fun sessionStoreforIntegrationTests(rule: ActivityTestRule<*>): SessionStore {
    val prefs = PreferenceManager.getDefaultSharedPreferences(rule.activity)
    prefs.edit().putString("blockstack_session", A_VALID_BLOCKSTACK_SESSION_JSON)
            .apply()
    return SessionStore(prefs)
}