#!/bin/bash

# Echo: Music Player Plus - Local Build Script
# This script helps you build APKs locally.

# Set colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting Echo Build Script...${NC}"

# Check for Java 17
if ! type -p java > /dev/null; then
    echo -e "${RED}Error: Java is not installed.${NC}"
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ]; then
    echo -e "${RED}Error: Java 17 or higher is required. Found version $JAVA_VER.${NC}"
    exit 1
fi

# Ensure gradlew is executable
chmod +x ./gradlew

# Build type selection
PS3='Select the build type: '
options=("Debug" "Release" "Stable" "Nightly" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Debug")
            BUILD_TYPE="Debug"
            break
            ;;
        "Release")
            BUILD_TYPE="Release"
            break
            ;;
        "Stable")
            BUILD_TYPE="Stable"
            break
            ;;
        "Nightly")
            BUILD_TYPE="Nightly"
            break
            ;;
        "Quit")
            exit 0
            ;;
        *) echo "Invalid option $REPLY";;
    esac
done

echo -e "${GREEN}Building $BUILD_TYPE APK...${NC}"

# Run Gradle build
./gradlew "assemble$BUILD_TYPE"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Build successful!${NC}"
    echo -e "APKs are located in: ${GREEN}app/build/outputs/apk/$(echo $BUILD_TYPE | tr '[:upper:]' '[:lower:]')/${NC}"
else
    echo -e "${RED}Build failed.${NC}"
    exit 1
fi
