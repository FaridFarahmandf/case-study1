/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit2.adapter.rxjava2;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.CallAdapter;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * A {@linkplain CallAdapter.Factory call adapter} which uses RxJava 2 for creating observables.
 *
 * <p>Adding this class to {@link Retrofit} allows you to return an {@link Observable}, {@link
 * Flowable}, {@link Single}, {@link Completable} or {@link Maybe} from service methods.
 *
 * <pre><code>
 * interface MyService {
 *   &#64;GET("user/me")
 *   Observable&lt;User&gt; getUser()
 * }
 * </code></pre>
 *
 * There are three configurations supported for the {@code Observable}, {@code Flowable}, {@code
 * Single}, {@link Completable} and {@code Maybe} type parameter:
 *
 * <ul>
 *   <li>Direct body (e.g., {@code Observable<User>}) calls {@code onNext} with the deserialized
 *       body for 2XX responses and calls {@code onError} with {@link HttpException} for non-2XX
 *       responses and {@link IOException} for network errors.
 *   <li>Response wrapped body (e.g., {@code Observable<Response<User>>}) calls {@code onNext} with
 *       a {@link Response} object for all HTTP responses and calls {@code onError} with {@link
 *       IOException} for network errors
 *   <li>Result wrapped body (e.g., {@code Observable<Result<User>>}) calls {@code onNext} with a
 *       {@link Result} object for all HTTP responses and errors.
 * </ul>
 */
public final class RxJava2CallAdapterFactory extends CallAdapter.Factory {
  /**
   * Returns an instance which creates synchronous observables that do not operate on any scheduler
   * by default.
   */
  public static RxJava2CallAdapterFactory create() {
    return new RxJava2CallAdapterFactory(null, false);
  }

  /** Returns an instance which creates asynchronous observables. */
  public static RxJava2CallAdapterFactory createAsync() {
    return new RxJava2CallAdapterFactory(null, true);
  }

  /**
   * Returns an instance which creates synchronous observables that {@linkplain
   * Observable#subscribeOn(Scheduler) subscribe on} {@code scheduler} by default.
   */
  @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
  public static RxJava2CallAdapterFactory createWithScheduler(Scheduler scheduler) {
    if (scheduler == null) throw new NullPointerException("scheduler == null");
    return new RxJava2CallAdapterFactory(scheduler, false);
  }

  private final @Nullable Scheduler scheduler;
  private final boolean isAsync;

  private RxJava2CallAdapterFactory(@Nullable Scheduler scheduler, boolean isAsync) {
    this.scheduler = scheduler;
    this.isAsync = isAsync;
  }

  @Override
  public @Nullable CallAdapter<?, ?> get(
      Type returnType, Annotation[] annotations, Retrofit retrofit) {
    Class<?> rawType = getRawType(returnType);

    if (rawType == Completable.class) {
      return createCompletableAdapter();
    }

    if (!isSupportedRxType(rawType)) {
      return null;
    }

    Type observableType = extractObservableType(returnType, rawType);
    ParsedType parsedType = parseObservableType(observableType);

    return new RxJava2CallAdapter(
        parsedType.responseType,
        scheduler,
        isAsync,
        parsedType.isResult,
        parsedType.isBody,
        rawType == Flowable.class,
        rawType == Single.class,
        rawType == Maybe.class,
        false);
  }

  private CallAdapter<?, ?> createCompletableAdapter() {
    return new RxJava2CallAdapter(
        Void.class, scheduler, isAsync, false, true, false, false, false, true);
  }

  private boolean isSupportedRxType(Class<?> rawType) {
    return rawType == Observable.class
        || rawType == Flowable.class
        || rawType == Single.class
        || rawType == Maybe.class;
  }

  private Type extractObservableType(Type returnType, Class<?> rawType) {
    if (!(returnType instanceof ParameterizedType)) {
      String name = rawType.getSimpleName();
      throw new IllegalStateException(
          name
              + " return type must be parameterized as "
              + name
              + "<Foo> or "
              + name
              + "<? extends Foo>");
    }
    return getParameterUpperBound(0, (ParameterizedType) returnType);
  }

  private ParsedType parseObservableType(Type observableType) {
    Class<?> rawObservableType = getRawType(observableType);

    if (rawObservableType == Response.class) {
      return new ParsedType(getInnerType(observableType, "Response"), false, false);
    }

    if (rawObservableType == Result.class) {
      return new ParsedType(getInnerType(observableType, "Result"), true, false);
    }

    return new ParsedType(observableType, false, true);
  }

  private Type getInnerType(Type type, String wrapperName) {
    if (!(type instanceof ParameterizedType)) {
      throw new IllegalStateException(
          wrapperName
              + " must be parameterized as "
              + wrapperName
              + "<Foo> or "
              + wrapperName
              + "<? extends Foo>");
    }
    return getParameterUpperBound(0, (ParameterizedType) type);
  }

  private static final class ParsedType {
    final Type responseType;
    final boolean isResult;
    final boolean isBody;

    ParsedType(Type responseType, boolean isResult, boolean isBody) {
      this.responseType = responseType;
      this.isResult = isResult;
      this.isBody = isBody;
    }
  }
}
