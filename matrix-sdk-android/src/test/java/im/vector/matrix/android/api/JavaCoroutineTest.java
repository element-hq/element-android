package im.vector.matrix.android.api;

import im.vector.matrix.android.api.pushrules.PushrulesConditionTest;
import im.vector.matrix.android.api.session.room.Room;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

public class JavaCoroutineTest {
    @Test
    @Ignore("This is a compile-time test")
    public void testCallingSuspendFunction() {
        Room room = new PushrulesConditionTest.MockRoom("0123", 1);

        room.invite("me", new MatrixCallback<Unit>() {
            @Override
            public void onSuccess(Unit data) {
                // Continue exec
            }

            @Override
            public void onFailure(@NotNull Throwable failure) {
                // Handle error
            }
        });
    }

}
