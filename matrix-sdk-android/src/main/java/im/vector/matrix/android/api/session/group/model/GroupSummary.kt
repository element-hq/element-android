package im.vector.matrix.android.api.session.group.model

data class GroupSummary(
        val groupId: String,
        val displayName: String = "",
        val shortDescription: String = "",
        val avatarUrl: String = ""
)