/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.wear.whereami.tile

import android.content.ComponentName
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.wear.tiles.manager.TileUiClient
import com.google.wear.whereami.R

/**
 * Debug Activity that will render our Tile. This Activity lives inside the debug package, so it
 * will not be included in release builds.
 */
class TilePreviewActivity : ComponentActivity() {
    lateinit var tileClient: TileUiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_tile)
        val rootLayout = findViewById<FrameLayout>(R.id.tile_container)

        tileClient = TileUiClient(
            context = this,
            component = ComponentName(this, WhereAmITileProviderService::class.java),
            parentView = rootLayout
        )
        tileClient.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        tileClient.close()
    }
}
