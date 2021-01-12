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
             common/elfop.cpp)

#设定存放编译出来的库文件的目录
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../libs/${ANDROID_ABI})

##并且把该目录设为连接目录
#LINK_DIRECTORIES(${PROJECT_SOURCE_DIR}/../libs/${ANDROID_ABI)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libra-DANDROID_STL=c++_sharedries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library(
    # Sets the name of the library.
    iWatch

    # Sets the library as a shared library.
    SHARED

    # Provides a relative path to your source file(s).
    ${SRC_LIST}
)

include_directories(
    common/elfop.h
    common/log.h

    art/art_method_11.h
    art/JNIEnvExt.h
    art/scoped_thread_state_change.h
    art/ScopedFastNativeObjectAccess.h
    art/value_object.h

    iwatch_impl.h
    ArtRestore.h
)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(
    # Sets the name of the path variable.
    log-lib

    # Specifies the name of the NDK library that
    # you want CMake to locate.
    log
)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(
    # Specifies the target library.
    iWatch

    # Links the target library to the log library
    # included in the NDK.
    ${log-lib}
)