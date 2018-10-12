package im.vector.matrix.android.internal.database.model

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import io.objectbox.relation.ToMany

@Entity
class RoomEntity {
    @Id var id: Long = 0
    @Convert(converter = MembershipConverter::class, dbType = String::class)
    var membership: Membership = Membership.NONE
    lateinit var roomId: String
    lateinit var chunks: ToMany<ChunkEntity>

    companion object;

    enum class Membership {
        JOINED,
        LEFT,
        INVITED,
        NONE
    }
}

class MembershipConverter : PropertyConverter<RoomEntity.Membership, String> {

    override fun convertToDatabaseValue(entityProperty: RoomEntity.Membership?): String? {
        return entityProperty?.name
    }

    override fun convertToEntityProperty(databaseValue: String?): RoomEntity.Membership? {
        return databaseValue?.let { RoomEntity.Membership.valueOf(databaseValue) }
    }

}