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

import androidx.wear.tiles.*

fun text(fn: LayoutElementBuilders.Text.Builder.() -> Unit): LayoutElementBuilders.Text {
    val builder = LayoutElementBuilders.Text.builder()
    fn(builder)
    return builder.build()
}

fun modifiers(fn: ModifiersBuilders.Modifiers.Builder.() -> Unit): ModifiersBuilders.Modifiers {
    val builder = ModifiersBuilders.Modifiers.builder()
    fn(builder)
    return builder.build()
}

fun activityClickable(
    packageName: String,
    activity: String
) = ModifiersBuilders.Clickable.builder()
    .setOnClick(
        ActionBuilders.LaunchAction.builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.builder()
                    .setPackageName(packageName)
                    .setClassName(activity)
                    .build()
            )
            .build()
    )

fun fontStyle(fn: LayoutElementBuilders.FontStyle.Builder.() -> Unit): LayoutElementBuilders.FontStyle {
    val builder = LayoutElementBuilders.FontStyle.builder()
    fn(builder)
    return builder.build()
}

fun TimelineBuilders.TimelineEntry.Builder.layout(fn: () -> LayoutElementBuilders.LayoutElement) {
    setLayout(LayoutElementBuilders.Layout.builder().setRoot(fn()).build())
}

fun tile(fn: TileBuilders.Tile.Builder.() -> Unit): TileBuilders.Tile {
    val builder = TileBuilders.Tile.builder()
    fn(builder)
    return builder.build()
}

fun TileBuilders.Tile.Builder.timeline(fn: TimelineBuilders.Timeline.Builder.() -> Unit) {
    val builder = TimelineBuilders.Timeline.builder()
    builder.fn()
    setTimeline(builder.build())
}

fun column(fn: LayoutElementBuilders.Column.Builder.() -> Unit): LayoutElementBuilders.Column {
    val builder = LayoutElementBuilders.Column.builder()
    builder.fn()
    return builder.build()
}

fun TimelineBuilders.Timeline.Builder.timelineEntry(fn: TimelineBuilders.TimelineEntry.Builder.() -> Unit) {
    val builder = TimelineBuilders.TimelineEntry.builder()
    fn(builder)
    addTimelineEntry(builder.build())
}

fun Float.toSpProp() = DimensionBuilders.SpProp.builder().setValue(this).build()

fun Float.toDpProp() = DimensionBuilders.DpProp.builder().setValue(this).build()

fun Int.toColorProp(): ColorBuilders.ColorProp =
    ColorBuilders.ColorProp.builder().setArgb(this).build()

fun String.toContentDescription() =
    ModifiersBuilders.Semantics.builder().setContentDescription(
        this
    ).build()