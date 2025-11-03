/*
 * Copyright (C) 2016 Jake Wharton
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
package retrofit_two.adapter.rxjava2;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.plugins.RxJavaPlugins;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;

final class RxJava2CallAdapter<R> implements CallAdapter<R, Object> {
  private final Type responseType;
  private final @Nullable Scheduler scheduler;
  private final boolean isAsync;
  private final AdapterConfig config;

  static final class AdapterConfig {
    final boolean isResult;
    final boolean isBody;
    final boolean isFlowable;
    final boolean isSingle;
    final boolean isMaybe;
    final boolean isCompletable;

    AdapterConfig(
        boolean isResult,
        boolean isBody,
        boolean isFlowable,
        boolean isSingle,
        boolean isMaybe,
        boolean isCompletable) {
      this.isResult = isResult;
      this.isBody = isBody;
      this.isFlowable = isFlowable;
      this.isSingle = isSingle;
      this.isMaybe = isMaybe;
      this.isCompletable = isCompletable;
    }
  }

  RxJava2CallAdapter(Type responseType, @Nullable Scheduler scheduler, boolean isAsync,
      AdapterConfig config) {
    this.responseType = responseType;
    this.scheduler = scheduler;
    this.isAsync = isAsync;
    this.config = config;
  }

  @Override
  public Type responseType() {
    return responseType;
  }

  @Override
  public Object adapt(Call<R> call) {
    Observable<Response<R>> responseObservable =
        isAsync ? new CallEnqueueObservable<>(call) : new CallExecuteObservable<>(call);

    Observable<?> observable;
    if (config.isResult) {
      observable = new ResultObservable<>(responseObservable);
    } else if (config.isBody) {
      observable = new BodyObservable<>(responseObservable);
    } else {
      observable = responseObservable;
    }

    if (scheduler != null) {
      observable = observable.subscribeOn(scheduler);
    }

  if (config.isFlowable) {
      // We only ever deliver a single value, and the RS spec states that you MUST request at least
      // one element which means we never need to honor backpressure.
      return observable.toFlowable(BackpressureStrategy.MISSING);
    }
    if (config.isSingle) {
      return observable.singleOrError();
    }
    if (config.isMaybe) {
      return observable.singleElement();
    }
    if (config.isCompletable) {
      return observable.ignoreElements();
    }
    return RxJavaPlugins.onAssembly(observable);
  }
}
