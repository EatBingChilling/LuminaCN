-dontwarn **
-renamesourcefileattribute null
-keep class io.netty.** { *; }
-keep class org.cloudburstmc.netty.** { *; }
-keep @io.netty.channel.ChannelHandler$Sharable class *
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class coelho.msftauth.api.** { *; }
-keep class com.cartethyia.util.** { *; }
-keep class com.cartethyia.constructors.AccountManager { *; }
-keep class com.project.lumina.relay.** { *; }
-keep class org.cloudburstmc.protocol.** { *; }
-keep class com.mycompany.application.** { *; }
-keep class com.cartethyia.CPPBridge.**  { *; }
-keep class com.cartethyia.constructors.ModuleManager.** {*;}
-keep class com.cartethyia.constructors.GameDataManager.** {*;}

