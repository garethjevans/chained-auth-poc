plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform(libs.spring.boot))
    api(platform(libs.spring.cloud))
}