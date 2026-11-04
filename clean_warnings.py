import os
import re

print("شروع پاکسازی هوشمند هشدارهای Lint...\n")

# ---------- ۱. پاک کردن رشته‌های بی‌استفاده از strings.xml ----------
strings_path = "app/src/main/res/values/strings.xml"
# لیستی از رشته‌هایی که لینت گفته استفاده نشدن
unused_strings = [
    "app_title", "active_status_title", "auto_focus", "waiting_for_both",
    "number_label", "barcode_label", "frame_width", "frame_height",
    "recognition_number_short", "recognition_barcode_short", "recognition_both_short",
    "quick_scan_label"
]

if os.path.exists(strings_path):
    with open(strings_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    for s in unused_strings:
        # پاک کردن خطی که شامل نام رشته است (به همراه فاصله‌های اضافی)
        content = re.sub(r'\s*<string name="' + s + r'">.*?</string>', '', content)
    
    with open(strings_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ رشته‌های بی‌استفاده از strings.xml پاک شدند.")

# ---------- ۲. پاک کردن رنگ‌های بی‌استفاده از colors.xml ----------
colors_path = "app/src/main/res/values/colors.xml"
unused_colors = ["warning", "camera_scrim"]

if os.path.exists(colors_path):
    with open(colors_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    for c in unused_colors:
        content = re.sub(r'\s*<color name="' + c + r'">.*?</color>', '', content)
    
    with open(colors_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("✅ رنگ‌های بی‌استفاده از colors.xml پاک شدند.")

print("\n🎉 پاکسازی منابع به پایان رسید! (توجه: به کدهای Kotlin دست نزدیم تا خطایی رخ نده)")