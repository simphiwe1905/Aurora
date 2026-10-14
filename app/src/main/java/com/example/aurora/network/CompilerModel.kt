package com.example.aurora.network

import com.google.gson.annotations.SerializedName

data class Language(
    val id: Int,
    val name: String
) {
    override fun toString(): String = name
}

data class SubmissionRequest(
    @SerializedName("source_code")
    val sourceCode: String,

    @SerializedName("language_id")
    val languageId: Int,

    val stdin: String = ""
)

data class SubmissionCreated(
    val token: String?
)

data class SubmissionStatus(
    val id: Int,
    val description: String
)

data class SubmissionResult(
    val token: String? = null,
    val stdout: String? = null,
    val stderr: String? = null,

    @SerializedName("compile_output")
    val compileOutput: String? = null,

    val message: String? = null,
    val status: SubmissionStatus? = null,
    val time: String? = null,
    val memory: Int? = null
)