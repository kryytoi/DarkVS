# =====================================================================
#  proguard-rules.pro — darkvisuals (Fabric mod)
#  Цель: обфусцировать свою бизнес-логику, но НЕ сломать Mixin и
#  не зацепить ссылки на ванильные классы Minecraft/Fabric API.
# =====================================================================

# --- 1. НИКОГДА не трогаем сам Minecraft / Fabric / Yarn / Mojang-маппинги ---
# Это внешние классы, доступные только в рантайме игрока. ProGuard видит их
# как "библиотечные" через -libraryjars, но на всякий случай явно защищаем
# любые ссылки на них от переименования/удаления как "unused".
-keep class net.minecraft.** { *; }
-keep class net.fabricmc.** { *; }
-keep class com.mojang.** { *; }
-dontwarn net.minecraft.**
-dontwarn net.fabricmc.**
-dontwarn com.mojang.**

# --- 2. Mixin: сами классы, их структура и точки внедрения ---
# Mixin читает байткод классов (дескрипторы методов/полей) во время
# трансформации. Переименование/удаление здесь = "target not found".
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }
-keepclassmembers @org.spongepowered.asm.mixin.Mixin class * {
    *;
}
-keep class org.spongepowered.asm.** { *; }
-dontwarn org.spongepowered.asm.**

# Аннотации Mixin должны остаться читаемыми в рантайме (RUNTIME retention)
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# --- 3. Точки входа мода (entrypoints из fabric.mod.json) ---
# dev.darkvisuals.emotes.network.EmotesNetworkInit
# dev.darkvisuals.darkvisuals
-keep class dev.darkvisuals.emotes.network.EmotesNetworkInit { *; }
-keep class dev.darkvisuals.darkvisuals { *; }

# --- 4. Kotlin-рантайм (мод грузит fabric-language-kotlin) ---
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# --- 5. Ресурсы, которые НЕЛЬЗЯ трогать/удалять как "мусор" ---
# ProGuard по умолчанию не лезет в resources, но если используете шейдинг/
# минификацию ресурсов — явно исключите:
#   fabric.mod.json
#   darkvisuals.mixins.json
#   simplevisuals-re*.json (refmap)
#   assets/darkvisuals/**
# (см. настройку keepresources/mergeservicefiles в вашем shadowJar/loom конфиге,
#  ProGuard эти правила напрямую не описывает — это на уровне Gradle-задачи)

# --- 6. Что МОЖНО и НУЖНО прятать: ваша внутренняя логика ---
# Всё, что НЕ Mixin-класс, НЕ entrypoint и НЕ обращается напрямую к net.minecraft.*
# по имени в конфиге — можно смело обфусцировать. Примеры пакетов, где обычно
# живёт "чувствительная" логика (замените на реальные пакеты вашего мода):
#   dev.darkvisuals.internal.**
#   dev.darkvisuals.render.impl.**
#   dev.darkvisuals.combat.**
# Для них НЕ добавляем -keep — ProGuard обфусцирует имена классов/методов/полей
# по умолчанию, если не встречается в правилах выше.

# --- 7. Общие безопасные настройки ---
# ВАЖНО: -dontoptimize принципиален. Оптимизатор ProGuard иногда неверно
# пересчитывает StackMapTable для современного байткода (лямбды, Java 21),
# из-за чего JVM падает с "VerifyError: Expecting a stackmap frame at branch
# target N" при загрузке класса. Отключаем именно ОПТИМИЗАЦИЮ потока байткода —
# переименование классов/методов/полей (то, ради чего всё это затевалось)
# при этом продолжает работать как обычно.
-dontoptimize
-dontshrink
-verbose

# --- Enum-безопасность: JDK ищет values()/valueOf() РЕФЛЕКСИВНО по имени
# (Class.getEnumConstants() -> getMethod("values")). Если их переименовать,
# EnumMap/EnumSet и любой Class.getEnumConstants() ловит NPE/exception.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Если после -dontoptimize всё ещё будут VerifyError в других местах —
# можно вместо полного отключения точечно выключить проблемные оптимизации:
# -optimizations !code/simplification/*,!code/merging,!code/allocation/variable

# Discord RPC (dev.firstdark) — встраивается в jar, трогать нельзя.
-keep class dev.firstdark.rpc.** { *; }
-dontwarn dev.firstdark.rpc.**

# Не переименовывать сами файлы .class нестандартно (иначе Mixin refmap,
# если он у вас используется, перестанет их находить)
-keepnames class * extends org.spongepowered.asm.mixin.transformer.MixinConfig

# --- 8. Не валить сборку из-за warning'ов по чужим библиотекам ---
# ProGuard по умолчанию считает сборку проваленной, если есть unresolved
# references в сторонних зависимостях (MixinExtras, voicechat-api, YACL и т.д.),
# которые не попали в -libraryjars полностью. Это не критично для нас — просто
# не даём сборке падать из-за них.
-dontwarn **
-ignorewarnings

# GSON-модели: НЕ переименовывать имена полей.
# Иначе atlas/metrics/glyphs -> null и меню висит на "загрузка шрифтов…".
-keepclassmembers class dev.darkvisuals.client.render.msdf.FontData {
    <fields>;
}
-keepclassmembers class dev.darkvisuals.client.render.msdf.FontData$* {
    <fields>;
}

# Конфиги — тоже читаются GSON по именам полей, иначе настройки молча ломаются:
-keepclassmembers class dev.darkvisuals.client.managers.ConfigManager$* {
    <fields>;
}
# --- DarkVisuals Bridge/UserTab/Cosmetics ---
# Эти классы работают через Fabric callbacks, JDK HttpClient/WebSocket и внешний API.
# В dev-запуске IntelliJ они не обфусцируются, а в .jar проходят через ProGuard,
# поэтому оставляем имена и публичные методы стабильными.
-keep class dev.darkvisuals.client.managers.UserTabManager { *; }
-keep class dev.darkvisuals.client.managers.CosmeticsSyncManager { *; }
-keep class dev.darkvisuals.client.managers.CosmeticsSyncManager$* { *; }
-keep class dev.darkvisuals.modules.impl.utility.UserTab { *; }
-keep class dev.darkvisuals.modules.impl.render.Cosmetics { *; }
-keep class dev.darkvisuals.emotes.EmoteManager { *; }
-keep class dev.darkvisuals.emotes.EmoteRegistry { *; }
-keep class dev.darkvisuals.emotes.network.** { *; }
-keepclassmembers class * implements java.net.http.WebSocket$Listener { *; }
