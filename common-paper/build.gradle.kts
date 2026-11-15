plugins {
    `java-library`
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    api(project(":common"))

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
