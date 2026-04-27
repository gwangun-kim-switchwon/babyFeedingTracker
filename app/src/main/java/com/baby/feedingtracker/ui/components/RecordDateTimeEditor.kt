package com.baby.feedingtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.baby.feedingtracker.ui.theme.LocalExtendedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 기록 편집 시트 상단의 날짜/시간 헤더.
 *
 * - "M월 d일 (E)" 형태의 날짜는 탭 시 DatePickerDialog 오픈
 * - "HH:mm" 시간은 탭 시 TimePickerDialog 오픈
 * - 미래 날짜와 1년 이전 날짜는 선택 불가 (신생아 앱 특성)
 * - 사용자 로컬 타임존(Calendar.getInstance()) 기준
 *
 * @param timestamp 현재 기록의 timestamp (Long, ms)
 * @param titleSuffix " 수유 기록" 등 시간/날짜 옆에 표시할 라벨
 * @param onTimestampChange 새 timestamp가 결정됐을 때 호출
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDateTimeEditor(
    timestamp: Long,
    titleSuffix: String,
    onTimestampChange: (Long) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.KOREA) }
    val dateFormat = remember { SimpleDateFormat("M월 d일 (E)", Locale.KOREA) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = dateFormat.format(Date(timestamp)),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { showDatePicker = true }
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { showTimePicker = true }
    ) {
        Text(
            text = timeFormat.format(Date(timestamp)),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = titleSuffix,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    if (showTimePicker) {
        val calendar = remember(timestamp) {
            Calendar.getInstance().apply { timeInMillis = timestamp }
        }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = timestamp
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onTimestampChange(newCal.timeInMillis)
                    showTimePicker = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("취소", color = LocalExtendedColors.current.subtleText)
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    if (showDatePicker) {
        // 1년 전 ~ 오늘 자정 직전(23:59:59)까지 허용
        val now = System.currentTimeMillis()
        val oneYearAgoMillis = remember(now) {
            Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val todayEndMillis = remember(now) {
            Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
        val todayYear = remember(now) {
            Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
        }
        val oneYearAgoYear = remember(now) {
            Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.YEAR, -1)
            }.get(Calendar.YEAR)
        }

        // DatePicker는 UTC 기준 millis를 다루므로, 현재 timestamp의 로컬 자정을
        // UTC 자정으로 변환해서 initialSelectedDateMillis에 전달한다.
        val initialUtcMidnight = remember(timestamp) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, cal.get(Calendar.YEAR))
                set(Calendar.MONTH, cal.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            utc.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMidnight,
            yearRange = oneYearAgoYear..todayYear,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // utcTimeMillis는 해당 날짜의 UTC 자정. 사용자 로컬 자정으로 환산해서 비교.
                    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                        timeInMillis = utcTimeMillis
                    }
                    val localCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    return localCal.timeInMillis in oneYearAgoMillis..todayEndMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year in oneYearAgoYear..todayYear
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val pickedUtc = datePickerState.selectedDateMillis
                    if (pickedUtc != null) {
                        // UTC 자정 → 로컬 year/month/day 추출 → 기존 hour/minute 보존
                        val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = pickedUtc
                        }
                        val newCal = Calendar.getInstance().apply {
                            timeInMillis = timestamp
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onTimestampChange(newCal.timeInMillis)
                    }
                    showDatePicker = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소", color = LocalExtendedColors.current.subtleText)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
