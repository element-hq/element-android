package im.vector.matrix.android.session.room.timeline

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.internal.database.helper.add
import im.vector.matrix.android.internal.database.helper.addAll
import im.vector.matrix.android.internal.database.helper.isUnlinked
import im.vector.matrix.android.internal.database.helper.lastStateIndex
import im.vector.matrix.android.internal.database.helper.merge
import im.vector.matrix.android.internal.database.model.ChunkEntity
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.createObject
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import kotlin.random.Random


internal class ChunkEntityTest : InstrumentedTest {

    private lateinit var monarchy: Monarchy

    @Before
    fun setup() {
        Realm.init(context())
        val testConfig = RealmConfiguration.Builder().inMemory().name("test-realm").build()
        monarchy = Monarchy.Builder().setRealmConfiguration(testConfig).build()
    }


    @Test
    fun add_shouldAdd_whenNotAlreadyIncluded() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvent = createFakeEvent(false)
            chunk.add(fakeEvent, PaginationDirection.FORWARDS)
            chunk.events.size shouldEqual 1
        }
    }

    @Test
    fun add_shouldNotAdd_whenAlreadyIncluded() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvent = createFakeEvent(false)
            chunk.add(fakeEvent, PaginationDirection.FORWARDS)
            chunk.add(fakeEvent, PaginationDirection.FORWARDS)
            chunk.events.size shouldEqual 1
        }
    }

    @Test
    fun add_shouldStateIndexIncremented_whenStateEventIsAddedForward() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvent = createFakeEvent(true)
            chunk.add(fakeEvent, PaginationDirection.FORWARDS)
            chunk.lastStateIndex(PaginationDirection.FORWARDS) shouldEqual 1
        }
    }

    @Test
    fun add_shouldStateIndexNotIncremented_whenNoStateEventIsAdded() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvent = createFakeEvent(false)
            chunk.add(fakeEvent, PaginationDirection.FORWARDS)
            chunk.lastStateIndex(PaginationDirection.FORWARDS) shouldEqual 0
        }
    }

    @Test
    fun addAll_shouldStateIndexIncremented_whenStateEventsAreAddedForward() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvents = createFakeListOfEvents(30)
            val numberOfStateEvents = fakeEvents.filter { it.isStateEvent() }.size
            chunk.addAll(fakeEvents, PaginationDirection.FORWARDS)
            chunk.lastStateIndex(PaginationDirection.FORWARDS) shouldEqual numberOfStateEvents
        }
    }

    @Test
    fun addAll_shouldStateIndexDecremented_whenStateEventsAreAddedBackward() {
        monarchy.runTransactionSync { realm ->
            val chunk: ChunkEntity = realm.createObject()
            val fakeEvents = createFakeListOfEvents(30)
            val numberOfStateEvents = fakeEvents.filter { it.isStateEvent() }.size
            val lastIsState = fakeEvents.last().isStateEvent()
            val expectedStateIndex = if (lastIsState) -numberOfStateEvents + 1 else -numberOfStateEvents
            chunk.addAll(fakeEvents, PaginationDirection.BACKWARDS)
            chunk.lastStateIndex(PaginationDirection.BACKWARDS) shouldEqual expectedStateIndex
        }
    }

    @Test
    fun merge_shouldAddEvents_whenMergingBackward() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            chunk1.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk2.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS)
            chunk1.merge(chunk2, PaginationDirection.BACKWARDS)
            chunk1.events.size shouldEqual 60
        }
    }

    @Test
    fun merge_shouldEventsBeLinked_whenMergingLinkedWithUnlinked() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            chunk1.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = true)
            chunk2.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = false)
            chunk1.merge(chunk2, PaginationDirection.BACKWARDS)
            chunk1.isUnlinked().shouldBeFalse()
        }
    }

    @Test
    fun merge_shouldEventsBeUnlinked_whenMergingUnlinkedWithUnlinked() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            chunk1.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = true)
            chunk2.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = true)
            chunk1.merge(chunk2, PaginationDirection.BACKWARDS)
            chunk1.isUnlinked().shouldBeTrue()
        }
    }

    @Test
    fun merge_shouldPrevTokenMerged_whenMergingForwards() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            val prevToken = "prev_token"
            chunk1.prevToken = prevToken
            chunk1.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = true)
            chunk2.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = true)
            chunk1.merge(chunk2, PaginationDirection.FORWARDS)
            chunk1.prevToken shouldEqual prevToken
        }
    }

    @Test
    fun merge_shouldNextTokenMerged_whenMergingBackwards() {
        monarchy.runTransactionSync { realm ->
            val chunk1: ChunkEntity = realm.createObject()
            val chunk2: ChunkEntity = realm.createObject()
            val nextToken = "next_token"
            chunk1.nextToken = nextToken
            chunk1.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = true)
            chunk2.addAll(createFakeListOfEvents(30), PaginationDirection.BACKWARDS, isUnlinked = true)
            chunk1.merge(chunk2, PaginationDirection.BACKWARDS)
            chunk1.nextToken shouldEqual nextToken
        }
    }


    private fun createFakeListOfEvents(size: Int = 10): List<Event> {
        return (0 until size).map { createFakeEvent(Random.nextBoolean()) }
    }

    private fun createFakeEvent(asStateEvent: Boolean = false): Event {
        val eventId = Random.nextLong(System.currentTimeMillis()).toString()
        val type = if (asStateEvent) EventType.STATE_ROOM_NAME else EventType.MESSAGE
        return Event(type, eventId)
    }

}