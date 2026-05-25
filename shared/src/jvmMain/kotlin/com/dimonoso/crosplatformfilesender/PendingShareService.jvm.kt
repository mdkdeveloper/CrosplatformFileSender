package com.dimonoso.crosplatformfilesender

actual fun createPendingShareService(): PendingShareService = InMemoryPendingShareService()
