/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.flink.lookup;

import org.apache.flink.table.data.RowData;
import org.apache.flink.table.functions.AsyncLookupFunction;
import org.apache.flink.table.functions.FunctionContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static org.apache.paimon.utils.FileUtils.COMMON_IO_FORK_JOIN_POOL;

/** A {@link AsyncLookupFunction} to wrap sync function. */
public class AsyncLookupFunctionWrapper extends AsyncLookupFunction {

    private final NewLookupFunction function;

    public AsyncLookupFunctionWrapper(NewLookupFunction function) {
        this.function = function;
    }

    @Override
    public void open(FunctionContext context) throws Exception {
        function.open(context);
    }

    private Collection<RowData> lookup(RowData keyRow) {
        try {
            synchronized (function) {
                return function.lookup(keyRow);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CompletableFuture<Collection<RowData>> asyncLookup(RowData keyRow) {
        return CompletableFuture.supplyAsync(() -> lookup(keyRow), COMMON_IO_FORK_JOIN_POOL);
    }

    @Override
    public void close() throws Exception {
        function.close();
    }
}
