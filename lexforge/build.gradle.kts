import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`

    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)

    alias(catalog.plugins.forge.gradle)
    alias(catalog.plugins.mixin)
}

val mod_id: String by rootProject

minecraft {
    mappings("official", catalog.versions.minecraft.asProvider().get())

    runs.all {
        mods {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("forge.enabledGameTestNamespaces", mod_id)
            property("terminal.jline", "true")
            mods { create(mod_id) { source(sourceSets.main.get()) } }
        }
    }

    runs {
        create("client") {
            property("log4j.configurationFile", "log4j2.xml")
            jvmArg("-XX:+AllowEnhancedClassRedefinition")
            args("--username", "Player")
        }
    }
}

mixin {
    config("$mod_id.mixins.json")
    add("main", "$mod_id.refmap.json")
}

jarJar.enable()

dependencies {
    minecraft(catalog.minecraft.forge)
    implementation(project(":common"))

    jarJar(project(":versions:1.20:lexforge-1.20")) {
        jarJar.ranged(this, "[$version,)")
        isTransitive = false
    }
}

tasks {
    withType<ProcessResources> { from(project(":common").sourceSets.main.get().resources) }
    withType<KotlinCompile> { source(project(":common").sourceSets.main.get().allSource) }
    withType<JavaCompile> { source(project(":common").sourceSets.main.get().allSource) }

    sourcesJar { from(project(":common").sourceSets.main.get().allSource) }

    jar {
        manifest {
            attributes("FMLModType" to "GAMELIBRARY", "MixinConfigs" to "$mod_id.mixins.json")
        }
    }

    this.jarJar {
        dependsOn(":versions:1.20:lexforge-1.20:reobfJar")
    }
}

rootProject.publishing {
    publications {
        named<MavenPublication>("maven") { artifact(tasks.jarJar) { classifier = "lexforge" } }
    }
}
