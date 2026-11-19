# Messages (پیام‌ها)

A fast, **text-only** SMS app for Android that replaces Google Messages.
Kotlin · Jetpack Compose · Room · Material 3 — UI in **فارسی / English**.

## Features
- Imports all existing SMS from the phone
- Shamsi (Jalali) and Gregorian dates, switchable in Settings
- Text messages only (no photos, video or media)
- Pin conversations, block numbers
- Delete messages or conversations with confirmation, with a **trash bin** (auto-emptied after 30 days)
- New chat with live search by contact name or phone number
- Choose the SIM per chat (in the chat screen) or globally (Settings)
- Copy a whole message or select and copy part of it
- Schedule a message for a date/time, recipient and SIM
- Smooth animations, springy micro-interactions, dark/light theme, RTL support

## Install
Download the APK from the latest [Release](../../releases), install it, grant the permissions
and make Messages your default SMS app (needed to receive messages and delete from the system store).

## Build
CI builds the release APK on every push/PR (`.github/workflows/build.yml`).
Pushing a tag like `v1.0.0` publishes the APK as a GitHub Release.

## Notes
- MMS/group messages are intentionally not supported.
- Scheduled messages use exact alarms and survive reboots.
