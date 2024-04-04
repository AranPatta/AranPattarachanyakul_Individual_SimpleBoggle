package com.example.simpleboggle

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

data class GridItem(val letter: String, var isSelected: Boolean = false, var isDisabled: Boolean = false, val position: Int)

class MainActivity : AppCompatActivity() {
    private var currentScore = 0
    private lateinit var validWords: Set<String>
    private lateinit var gridLayout: GridLayout
    private val usedWords = mutableSetOf<String>()
    private val currentWord = StringBuilder()
    private val currentRoundSelectedPositions = mutableSetOf<Int>()
    private val gameDisabledPositions = mutableSetOf<Int>()

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeDetector: ShakeDetector? = null

    var selectedWord = ""

    private val gridItems = mutableListOf<GridItem>()
    private val selectedPositions = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        validWords = loadWords()
        setupShakeDetector()
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


                    if (position in gridItems.indices && !selectedPositions.contains(position) && !currentRoundSelectedPositions.contains(position)) {
                        val gridItem = gridItems[position]
                        currentRoundSelectedPositions.add(position)
                        selectedPositions.add(position)
                        updateGridItemSelection(gridItem, isSelected = true, gridItem.isDisabled)

                        currentWord.append(gridItem.letter)

                        findViewById<TextView>(R.id.currentWordTextView).text = currentWord.toString()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    gridItems.forEach { it.isSelected = false }
                    gridItems.forEach { updateGridItemSelection(it, false, it.isDisabled) }
                    true
                }
                else -> false
            }
        }
    }

    private fun updateGridItemSelection(gridItem: GridItem, isSelected: Boolean, isDisabled: Boolean) {
        val textView = gridLayout.findViewWithTag<TextView>("Letter${gridItem.position}")
        textView?.let {
            if (isDisabled) {
                it.setBackgroundColor(Color.GRAY)
                gridItem.isSelected = true
            } else if (isSelected) {
                it.setBackgroundColor(Color.CYAN)
            } else if (!gameDisabledPositions.contains(gridItem.position)){
                it.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun loadWords(): Set<String> {
        val words = mutableSetOf<String>()
        try {
            assets.open("Dictionary.txt").bufferedReader().useLines { lines ->
                lines.forEach { word ->
                    if(word.length >= 4 && containsAtLeastTwoVowels(word)) {
                        words.add(word.toLowerCase().trim())
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return words
    }

    fun containsAtLeastTwoVowels(input: String): Boolean {
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U')
        var vowelCount = 0

        for (char in input) {
            if (char in vowels) {
                vowelCount++
                if (vowelCount >= 2) {
                    return true
                }
            }
        }

        return false
    }

    private fun setupGrid() {
        gridItems.clear()
        selectedPositions.clear()
        val letters = listOf("C", "A", "T", "D",
            "O", "O", "O", "O",
            "G", "N", "M", "B",
            "E", "F", "H", "I")
        letters.forEachIndexed { index, letter ->
            gridItems.add(GridItem(letter, false, false, index))
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
            wordSubmit()
        }

        clearButton.setOnClickListener {
            clearCurrentWordSelection()
        }

        newGameButton.setOnClickListener {
            resetGame()
        }
    }

    private fun wordSubmit(){
        val word = currentWord.toString().toLowerCase(Locale.getDefault())
        val currentWordTextView: TextView = findViewById(R.id.currentWordTextView)

        if (word.isNotEmpty() && validWords.contains(word.toLowerCase(Locale.getDefault())) && !usedWords.contains(word)) {
            Log.d("WORD", word)
            gameDisabledPositions.addAll(currentRoundSelectedPositions)
            Log.d("index", gameDisabledPositions.toString())
            gameDisabledPositions.forEach { position ->
                val gridItem = gridItems[position]
                updateGridItemSelection(gridItem, isSelected = false, isDisabled = true)
            }
            val score = calculateScore(word)
            findViewById<TextView>(R.id.scoreTextView).text = "Score: $score"
            usedWords.add(word)
            currentScore += score
            showToast("Correct +$score")
            currentWordTextView.text = ""
            currentWord.clear()
            currentRoundSelectedPositions.clear()

        } else if (usedWords.contains(word)){
            showToast("Word used.")
            currentWordTextView.text = ""
            currentWord.clear()
            currentRoundSelectedPositions.clear()
            gameDisabledPositions.forEach { position ->
                val gridItem = gridItems[position]
                updateGridItemSelection(gridItem, isSelected = false, gridItem.isDisabled)
            }
        } else {
            Log.d("WORD", word)
            currentScore -= 10
            showToast("Incorrect -10")
            findViewById<TextView>(R.id.scoreTextView).text = "Score: $currentScore"
            currentWordTextView.text = word
            currentRoundSelectedPositions.clear()
            gameDisabledPositions.forEach { position ->
                val gridItem = gridItems[position]
                updateGridItemSelection(gridItem, isSelected = false, gridItem.isDisabled)
            }
        }
        clearCurrentWordSelection()
        currentWord.clear()
        currentWordTextView.text = ""
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
        gridItems.forEach {
            if (!gameDisabledPositions.contains(it.position)) {
                updateGridItemSelection(it, isSelected = false, isDisabled = false)
            }
        }
        selectedPositions.clear()
        findViewById<TextView>(R.id.currentWordTextView).text = ""
    }

    private fun resetGame() {
        gridItems.forEach { it.isSelected = false }
        currentRoundSelectedPositions.clear()
        gameDisabledPositions.clear()
        gridItems.forEach { updateGridItemSelection(it, false, false) }
        usedWords.clear()
        setupGrid()
        currentScore = 0
        findViewById<TextView>(R.id.scoreTextView).text = "Score: $currentScore"
        currentWord.clear()
        findViewById<TextView>(R.id.currentWordTextView).text = ""
    }

    private fun setupShakeDetector() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeDetector = ShakeDetector {
            resetGame()
        }
        accelerometer?.also { acc ->
            sensorManager.registerListener(shakeDetector, acc, SensorManager.SENSOR_DELAY_UI)
        }
    }
    inner class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {
        private val shakeThreshold = 1.5f
        private val timeThreshold = 500
        private var lastUpdate: Long = 0
        private var last_x: Float = 0.0f
        private var last_y: Float = 0.0f
        private var last_z: Float = 0.0f

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        override fun onSensorChanged(event: SensorEvent) {
            val currentTime = System.currentTimeMillis()
            if ((currentTime - lastUpdate) > timeThreshold) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                Log.d("X", x.toString())
                Log.d("Y", y.toString())
                Log.d("Z", z.toString())

                val deltaX = kotlin.math.abs(last_x - x)
                val deltaY = kotlin.math.abs(last_y - y)
                val deltaZ = kotlin.math.abs(last_z - z)

                if ((deltaX > shakeThreshold && deltaY > shakeThreshold) ||
                    (deltaX > shakeThreshold && deltaZ > shakeThreshold) ||
                    (deltaY > shakeThreshold && deltaZ > shakeThreshold)) {
                    Log.d("Shake", "Shaken")
                    onShake()
                }

                lastUpdate = currentTime
                last_x = x
                last_y = y
                last_z = z
            }
        }
    }
}
