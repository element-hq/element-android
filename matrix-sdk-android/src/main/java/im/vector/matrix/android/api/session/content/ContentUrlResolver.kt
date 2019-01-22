package im.vector.matrix.android.api.session.content

object ContentUrlResolver {

    private const val MATRIX_CONTENT_URI_SCHEME = "mxc://"
    private const val MEDIA_URL = "https://matrix.org/_matrix/media/v1/download/"

    /**
     * Get the actual URL for accessing the full-size image of a Matrix media content URI.
     *
     * @param contentUrl  the Matrix media content URI (in the form of "mxc://...").
     * @return the URL to access the described resource, or null if the url is invalid.
     */
    fun resolve(contentUrl: String?): String? {
        if (contentUrl.isValidMatrixContentUrl()) {
            return contentUrl?.replace(MATRIX_CONTENT_URI_SCHEME, MEDIA_URL)
        }
        return null
    }

    /**
     * Check whether an url is a valid matrix content url.
     *
     * @param contentUrl the content URL (in the form of "mxc://...").
     * @return true if contentUrl is valid.
     */
    private fun String?.isValidMatrixContentUrl(): Boolean {
        return !this.isNullOrEmpty() && startsWith(MATRIX_CONTENT_URI_SCHEME)
    }

}