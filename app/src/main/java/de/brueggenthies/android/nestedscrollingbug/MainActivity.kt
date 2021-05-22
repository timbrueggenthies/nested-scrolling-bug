package de.brueggenthies.android.nestedscrollingbug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.brueggenthies.android.nestedscrollingbug.ui.theme.NestedScrollingBugTheme
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {

    private val testItems = (0..30).map { it }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NestedScrollingBugTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Column {
                        val consumer = OffsetScrollingNestedScrollingConsumer()
                        OffsetScrollingAppbarLayout(consumer = consumer) {
                            Text(
                                text="This should scroll off",
                                fontSize = 24.sp,
                                modifier = Modifier.scrollOff()
                            )
                            Text(
                                text="This should stay",
                                fontSize = 24.sp
                            )
                        }
                        LazyColumn(modifier = Modifier.nestedScroll(consumer)) {
                            items(testItems) { item ->
                                Text(text = item.toString(), modifier = Modifier.padding(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OffsetScrollingAppbarLayout(
    consumer: OffsetScrollingNestedScrollingConsumer = OffsetScrollingNestedScrollingConsumer(),
    content: @Composable OffsetScrollingAppbarLayoutScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height + consumer.offset.toInt()) {
                    placeable.place(0, consumer.offset.toInt())
                }
            }
    ) {
        val scopeImpl = OffsetScrollingAppbarLayoutScopeImpl(this, consumer)
        scopeImpl.content()
    }
}

class OffsetScrollingAppbarLayoutScopeImpl(
    private val columnScope: ColumnScope,
    private val offsetHeightSum: OffsetSum
) : ColumnScopeDelegation(columnScope), OffsetScrollingAppbarLayoutColumnScope {

    override fun Modifier.scrollOff(): Modifier {
        return composed {
            var previousHeight by remember { mutableStateOf(0) }
            onSizeChanged {
                offsetHeightSum.add(previousHeight, it.height)
                previousHeight = it.height
            }
        }
    }
}

abstract class ColumnScopeDelegation(columnScope: ColumnScope) : ColumnScope by columnScope

interface OffsetScrollingAppbarLayoutColumnScope : OffsetScrollingAppbarLayoutScope, ColumnScope

interface OffsetScrollingAppbarLayoutScope {

    fun Modifier.scrollOff(): Modifier
}

interface OffsetSum {

    fun add(previous: Int, new: Int)
}

class OffsetScrollingNestedScrollingConsumer : NestedScrollConnection, OffsetSum {

    private var maxOffset = 0f

    private val offsetState = mutableStateOf(0f)
    val offset by offsetState

    override fun add(previous: Int, new: Int) {
        maxOffset = maxOffset - previous + new
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y > 0) return Offset.Zero
        val newOffset = min(0f, max(-maxOffset, offsetState.value + available.y))
        val consumed = Offset(x = 0f, y = newOffset - offsetState.value)
        offsetState.value = newOffset
        return consumed
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val newOffset = min(0f, max(-maxOffset, offsetState.value + available.y))
        val newConsumed = Offset(x = 0f, y = newOffset - offsetState.value)
        offsetState.value = newOffset
        return newConsumed
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return available
    }
}
