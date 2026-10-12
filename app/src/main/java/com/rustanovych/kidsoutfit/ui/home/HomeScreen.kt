package com.rustanovych.kidsoutfit.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.ads.AdBanner
import com.rustanovych.kidsoutfit.ui.assets.outfitDescription
import com.rustanovych.kidsoutfit.ui.components.StatusBarIcons
import com.rustanovych.kidsoutfit.ui.home.components.AdviceBanner
import com.rustanovych.kidsoutfit.ui.home.components.DaySwitcher
import com.rustanovych.kidsoutfit.ui.home.components.HeroCharacter
import com.rustanovych.kidsoutfit.ui.home.components.HomeErrorState
import com.rustanovych.kidsoutfit.ui.home.components.HomeHeader
import com.rustanovych.kidsoutfit.ui.home.components.HomeSkeleton
import com.rustanovych.kidsoutfit.ui.home.components.NotificationPromptCard
import com.rustanovych.kidsoutfit.ui.home.components.SegmentStrip
import com.rustanovych.kidsoutfit.ui.home.components.StaleBar
import com.rustanovych.kidsoutfit.ui.home.components.WeatherNowCard
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors
import org.koin.androidx.compose.koinViewModel

/** How much of the usable screen height the character takes. */
private const val HERO_HEIGHT_FRACTION = 0.45f
private const val BACKGROUND_CROSSFADE_MS = 600
private const val BANNER_FADE_MS = 200

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showAds by viewModel.showAds.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::onNotificationPermissionResult,
    )
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                // Only emitted on Android 13+, where the permission exists.
                HomeEffect.RequestNotificationPermission ->
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    HomeContent(
        state = state,
        onDaySelected = viewModel::onDaySelected,
        onSegmentSelected = viewModel::onSegmentSelected,
        onRefresh = viewModel::onRefresh,
        onOpenSettings = onOpenSettings,
        onNotificationPromptAccepted = viewModel::onNotificationPromptAccepted,
        onNotificationPromptDeclined = viewModel::onNotificationPromptDeclined,
        modifier = modifier,
        bottomBar = { if (showAds) AdBanner() },
    )
}

/**
 * Stateless home screen: a weather illustration as the backdrop, glass cards on top.
 *
 * @param bottomBar Slot for the ad banner. It becomes the Scaffold's bottom bar, so the
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
    onNotificationPromptAccepted: () -> Unit = {},
    onNotificationPromptDeclined: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
) {
    val glass = LocalGlassColors.current
    val sidePadding = dimensionResource(R.dimen.screen_horizontal_padding)
    val spacing = dimensionResource(R.dimen.home_section_spacing)

    // The top of every background is lightened by the scrim below, so the icons are always dark.
    StatusBarIcons(darkIcons = true)
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 45% of what the app can actually draw in: the status and navigation bars are not ours.
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        val usableHeight = maxHeight - insets.calculateTopPadding() - insets.calculateBottomPadding()
        val heroHeight = (usableHeight * HERO_HEIGHT_FRACTION).coerceAtLeast(dimensionResource(R.dimen.home_hero_min_height))

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
                                onNotificationPromptAccepted = onNotificationPromptAccepted,
                                onNotificationPromptDeclined = onNotificationPromptDeclined,
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
    onNotificationPromptAccepted: () -> Unit,
    onNotificationPromptDeclined: () -> Unit,
) {
    val sidePadding = dimensionResource(R.dimen.screen_horizontal_padding)
    state.weatherNow?.let { WeatherNowCard(it, Modifier.padding(horizontal = sidePadding)) }

    if (state.showNotificationPrompt) {
        NotificationPromptCard(
            onAccept = onNotificationPromptAccepted,
            onDecline = onNotificationPromptDeclined,
            modifier = Modifier.padding(horizontal = sidePadding),
        )
    }

    UpcomingAdvice(state, Modifier.padding(horizontal = sidePadding))

    state.currentOutfitLabel?.let { label ->
        HeroCharacter(
            outfits = state.currentOutfits,
            outfitLabel = label,
            description = outfitDescription(label, state.gender),
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

/**
 * The warning about the next outfit change. Its space is kept even when there is nothing left to
 * warn about (the last segment is shown), only faded out: removing it would pull the hero and the
 * strip up from under the finger that just tapped a card.
 */
@Composable
private fun UpcomingAdvice(state: HomeUiState, modifier: Modifier = Modifier) {
    // A day with a single outfit has no changes at all, so there is nothing to reserve room for.
    val advice = state.upcomingAdvice ?: state.advices.lastOrNull() ?: return
    val visible = state.upcomingAdvice != null
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(BANNER_FADE_MS),
        label = "adviceAlpha",
    )
    AdviceBanner(
        advice = advice,
        modifier = modifier
            .alpha(alpha)
            // Faded out means gone for a screen reader too.
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
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
