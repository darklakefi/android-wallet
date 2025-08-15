package fi.darklake.wallet

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class DarklakeApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(false)
            .allowHardware(false) // This can help with some asset loading issues
            .build()
    }
}