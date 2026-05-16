package com.dimonoso.crosplatformfilesender

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform