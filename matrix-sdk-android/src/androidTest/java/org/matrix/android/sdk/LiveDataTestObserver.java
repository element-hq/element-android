/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public final class LiveDataTestObserver<T> implements Observer<T> {
  private final List<T> valueHistory = new ArrayList<>();
  private final List<Observer<T>> childObservers = new ArrayList<>();

  @Deprecated // will be removed in version 1.0
  private final LiveData<T> observedLiveData;

  private CountDownLatch valueLatch = new CountDownLatch(1);

  private LiveDataTestObserver(LiveData<T> observedLiveData) {
    this.observedLiveData = observedLiveData;
  }

  @Override
  public void onChanged(@Nullable T value) {
    valueHistory.add(value);
    valueLatch.countDown();
    for (Observer<T> childObserver : childObservers) {
      childObserver.onChanged(value);
    }
  }

  public T value() {
    assertHasValue();
    return valueHistory.get(valueHistory.size() - 1);
  }

  public List<T> valueHistory() {
    return Collections.unmodifiableList(valueHistory);
  }

  /**
   * Disposes and removes observer from observed live data.
   *
   * @return This Observer
   * @deprecated Please use {@link LiveData#removeObserver(Observer)} instead, will be removed in 1.0
   */
  @Deprecated
  public LiveDataTestObserver<T> dispose() {
    observedLiveData.removeObserver(this);
    return this;
  }

  public LiveDataTestObserver<T> assertHasValue() {
    if (valueHistory.isEmpty()) {
      throw fail("Observer never received any value");
    }

    return this;
  }

  public LiveDataTestObserver<T> assertNoValue() {
    if (!valueHistory.isEmpty()) {
      throw fail("Expected no value, but received: " + value());
    }

    return this;
  }

  public LiveDataTestObserver<T> assertHistorySize(int expectedSize) {
    int size = valueHistory.size();
    if (size != expectedSize) {
      throw fail("History size differ; Expected: " + expectedSize + ", Actual: " + size);
    }
    return this;
  }

  public LiveDataTestObserver<T> assertValue(T expected) {
    T value = value();

    if (expected == null && value == null) {
      return this;
    }

    if (!value.equals(expected)) {
      throw fail("Expected: " + valueAndClass(expected) + ", Actual: " + valueAndClass(value));
    }

    return this;
  }

  public LiveDataTestObserver<T> assertValue(Function<T, Boolean> valuePredicate) {
    T value = value();

    if (!valuePredicate.apply(value)) {
      throw fail("Value not present");
    }

    return this;
  }

  public LiveDataTestObserver<T> assertNever(Function<T, Boolean> valuePredicate) {
    int size = valueHistory.size();
    for (int valueIndex = 0; valueIndex < size; valueIndex++) {
      T value = this.valueHistory.get(valueIndex);
      if (valuePredicate.apply(value)) {
        throw fail("Value at position " + valueIndex + " matches predicate "
          + valuePredicate.toString() + ", which was not expected.");
      }
    }

    return this;
  }

  /**
   * Awaits until this TestObserver has any value.
   * <p>
   * If this TestObserver has already value then this method returns immediately.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public LiveDataTestObserver<T> awaitValue() throws InterruptedException {
    valueLatch.await();
    return this;
  }

  /**
   * Awaits the specified amount of time or until this TestObserver has any value.
   * <p>
   * If this TestObserver has already value then this method returns immediately.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public LiveDataTestObserver<T> awaitValue(long timeout, TimeUnit timeUnit) throws InterruptedException {
    valueLatch.await(timeout, timeUnit);
    return this;
  }

  /**
   * Awaits until this TestObserver receives next value.
   * <p>
   * If this TestObserver has already value then it awaits for another one.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public LiveDataTestObserver<T> awaitNextValue() throws InterruptedException {
    return withNewLatch().awaitValue();
  }


  /**
   * Awaits the specified amount of time or until this TestObserver receives next value.
   * <p>
   * If this TestObserver has already value then it awaits for another one.
   *
   * @return this
   * @throws InterruptedException if the current thread is interrupted while waiting
   */
  public LiveDataTestObserver<T> awaitNextValue(long timeout, TimeUnit timeUnit) throws InterruptedException {
    return withNewLatch().awaitValue(timeout, timeUnit);
  }

  private LiveDataTestObserver<T> withNewLatch() {
    valueLatch = new CountDownLatch(1);
    return this;
  }

  private AssertionError fail(String message) {
    return new AssertionError(message);
  }

  private static String valueAndClass(Object value) {
    if (value != null) {
      return value + " (class: " + value.getClass().getSimpleName() + ")";
    }
    return "null";
  }

  public static <T> LiveDataTestObserver<T> create() {
    return new LiveDataTestObserver<>(new MutableLiveData<T>());
  }

  public static <T> LiveDataTestObserver<T> test(LiveData<T> liveData) {
    LiveDataTestObserver<T> observer = new LiveDataTestObserver<>(liveData);
    liveData.observeForever(observer);
    return observer;
  }
}