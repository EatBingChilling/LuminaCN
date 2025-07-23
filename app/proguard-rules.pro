-dontwarn **
-renamesourcefileattribute null
-keep class io.netty.** { *; }
-keep class org.cloudburstmc.netty.** { *; }
-keep @io.netty.channel.ChannelHandler$Sharable class *
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class coelho.msftauth.api.** { *; }
-keep class com.project.luminacn.util.** { *; }
-keep class com.project.luminacn.constructors.AccountManager { *; }
-keep class com.project.lumina.relay.** { *; }
-keep class org.cloudburstmc.protocol.** { *; }
-keep class com.mycompany.application.** { *; }
-keep class com.project.luminacn.CPPBridge.**  { *; }
-keep class com.project.luminacn.constructors.ModuleManager.** {*;}
-keep class com.project.luminacn.constructors.GameDataManager.** {*;}

