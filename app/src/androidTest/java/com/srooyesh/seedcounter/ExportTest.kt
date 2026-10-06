package com.srooyesh.seedcounter

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportTest {

    @Test
    fun testExportExcel() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // ۱. شروع جلسه
            onView(withId(R.id.variety)).perform(typeText("Test"), closeSoftKeyboard())
            onView(withId(R.id.customer)).perform(typeText("Customer"), closeSoftKeyboard())
            onView(withId(R.id.start)).perform(click())
            Thread.sleep(1500)

            // ۲. اضافه کردن یه رکورد دستی
            onView(withId(R.id.manual)).perform(typeText("1234"), closeSoftKeyboard())
            onView(withId(R.id.add)).perform(click())
            Thread.sleep(1500)

            // ۳. زدن دکمه خروجی اکسل
            onView(withId(R.id.export)).perform(click())
            Thread.sleep(3000)

            // ۴. بررسی فایل ساخته‌شده
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val files = context.filesDir.listFiles { f ->
                f.name.startsWith("SeedExitReport_") && f.name.endsWith(".xlsx")
            }

            assertTrue("هیچ فایل اکسلی ساخته نشد!", files != null && files.isNotEmpty())
            assertTrue("فایل اکسل خالی است!", files!!.first().length() > 0)
        }
    }
}
