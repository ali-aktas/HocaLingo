package com.hocalingo.app.feature.home

import com.hocalingo.app.core.base.Result

/**
 * Home Repository Interface - v2.1
 * ✅ App launch tracking eklendi (streak için)
 */
interface HomeRepository {

    /**
     * Kullanıcı adını getir
     */
    suspend fun getUserName(): Result<String>

    /**
     * Uygulama açılışını track et (streak için)
     * Bugün ilk kez açılıyorsa DailyStatsEntity oluştur/güncelle
     */
    suspend fun trackAppLaunch(): Result<Unit>

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