plugins {
    id("module")
}

dependencies {
    compileOnly(projects.detektApi)
    testImplementation(projects.detektTest)
}
