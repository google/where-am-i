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
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders.Tile
import androidx.wear.tiles.TileProviderService
import com.dropbox.android.external.store4.get
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.wear.whereami.WhereAmIActivity
import com.google.wear.whereami.WhereAmIApplication
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.describeLocation
import com.google.wear.whereami.getTimeAgo
import com.google.wear.whereami.kt.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asListenableFuture
import kotlinx.coroutines.withContext

const val STABLE_RESOURCES_VERSION = "1"

class WhereAmITileProviderService : TileProviderService() {
    private lateinit var applicationScope: CoroutineScope
    private lateinit var locationViewModel: LocationViewModel

    override fun onCreate() {
        super.onCreate()

        locationViewModel = (this.applicationContext as WhereAmIApplication).locationViewModel
        applicationScope = (this.applicationContext as WhereAmIApplication).applicationScope
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(ResourceBuilders.Resources.builder().setVersion(STABLE_RESOURCES_VERSION).build())
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<Tile> {
        return applicationScope.async {
            suspendTileRequest(requestParams)
        }.asListenableFuture()
    }

    private suspend fun suspendTileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        return withContext(Dispatchers.IO) {
            val location = locationViewModel.store.get(LocationViewModel.Current)

            tile {
                setResourcesVersion(STABLE_RESOURCES_VERSION)

                timeline {
                    timelineEntry {
                        layout {
                            column {
                                setHorizontalAlignment(HORIZONTAL_ALIGN_CENTER)

                                setModifiers(
                                    modifiers {
                                        setSemantics(describeLocation(location).toContentDescription())
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
                                        setText(describeLocation(location))
                                    }
                                )
                                addContent(
                                    text {
                                        setMaxLines(1)
                                        setFontStyle(fontStyle {
                                            setSize(12f.toSpProp())
                                        })
                                        setText(getTimeAgo(location.time).toString())
                                    }
                                )
                            }
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
