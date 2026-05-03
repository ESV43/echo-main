# iOS Porting Setup Guide

Follow these steps to enable iOS support using the files I've prepared in this folder.

## Step 1: Update the Shared Module
1. Open `common/build.gradle.kts` in your main repository.
2. Replace the contents with what I've provided in `apple/common_build.gradle.kts.updated`.
3. This tells Gradle to compile your shared logic for iPhones and Macs.

## Step 2: Enable Remote Builds (GitHub Actions)
1. In your main repository, create a directory: `.github/workflows/`.
2. Move `apple/build-ios.yml` (for testing) and `apple/release-ios.yml` (for production) to `.github/workflows/`.
3. Push to GitHub.
4. Go to the "Actions" tab on GitHub.
   - Use **Build iOS IPA** for quick checks.
   - Use **iOS Release Production** to create a numbered build. It will ask you for a version name (e.g., `1.0.1`) and automatically handle the build number.

## Step 3: Local Xcode Setup (If you have a Mac)
1. Copy the `apple/iosApp` folder to your root directory.
2. Open the Xcode project.
3. Link the `common` framework (Gradle will generate this in `common/build/bin/iosArm64/releaseFramework/common.framework`).

## Important Notes on Signing
Generating a real `.ipa` that can be installed on a physical iPhone requires an **Apple Developer Account** ($99/year). 
- If you have one, you'll need to add your `P12` certificate and `MobileProvision` profile to GitHub Secrets.
- If you don't have one, the workflow I provided builds an **unsigned app bundle**, which is perfect for verifying that the code compiles and works, but cannot be installed on a non-jailbroken phone without being signed first.
