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
package com.google.wear.whereami.tile

import android.content.Context
import android.util.Log
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.wear.whereami.WhereAmIActivity
import com.google.wear.whereami.applicationScope
import com.google.wear.whereami.data.LocationResult
import com.google.wear.whereami.kt.*
import com.google.wear.whereami.locationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.asListenableFuture
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val STABLE_RESOURCES_VERSION = "1"

class WhereAmITileProviderService : TileService() {
    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder().setVersion(STABLE_RESOURCES_VERSION).build()
        )
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        return this.applicationScope.async {
            suspendTileRequest(requestParams)
        }.asListenableFuture()
    }

    private suspend fun suspendTileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        Log.i("WhereAmI", "tileRequest $requestParams")

        val location = locationViewModel.databaseLocationStream().first()

        // Force a refresh if we have stale (> 20 minutes) results or errors
        if (location.freshness > LocationResult.Freshness.STALE_EXACT) {
            applicationScope.launch {
                // force a refresh
                locationViewModel.readFreshLocationResult(
                    freshLocation = null
                )
            }
        }

        return tile {
            setResourcesVersion(STABLE_RESOURCES_VERSION)
            setFreshnessIntervalMillis(0L)

            timeline {
                timelineEntry {
                    layout {
                        column {
                            setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

                            setModifiers(
                                modifiers {
                                    setSemantics(location.description.toContentDescription())
                                    setClickable(
                                        activityClickable(
                                            this@WhereAmITileProviderService.packageName,
                                            WhereAmIActivity::class.java.name
                                        )
                                    )
                                }
                            )
                            addContent(
                                text {
                                    setMaxLines(2)
                                    setFontStyle(fontStyle {
                                        setSize(16f.toSpProp())
                                    })
                                    setText(location.description)
                                }
                            )
                            addContent(
                                text {
                                    setMaxLines(1)
                                    setFontStyle(fontStyle {
                                        setSize(12f.toSpProp())
                                    })
                                    setText(location.formattedTime)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun Context.forceTileUpdate() {
            getUpdater(applicationContext).requestUpdate(WhereAmITileProviderService::class.java)
        }
    }
}
