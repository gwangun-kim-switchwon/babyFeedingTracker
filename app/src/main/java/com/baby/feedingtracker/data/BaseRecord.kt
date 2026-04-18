package com.baby.feedingtracker.data

interface BaseRecord {
    val id: String
    val timestamp: Long
    val note: String?
    val recordedBy: String?  // 기록한 사용자의 uid
}
