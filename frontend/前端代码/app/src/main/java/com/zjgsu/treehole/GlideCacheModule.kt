package com.zjgsu.treehole

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions

@GlideModule
class GlideCacheModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(
            RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565)
                // Disk cache all images but memory cache is selective
                .diskCacheStrategy(DiskCacheStrategy.ALL)
        )
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, 250 * 1024 * 1024))
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
