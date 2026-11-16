# Preserve useful release stack traces without exposing source filenames.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove verbose development logging from optimized release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Apache POI exposes optional desktop, compression, XPath, logging, and OSGi APIs.
# Scout Alert uses only OOXML workbook parsing on Android.
-dontwarn aQute.bnd.annotation.**
-dontwarn com.github.luben.zstd.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn java.awt.**
-dontwarn javax.xml.stream.**
-dontwarn net.sf.saxon.**
-dontwarn org.apache.batik.**
-dontwarn org.osgi.**

# Preserve Apache POI & XMLBeans package/class names for .xsb schema resolution
-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class com.microsoft.schemas.** { *; }
-keep class org.etsi.uri.** { *; }
-keep class org.w3.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class com.zaxxer.sparsebits.** { *; }
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.io.** { *; }

-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn com.microsoft.schemas.**
-dontwarn org.etsi.uri.**
-dontwarn org.w3.**
-dontwarn javax.xml.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn org.slf4j.**
