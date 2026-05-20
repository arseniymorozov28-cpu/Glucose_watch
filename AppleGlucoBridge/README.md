# AppleGlucoBridge

This folder is the new Apple-focused project area.

Current stage:
- `legacy-android-copy/` keeps a separate copy of the original Android/Wear project.
- Stage 1 is implemented: the iPhone app signs in to LibreLinkUp, fetches the latest glucose value, and publishes it through a custom BLE GATT service.
- Stage 2 is intentionally left for the next step: the Galaxy Watch app connects as a BLE central, reads the payload, and renders it.

BLE protocol for the watch side:
- Local name: `GlucoBridge`
- Service UUID: `6F5A1000-BD38-4B9A-9A1F-8A9F2A120001`
- Characteristic UUID: `6F5A1001-BD38-4B9A-9A1F-8A9F2A120001`
- Characteristic properties: `read`, `notify`
- Payload encoding: UTF-8 JSON

Payload fields:
- `valueMgDl`
- `valueMmolL`
- `trendArrow`
- `trendText`
- `trendMessage`
- `timestamp`
- `isHigh`
- `isLow`
- `patientName`
- `targetLowMmolL`
- `targetHighMmolL`

Current assumption for the indie workflow:
- the iPhone app is opened when you want it to refresh LibreLinkUp and broadcast the latest glucose value
- the Galaxy Watch side will connect directly to that BLE service and subscribe for updates

How to generate the Xcode project on a Mac:

```bash
brew install xcodegen
cd AppleGlucoBridge
xcodegen generate
open AppleGlucoBridge.xcodeproj
```

Why there is no `.ipa` in this workspace:
- `.ipa` creation requires macOS, Xcode, and Apple code signing.
- This workspace is running on Windows, so I can prepare the project files and source code, but I cannot produce a signed iPhone build artifact here.

GitHub path for `.ipa`:
- use `.github/workflows/build-ios-ipa.yml`
- workflow setup notes are in `GITHUB_IPA.md`
