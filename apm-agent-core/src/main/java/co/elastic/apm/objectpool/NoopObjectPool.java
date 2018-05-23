/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 the original author or authors
 * %%
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
 * #L%
 */
package co.elastic.apm.objectpool;

import javax.annotation.Nullable;
import java.io.IOException;

public class NoopObjectPool<T extends Recyclable> implements ObjectPool<T> {

    private final RecyclableObjectFactory<T> recyclableObjectFactory;

    public NoopObjectPool(RecyclableObjectFactory<T> recyclableObjectFactory) {
        this.recyclableObjectFactory = recyclableObjectFactory;
    }

    @Nullable
    @Override
    public T tryCreateInstance() {
        return recyclableObjectFactory.createInstance();
    }

    @Override
    public T createInstance() {
        return recyclableObjectFactory.createInstance();
    }

    @Override
    public void fillFromOtherPool(ObjectPool<T> otherPool, int maxElements) {

    }

    @Override
    public void recycle(T obj) {

    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public int getObjectsInPool() {
        return 0;
    }

    @Override
    public long getGarbageCreated() {
        return 0;
    }

    @Override
    public void close() throws IOException {
    }
}