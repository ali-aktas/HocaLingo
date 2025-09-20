package com.hocalingo.app.feature.home.domain

import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.feature.home.presentation.DailyGoalProgress
import com.hocalingo.app.feature.home.presentation.MonthlyStats

/**
 * Home Repository Interface - v2.0
 * Real data tracking için güncellenmiş interface
 */
interface HomeRepository {

    /**
     * Kullanıcı adını getir
     */
    suspend fun getUserName(): Result<String>

    /**
     * Streak günlerini hesapla
     * Uygulamaya sürekli giriş yapılan gün sayısı
     */
    suspend fun getStreakDays(): Result<Int>

    /**
     * Günlük hedef ilerlemesini getir
     * Aktif deck kartları vs completed kartlar
     */
    suspend fun getDailyGoalProgress(): Result<DailyGoalProgress>

    /**
     * Aylık istatistikleri getir
     * Çalışma süresi, aktif günler, disiplin puanı, chart data
     */
    suspend fun getMonthlyStats(): Result<MonthlyStats>
}