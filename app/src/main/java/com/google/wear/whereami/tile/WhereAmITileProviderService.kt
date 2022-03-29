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

import androidx.wear.tiles.LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.tiles.CoroutinesTileService
import com.google.wear.whereami.WhereAmIActivity
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.describeLocation
import com.google.wear.whereami.kt.activityClickable
import com.google.wear.whereami.kt.column
import com.google.wear.whereami.kt.fontStyle
import com.google.wear.whereami.kt.layout
import com.google.wear.whereami.kt.modifiers
import com.google.wear.whereami.kt.text
import com.google.wear.whereami.kt.tile
import com.google.wear.whereami.kt.timeline
import com.google.wear.whereami.kt.timelineEntry
import com.google.wear.whereami.kt.toContentDescription
import com.google.wear.whereami.kt.toSpProp

class WhereAmITileProviderService : CoroutinesTileService() {
    private lateinit var locationViewModel: LocationViewModel

    override fun onCreate() {
        super.onCreate()

        locationViewModel = LocationViewModel(applicationContext)
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): Tile {
        val location = locationViewModel.readLocationResult()

        return tile {
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

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(STABLE_RESOURCES_VERSION)
            .build()
    }

    companion object {
        const val STABLE_RESOURCES_VERSION = "1"
    }
}
