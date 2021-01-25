# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.
project("iWatch")

set(CMAKE_CXX_FLAGS "-std=c++11 -DANDROID_STL=c++_shared")

set(SRC_LIST iwatch.cpp
             iwatch_impl.cpp
             ArtRestore.cpp
             field_hook.cpp
             common/elfop.cpp
             common/elf_op.cpp)

# 指定so生成到libs目录
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../libs/${ANDROID_ABI})

##并且把该目录设为连接目录
#LINK_DIRECTORIES(${PROJECT_SOURCE_DIR}/../libs/${ANDROID_ABI)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libra-DANDROID_STL=c++_sharedries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
# Specify your native library name, type and the cpp source files.
add_library(
    # Sets the name of the library.
    iWatch

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
    ${SRC_LIST}
)


## ${PHPCPP_ARCH} 这个也可以
#if ("${ANDROID_ABI}" STREQUAL "x86_64")
#    include_directories(${ANDROID_SYSROOT}/usr/include/x86_64-linux-android
#            /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/sysroot/usr/include)
#elseif ("${ANDROID_ABI}" STREQUAL "x86")
#    include_directories(${ANDROID_SYSROOT}/usr/include/i686-linux-android
#            /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/sysroot/usr/include)
#elseif ("${ANDROID_ABI}" STREQUAL "arm64-v8a")
#    include_directories(${ANDROID_SYSROOT}/usr/include/aarch64-linux-android
#            /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/sysroot/usr/include)
#elseif ("${ANDROID_ABI}" STREQUAL "armeabi-v7a")
#    include_directories(${ANDROID_SYSROOT}/usr/include/arm-linux-androideabi,
#            /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/sysroot/usr/include)
#endif()

# Set the header files directory.
include_directories(
    common/elfop.h
    common/elf_op.h
    common/log.h
    common/constants.h

    art/art_method_11.h
    art/JNIEnvExt.h
    art/scoped_thread_state_change.h
    art/ScopedFastNativeObjectAccess.h
    art/value_object.h

    iwatch_impl.h
    ArtRestore.h
)

#if ("${ANDROID_ABI}" STREQUAL "x86_64")
#    find_library(/Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/platforms/android-29/arch-x86_64/usr/lib64/liblog.so
#                 log-lib
#                 log)
#elseif ("${ANDROID_ABI}" STREQUAL "x86")
#    find_library(/Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/platforms/android-29/arch-x86/usr/lib/liblog.so
#                 log-lib
#                 log)
#elseif ("${ANDROID_ABI}" STREQUAL "arm64-v8a")
#    find_library(/Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/platforms/android-29/arch-arm64/usr/lib64/liblog.so
#                 log-lib
#                 log)
#elseif ("${ANDROID_ABI}" STREQUAL "armeabi-v7a")
#    find_library(/Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/platforms/android-29/arch-arm/usr/lib/liblog.so
#                 log-lib
#                 log)
#endif()

## Searches for a specified prebuilt library and stores the path as a
## variable. Because CMake includes system libraries in the search path by
## default, you only need to specify the name of the public NDK library
## you want to add. CMake verifies that the library exists before
## completing its build.
## Mention the Android NDK library name to locate.
find_library( # find_library 这个函数的意思是给系统的log库起个别名，命名为log-lib。
    # 解决 cmake 找到 log-lib
#    PATHS /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/sysroot/usr/lib/
#    PATHS /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/toolchains/
#    PATHS /Users/habbyge/Library/Android/sdk/ndk-bundle/platforms/android-29/arch-arm/usr/lib/liblog.so
#        /Users/habbyge/Library/Android/sdk/ndk-bundle/platforms/android-29/arch-arm/usr/lib/
#    PATHS /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/android/
    # 解决 cmake 找到 jni.h
#    PATHS /Users/habbyge/Library/Android/sdk/ndk/21.2.6472646/sysroot/usr/include/
    # Sets the name of the path variable.
    log-lib

    # Specifies the name of the NDK library that
    # you want CMake to locate.
    log
)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
# Link the Android NDK library with your native library.
target_link_libraries( // # target_link_libraries 这个函数的意思是刚刚之前的命名的库一起link进去
    # Specifies the target library.
    iWatch

    # Links the target library to the log library
    # included in the NDK.
    ${log-lib}
)
