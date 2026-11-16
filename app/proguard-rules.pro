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
