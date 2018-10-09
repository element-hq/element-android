package im.vector.matrix.android.internal.legacy.util;


import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public final class PolymorphicRequestBodyConverter<T> implements Converter<T, RequestBody> {
    public static final Factory FACTORY = new Factory() {
        @Override public Converter<?, RequestBody> requestBodyConverter(
            Type type,
            Annotation[] parameterAnnotations,
            Annotation[] methodAnnotations,
            Retrofit retrofit
        ) {
            return new PolymorphicRequestBodyConverter<>(
                this, parameterAnnotations, methodAnnotations, retrofit
            );
        }
    };

    private final Factory skipPast;
    private final Annotation[] parameterAnnotations;
    private final Annotation[] methodsAnnotations;
    private final Retrofit retrofit;
    private final Map<Class<?>, Converter<T, RequestBody>> cache = new LinkedHashMap<>();

    PolymorphicRequestBodyConverter(
        Factory skipPast,
        Annotation[] parameterAnnotations,
        Annotation[] methodsAnnotations,
        Retrofit retrofit) {
        this.skipPast = skipPast;
        this.parameterAnnotations = parameterAnnotations;
        this.methodsAnnotations = methodsAnnotations;
        this.retrofit = retrofit;
    }

    @Override public RequestBody convert(T value) throws IOException {
        Class<?> cls = value.getClass();
        Converter<T, RequestBody> requestBodyConverter;
        synchronized (cache) {
            requestBodyConverter = cache.get(cls);
        }
        if (requestBodyConverter == null) {
            requestBodyConverter = retrofit.nextRequestBodyConverter(
                skipPast, cls, parameterAnnotations, methodsAnnotations
            );
            synchronized (cache) {
                cache.put(cls, requestBodyConverter);
            }
        }
        return requestBodyConverter.convert(value);
    }
}
