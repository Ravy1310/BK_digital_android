package com.example.login.api.models

import com.google.gson.annotations.SerializedName

// ========== DASHBOARD DATA ==========
data class DashboardData(
    @SerializedName("total_tests")
    val totalTests: Int,

    @SerializedName("completed_tests")
    val completedTests: Int,

    @SerializedName("average_score")
    val averageScore: Double,

    @SerializedName("recent_tests")
    val recentTests: List<Test>
)

// ========== TEST MODELS ==========
data class Test(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("questions_count")
    val questionsCount: Int,

    @SerializedName("is_completed")
    val isCompleted: Boolean,

    @SerializedName("score")
    val score: Double?
)

data class Answer(
    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("selected_option")
    val selectedOption: Int
)

data class TestResult(
    @SerializedName("test_id")
    val testId: Int,

    @SerializedName("score")
    val score: Double,

    @SerializedName("correct_answers")
    val correctAnswers: Int,

    @SerializedName("total_questions")
    val totalQuestions: Int,

    @SerializedName("time_spent")
    val timeSpent: Int
)