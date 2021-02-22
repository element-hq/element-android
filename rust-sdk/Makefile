x86_64:
	cargo build --target x86_64-linux-android
	install -D target/x86_64-linux-android/debug/libmatrix_crypto.so ../matrix-sdk-android/src/main/jniLibs/x86_64/libuniffi_olm.so

x86:
	cargo build --target i686-linux-android
	install -D target/i686-linux-android/debug/libmatrix_crypto.so ../matrix-sdk-android/src/main/jniLibs/x86/libuniffi_olm.so

aarch64:
	cargo build --target aarch64-linux-android
	install -D target/aarch64-linux-android/debug/libmatrix_crypto.so ../matrix-sdk-android/src/main/jniLibs/arm64-v8a/libuniffi_olm.so
