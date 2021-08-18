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

import androidx.wear.tiles.*
import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.TileBuilders.Tile
import com.google.wear.whereami.WhereAmIActivity
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.format.describeLocation
import com.google.wear.whereami.kt.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhereAmITileProviderService : CoroutinesTileProviderService() {
    private lateinit var locationViewModel: LocationViewModel

    override fun onCreate() {
        super.onCreate()

        locationViewModel = LocationViewModel(applicationContext)
    }

    override suspend fun suspendTileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        return withContext(Dispatchers.IO) {
            val location = locationViewModel.readLocationResult()

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
                            }
                        }
                    }
                }
            }
        }
    }
}
