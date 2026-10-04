package com.rustanovych.kidsoutfit.ui.home

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rustanovych.kidsoutfit.R
import com.rustanovych.kidsoutfit.domain.model.ChildGender
import com.rustanovych.kidsoutfit.ui.home.components.SegmentStrip
import com.rustanovych.kidsoutfit.ui.theme.KidsWeatherOutfitTheme

private const val PHONE_WIDTH = 360
private const val PHONE_HEIGHT = 800

@Composable
private fun PreviewHome(state: HomeUiState) {
    KidsWeatherOutfitTheme {
        HomeContent(
            state = state,
            onDaySelected = {},
            onSegmentSelected = {},
            onRefresh = {},
            onOpenSettings = {},
        )
    }
}

@Preview(name = "Both, light", widthDp = PHONE_WIDTH, heightDp = PHONE_HEIGHT)
@Composable
private fun HomeBothLightPreview() = PreviewHome(HomePreviewData.loaded())

@Preview(
    name = "Both, dark",
    widthDp = PHONE_WIDTH,
    heightDp = PHONE_HEIGHT,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeBothDarkPreview() = PreviewHome(HomePreviewData.loaded())

@Preview(name = "Girl, segment selected", widthDp = PHONE_WIDTH, heightDp = PHONE_HEIGHT)
@Composable
private fun HomeGirlSelectedPreview() =
    PreviewHome(HomePreviewData.loaded(gender = ChildGender.GIRL, selectedIndex = 2))

@Preview(name = "Boy, rain", widthDp = PHONE_WIDTH, heightDp = PHONE_HEIGHT)
@Composable
private fun HomeBoyRainPreview() = PreviewHome(
    HomePreviewData.loaded(
        gender = ChildGender.BOY,
        backgroundRes = R.drawable.bg_rain,
        iconRes = R.drawable.ic_weather_rain,
    ),
)

@Preview(name = "Stale data", widthDp = PHONE_WIDTH, heightDp = PHONE_HEIGHT)
@Composable
private fun HomeStalePreview() = PreviewHome(HomePreviewData.stale)

@Preview(name = "Loading", widthDp = PHONE_WIDTH, heightDp = PHONE_HEIGHT)
@Composable
private fun HomeLoadingPreview() = PreviewHome(HomePreviewData.loading)

@Preview(name = "Error", widthDp = PHONE_WIDTH, heightDp = PHONE_HEIGHT)
@Composable
private fun HomeErrorPreview() = PreviewHome(HomePreviewData.error)

/**
 * All 12 sets at the real card size, 4 per row (a preview cannot scroll), to judge whether the
 * upper-body crop is enough to tell the sets apart.
 */
@Composable
private fun AllSetsStrips(genders: List<ChildGender>) {
    KidsWeatherOutfitTheme {
        Column(
            modifier = Modifier
                .background(Color(0xFFBFE6F8))
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            genders.forEach { gender ->
                HomePreviewData.allSetsSegments(gender, currentIndex = 1).chunked(4).forEach { row ->
                    SegmentStrip(
                        segments = row,
                        selectedIndex = if (gender == genders.first()) 2 else null,
                        focusIndex = null,
                        onSegmentSelected = {},
                    )
                }
            }
        }
    }
}

@Preview(name = "Strip: all 12 sets, boy and girl", widthDp = PHONE_WIDTH, heightDp = 860)
@Composable
private fun SegmentStripAllSetsSinglePreview() = AllSetsStrips(listOf(ChildGender.BOY, ChildGender.GIRL))

@Preview(name = "Strip: all 12 sets, both", widthDp = PHONE_WIDTH, heightDp = 440)
@Composable
private fun SegmentStripAllSetsBothPreview() = AllSetsStrips(listOf(ChildGender.BOTH))
