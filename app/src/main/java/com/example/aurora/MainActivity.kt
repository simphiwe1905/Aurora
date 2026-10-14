package com.example.aurora

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.aurora.network.Language
import com.example.aurora.network.RetrofitClient
import com.example.aurora.network.SubmissionRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var codeEditor: EditText
    private lateinit var stdinEditor: EditText
    private lateinit var runButton: Button
    private lateinit var statusText: TextView
    private lateinit var outputText: TextView

    private var languages = emptyList<Language>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        languageSpinner = findViewById(R.id.languageSpinner)
        codeEditor = findViewById(R.id.codeEditor)
        stdinEditor = findViewById(R.id.stdinEditor)
        runButton = findViewById(R.id.runButton)
        statusText = findViewById(R.id.statusText)
        outputText = findViewById(R.id.outputText)

        val clearOutputButton = findViewById<Button>(R.id.clearOutputButton)
        val outputText = findViewById<TextView>(R.id.outputText)
        val statusText = findViewById<TextView>(R.id.statusText)

        clearOutputButton.setOnClickListener {
            outputText.text = ""
            statusText.text = "Output cleared"
        }


        val resetCodeButton = findViewById<Button>(R.id.resetCodeButton)

        resetCodeButton.setOnClickListener {
            codeEditor.text.clear()
            statusText.text = "Code editor reset"
        }


        val lineNumbers = findViewById<TextView>(R.id.lineNumbers)
        val codeEditor = findViewById<EditText>(R.id.codeEditor)

        fun updateLineNumbers() {
            val lineCount = codeEditor.lineCount.coerceAtLeast(1)
            lineNumbers.text = (1..lineCount).joinToString("\n")
        }

        updateLineNumbers()

        codeEditor.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                updateLineNumbers()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })


        runButton.setOnClickListener {
            runCode()
        }

        loadLanguages()
    }

    private fun loadLanguages() {
        lifecycleScope.launch {
            try {
                statusText.text = "Loading programming languages..."

                languages = RetrofitClient.api.getLanguages()
                    .sortedBy { it.name.lowercase() }

                val adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    languages.map { it.name }
                )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                languageSpinner.adapter = adapter

                val javaIndex = languages.indexOfFirst {
                    it.name.contains("Java", ignoreCase = true)
                }

                if (javaIndex >= 0) {
                    languageSpinner.setSelection(javaIndex)
                }

                statusText.text =
                    "Ready. Select a language and write your code."

            } catch (e: Exception) {
                statusText.text =
                    "Could not load languages: ${e.message}"
            }
        }
    }

    private fun runCode() {
        val sourceCode = codeEditor.text.toString()

        if (sourceCode.isBlank()) {
            statusText.text = "Please enter some code first."
            return
        }

        if (languages.isEmpty()) {
            statusText.text = "Languages are still loading."
            return
        }

        val selectedLanguage = languages.getOrNull(
            languageSpinner.selectedItemPosition
        )

        if (selectedLanguage == null) {
            statusText.text = "Please select a programming language."
            return
        }

        lifecycleScope.launch {
            runButton.isEnabled = false
            outputText.text = ""
            statusText.text = "Submitting your code..."

            try {
                val response = RetrofitClient.api.createSubmission(
                    request = SubmissionRequest(
                        sourceCode = sourceCode,
                        languageId = selectedLanguage.id,
                        stdin = stdinEditor.text.toString()
                    )
                )

                if (!response.isSuccessful) {
                    statusText.text =
                        "Submission failed: HTTP ${response.code()}"
                    outputText.text =
                        response.errorBody()?.string().orEmpty()
                    return@launch
                }

                val token = response.body()?.token

                if (token.isNullOrBlank()) {
                    statusText.text =
                        "The compiler did not return a submission token."
                    return@launch
                }

                var attempts = 0

                while (attempts < 30) {
                    delay(1000)
                    attempts++

                    val result =
                        RetrofitClient.api.getSubmission(token)

                    val executionStatus =
                        result.status?.description ?: "Unknown"

                    statusText.text = "Status: $executionStatus"

                    when (result.status?.id) {
                        1, 2 -> continue

                        else -> {
                            outputText.text = buildString {
                                appendLine("Status: $executionStatus")

                                result.stdout?.let {
                                    appendLine("\nOutput:\n$it")
                                }

                                result.stderr?.let {
                                    appendLine("\nRuntime error:\n$it")
                                }

                                result.compileOutput?.let {
                                    appendLine("\nCompiler output:\n$it")
                                }

                                result.message?.let {
                                    appendLine("\nMessage:\n$it")
                                }

                                if (
                                    result.stdout == null &&
                                    result.stderr == null &&
                                    result.compileOutput == null &&
                                    result.message == null
                                ) {
                                    appendLine("No output was returned.")
                                }
                            }

                            statusText.text =
                                "Execution finished: $executionStatus"
                            return@launch
                        }
                    }
                }

                statusText.text =
                    "Execution timed out. Please try again."

            } catch (e: Exception) {
                statusText.text =
                    "Compiler error: ${e.message}"
            } finally {
                runButton.isEnabled = true
            }
        }
    }
}