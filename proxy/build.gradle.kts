plugins {
    `java-library`
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-velocity")
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")
    implementation(project(":common"))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runVelocity {
        velocityVersion("4.2.1-SNAPSHOT")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("com.zaxxer.hikari", "user.meistertisch.de.hordefall.libs.hikari")
        relocate("org.mariadb.jdbc", "user.meistertisch.de.hordefall.libs.mariadb")
        relocate("redis.clients", "user.meistertisch.de.hordefall.libs.redis")
        relocate("org.apache.commons.pool2", "user.meistertisch.de.hordefall.libs.pool2")

        dependencies {
            exclude(dependency("org.slf4j:slf4j-api"))
            exclude(dependency("com.google.code.gson:gson"))
            exclude(dependency("com.google.errorprone:error_prone_annotations"))
        }

        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        mergeServiceFiles()
    }
}
