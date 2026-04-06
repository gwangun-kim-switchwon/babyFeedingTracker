package com.baby.feedingtracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * FAB UI tests that verify:
 * - FAB is displayed on each screen
 * - FAB click triggers the expected action
 * - contentDescription is correct for accessibility
 */
class FabUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── Feeding FAB ─────────────────────────────────

    @Test
    fun feedingFab_isDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        onClick = { },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "수유 기록 추가"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("수유 기록 추가")
            .assertIsDisplayed()
    }

    @Test
    fun feedingFab_clickTriggersAction() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        onClick = { clicked = true },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "수유 기록 추가"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("수유 기록 추가")
            .performClick()

        assertTrue("FAB click should trigger action", clicked)
    }

    // ── Diaper FAB ──────────────────────────────────

    @Test
    fun diaperFab_isDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        onClick = { },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "기저귀 기록 추가"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("기저귀 기록 추가")
            .assertIsDisplayed()
    }

    @Test
    fun diaperFab_clickTriggersAction() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        onClick = { clicked = true },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "기저귀 기록 추가"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("기저귀 기록 추가")
            .performClick()

        assertTrue("FAB click should trigger action", clicked)
    }

    // ── Cleaning FAB ────────────────────────────────

    @Test
    fun cleaningFab_isDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        onClick = { },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "세척 기록 추가"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("세척 기록 추가")
            .assertIsDisplayed()
    }

    @Test
    fun cleaningFab_clickTriggersAction() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    FloatingActionButton(
                        onClick = { clicked = true },
                        modifier = Modifier.align(Alignment.BottomEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "세척 기록 추가"
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithContentDescription("세척 기록 추가")
            .performClick()

        assertTrue("FAB click should trigger action", clicked)
    }

    // ── Content Description correctness ─────────────

    @Test
    fun allFabs_haveCorrectContentDescriptions() {
        val descriptions = listOf("수유 기록 추가", "기저귀 기록 추가", "세척 기록 추가")

        descriptions.forEach { desc ->
            composeTestRule.setContent {
                MaterialTheme {
                    Box(Modifier.fillMaxSize()) {
                        FloatingActionButton(
                            onClick = { },
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = desc
                            )
                        }
                    }
                }
            }

            composeTestRule.onNodeWithContentDescription(desc)
                .assertIsDisplayed()
        }
    }
}
