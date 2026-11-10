package com.carhome.launcher

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GitHub's "latest release" answer, as CarHome reads it. */
class UpdateReleaseTest {

    private fun release(assetName: String = "carhome.apk", url: String = GOOD_URL, notes: String = "CarHome 1.2.9.\n\nversionCode: 12") =
        JSONObject().apply {
            put("tag_name", "carhome-21")
            put("name", "CarHome 1.2.9 (build 21)")
            put("body", notes)
            put("assets", org.json.JSONArray().put(JSONObject().apply {
                put("name", assetName)
                put("browser_download_url", url)
                put("size", 11_156_166)
            }))
        }

    @Test fun readsVersionAndDownload() {
        val r = UpdateRelease.fromJson(release())!!
        assertEquals("1.2.9", r.versionName)
        assertEquals(12, r.versionCode)
        assertEquals(GOOD_URL, r.apkUrl)
        assertEquals(11_156_166L, r.apkSize)
    }

    @Test fun olderReleaseNotesWithoutACodeStillWork() {
        val r = UpdateRelease.fromJson(release(notes = "Automatic build of abc"))!!
        assertNull(r.versionCode)
        assertEquals("1.2.9", r.versionName)
    }

    @Test fun noApkMeansNoUpdate() = assertNull(UpdateRelease.fromJson(release(assetName = "screenshots.zip")))

    @Test fun aDownloadFromElsewhereIsRefused() =
        assertNull(UpdateRelease.fromJson(release(url = "https://example.com/carhome.apk")))

    @Test fun emptyAnswerIsNoUpdate() = assertNull(UpdateRelease.fromJson(JSONObject()))

    private companion object {
        const val GOOD_URL = "https://github.com/ahmednuw1-lab/openlauncher-custom/releases/download/carhome-21/carhome.apk"
    }

    @Test fun readsTheApkDigestWhenGitHubPublishesOne() {
        val hex = "e6b8e3be963f8c4c7860ff62c4c4d07e5c76d07a28a54d7a62ad42faf4bdd1aa"
        val o = release()
        o.getJSONArray("assets").getJSONObject(0).put("digest", "sha256:$hex")
        assertEquals(hex, UpdateRelease.fromJson(o)!!.sha256)
    }

    @Test fun aReleaseWithoutADigestStillWorks() = assertNull(UpdateRelease.fromJson(release())!!.sha256)
}
