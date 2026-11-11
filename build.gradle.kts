plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

// Die Minecraft-Versionen stehen in gradle.properties (net.labymod.minecraft-versions).
val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "de.angelbot"
version = providers.environmentVariable("VERSION").getOrElse("1.0.0")

labyMod {
    defaultPackageName = "de.angelbot" // Hauptpackage, wird von allen Modulen genutzt

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    // Auf true setzen, um sich im Entwicklungs-Client mit dem eigenen Minecraft-Konto anzumelden
                    // devLogin = true
                }
            }
        }
    }

    addonInfo {
        namespace = "angelbot"
        displayName = "Angelbot"
        author = "luciferx372x"
        description = "Fishes for you: detects the bite, reels in the rod and casts it again."
        // The addon accesses Minecraft directly (rod, right click), so no "*" but a version range.
        minecraftVersion = "26.1<26.3"
        version = rootProject.version.toString()
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version

    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
