package com.google.wear.whereami

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.wear.whereami.data.LocationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LocationResultDisplay(location: LocationResult, refreshFn: suspend () -> Unit) {
    MaterialTheme() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Center,
            horizontalAlignment = CenterHorizontally,
        ) {
            Text(
                text = location.description,
                style = MaterialTheme.typography.title2,
                textAlign = TextAlign.Center,
                color = if (location.error != null) Color.Red else Color.Unspecified,
                modifier = Modifier
                    .semantics {
                        testTag = "Location"
                        contentDescription = location.description
                    }.padding(horizontal = 20.dp),
            )
            Text(
                text = location.formattedTime,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics {
                    testTag = "Time"
                    contentDescription = "Updated at " + location.formattedTime
                }
            )
            Spacer(modifier = Modifier.height(10.dp))

            val coroutineScope = rememberCoroutineScope()
            val refreshing = remember { mutableStateOf(false) }

            val refreshOnClick: () -> Unit = {
                coroutineScope.launch {
                    refreshing.value = true
                    try {
                        refreshFn()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        refreshing.value = false
                    }
                }
            }

            Button(
                enabled = !refreshing.value, onClick = refreshOnClick,
                modifier = Modifier.semantics {
                    testTag = "Refresh"
                    contentDescription = "Refresh Location"
                },
            ) {
                Text(
                    text = "Refresh",
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_WATCH,
    device = "id:wear_round",
    widthDp = 240,
    heightDp = 240,
    backgroundColor = 0xFF3D3D3D,
    showBackground = true
)
@Composable
fun ComposablePreview() {
    LocationResultDisplay(
        LocationResult(locationName = "1600 Amphitheatre Parkway"),
        refreshFn = {
            delay(1000)
        })
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_WATCH,
    device = "id:wear_round",
    widthDp = 240,
    heightDp = 240,
    backgroundColor = 0xFF3D3D3D,
    showBackground = true
)
@Composable
fun ComposablePreview2() {
    LocationResultDisplay(
        LocationResult.PermissionError,
        refreshFn = {
            delay(1000)
        })
}
