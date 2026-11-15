plugins {
    `java-library`
}

dependencies {
    api("com.zaxxer:HikariCP:7.1.0")
    api("org.mariadb.jdbc:mariadb-java-client:3.5.10")
    api("redis.clients:jedis:8.1.0-beta1")
    compileOnly("com.google.code.gson:gson:2.14.0")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
