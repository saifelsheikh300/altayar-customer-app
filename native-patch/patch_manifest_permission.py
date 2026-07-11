#!/usr/bin/env python3
"""
كباسيتور مبيضيفش أي أذونات مايك في AndroidManifest تلقائي، والموقع محتاج إذن
RECORD_AUDIO عشان يقدر يسجل صوت (navigator.mediaDevices.getUserMedia). السكريبت
ده بيضيف سطر الإذن بس في المانيفست، من غير ما يلمس أي حاجة تانية فيه.
"""
import sys

path = "android/app/src/main/AndroidManifest.xml"

with open(path, "r", encoding="utf-8") as f:
    content = f.read()

permission_line = '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n'

if "android.permission.RECORD_AUDIO" in content:
    print("إذن المايك موجود بالفعل، لم يتم تعديل أي شيء.")
    sys.exit(0)

marker = "<application"
idx = content.find(marker)
if idx == -1:
    print("لم يتم إيجاد وسم <application>، لم يتم تعديل أي شيء.")
    sys.exit(1)

content = content[:idx] + permission_line + content[idx:]

with open(path, "w", encoding="utf-8") as f:
    f.write(content)

print("تم إضافة إذن المايك بنجاح.")
