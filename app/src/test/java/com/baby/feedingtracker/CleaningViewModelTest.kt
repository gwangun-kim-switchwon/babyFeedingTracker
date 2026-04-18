package com.baby.feedingtracker

import com.baby.feedingtracker.data.CleaningRecord
import com.baby.feedingtracker.data.CleaningRepository
import com.baby.feedingtracker.data.DataResult
import com.baby.feedingtracker.ui.cleaning.CleaningViewModel
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
class CleaningViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: CleaningRepository
    private lateinit var viewModel: CleaningViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mock()
        whenever(repository.recentRecords).thenReturn(flowOf(emptyList()))

        viewModel = CleaningViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addRecord delegates to repository and sets lastAddedRecord`() = runTest(testDispatcher) {
        val record = CleaningRecord(id = "c-1", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(DataResult.Success(record))

        viewModel.addRecord()

        verify(repository).addRecord()
        assertEquals(record, viewModel.lastAddedRecord.value)
    }

    @Test
    fun `deleteRecord delegates to repository`() = runTest(testDispatcher) {
        val record = CleaningRecord(id = "c-del", timestamp = 2000L)

        viewModel.deleteRecord(record)

        verify(repository).deleteRecord(record)
    }

    @Test
    fun `clearLastAddedRecord resets to null`() = runTest(testDispatcher) {
        val record = CleaningRecord(id = "c-1", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(DataResult.Success(record))

        viewModel.addRecord()
        assertNotNull(viewModel.lastAddedRecord.value)

        viewModel.clearLastAddedRecord()
        assertNull(viewModel.lastAddedRecord.value)
    }

    @Test
    fun `uiState initial value has empty records and empty perTypeElapsed`() {
        val state = viewModel.uiState.value
        assertEquals(emptyList<CleaningRecord>(), state.records)
        assertNull(state.elapsedMinutes)
        assertEquals(emptyMap<String, Long>(), state.perTypeElapsed)
    }

    @Test
    fun `uiState collects records from repository flow`() = runTest(testDispatcher) {
        val records = listOf(
            CleaningRecord(id = "1", timestamp = 1000L, itemType = "bottle"),
            CleaningRecord(id = "2", timestamp = 2000L, itemType = "pot")
        )
        whenever(repository.recentRecords).thenReturn(flowOf(records))

        viewModel = CleaningViewModel(repository)

        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect { }
        }

        val state = viewModel.uiState.value
        assertEquals(2, state.records.size)
    }

    @Test
    fun `updateItemType delegates to repository`() = runTest(testDispatcher) {
        viewModel.updateItemType("c-1", "pump")

        verify(repository).updateItemType("c-1", "pump")
    }

    @Test
    fun `updateTimestamp delegates to repository`() = runTest(testDispatcher) {
        viewModel.updateTimestamp("c-1", 5000L)

        verify(repository).updateTimestamp("c-1", 5000L)
    }

    @Test
    fun `addRecord debounces rapid calls`() = runTest(testDispatcher) {
        val record = CleaningRecord(id = "c-1", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(DataResult.Success(record))

        viewModel.addRecord()
        viewModel.addRecord()

        verify(repository).addRecord()
    }
}
