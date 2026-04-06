package com.baby.feedingtracker

import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.UserRepository
import com.baby.feedingtracker.ui.feeding.FeedingViewModel
import com.google.firebase.auth.FirebaseAuth
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
class FeedingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: FeedingRepository
    private lateinit var userRepository: UserRepository
    private lateinit var googleAuthHelper: GoogleAuthHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var viewModel: FeedingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mock()
        userRepository = mock()
        googleAuthHelper = mock()
        auth = mock()

        whenever(repository.allRecords).thenReturn(flowOf(emptyList()))
        whenever(repository.latestRecord).thenReturn(flowOf(null))
        whenever(googleAuthHelper.isLoggedIn()).thenReturn(false)
        whenever(auth.currentUser).thenReturn(null)

        viewModel = FeedingViewModel(
            repository = repository,
            userRepository = userRepository,
            googleAuthHelper = googleAuthHelper,
            auth = auth
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addRecord delegates to repository and sets lastAddedRecord`() = runTest(testDispatcher) {
        val record = FeedingRecord(id = "test-id", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(record)

        viewModel.addRecord()

        verify(repository).addRecord()
        assertEquals(record, viewModel.lastAddedRecord.value)
    }

    @Test
    fun `deleteRecord delegates to repository`() = runTest(testDispatcher) {
        val record = FeedingRecord(id = "del-id", timestamp = 2000L)

        viewModel.deleteRecord(record)

        verify(repository).deleteRecord(record)
    }

    @Test
    fun `clearLastAddedRecord resets to null`() = runTest(testDispatcher) {
        val record = FeedingRecord(id = "test-id", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(record)

        viewModel.addRecord()
        assertNotNull(viewModel.lastAddedRecord.value)

        viewModel.clearLastAddedRecord()
        assertNull(viewModel.lastAddedRecord.value)
    }

    @Test
    fun `uiState initial value has empty records and null elapsed`() {
        val state = viewModel.uiState.value
        assertEquals(emptyList<FeedingRecord>(), state.records)
        assertNull(state.elapsedMinutes)
    }

    @Test
    fun `uiState collects records from repository flow`() = runTest(testDispatcher) {
        val records = listOf(
            FeedingRecord(id = "1", timestamp = 1000L),
            FeedingRecord(id = "2", timestamp = 2000L)
        )
        whenever(repository.allRecords).thenReturn(flowOf(records))
        whenever(repository.latestRecord).thenReturn(flowOf(records.last()))

        viewModel = FeedingViewModel(
            repository = repository,
            userRepository = userRepository,
            googleAuthHelper = googleAuthHelper,
            auth = auth
        )

        // Collect in background to activate WhileSubscribed stateIn
        backgroundScope.launch(testDispatcher) {
            viewModel.uiState.collect { }
        }

        val state = viewModel.uiState.value
        assertEquals(2, state.records.size)
        assertEquals("1", state.records[0].id)
        assertEquals("2", state.records[1].id)
    }

    @Test
    fun `addRecord debounces rapid calls`() = runTest(testDispatcher) {
        val record = FeedingRecord(id = "test-id", timestamp = 1000L)
        whenever(repository.addRecord()).thenReturn(record)

        viewModel.addRecord()
        viewModel.addRecord()

        verify(repository).addRecord()
    }

    @Test
    fun `updateRecordType delegates to repository`() = runTest(testDispatcher) {
        viewModel.updateRecordType("id-1", "breast", null, 10, 5)

        verify(repository).updateRecord("id-1", "breast", null, 10, 5)
    }

    @Test
    fun `updateRecordTimestamp delegates to repository`() = runTest(testDispatcher) {
        viewModel.updateRecordTimestamp("id-1", 5000L)

        verify(repository).updateTimestamp("id-1", 5000L)
    }
}
