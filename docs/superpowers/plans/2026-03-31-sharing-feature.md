# Sharing Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable two users (e.g., parents) to share feeding records in real-time by linking accounts with a 6-character invite code, so both see and write to the same Firestore data.

**Architecture:** Anonymous Firebase users upgrade to Google Sign-In via `linkWithCredential` (preserving uid and existing data). A `UserProfile` document stores `dataOwnerUid` which determines whose `feeding_records` collection both users read/write. Invite codes are ephemeral Firestore documents (10-minute TTL) that broker the one-time link between host and guest.

**Tech Stack:** Firebase Auth (anonymous + Google provider linking), Firestore (profiles, invite codes), Google Play Services Auth (Google Sign-In), Jetpack Compose Material3 (ModalBottomSheet), Kotlin Coroutines/Flow

**Spec:** `docs/superpowers/specs/2026-03-31-sharing-feature-design.md`

---

## Task 1: Build config -- add play-services-auth dependency

- [ ] Add Google Play Services Auth dependency to `app/build.gradle.kts`

**File:** `app/build.gradle.kts`

**Edit:** old_string -> new_string

old_string:
```kotlin
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
```

new_string:
```kotlin
    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/build.gradle.kts && git commit -m "Add play-services-auth dependency for Google Sign-In"
```

---

## Task 2: GoogleAuthHelper -- Google Sign-In + anonymous account linking

- [ ] Create `app/src/main/java/com/baby/feedingtracker/data/GoogleAuthHelper.kt`

**File (CREATE):** `app/src/main/java/com/baby/feedingtracker/data/GoogleAuthHelper.kt`

```kotlin
package com.baby.feedingtracker.data

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthHelper(
    private val auth: FirebaseAuth,
    context: Context
) {
    private val googleSignInClient: GoogleSignInClient

    init {
        val webClientId = context.getString(
            context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        )
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun isLoggedIn(): Boolean {
        val user = auth.currentUser ?: return false
        return user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }
    }

    fun currentUserEmail(): String? {
        val user = auth.currentUser ?: return null
        return user.providerData
            .firstOrNull { it.providerId == GoogleAuthProvider.PROVIDER_ID }
            ?.email
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(data: Intent?): Result<String> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
                ?: return Result.failure(Exception("Google ID token is null"))

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val currentUser = auth.currentUser

            if (currentUser != null && currentUser.isAnonymous) {
                // Link anonymous account with Google credential (preserves uid)
                val result = currentUser.linkWithCredential(credential).await()
                Result.success(result.user!!.uid)
            } else if (currentUser != null) {
                // Already signed in with some provider, just return uid
                Result.success(currentUser.uid)
            } else {
                // No current user, sign in directly
                val result = auth.signInWithCredential(credential).await()
                Result.success(result.user!!.uid)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/data/GoogleAuthHelper.kt && git commit -m "Add GoogleAuthHelper for Google Sign-In and anonymous account linking"
```

---

## Task 3: UserRepository -- profile + invite code management

- [ ] Create `app/src/main/java/com/baby/feedingtracker/data/UserRepository.kt`

**File (CREATE):** `app/src/main/java/com/baby/feedingtracker/data/UserRepository.kt`

```kotlin
package com.baby.feedingtracker.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date

// ──────────────────────────────────────────────
// Data classes
// ──────────────────────────────────────────────

data class UserProfile(
    val linkedTo: String? = null,
    val dataOwnerUid: String = "",
    val email: String? = null
)

sealed class SharingState {
    object NotConnected : SharingState()
    data class Connected(val partnerEmail: String?) : SharingState()
}

// ──────────────────────────────────────────────
// Repository
// ──────────────────────────────────────────────

class UserRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // ── Profile operations ──────────────────────

    private fun profileDocRef(uid: String) =
        firestore.collection("users").document(uid).collection("profile").document("linked")

    fun getProfile(uid: String): Flow<UserProfile?> {
        return profileDocRef(uid).snapshots().map { snapshot ->
            if (snapshot.exists()) {
                UserProfile(
                    linkedTo = snapshot.getString("linkedTo"),
                    dataOwnerUid = snapshot.getString("dataOwnerUid") ?: uid,
                    email = snapshot.getString("email")
                )
            } else {
                null
            }
        }
    }

    suspend fun createProfile(uid: String, email: String?) {
        val data = hashMapOf(
            "dataOwnerUid" to uid,
            "linkedTo" to null,
            "email" to email
        )
        profileDocRef(uid).set(data).await()
    }

    suspend fun getDataOwnerUid(uid: String): String {
        val snapshot = profileDocRef(uid).get().await()
        return if (snapshot.exists()) {
            snapshot.getString("dataOwnerUid") ?: uid
        } else {
            uid
        }
    }

    // ── Invite code operations ──────────────────

    private fun inviteCodeDocRef(code: String) =
        firestore.collection("invite_codes").document(code)

    suspend fun generateInviteCode(): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated")

        val code = buildString {
            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            repeat(6) {
                append(chars.random())
            }
        }

        val now = Timestamp.now()
        val expiresAt = Timestamp(Date(now.toDate().time + 10 * 60 * 1000)) // +10 minutes

        val data = hashMapOf(
            "hostUid" to uid,
            "createdAt" to now,
            "expiresAt" to expiresAt
        )
        inviteCodeDocRef(code).set(data).await()

        return code
    }

    suspend fun redeemInviteCode(code: String): Result<String> {
        val currentUid = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not authenticated"))

        return try {
            val codeSnapshot = inviteCodeDocRef(code).get().await()

            if (!codeSnapshot.exists()) {
                return Result.failure(Exception("Invalid invite code"))
            }

            val hostUid = codeSnapshot.getString("hostUid")
                ?: return Result.failure(Exception("Invalid invite code data"))

            val expiresAt = codeSnapshot.getTimestamp("expiresAt")
                ?: return Result.failure(Exception("Invalid invite code data"))

            if (expiresAt.toDate().before(Date())) {
                // Clean up expired code
                inviteCodeDocRef(code).delete().await()
                return Result.failure(Exception("Invite code has expired"))
            }

            if (hostUid == currentUid) {
                return Result.failure(Exception("Cannot use your own invite code"))
            }

            // Read host profile to get host's email
            val hostProfileSnapshot = profileDocRef(hostUid).get().await()
            val hostEmail = hostProfileSnapshot.getString("email")

            // Read guest (current user) profile to get guest's email
            val guestProfileSnapshot = profileDocRef(currentUid).get().await()
            val guestEmail = guestProfileSnapshot.getString("email")

            // Update host profile: linkedTo = guest uid
            profileDocRef(hostUid).update(
                mapOf("linkedTo" to currentUid)
            ).await()

            // Update guest profile: linkedTo = host uid, dataOwnerUid = host uid
            profileDocRef(currentUid).update(
                mapOf(
                    "linkedTo" to hostUid,
                    "dataOwnerUid" to hostUid
                )
            ).await()

            // Delete the used invite code
            inviteCodeDocRef(code).delete().await()

            Result.success(hostUid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Sharing state ───────────────────────────

    fun sharingState(uid: String): Flow<SharingState> {
        return getProfile(uid).map { profile ->
            if (profile == null || profile.linkedTo == null) {
                SharingState.NotConnected
            } else {
                // Read partner's profile to get their email
                val partnerUid = profile.linkedTo
                val partnerSnapshot = profileDocRef(partnerUid).get().await()
                val partnerEmail = partnerSnapshot.getString("email")
                SharingState.Connected(partnerEmail = partnerEmail)
            }
        }
    }
}
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/data/UserRepository.kt && git commit -m "Add UserRepository for profile and invite code management"
```

---

## Task 4: AppContainer -- wire everything, profile-based data path

- [ ] Modify `app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt` to expose UserRepository, GoogleAuthHelper, and resolve dataOwnerUid before initializing the feeding repository

**File:** `app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt`

**Replace entire file with:**

```kotlin
package com.baby.feedingtracker.di

import android.content.Context
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.FirestoreDataSource
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppContainer(context: Context) {
    val auth: FirebaseAuth = Firebase.auth
    private val firestore = Firebase.firestore

    val userRepository = UserRepository(firestore, auth)
    val googleAuthHelper = GoogleAuthHelper(auth, context)

    private val _repository = MutableStateFlow<FeedingRepository?>(null)
    val repository: StateFlow<FeedingRepository?> = _repository.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            resolveAndInitRepository(currentUser.uid)
        } else {
            auth.signInAnonymously().addOnSuccessListener { result ->
                resolveAndInitRepository(result.user!!.uid)
            }
        }
    }

    private fun resolveAndInitRepository(uid: String) {
        scope.launch {
            val dataOwnerUid = userRepository.getDataOwnerUid(uid)
            initRepository(dataOwnerUid)
        }
    }

    private fun initRepository(dataOwnerUid: String) {
        val dataSource = FirestoreDataSource(firestore, dataOwnerUid)
        _repository.value = FeedingRepository(dataSource)
    }

    /**
     * Called after a guest redeems an invite code. Re-initializes the repository
     * to point at the host's data collection.
     */
    fun reinitializeWithDataOwner(dataOwnerUid: String) {
        initRepository(dataOwnerUid)
    }
}
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt && git commit -m "Wire UserRepository and GoogleAuthHelper in AppContainer with profile-based data path"
```

---

## Task 5: MainViewModel -- sharing state + methods

- [ ] Modify `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt` to add sharing state, login state, and invite code methods

**File:** `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt`

**Replace entire file with:**

```kotlin
package com.baby.feedingtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.FeedingRepository
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.SharingState
import com.baby.feedingtracker.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val records: List<FeedingRecord> = emptyList(),
    val elapsedMinutes: Long? = null
)

class MainViewModel(
    private val repository: FeedingRepository,
    private val userRepository: UserRepository,
    private val googleAuthHelper: GoogleAuthHelper,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(60_000L)
        }
    }

    private val _refreshTrigger = MutableStateFlow(0L)

    val uiState: StateFlow<MainUiState> = combine(
        repository.allRecords,
        repository.latestRecord,
        ticker,
        _refreshTrigger
    ) { records, latest, _, _ ->
        val now = System.currentTimeMillis()
        val elapsed = latest?.let {
            (now - it.timestamp) / 60_000L
        }
        MainUiState(
            records = records,
            elapsedMinutes = elapsed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    // -- Last added record (for auto-opening bottom sheet) --

    private val _lastAddedRecord = MutableStateFlow<FeedingRecord?>(null)
    val lastAddedRecord: StateFlow<FeedingRecord?> = _lastAddedRecord.asStateFlow()

    fun clearLastAddedRecord() {
        _lastAddedRecord.value = null
    }

    // -- Debounce for add --

    private var lastRecordTime = 0L
    private val debounceInterval = 2_000L

    fun addRecord() {
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < debounceInterval) return
        lastRecordTime = now
        viewModelScope.launch {
            val record = repository.addRecord()
            _refreshTrigger.value = now
            _lastAddedRecord.value = record
        }
    }

    fun deleteRecord(record: FeedingRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun updateRecordType(recordId: String, type: String?, amountMl: Int?) {
        viewModelScope.launch {
            repository.updateRecord(recordId, type, amountMl)
        }
    }

    // ══════════════════════════════════════════════
    // Sharing Feature
    // ══════════════════════════════════════════════

    private val _isGoogleLoggedIn = MutableStateFlow(googleAuthHelper.isLoggedIn())
    val isGoogleLoggedIn: StateFlow<Boolean> = _isGoogleLoggedIn.asStateFlow()

    private val _sharingState = MutableStateFlow<SharingState>(SharingState.NotConnected)
    val sharingState: StateFlow<SharingState> = _sharingState.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _sharingError = MutableStateFlow<String?>(null)
    val sharingError: StateFlow<String?> = _sharingError.asStateFlow()

    init {
        observeSharingState()
    }

    private fun observeSharingState() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            userRepository.sharingState(uid).collect { state ->
                _sharingState.value = state
            }
        }
    }

    fun refreshLoginState() {
        _isGoogleLoggedIn.value = googleAuthHelper.isLoggedIn()
        // After login, ensure profile exists and start observing sharing state
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val email = googleAuthHelper.currentUserEmail()
            userRepository.createProfile(uid, email)
            observeSharingState()
        }
    }

    fun generateInviteCode() {
        viewModelScope.launch {
            try {
                val code = userRepository.generateInviteCode()
                _inviteCode.value = code
                _sharingError.value = null
            } catch (e: Exception) {
                _sharingError.value = e.message
            }
        }
    }

    fun redeemInviteCode(code: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = userRepository.redeemInviteCode(code.uppercase())
            result.onSuccess { hostUid ->
                _sharingError.value = null
                onSuccess(hostUid)
            }
            result.onFailure { e ->
                _sharingError.value = e.message
            }
        }
    }

    fun clearInviteCode() {
        _inviteCode.value = null
    }

    fun clearSharingError() {
        _sharingError.value = null
    }

    companion object {
        fun factory(
            repository: FeedingRepository,
            userRepository: UserRepository,
            googleAuthHelper: GoogleAuthHelper,
            auth: FirebaseAuth
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repository, userRepository, googleAuthHelper, auth) as T
                }
            }
        }
    }
}
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt && git commit -m "Add sharing state, login state, and invite code methods to MainViewModel"
```

---

## Task 6: ShareBottomSheet UI

- [ ] Create `app/src/main/java/com/baby/feedingtracker/ui/ShareBottomSheet.kt`

**File (CREATE):** `app/src/main/java/com/baby/feedingtracker/ui/ShareBottomSheet.kt`

```kotlin
package com.baby.feedingtracker.ui

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.SharingState
import com.baby.feedingtracker.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    sharingState: SharingState,
    isGoogleLoggedIn: Boolean,
    inviteCode: String?,
    sharingError: String?,
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    onGenerateCode: () -> Unit,
    onRedeemCode: (String) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            when (sharingState) {
                is SharingState.Connected -> {
                    ConnectedContent(partnerEmail = sharingState.partnerEmail)
                }
                is SharingState.NotConnected -> {
                    if (!isGoogleLoggedIn) {
                        NotLoggedInContent(
                            googleAuthHelper = googleAuthHelper,
                            googleSignInLauncher = googleSignInLauncher
                        )
                    } else if (inviteCode != null) {
                        CodeGeneratedContent(code = inviteCode)
                    } else {
                        LoggedInNotConnectedContent(
                            sharingError = sharingError,
                            onGenerateCode = onGenerateCode,
                            onRedeemCode = onRedeemCode,
                            onClearError = onClearError
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
// State A: Not logged in (anonymous)
// ──────────────────────────────────────────────

@Composable
private fun NotLoggedInContent(
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    Text(
        text = "공유하기",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "배우자와 수유 기록을 함께\n보려면 로그인이 필요합니다",
        style = MaterialTheme.typography.bodyLarge,
        color = LocalExtendedColors.current.subtleText,
        lineHeight = 24.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = {
            val intent = googleAuthHelper.getSignInIntent()
            googleSignInLauncher.launch(intent)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = MaterialTheme.colorScheme.onBackground
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        Text(
            text = "G  Google로 로그인",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ──────────────────────────────────────────────
// State B: Logged in, not connected
// ──────────────────────────────────────────────

@Composable
private fun LoggedInNotConnectedContent(
    sharingError: String?,
    onGenerateCode: () -> Unit,
    onRedeemCode: (String) -> Unit,
    onClearError: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }

    Text(
        text = "공유하기",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Generate invite code button
    Button(
        onClick = onGenerateCode,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = "초대 코드 만들기",
            style = MaterialTheme.typography.labelLarge
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Divider with "또는"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LocalExtendedColors.current.divider)
        )
        Text(
            text = "  또는  ",
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.subtleText
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LocalExtendedColors.current.divider)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Code input section
    Text(
        text = "초대 코드 입력",
        style = MaterialTheme.typography.labelMedium,
        color = LocalExtendedColors.current.subtleText
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 6-character code input field
    BasicTextField(
        value = codeInput,
        onValueChange = { newValue ->
            if (newValue.length <= 6) {
                codeInput = newValue.uppercase()
                onClearError()
            }
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters
        ),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (sharingError != null) {
                            LocalExtendedColors.current.deleteColor
                        } else {
                            LocalExtendedColors.current.divider
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                // Show individual character boxes
                repeat(6) { index ->
                    val char = codeInput.getOrNull(index)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(
                                width = 1.dp,
                                color = LocalExtendedColors.current.divider,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (index < 5) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            // Hidden actual text field for keyboard input
            Box(modifier = Modifier.size(0.dp)) {
                innerTextField()
            }
        }
    )

    // Error message
    if (sharingError != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sharingError,
            style = MaterialTheme.typography.bodySmall,
            color = LocalExtendedColors.current.deleteColor
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Connect button
    Button(
        onClick = { onRedeemCode(codeInput) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = codeInput.length == 6,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            text = "연결하기",
            style = MaterialTheme.typography.labelLarge
        )
    }
}

// ──────────────────────────────────────────────
// State C: Code generated
// ──────────────────────────────────────────────

@Composable
private fun CodeGeneratedContent(code: String) {
    val clipboardManager = LocalClipboardManager.current

    Text(
        text = "공유하기",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "초대 코드",
        style = MaterialTheme.typography.labelMedium,
        color = LocalExtendedColors.current.subtleText
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Code display
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        code.forEach { char ->
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "10분 후 만료",
        style = MaterialTheme.typography.bodySmall,
        color = LocalExtendedColors.current.subtleText,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Copy button
    OutlinedButton(
        onClick = {
            clipboardManager.setText(AnnotatedString(code))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "코드 복사하기",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ──────────────────────────────────────────────
// State D: Connected
// ──────────────────────────────────────────────

@Composable
private fun ConnectedContent(partnerEmail: String?) {
    Text(
        text = "공유 상태",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "연결됨",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    if (partnerEmail != null) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$partnerEmail 과 공유 중",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalExtendedColors.current.subtleText
        )
    }
}
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/ShareBottomSheet.kt && git commit -m "Add ShareBottomSheet UI with 4 states: not logged in, not connected, code generated, connected"
```

---

## Task 7: MainScreen -- share icon + status dot + bottom sheet integration

- [ ] Modify `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt` to add share icon with status dot and wire ShareBottomSheet

**File:** `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt`

**Edit 1:** Add missing imports at the top of the import block.

old_string:
```kotlin
import androidx.compose.foundation.clickable
```

new_string:
```kotlin
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.clickable
```

**Edit 2:** Add import for sharing types and Icon.

old_string:
```kotlin
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
```

new_string:
```kotlin
import com.baby.feedingtracker.data.FeedingRecord
import com.baby.feedingtracker.data.GoogleAuthHelper
import com.baby.feedingtracker.data.SharingState
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
```

**Edit 3:** Update MainScreen function signature to accept sharing dependencies and add bottom sheet state.

old_string:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastAddedRecord by viewModel.lastAddedRecord.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    var selectedRecord by remember { mutableStateOf<FeedingRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<FeedingRecord?>(null) }
```

new_string:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    googleAuthHelper: GoogleAuthHelper,
    googleSignInLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastAddedRecord by viewModel.lastAddedRecord.collectAsStateWithLifecycle()
    val sharingState by viewModel.sharingState.collectAsStateWithLifecycle()
    val isGoogleLoggedIn by viewModel.isGoogleLoggedIn.collectAsStateWithLifecycle()
    val inviteCode by viewModel.inviteCode.collectAsStateWithLifecycle()
    val sharingError by viewModel.sharingError.collectAsStateWithLifecycle()
    val extendedColors = LocalExtendedColors.current
    var selectedRecord by remember { mutableStateOf<FeedingRecord?>(null) }
    var isNewRecord by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<FeedingRecord?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }
```

**Edit 4:** Add ShareBottomSheet just before the main Box layout (after the record edit bottom sheet block).

old_string:
```kotlin
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientTop,
                        extendedColors.gradientBottom
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
```

new_string:
```kotlin
    // Share bottom sheet
    if (showShareSheet) {
        ShareBottomSheet(
            sharingState = sharingState,
            isGoogleLoggedIn = isGoogleLoggedIn,
            inviteCode = inviteCode,
            sharingError = sharingError,
            googleAuthHelper = googleAuthHelper,
            googleSignInLauncher = googleSignInLauncher,
            onGenerateCode = { viewModel.generateInviteCode() },
            onRedeemCode = { code ->
                viewModel.redeemInviteCode(code) { /* onSuccess handled by reinit */ }
            },
            onClearError = { viewModel.clearSharingError() },
            onDismiss = {
                showShareSheet = false
                viewModel.clearInviteCode()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientTop,
                        extendedColors.gradientBottom
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
```

**Edit 5:** Update ElapsedTimeSection call to include the share icon.

old_string:
```kotlin
            // -- 상단: 경과 시간 영역 --
            ElapsedTimeSection(
                elapsedMinutes = uiState.elapsedMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
            )
```

new_string:
```kotlin
            // -- 상단: 경과 시간 영역 + 공유 아이콘 --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
            ) {
                ElapsedTimeSection(
                    elapsedMinutes = uiState.elapsedMinutes,
                    modifier = Modifier.fillMaxWidth()
                )

                // Share icon with status dot
                Box(
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = { showShareSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "공유",
                            tint = LocalExtendedColors.current.subtleText
                        )
                    }
                    // Status dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (sharingState is SharingState.Connected) {
                                    Color(0xFF4CAF50) // green
                                } else {
                                    Color(0xFFFF6B6B) // red
                                }
                            )
                    )
                }
            }
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt && git commit -m "Add share icon with status dot and ShareBottomSheet integration to MainScreen"
```

---

## Task 8: MainActivity -- pass dependencies, handle Google Sign-In result

- [ ] Modify `app/src/main/java/com/baby/feedingtracker/MainActivity.kt` to pass new dependencies and register Google Sign-In activity result launcher

**File:** `app/src/main/java/com/baby/feedingtracker/MainActivity.kt`

**Replace entire file with:**

```kotlin
package com.baby.feedingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baby.feedingtracker.ui.MainScreen
import com.baby.feedingtracker.ui.MainViewModel
import com.baby.feedingtracker.ui.theme.BabyFeedingTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BabyFeedingApp

        setContent {
            BabyFeedingTrackerTheme {
                val repository by app.container.repository.collectAsState()
                val coroutineScope = rememberCoroutineScope()

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    coroutineScope.launch {
                        val signInResult = app.container.googleAuthHelper
                            .handleSignInResult(result.data)
                        signInResult.onSuccess { uid ->
                            // Create profile after successful login
                            val email = app.container.googleAuthHelper.currentUserEmail()
                            app.container.userRepository.createProfile(uid, email)
                        }
                    }
                }

                if (repository != null) {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModel.factory(
                            repository!!,
                            app.container.userRepository,
                            app.container.googleAuthHelper,
                            app.container.auth
                        )
                    )

                    // Refresh login state after Google Sign-In callback
                    val googleSignInLauncherWithRefresh = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult()
                    ) { result ->
                        coroutineScope.launch {
                            val signInResult = app.container.googleAuthHelper
                                .handleSignInResult(result.data)
                            signInResult.onSuccess { uid ->
                                val email = app.container.googleAuthHelper.currentUserEmail()
                                app.container.userRepository.createProfile(uid, email)
                                viewModel.refreshLoginState()
                            }
                        }
                    }

                    MainScreen(
                        viewModel = viewModel,
                        googleAuthHelper = app.container.googleAuthHelper,
                        googleSignInLauncher = googleSignInLauncherWithRefresh
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
```

**Verify:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew assembleDebug
```

**Commit:**
```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git add app/src/main/java/com/baby/feedingtracker/MainActivity.kt && git commit -m "Pass sharing dependencies to ViewModel and handle Google Sign-In result in MainActivity"
```

---

## Task 9: Build verification

- [ ] Run a full debug build to confirm all tasks compile together

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && cd /Users/seonhoappa/projects/baby-feeding-tracker && ./gradlew clean assembleDebug
```

If the build succeeds, commit any remaining changes and tag the feature:

```bash
cd /Users/seonhoappa/projects/baby-feeding-tracker && git log --oneline -8
```

---

## Summary of files changed

| File | Action | Task |
|------|--------|------|
| `app/build.gradle.kts` | Modify | 1 |
| `app/src/main/java/com/baby/feedingtracker/data/GoogleAuthHelper.kt` | Create | 2 |
| `app/src/main/java/com/baby/feedingtracker/data/UserRepository.kt` | Create | 3 |
| `app/src/main/java/com/baby/feedingtracker/di/AppContainer.kt` | Modify (full replace) | 4 |
| `app/src/main/java/com/baby/feedingtracker/ui/MainViewModel.kt` | Modify (full replace) | 5 |
| `app/src/main/java/com/baby/feedingtracker/ui/ShareBottomSheet.kt` | Create | 6 |
| `app/src/main/java/com/baby/feedingtracker/ui/MainScreen.kt` | Modify (5 edits) | 7 |
| `app/src/main/java/com/baby/feedingtracker/MainActivity.kt` | Modify (full replace) | 8 |

## Prerequisites (manual steps required before running)

1. Firebase Console: Authentication > Sign-in method > enable **Google**
2. Firebase Console: Project settings > App > add **SHA-1 fingerprint** (debug keystore)
3. Re-download `google-services.json` after SHA-1 registration and place in `app/`
4. Update Firestore security rules per spec section 7
