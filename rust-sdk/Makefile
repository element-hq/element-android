# x86_64:
# 	cargo build --release --target x86_64-linux-android
# 	mkdir -p ../matrix-sdk-android/src/main/jniLibs/x86_64/
# 	cp target/x86_64-linux-android/release/libmatrix_crypto.so ../matrix-sdk-android/src/main/jniLibs/x86_64/libuniffi_olm.so

x86:
	cargo build --release --target i686-linux-android
	mkdir -p ../matrix-sdk-android/src/main/jniLibs/x86/
	cp target/i686-linux-android/release/libmatrix_crypto.so ../matrix-sdk-android/src/main/jniLibs/x86/libuniffi_olm.so

aarch64:
	cargo build --release --target aarch64-linux-android
	mkdir -p ../matrix-sdk-android/src/main/jniLibs/arm64-v8a/
	cp target/aarch64-linux-android/release/libmatrix_crypto.so ../matrix-sdk-android/src/main/jniLibs/arm64-v8a/libuniffi_olm.so

armv7-linux-androideabi:
	cargo build --release --target armv7-linux-androideabi
	mkdir -p ../matrix-sdk-android/src/main/jniLibs/armeabi-v7a/
	cp target/armv7-linux-androideabi/release/libmatrix_crypto.so ../matrix-sdk-android/src/main/jniLibs/armeabi-v7a/libuniffi_olm.so