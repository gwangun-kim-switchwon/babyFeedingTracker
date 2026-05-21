package com.baby.feedingtracker

import com.baby.feedingtracker.data.DataResult
import com.baby.feedingtracker.data.GrowthRecord
import com.baby.feedingtracker.data.GrowthRepository
import com.baby.feedingtracker.ui.growth.GrowthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GrowthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: GrowthRepository
    private lateinit var viewModel: GrowthViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        whenever(repository.recentRecords).thenReturn(flowOf(emptyList()))
        viewModel = GrowthViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty records and null latest values`() = runTest {
        assertEquals(emptyList<GrowthRecord>(), viewModel.uiState.value.records)
        assertNull(viewModel.uiState.value.latestWeight)
        assertNull(viewModel.uiState.value.latestHeight)
        assertNull(viewModel.uiState.value.latestHead)
    }

    @Test
    fun `addRecord with no measurements sets errorMessage`() = runTest {
        viewModel.addRecord(GrowthRecord())
        assertEquals("최소 하나의 측정값을 입력해주세요", viewModel.errorMessage.value)
    }

    @Test
    fun `addRecord with only weight does not set errorMessage`() = runTest {
        whenever(repository.addGrowthRecord(any()))
            .thenReturn(DataResult.Success(GrowthRecord(id = "1", weightKg = 4.8f)))
        viewModel.addRecord(GrowthRecord(weightKg = 4.8f))
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `addRecord delegates to repository`() = runTest {
        val record = GrowthRecord(weightKg = 4.8f)
        whenever(repository.addGrowthRecord(any()))
            .thenReturn(DataResult.Success(record.copy(id = "new")))
        viewModel.addRecord(record)
        verify(repository).addGrowthRecord(any())
    }

    @Test
    fun `repository error sets errorMessage`() = runTest {
        whenever(repository.addGrowthRecord(any()))
            .thenReturn(DataResult.Error("저장에 실패했습니다"))
        viewModel.addRecord(GrowthRecord(weightKg = 4.8f))
        assertEquals("저장에 실패했습니다", viewModel.errorMessage.value)
    }

    @Test
    fun `latestWeight returns most recent record with non-null weight`() = runTest {
        val records = listOf(
            GrowthRecord(id = "1", timestamp = 3000L, heightCm = 56f),         // no weight
            GrowthRecord(id = "2", timestamp = 2000L, weightKg = 4.8f),         // latest weight
            GrowthRecord(id = "3", timestamp = 1000L, weightKg = 4.1f),
        )
        whenever(repository.recentRecords).thenReturn(flowOf(records))
        val vm = GrowthViewModel(repository)
        assertEquals(4.8f, vm.uiState.value.latestWeight)
    }

    @Test
    fun `clearError resets errorMessage to null`() = runTest {
        viewModel.addRecord(GrowthRecord())  // sets error
        viewModel.clearError()
        assertNull(viewModel.errorMessage.value)
    }
}
