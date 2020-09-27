/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.core.graphics.drawable.toBitmap
import com.amulyakhare.textdrawable.TextDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.target.Target
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.glide.GlideRequest
import im.vector.app.core.glide.GlideRequests
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

/**
 * This helper centralise ways to retrieve avatar into ImageView or even generic Target<Drawable>
 */

class AvatarRenderer @Inject constructor(private val activeSessionHolder: ActiveSessionHolder,
                                         private val matrixItemColorProvider: MatrixItemColorProvider) {

    companion object {
        private const val THUMBNAIL_SIZE = 250
    }

    @UiThread
    fun render(matrixItem: MatrixItem, imageView: ImageView) {
        render(GlideApp.with(imageView),
                matrixItem,
                DrawableImageViewTarget(imageView))
    }

    fun clear(imageView: ImageView) {
        // It can be called after recycler view is destroyed, just silently catch
        tryOrNull { GlideApp.with(imageView).clear(imageView) }
    }

    @UiThread
    fun render(matrixItem: MatrixItem, imageView: ImageView, glideRequests: GlideRequests) {
        render(glideRequests,
                matrixItem,
                DrawableImageViewTarget(imageView))
    }

    @UiThread
    fun render(mappedContact: MappedContact, imageView: ImageView) {
        // Create a Fake MatrixItem, for the placeholder
        val matrixItem = MatrixItem.UserItem(
                // Need an id starting with @
                id = "@${mappedContact.displayName}",
                displayName = mappedContact.displayName
        )

        val placeholder = getPlaceholderDrawable(matrixItem)
        GlideApp.with(imageView)
                .load(mappedContact.photoURI)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(placeholder)
                .into(imageView)
    }

    @UiThread
    fun render(glideRequests: GlideRequests,
               matrixItem: MatrixItem,
               target: Target<Drawable>) {
        val placeholder = getPlaceholderDrawable(matrixItem)
        buildGlideRequest(glideRequests, matrixItem.avatarUrl)
                .placeholder(placeholder)
                .into(target)
    }

    @AnyThread
    @Throws
    fun shortcutDrawable(glideRequests: GlideRequests, matrixItem: MatrixItem, iconSize: Int): Bitmap {
        return glideRequests
                .asBitmap()
                .apply {
                    val resolvedUrl = resolvedUrl(matrixItem.avatarUrl)
                    if (resolvedUrl != null) {
                        load(resolvedUrl)
                    } else {
                        val avatarColor = matrixItemColorProvider.getColor(matrixItem)
                        load(TextDrawable.builder()
                                .beginConfig()
                                .bold()
                                .endConfig()
                                .buildRect(matrixItem.firstLetterOfDisplayName(), avatarColor)
                                .toBitmap(width = iconSize, height = iconSize))
                    }
                }
                .submit(iconSize, iconSize)
                .get()
    }

    @AnyThread
    fun getCachedDrawable(glideRequests: GlideRequests, matrixItem: MatrixItem): Drawable {
        return buildGlideRequest(glideRequests, matrixItem.avatarUrl)
                .onlyRetrieveFromCache(true)
                .submit()
                .get()
    }

    @AnyThread
    fun getPlaceholderDrawable(matrixItem: MatrixItem): Drawable {
        val avatarColor = matrixItemColorProvider.getColor(matrixItem)
        return TextDrawable.builder()
                .beginConfig()
                .bold()
                .endConfig()
                .buildRound(matrixItem.firstLetterOfDisplayName(), avatarColor)
    }

    // PRIVATE API *********************************************************************************

    private fun buildGlideRequest(glideRequests: GlideRequests, avatarUrl: String?): GlideRequest<Drawable> {
        val resolvedUrl = resolvedUrl(avatarUrl)
        return glideRequests
                .load(resolvedUrl)
                .apply(RequestOptions.circleCropTransform())
    }

    private fun resolvedUrl(avatarUrl: String?): String? {
        return activeSessionHolder.getSafeActiveSession()?.contentUrlResolver()
                ?.resolveThumbnail(avatarUrl, THUMBNAIL_SIZE, THUMBNAIL_SIZE, ContentUrlResolver.ThumbnailMethod.SCALE)
    }
}
