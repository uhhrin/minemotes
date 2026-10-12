import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.security.MessageDigest
import java.util.Locale
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Zip

plugins {
	id("net.fabricmc.fabric-loom-remap")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.3.10"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

base {
	archivesName = providers.gradleProperty("archives_base_name")
}

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("minemotes") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

fabricApi {
	configureDataGeneration {
		client = true
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
}

tasks.processResources {
	inputs.property("version", version)
	inputs.property("minecraft_version", providers.gradleProperty("minecraft_version"))
	inputs.property("loader_version", providers.gradleProperty("loader_version"))
	inputs.property("fabric_api_version", providers.gradleProperty("fabric_api_version"))
	inputs.property("fabric_kotlin_version", providers.gradleProperty("fabric_kotlin_version"))

	filesMatching("fabric.mod.json") {
		expand(
			"version" to version,
			"minecraft_version" to providers.gradleProperty("minecraft_version").get(),
			"loader_version" to providers.gradleProperty("loader_version").get(),
			"fabric_api_version" to providers.gradleProperty("fabric_api_version").get(),
			"fabric_kotlin_version" to providers.gradleProperty("fabric_kotlin_version").get()
		)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	inputs.property("archivesName", base.archivesName)

	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
	}
}

val emoteResourcePackDir = layout.buildDirectory.dir("resourcepack/minemotes-emotes")

tasks.register<Sync>("prepareEmoteResourcePack") {
	from("src/main/resources/assets") {
		into("assets")
	}
	from("resourcepack/pack.mcmeta")
	into(emoteResourcePackDir)

	from("src/main/resources/assets/minemotes/icon.png") {
		rename { "pack.png" }
	}
}

val buildEmoteResourcePack = tasks.register<Zip>("buildEmoteResourcePack") {
	dependsOn("prepareEmoteResourcePack")
	from(emoteResourcePackDir)
	destinationDirectory.set(layout.buildDirectory.dir("resourcepack"))
	archiveFileName.set("minemotes-emotes-resource-pack.zip")
}

tasks.register("printEmoteResourcePackSha1") {
	dependsOn(buildEmoteResourcePack)
	doLast {
		val zipFile = buildEmoteResourcePack.get().archiveFile.get().asFile
		val sha1 = MessageDigest.getInstance("SHA-1")
			.digest(zipFile.readBytes())
			.joinToString(separator = "") { byte ->
				String.format(Locale.ROOT, "%02x", byte)
			}
		println("Resource pack: ${zipFile.absolutePath}")
		println("SHA1: $sha1")
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			artifactId = base.archivesName.get()
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}

