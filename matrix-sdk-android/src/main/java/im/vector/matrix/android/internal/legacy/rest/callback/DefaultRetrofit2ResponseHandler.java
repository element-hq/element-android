package im.vector.matrix.android.internal.legacy.rest.callback;

import im.vector.matrix.android.internal.legacy.rest.model.HttpError;

import java.io.IOException;

import retrofit2.Response;

public class DefaultRetrofit2ResponseHandler {
    public static <T> void handleResponse(Response<T> response, Listener<T> listener)
        throws IOException {
        if (response.isSuccessful()) {
            listener.onSuccess(response);
        } else {
            String errorBody = response.errorBody().string();
            listener.onHttpError(new HttpError(errorBody, response.code()));
        }
    }

    public interface Listener<T> {
        void onSuccess(Response<T> response);
        void onHttpError(HttpError httpError);
    }
}
