-dontwarn **
-renamesourcefileattribute null
-keep class io.netty.** { *; }
-keep class org.cloudburstmc.netty.** { *; }
-keep @io.netty.channel.ChannelHandler$Sharable class *
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class coelho.msftauth.api.** { *; }
-keep class com.phoenix.luminacn.util.** { *; }
-keep class com.phoenix.luminacn.constructors.AccountManager { *; }
-keep class com.project.lumina.relay.** { *; }
-keep class org.cloudburstmc.protocol.** { *; }
-keep class com.mycompany.application.** { *; }
-keep class com.phoenix.luminacn.CPPBridge.**  { *; }
-keep class com.phoenix.luminacn.constructors.ModuleManager.** {*;}
-keep class com.phoenix.luminacn.constructors.GameDataManager.** {*;}

