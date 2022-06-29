# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_CXX_STANDARD 14)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

set(distribution_OPUS_DIR ${CMAKE_SOURCE_DIR}/opus)

add_library(lib_opus SHARED IMPORTED)
set_target_properties(lib_opus PROPERTIES IMPORTED_LOCATION
        ${distribution_OPUS_DIR}/libs/${ANDROID_ABI}/libopus.so)

add_library(lib_opusenc SHARED IMPORTED)
set_target_properties(lib_opusenc PROPERTIES IMPORTED_LOCATION
        ${distribution_OPUS_DIR}/libs/${ANDROID_ABI}/libopusenc.so)

add_library( # Sets the name of the library.
             opuscodec

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             codec/CodecOggOpus.cpp
             opuscodec.cpp)

target_include_directories(opuscodec PRIVATE
                           ${distribution_OPUS_DIR}/include)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       opuscodec
                       android
                       lib_opusenc
                       lib_opus
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
