plugins {
    `java-library`
    id("com.gradleup.shadow")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    implementation(project(":common-paper"))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
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
