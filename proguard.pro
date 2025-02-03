-ignorewarnings

# No ofuscar las clases del paquete com.zaxxer.hikari
-keep class com.zaxxer.hikari.** { *; }
-keep class org.slf4j.** { *; }
-dontwarn com.zaxxer.hikari.**

# Conservar atributos importantes (anotaciones, nombres de archivo fuente, números de línea)
-keepattributes *Annotation*,Signature,EnclosingMethod,SourceFile,LineNumberTable

# Conservar la clase principal (ajusta el nombre completo de tu clase principal)
-keep public class com.erosmari.lumen.Lumen {
    public *;
}

# Evitar optimizaciones agresivas (para que no se inlinen literales y se mantengan tus placeholders)
-dontoptimize

# Evitar advertencias de dependencias externas (ajusta según las librerías usadas)
-dontwarn org.bukkit.**
-dontwarn net.md_5.bungee.**
-dontwarn org.slf4j.**
-dontwarn net.kyori.adventure.**
-dontwarn net.coreprotect.**
-dontwarn com.fastasyncworldedit.**
-dontwarn com.sk89q.worldedit.**