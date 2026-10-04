package com.rustanovych.kidsoutfit.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ui.home.components.AdviceBanner
import com.rustanovych.kidsoutfit.ui.home.components.DaySwitcher
import com.rustanovych.kidsoutfit.ui.home.components.HeroCharacter
import com.rustanovych.kidsoutfit.ui.home.components.HomeErrorState
import com.rustanovych.kidsoutfit.ui.home.components.HomeHeader
import com.rustanovych.kidsoutfit.ui.home.components.HomeSkeleton
import com.rustanovych.kidsoutfit.ui.home.components.SegmentStrip
import com.rustanovych.kidsoutfit.ui.home.components.StaleBar
import com.rustanovych.kidsoutfit.ui.home.components.WeatherNowCard
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors
import org.koin.androidx.compose.koinViewModel

/** How much of the screen height the character takes. */
private const val HERO_HEIGHT_FRACTION = 0.45f
private const val BACKGROUND_CROSSFADE_MS = 600

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onDaySelected = viewModel::onDaySelected,
        onSegmentSelected = viewModel::onSegmentSelected,
        onRefresh = viewModel::onRefresh,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

/**
 * Stateless home screen: a weather illustration as the backdrop, glass cards on top.
 *
 * @param bottomBar Slot for the stage 7 ad banner. It becomes the Scaffold's bottom bar, so the
 * scrolling content always ends above it and nothing is covered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onDaySelected: (Day) -> Unit,
    onSegmentSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
) {
    val glass = LocalGlassColors.current
    val sidePadding = dimensionResource(R.dimen.screen_horizontal_padding)
    val spacing = dimensionResource(R.dimen.home_section_spacing)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val heroHeight = (maxHeight * HERO_HEIGHT_FRACTION).coerceAtLeast(dimensionResource(R.dimen.home_hero_min_height))

        HomeBackground(state)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.home_top_scrim_height))
                .background(Brush.verticalGradient(listOf(glass.scrim, Color.Transparent))),
        )

        Scaffold(containerColor = Color.Transparent, bottomBar = bottomBar) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                if (state.isStale) StaleBar(state.updatedAtLabel, onRefresh)
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(spacing),
                    ) {
                        HomeHeader(
                            locationName = state.locationName,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.padding(horizontal = sidePadding),
                        )
                        when {
                            state.error -> HomeErrorState(onRetry = onRefresh, modifier = Modifier.padding(top = 24.dp))
                            state.isLoading -> HomeSkeleton(heroHeight)
                            else -> LoadedContent(
                                state = state,
                                heroHeight = heroHeight,
                                onDaySelected = onDaySelected,
                                onSegmentSelected = onSegmentSelected,
                            )
                        }
                        Spacer(Modifier.height(dimensionResource(R.dimen.home_content_bottom_margin) - spacing))
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadedContent(
    state: HomeUiState,
    heroHeight: Dp,
    onDaySelected: (Day) -> Unit,
    onSegmentSelected: (Int) -> Unit,
) {
    val sidePadding = dimensionResource(R.dimen.screen_horizontal_padding)
    state.weatherNow?.let { WeatherNowCard(it, Modifier.padding(horizontal = sidePadding)) }

    AnimatedVisibility(visible = state.upcomingAdvice != null) {
        // Hold on to the last advice so the card keeps its text while it animates out.
        state.upcomingAdvice?.let { AdviceBanner(it, Modifier.padding(horizontal = sidePadding)) }
    }

    state.currentOutfitLabel?.let { label ->
        HeroCharacter(
            outfits = state.currentOutfits,
            outfitLabel = label,
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
        )
    }

    DaySwitcher(
        selectedDay = state.selectedDay,
        onDaySelected = onDaySelected,
        modifier = Modifier.padding(horizontal = sidePadding),
    )
    SegmentStrip(
        segments = state.segments,
        selectedIndex = state.selectedSegmentIndex,
        focusIndex = state.shownSegmentIndex,
        onSegmentSelected = onSegmentSelected,
    )
}

/** The weather picture, cross-faded as the scene changes; a pale gradient when there is no weather yet. */
@Composable
private fun HomeBackground(state: HomeUiState) {
    val glass = LocalGlassColors.current
    val scene: Int? = if (state.isLoading || state.error) null else state.backgroundRes
    Crossfade(
        targetState = scene,
        animationSpec = tween(BACKGROUND_CROSSFADE_MS),
        label = "homeBackground",
    ) { res ->
        if (res != null) {
            // Decorative: the weather is described by the weather card.
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(glass.pageTop, glass.pageBottom))),
            )
        }
    }
}
