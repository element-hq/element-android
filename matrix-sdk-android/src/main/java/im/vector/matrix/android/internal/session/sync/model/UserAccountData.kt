package im.vector.matrix.android.internal.session.sync.model

interface UserAccountData {

    companion object {
        const val TYPE_IGNORED_USER_LIST = "m.ignored_user_list"
        const val TYPE_DIRECT_MESSAGES = "m.direct"
        const val TYPE_PREVIEW_URLS = "org.matrix.preview_urls"
        const val TYPE_WIDGETS = "m.widgets"
    }
}