import os
import re

def replace_in_file(filepath, replacements):
    if not os.path.exists(filepath):
        print(f"⚠️ فایل پیدا نشد: {filepath}")
        return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    for old, new in replacements:
        content = content.replace(old, new)
        
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"✅ اصلاح شد: {filepath}")
    else:
        print(f"⏭️ تغییری لازم نبود: {filepath}")

print("شروع اصلاح خودکار خطاهای Lint...\n")

# 1. فیکس AndroidManifest.xml
replace_in_file("app/src/main/AndroidManifest.xml", [
    ('<manifest xmlns:android="http://schemas.android.com/apk/res/android">', 
     '<manifest xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:tools="http://schemas.android.com/tools">'),
    ('android:enableOnBackInvokedCallback="true">', 
     'android:enableOnBackInvokedCallback="true"\n        tools:targetApi="33">'),
    ('android:allowBackup="false"', 
     'android:allowBackup="false"\n        tools:targetApi="31"')
])

# 2. فیکس فایل‌های Layout (tint و Hardcoded text)
replace_in_file("app/src/main/res/layout/activity_main.xml", [
    ('android:tint="@android:color/white"', 'app:tint="@android:color/white"')
])
replace_in_file("app/src/main/res/layout/camera.xml", [
    ('android:tint="@android:color/white"', 'app:tint="@android:color/white"'),
    ('android:tint="@color/primary_dark"', 'app:tint="@color/primary_dark"'),
    ('android:contentDescription="خروج"', 'android:contentDescription="@string/exit_camera"'),
    ('android:inputType="text"', 'android:inputType="number"\n                    android:autofillHints="none"\n                    android:importantForAutofill="no"')
])
replace_in_file("app/src/main/res/layout/activity_settings.xml", [
    ('android:text="تا"', 'android:text="@string/to_word"')
])

# 3. فیکس کدهای Kotlin (UseKtx و ExperimentalGetImage)
replace_in_file("app/src/main/java/com/srooyesh/seedcounter/NumberRecognitionEngine.kt", [
    ('import android.graphics.Rect', 'import android.graphics.Rect\nimport androidx.camera.core.ExperimentalGetImage'),
    ('    fun process(\n        proxy: ImageProxy,', '    @OptIn(ExperimentalGetImage::class)\n    fun process(\n        proxy: ImageProxy,')
])

replace_in_file("app/src/main/java/com/srooyesh/seedcounter/SettingsStore.kt", [
    ('import android.content.SharedPreferences', 'import android.content.SharedPreferences\nimport androidx.core.content.edit'),
    ('prefs.edit()', 'prefs.edit {'),
    ('    fun save(settings: AppSettings) {', '    fun save(settings: AppSettings) {\n        prefs.edit {'),
    # اصلاح ساده برای KTX edit
])

replace_in_file("app/src/main/java/com/srooyesh/seedcounter/CameraActivity.kt", [
    ('preview.setOnTouchListener { _, event ->', 'preview.setOnTouchListener { view, event ->'),
    ('return false\n        }', 'view.performClick()\n            false\n        }')
])

# 4. اضافه کردن رشته‌های جدید به strings.xml
strings_path = "app/src/main/res/values/strings.xml"
if os.path.exists(strings_path):
    with open(strings_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '<string name="exit_camera">' not in content:
        new_strings = '''
    <string name="exit_camera">خروج</string>
    <string name="to_word">تا</string>
'''
        content = content.replace('</resources>', new_strings + '</resources>')
        with open(strings_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print("✅ رشته‌های جدید به strings.xml اضافه شد.")

# 5. حذف فایل‌های بی‌استفاده
unused_files = [
    "app/src/main/res/drawable/ic_check.xml",
    "app/src/main/res/drawable/scan_reticle.xml"
]
for f in unused_files:
    if os.path.exists(f):
        os.remove(f)
        print(f"🗑️ فایل بی‌استفاده حذف شد: {f}")

# حذف پوشه mipmap-anydpi-v26 و انتقال به mipmap-anydpi
if os.path.exists("app/src/main/res/mipmap-anydpi-v26"):
    os.rename("app/src/main/res/mipmap-anydpi-v26", "app/src/main/res/mipmap-anydpi")
    print("📁 پوشه mipmap-anydpi-v26 به mipmap-anydpi تغییر نام یافت.")

print("\n🎉 عملیات اصلاح فایل‌ها به پایان رسید!")
print("لطفا حالا فایل build.gradle را باز کنید تا نسخه کتابخانه‌ها را آپدیت کنیم.")