# GitHub IPA Build

The repository now includes a GitHub Actions workflow:
- `.github/workflows/build-ios-ipa.yml`

It generates the Xcode project with `xcodegen`, archives the iPhone app, exports an `.ipa`, and uploads it as a workflow artifact.

Required GitHub secrets:
- `IOS_BUILD_CERTIFICATE_BASE64`
- `IOS_P12_PASSWORD`
- `IOS_KEYCHAIN_PASSWORD`
- `IOS_PROVISION_PROFILE_BASE64`
- `IOS_DEVELOPMENT_TEAM`

Recommended setup:
1. Export your iPhone signing certificate as `.p12`.
2. Base64-encode the `.p12` and save it to `IOS_BUILD_CERTIFICATE_BASE64`.
3. Base64-encode your `.mobileprovision` file and save it to `IOS_PROVISION_PROFILE_BASE64`.
4. Set your Apple Team ID in `IOS_DEVELOPMENT_TEAM`.
5. Run the `Build iPhone IPA` workflow from GitHub Actions.

Notes:
- the workflow currently exports an `ad-hoc` build
- if you want `development` or `app-store`, change `AppleGlucoBridge/exportOptions.plist`
- the workflow uploads the generated `.ipa` as a GitHub Actions artifact
