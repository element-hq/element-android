package im.vector.matrix.android.internal.legacy.rest.model;

public final class HttpError {
    private final String errorBody;
    private final int httpCode;

    public HttpError(String errorBody, int httpCode) {
        this.errorBody = errorBody;
        this.httpCode = httpCode;
    }

    public String getErrorBody() {
        return errorBody;
    }

    public int getHttpCode() {
        return httpCode;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpError httpError = (HttpError) o;

        if (httpCode != httpError.httpCode) return false;
        return errorBody != null ?
            errorBody.equals(httpError.errorBody) :
            httpError.errorBody == null;
    }

    @Override public int hashCode() {
        int result = errorBody != null ? errorBody.hashCode() : 0;
        result = 31 * result + httpCode;
        return result;
    }

    @Override public String toString() {
        return "HttpError{" +
            "errorBody='" + errorBody + '\'' +
            ", httpCode=" + httpCode +
            '}';
    }
}
