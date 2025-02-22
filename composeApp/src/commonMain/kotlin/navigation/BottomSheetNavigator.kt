package navigation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.SwipeableDefaults
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.Stack
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.compositionUniqueId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

typealias BottomSheetNavigatorContent = @Composable (bottomSheetNavigator: BottomSheetNavigator) -> Unit

val LocalBottomSheetNavigator: ProvidableCompositionLocal<BottomSheetNavigator> =
    staticCompositionLocalOf { error("BottomSheetNavigator not initialized") }

@OptIn(InternalVoyagerApi::class)
@ExperimentalMaterialApi
@Composable
fun BottomSheetNavigator(
    modifier: Modifier = Modifier,
    hideOnBackPress: Boolean = true,
    scrimColor: Color = ModalBottomSheetDefaults.scrimColor,
    sheetShape: Shape = androidx.compose.material3.MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    sheetContentColor: Color = androidx.compose.material3.contentColorFor(sheetBackgroundColor),
    sheetGesturesEnabled: Boolean = true,
    skipHalfExpanded: Boolean = true,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    key: String = compositionUniqueId(),
    sheetContent: BottomSheetNavigatorContent = { CurrentScreen() },
    content: BottomSheetNavigatorContent
) {
    var hideBottomSheet: (() -> Unit)? = null
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { state ->
            if (state == ModalBottomSheetValue.Hidden) {
                hideBottomSheet?.invoke()
            }
            true
        },
        skipHalfExpanded = skipHalfExpanded,
        animationSpec = animationSpec
    )

    Navigator(BottomSheetScreen, onBackPressed = null, key = key) { navigator ->
        val bottomSheetNavigator = remember(navigator, sheetState, coroutineScope) {
            BottomSheetNavigator(navigator, sheetState, coroutineScope)
        }

        hideBottomSheet = bottomSheetNavigator::hide

        CompositionLocalProvider(LocalBottomSheetNavigator provides bottomSheetNavigator) {
            ModalBottomSheetLayout(
                modifier = modifier,
                scrimColor = scrimColor,
                sheetState = sheetState,
                sheetShape = sheetShape,
                sheetElevation = sheetElevation,
                sheetBackgroundColor = sheetBackgroundColor,
                sheetContentColor = sheetContentColor,
                sheetGesturesEnabled = sheetGesturesEnabled,
                sheetContent = {
                    M2M3ColorSync {
                        BottomSheetNavigatorBackHandler(
                            bottomSheetNavigator,
                            sheetState,
                            hideOnBackPress
                        )
                        sheetContent(bottomSheetNavigator)
                    }
                },
                content = {
                    M2M3ColorSync {
                        content(bottomSheetNavigator)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
class BottomSheetNavigator internal constructor(
    private val navigator: Navigator,
    private val sheetState: ModalBottomSheetState,
    private val coroutineScope: CoroutineScope
) : Stack<Screen> by navigator {

    val isVisible: Boolean
        get() = sheetState.isVisible

    val progress: Float
        get() = sheetState.progress

    fun show(screen: Screen) {
        coroutineScope.launch {
            replaceAll(screen)
            sheetState.show()
        }
    }

    fun hide() {
        coroutineScope.launch {
            if (isVisible) {
                sheetState.hide()
                replaceAll(BottomSheetScreen)
            }
        }
    }

    @Composable
    fun saveableState(
        key: String,
        content: @Composable () -> Unit
    ) {
        navigator.saveableState(key, content = content)
    }
}

private object BottomSheetScreen : Screen {

    @Composable
    override fun Content() {
        Spacer(modifier = Modifier.height(1.dp))
    }
}

@ExperimentalMaterialApi
@Composable
private fun BottomSheetNavigatorBackHandler(
    navigator: BottomSheetNavigator,
    bottomSheetState: ModalBottomSheetState,
    hideOnBackPress: Boolean
) {
    BackHandler(enabled = bottomSheetState.isVisible) {
        if (navigator.pop().not() && hideOnBackPress) {
            navigator.hide()
        }
    }
}

@Composable
private fun M2M3ColorSync(content: @Composable () -> Unit) {
    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides androidx.compose.material.LocalContentColor.current) {
        content()
    }
}