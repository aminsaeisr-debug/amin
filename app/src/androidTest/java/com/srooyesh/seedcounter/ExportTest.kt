package com.srooyesh.seedcounter

import androidx.core.widget.NestedScrollView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** فایل‌های اکسلی که توسط ExcelExporter ساخته می‌شوند (پیشوند واقعی). */
    private fun existingReports(): Array<File>? =
        context.filesDir.listFiles { f ->
            f.name.startsWith("seed-mas-report_") && f.name.endsWith(".xlsx")
        }

    @Before
    fun cleanBefore() {
        // پاک‌سازی جلسه فعال، رکوردها و فایل‌های گزارش پیش از هر اجرا.
        SeedRepository(context).deleteAll()
        existingReports()?.forEach { it.delete() }
    }

    @After
    fun cleanAfter() {
        SeedRepository(context).deleteAll()
        existingReports()?.forEach { it.delete() }
    }

    @Test
    fun testExportExcel() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // ۱. شروع جلسه
            onView(withId(R.id.variety)).perform(typeText("Test"), closeSoftKeyboard())
            onView(withId(R.id.customer)).perform(typeText("Customer"), closeSoftKeyboard())
            scrollTo(R.id.start)
            onView(withId(R.id.start)).perform(click())
            assertTrue(
                "جلسه فعال ساخته نشد",
                waitFor { SeedRepository(context).getActiveSession() != null }
            )

            // ۲. اضافه کردن یک رکورد دستی
            scrollTo(R.id.manual)
            onView(withId(R.id.manual)).perform(typeText("1234"), closeSoftKeyboard())
            onView(withId(R.id.add)).perform(click())
            assertTrue(
                "رکورد دستی اضافه نشد",
                waitFor { SeedRepository(context).getAllRows().isNotEmpty() }
            )

            // ۳. زدن دکمه خروجی اکسل
            scrollTo(R.id.export)
            onView(withId(R.id.export)).perform(click())

            // ۴. انتظار برای ساخته شدن فایل با polling (حداکثر ۱۰ ثانیه)
            val created = waitFor(10_000L) {
                existingReports()?.any { it.length() > 0L } == true
            }
            assertTrue("هیچ فایل اکسلی ساخته نشد!", created)

            val reports = existingReports()
            assertTrue("فایل اکسل خالی است!", reports != null && reports.isNotEmpty())
            assertTrue("حجم فایل اکسل صفر است!", reports!!.first().length() > 0L)
        }
    }

    /**
     * اسکرول تا زمانی که ویو در صفحه دیده شود.
     * NestedScrollView با Espresso.scrollTo() سازگار نیست؛ از swipeUp استفاده می‌کنیم.
     */
    private fun scrollTo(viewId: Int) {
        repeat(6) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()))
                return
            } catch (_: Exception) {
                onView(isAssignableFrom(NestedScrollView::class.java)).perform(swipeUp())
                Thread.sleep(200L)
            }
        }
        // اگر بعد از ۶ سوایپ هم دیده نشد، با خطای واضح fail شود.
        onView(withId(viewId)).check(matches(isDisplayed()))
    }

    /** انتظار شرطی؛ پایدارتر از Thread.sleep ثابت روی CI. */
    private fun waitFor(timeoutMs: Long = 5_000L, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(100L)
        }
        return condition()
    }
}
