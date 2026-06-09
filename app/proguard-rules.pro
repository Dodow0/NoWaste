# Keep Room generated code and Compose defaults handled by the Android Gradle plugin.

# ML Kit text recognition registers internal components through generated/runtime metadata.
# Keep these classes in minified release builds to avoid recognizer initialization crashes.
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_bundled_common.** { *; }
