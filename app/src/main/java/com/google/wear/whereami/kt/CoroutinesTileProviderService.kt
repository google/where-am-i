// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.wear.whereami.kt

import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders.Resources
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileProviderService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture

const val STABLE_RESOURCES_VERSION = "1"

abstract class CoroutinesTileProviderService : TileProviderService() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        return serviceScope.async {
            suspendTileRequest(requestParams)
        }.asListenableFuture()
    }

    abstract suspend fun suspendTileRequest(requestParams: RequestBuilders.TileRequest): Tile

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<Resources> =
        Futures.immediateFuture(Resources.builder().setVersion(STABLE_RESOURCES_VERSION).build())
}