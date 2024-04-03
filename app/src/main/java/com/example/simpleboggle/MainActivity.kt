package com.example.simpleboggle

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.GridLayout
import android.widget.Toast
import java.io.IOException
import java.util.Locale

data class GridItem(val letter: String, var isSelected: Boolean = false, val position: Int)

class MainActivity : AppCompatActivity() {
    private var currentScore = 0
    private lateinit var validWords: Set<String>
    private lateinit var gridLayout: GridLayout
    private val usedWords = mutableSetOf<String>()


    private val currentWord = StringBuilder()

    var selectedWord = ""


    private val gridItems = mutableListOf<GridItem>()
    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        validWords = loadWords()
        setupGrid()
        setupButtons()

        gridLayout = findViewById(R.id.lettersGrid)
        setupTouchListener(gridLayout)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener(gridLayout: GridLayout) {
        gridLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val cellWidth = gridLayout.width / gridLayout.columnCount
                    val cellHeight = gridLayout.height / gridLayout.rowCount
                    val column = (event.x / cellWidth).toInt()
                    val row = (event.y / cellHeight).toInt()
                    val position = row * gridLayout.columnCount + column


                    if (position in gridItems.indices && !selectedPositions.contains(position)) {
                        val gridItem = gridItems[position]
                        selectedPositions.add(position)
                        updateGridItemSelection(gridItem, true)

                        currentWord.append(gridItem.letter)

                        findViewById<TextView>(R.id.currentWordTextView).text = currentWord.toString()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    gridItems.forEach { it.isSelected = false }
                    gridItems.forEach { updateGridItemSelection(it, false) }
                    true
                }
                else -> false
            }
        }
    }

    private fun updateGridItemSelection(gridItem: GridItem, isSelected: Boolean) {
        val textView = gridLayout.findViewWithTag<TextView>("Letter${gridItem.position}")
        textView?.let {
            if (isSelected) {
                it.setBackgroundColor(Color.GRAY)
                gridItem.isSelected = true
            } else {
                it.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun loadWords(): Set<String> {
        val words = mutableSetOf<String>()
        try {
            assets.open("Dictionary.txt").bufferedReader().useLines { lines ->
                lines.forEach { word ->
                    words.add(word.toLowerCase().trim())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return words
    }

    private fun setupGrid() {
        gridItems.clear()
        selectedPositions.clear()
        val letters = listOf("C", "A", "T", "D",
            "O", "O", "O", "O",
            "G", "N", "M", "B",
            "E", "F", "H", "I")
        letters.forEachIndexed { index, letter ->
            gridItems.add(GridItem(letter, false, index))
        }
        val gridLayout: GridLayout = findViewById(R.id.lettersGrid)
        gridLayout.removeAllViews()

        val columnCount = 4
        val rowCount = letters.size / columnCount
        gridLayout.columnCount = columnCount
        gridLayout.rowCount = rowCount

        for ((index, letter) in letters.withIndex()) {
            val textView = TextView(this).apply {
                text = letter
                tag = "Letter$index"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            }

            val layoutParams = GridLayout.LayoutParams(
                GridLayout.spec(index / columnCount, 1f),
                GridLayout.spec(index % columnCount, 1f)
            ).apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(10, 10, 10, 10)
            }

            textView.layoutParams = layoutParams

            gridLayout.addView(textView)
        }
    }


    private fun setupButtons() {
        val submitButton = findViewById<Button>(R.id.submitWordButton)
        val clearButton = findViewById<Button>(R.id.clearWordButton)
        val newGameButton = findViewById<Button>(R.id.newGameButton)

        submitButton.setOnClickListener {
            val word = currentWord.toString().toLowerCase(Locale.getDefault())
            val currentWordTextView: TextView = findViewById(R.id.currentWordTextView)

            if (word.isNotEmpty() && validWords.contains(word.toLowerCase(Locale.getDefault())) && !usedWords.contains(word)) {
                val score = calculateScore(word)
                findViewById<TextView>(R.id.scoreTextView).text = "Score: $score"
                usedWords.add(word)
                currentScore += score
                showToast("Correct +$score")
                currentWordTextView.text = ""

            } else if (usedWords.contains(word)){
                showToast("Word used.")
                currentWordTextView.text = ""
            } else {
                Log.d("WORD", word)
                currentScore -= 10
                showToast("Incorrect -10")
                findViewById<TextView>(R.id.scoreTextView).text = "Score: $currentScore"
                currentWordTextView.text = word
            }
            clearCurrentWordSelection()
            currentWord.clear()
            currentWordTextView.text = ""
        }

        clearButton.setOnClickListener {
            clearCurrentWordSelection()
        }

        newGameButton.setOnClickListener {
            resetGame()
        }
    }

    private fun calculateScore(word: String): Int {
        val vowels = setOf('A', 'E', 'I', 'O', 'U')
        val specialConsonants = setOf('S', 'Z', 'P', 'X', 'Q')
        var score = 0
        var containsSpecialConsonant = false

        for (char in word.toUpperCase(Locale.getDefault())) {
            when {
                vowels.contains(char) -> score += 5
                specialConsonants.contains(char) -> {
                    score += 1
                    containsSpecialConsonant = true
                }
                else -> score += 1
            }
        }

        if (containsSpecialConsonant) {
            score *= 2
        }

        return score
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearCurrentWordSelection() {
        currentWord.clear()
        selectedWord = ""
        selectedPositions.clear()
        findViewById<TextView>(R.id.currentWordTextView).text = ""
    }

    private fun resetGame() {
        gridItems.forEach { it.isSelected = false }
        gridItems.forEach { updateGridItemSelection(it, false) }
        usedWords.clear()
        setupGrid()
        currentScore = 0
        findViewById<TextView>(R.id.scoreTextView).text = "Score: $currentScore"
        currentWord.clear()
        findViewById<TextView>(R.id.currentWordTextView).text = ""
    }



}
