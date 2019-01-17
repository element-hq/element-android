package im.vector.riotredesign.core.resources

import android.content.res.Resources
import androidx.annotation.NonNull
import androidx.annotation.StringRes

class StringProvider(private val resources: Resources) {

    /**
     * Returns a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     * @return The string data associated with the resource, stripped of styled
     * text information.
     */
    @NonNull
    fun getString(@StringRes resId: Int): String {
        return resources.getString(resId)
    }

    /**
     * Returns a localized formatted string from the application's package's
     * default string table, substituting the format arguments as defined in
     * [java.util.Formatter] and [java.lang.String.format].
     *
     * @param resId Resource id for the format string
     * @param formatArgs The format arguments that will be used for
     * substitution.
     * @return The string data associated with the resource, formatted and
     * stripped of styled text information.
     */
    @NonNull
    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return resources.getString(resId, *formatArgs)
    }


}