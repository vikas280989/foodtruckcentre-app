package au.com.foodtruckcentre.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class FtcApplication : Application(), ImageLoaderFactory {
    /** Image loading with memory + disk caching for website images. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(true)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(40L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .build()
}
