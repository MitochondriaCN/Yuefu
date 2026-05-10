pluginManagement {
    repositories {

        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 阿里云 Google 镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云公共镜像 (代替 mavenCentral)
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Yuefu"
include(":app")
