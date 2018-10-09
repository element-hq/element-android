package im.vector.matrix.android.internal.legacy.rest.callback;

import im.vector.matrix.android.internal.legacy.rest.model.HttpError;
import im.vector.matrix.android.internal.legacy.rest.model.HttpException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DefaultRetrofit2CallbackWrapper<T>
    implements Callback<T>, DefaultRetrofit2ResponseHandler.Listener<T> {

    private final ApiCallback<T> apiCallback;

    public DefaultRetrofit2CallbackWrapper(ApiCallback<T> apiCallback) {
        this.apiCallback = apiCallback;
    }

    public ApiCallback<T> getApiCallback() {
        return apiCallback;
    }

    @Override public void onResponse(Call<T> call, Response<T> response) {
        try {
            handleResponse(response);
        } catch (IOException e) {
            apiCallback.onUnexpectedError(e);
        }
    }

    private void handleResponse(Response<T> response) throws IOException {
        DefaultRetrofit2ResponseHandler.handleResponse(response, this);
    }

    @Override public void onFailure(Call<T> call, Throwable t) {
        apiCallback.onNetworkError((Exception) t);
    }

    @Override public void onSuccess(Response<T> response) {
        apiCallback.onSuccess(response.body());
    }

    @Override public void onHttpError(HttpError httpError) {
        apiCallback.onNetworkError(new HttpException(httpError));
    }
}
