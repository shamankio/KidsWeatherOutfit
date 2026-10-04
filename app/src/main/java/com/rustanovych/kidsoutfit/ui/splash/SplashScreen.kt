package com.rustanovych.kidsoutfit.ui.splash

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.domain.model.OutfitSet
import com.rustanovych.kidsoutfit.ui.assets.outfitDrawable
import com.rustanovych.kidsoutfit.ui.home.components.HomeErrorState
import com.rustanovych.kidsoutfit.ui.theme.KidsWeatherOutfitTheme
import com.rustanovych.kidsoutfit.ui.theme.LocalGlassColors
import org.koin.androidx.compose.koinViewModel

/**
 * The screen after the system splash. It owns the one permission this app asks for, and asks only
 * when the user taps the button — never on its own, and never again after a refusal.
 */
@Composable
fun SplashRoute(
    onDone: () -> Unit,
    onOpenCitySearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::onPermissionResult,
    )

    LaunchedEffect(state) {
        if (state == SplashUiState.Done) onDone()
    }
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SplashEffect.OpenManualCity -> onOpenCitySearch()
            }
        }
    }

    SplashContent(
        state = state,
        onAllowLocation = { permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
        onEnterCity = onOpenCitySearch,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@Composable
fun SplashContent(
    state: SplashUiState,
    onAllowLocation: () -> Unit,
    onEnterCity: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glass = LocalGlassColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(glass.pageTop, glass.pageBottom)))
            .safeDrawingPadding(),
    ) {
        when (state) {
            SplashUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HomeErrorState(onRetry = onRetry)
                // A broken GPS fix is common; a typed city is the way out that needs no sensors.
                TextButton(onClick = onEnterCity, modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = stringResource(R.string.onboarding_enter_city),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = glass.link,
                    )
                }
            }

            else -> OnboardingContent(
                loading = state != SplashUiState.NeedsLocation,
                onAllowLocation = onAllowLocation,
                onEnterCity = onEnterCity,
            )
        }
    }
}

@Composable
private fun OnboardingContent(
    loading: Boolean,
    onAllowLocation: () -> Unit,
    onEnterCity: () -> Unit,
) {
    val glass = LocalGlassColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Illustration and copy scroll when the font is large; the actions stay pinned in thumb reach.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Illustration()
            Column(
                modifier = Modifier.padding(top = 32.dp, start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    color = glass.ink,
                )
                Text(
                    text = stringResource(if (loading) R.string.splash_loading_body else R.string.onboarding_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = glass.inkSecondary,
                    modifier = Modifier.widthIn(max = 320.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            if (loading) {
                CircularProgressIndicator(color = glass.accent)
            } else {
                Button(
                    onClick = onAllowLocation,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = glass.accent, contentColor = glass.onAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.button_height)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ms_near_me),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_allow_location),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TextButton(
                    onClick = onEnterCity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_enter_city),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = glass.link,
                    )
                }
            }
        }
    }
}

/** The child under an umbrella on a white disc with soft rings, as in the design. */
@Composable
private fun Illustration() {
    val glass = LocalGlassColors.current
    val ringInner = dimensionResource(R.dimen.home_error_halo_inner)
    val ringOuter = dimensionResource(R.dimen.home_error_halo_outer)
    Box(
        modifier = Modifier
            // Room above for the outer ring, which is drawn outside the disc's bounds.
            .padding(top = 48.dp)
            .size(300.dp)
            .drawBehind {
                drawCircle(glass.accent.copy(alpha = 0.06f), radius = size.minDimension / 2 + ringOuter.toPx() + 4.dp.toPx())
                drawCircle(glass.accent.copy(alpha = 0.10f), radius = size.minDimension / 2 + ringInner.toPx() + 2.dp.toPx())
            }
            .background(Color.White, CircleShape),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Image(
            painter = painterResource(outfitDrawable(OutfitSet.COLD_RAIN, ChildGender.GIRL)),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 8.dp),
        )
    }
}

@Preview(name = "Needs location", widthDp = 360, heightDp = 800)
@Composable
private fun SplashNeedsLocationPreview() {
    KidsWeatherOutfitTheme { SplashContent(SplashUiState.NeedsLocation, {}, {}, {}) }
}

@Preview(name = "Loading", widthDp = 360, heightDp = 800)
@Composable
private fun SplashLoadingPreview() {
    KidsWeatherOutfitTheme { SplashContent(SplashUiState.Loading, {}, {}, {}) }
}

@Preview(name = "Error", widthDp = 360, heightDp = 800)
@Composable
private fun SplashErrorPreview() {
    KidsWeatherOutfitTheme { SplashContent(SplashUiState.Error, {}, {}, {}) }
}
