package com.baby.feedingtracker

import com.baby.feedingtracker.data.DataResult
import com.baby.feedingtracker.data.DiaperRecord
import com.baby.feedingtracker.data.DiaperRepository
import com.baby.feedingtracker.ui.diaper.DiaperViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DiaperViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: DiaperRepository
    private lateinit var viewModel: DiaperViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mock()
        whenever(repository.recentRecords).thenReturn(flowOf(emptyList()))

        viewModel = DiaperViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addRecord delegates to repository and sets lastAddedRecord`() = runTest(testDispatcher) {
        val record = DiaperRecord(id = "d-1", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(DataResult.Success(record))

        viewModel.addRecord()

        verify(repository).addRecord()
        assertEquals(record, viewModel.lastAddedRecord.value)
    }

    @Test
    fun `deleteRecord delegates to repository`() = runTest(testDispatcher) {
        val record = DiaperRecord(id = "d-del", timestamp = 2000L)

        viewModel.deleteRecord(record)

        verify(repository).deleteRecord(record)
    }

    @Test
    fun `clearLastAddedRecord resets to null`() = runTest(testDispatcher) {
        val record = DiaperRecord(id = "d-1", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(DataResult.Success(record))

        viewModel.addRecord()
        assertNotNull(viewModel.lastAddedRecord.value)

        viewModel.clearLastAddedRecord()
        assertNull(viewModel.lastAddedRecord.value)
    }

    @Test
    fun `uiState initial value has empty records`() {
        val state = viewModel.uiState.value
        assertEquals(emptyList<DiaperRecord>(), state.records)
        assertNull(state.elapsedMinutes)
        assertEquals(0, state.todayDiaperCount)
        assertEquals(0, state.todayUrineCount)
        assertEquals(0, state.todayStoolCount)
    }

    @Test
    fun `uiState collects records from repository flow`() = runTest(testDispatcher) {
        val records = listOf(
            DiaperRecord(id = "1", timestamp = 1000L, type = "diaper"),
            DiaperRecord(id = "2", timestamp = 2000L, type = "urine")
        )
        whenever(repository.recentRecords).thenReturn(flowOf(records))

        viewModel = DiaperViewModel(repository)

        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect { }
        }

        val state = viewModel.uiState.value
        assertEquals(2, state.records.size)
    }

    @Test
    fun `updateType delegates to repository`() = runTest(testDispatcher) {
        viewModel.updateType("d-1", "stool")

        verify(repository).updateType("d-1", "stool")
    }

    @Test
    fun `updateTimestamp delegates to repository`() = runTest(testDispatcher) {
        viewModel.updateTimestamp("d-1", 5000L)

        verify(repository).updateTimestamp("d-1", 5000L)
    }

    @Test
    fun `addRecord debounces rapid calls`() = runTest(testDispatcher) {
        val record = DiaperRecord(id = "d-1", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(DataResult.Success(record))

        viewModel.addRecord()
        viewModel.addRecord()

        verify(repository).addRecord()
    }
}
