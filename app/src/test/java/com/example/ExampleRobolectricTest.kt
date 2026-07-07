package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Asset
import com.example.data.SecurityDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aegis Red Audit", appName)
    }

    @Test
    fun `test database operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = SecurityDatabase.getDatabase(context)
        val dao = db.securityDao()

        val asset = Asset(
            name = "Test Portal",
            url = "example.com",
            description = "Test description",
            category = "Web Application",
            tags = "prod,test"
        )

        val insertedId = dao.insertAsset(asset)
        assertEquals(1L, insertedId)

        val retrieved = dao.getAllAssets().first()
        assertEquals(1, retrieved.size)
        assertEquals("Test Portal", retrieved[0].name)
    }
}
