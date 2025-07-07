package com.example.ca

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.all
import kotlin.collections.flatMap

class PlayFragment : Fragment() {
    private lateinit var username: String
    private lateinit var selectedBitmaps: List<Bitmap>
    private lateinit var imageViews: List<ImageView>
    private lateinit var timerTextView: TextView
    private val flippedCards = mutableListOf<ImageView>()
    private var isChecking = false
    private var seconds = 0
    private var isGameRunning = false
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var adView: AdView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_play, container, false)
        val cacheDir = requireContext().cacheDir
        setupAdView(view)
        username = arguments?.getString("username") ?: "Unknown"
        selectedBitmaps = (0 until 6).mapNotNull { index ->
            val file = File(cacheDir, "selected_image_$index.png")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        }
        Log.d("PlayFragment", "username = $username")

        val imageViewIds = listOf(
            R.id.image1, R.id.image2, R.id.image3,
            R.id.image4, R.id.image5, R.id.image6,
            R.id.image7, R.id.image8, R.id.image9,
            R.id.image10, R.id.image11, R.id.image12
        )
        imageViews = imageViewIds.map { view.findViewById(it) }
        timerTextView = view.findViewById(R.id.timerTextView)
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                seconds++
                updateTimerText()
                handler.postDelayed(this, 1000)
            }
        }
        resetGame()
        imageViews.forEach { iv ->
            iv.setOnClickListener { handleCardClick(iv) }
        }
        return view
    }

    private fun setupAdView(view: View) {
        MobileAds.initialize(requireContext()) {}

        adView = view.findViewById(R.id.adView)

        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        val adRefreshHandler = Handler(Looper.getMainLooper())
        adRefreshHandler.postDelayed(object : Runnable {
            override fun run() {
                adView.loadAd(AdRequest.Builder().build())
                adRefreshHandler.postDelayed(this, 10000)
            }
        }, 10000)
    }

    private fun resetGame() {
        seconds = 0
        isGameRunning = false
        handler.removeCallbacks(runnable)
        updateTimerText()

        val pairedBitmaps = selectedBitmaps.flatMap { listOf(it, it) }.shuffled()

        for (i in imageViews.indices) {
            imageViews[i].setImageResource(R.drawable.back)
            imageViews[i].tag = pairedBitmaps[i]
            imageViews[i].isClickable = true
            imageViews[i].alpha = 1f
        }

        flippedCards.clear()
        isChecking = false
    }

    private fun startTimer() {
        if (!isGameRunning) {
            isGameRunning = true
            handler.postDelayed(runnable, 1000)
        }
    }

    private fun stopTimer() {
        isGameRunning = false
        handler.removeCallbacks(runnable)
    }
    private fun updateTimerText() {
        val minutes = seconds / 60
        val secs = seconds % 60
        timerTextView.text = String.format("%02d:%02d", minutes, secs)
    }
    private fun handleCardClick(card: ImageView) {
        if (!isGameRunning) startTimer()
        if (isChecking || flippedCards.contains(card) || !card.isClickable) return

        val bitmap = card.tag as Bitmap
        card.setImageBitmap(bitmap)
        flippedCards.add(card)

        if (flippedCards.size == 2) {
            isChecking = true
            checkForMatch()
        }
    }

    private fun checkForMatch() {
        val card1 = flippedCards[0]
        val card2 = flippedCards[1]

        if (card1 == card2) {
            card1.setImageResource(R.drawable.back)
            flippedCards.clear()
            isChecking = false
            return
        }

        if (card1.tag == card2.tag) {
            card1.alpha = 0.5f
            card2.alpha = 0.5f
            card1.isClickable = false
            card2.isClickable = false
            flippedCards.clear()
            isChecking = false
            checkGameCompletion()
        } else {
            card1.postDelayed({
                card1.setImageResource(R.drawable.back)
                card2.setImageResource(R.drawable.back)
                flippedCards.clear()
                isChecking = false
            }, 1000)
        }
    }

    private fun checkGameCompletion() {
        if (imageViews.all { !it.isClickable }) {
            stopTimer()
            val timeInSeconds = seconds.toFloat().toString()
            Toast.makeText(requireContext(), "游戏完成！用时: ${timerTextView.text}", Toast.LENGTH_LONG).show()
            // 调用 API 上传成绩
            lifecycleScope.launch {
                try {
                    val bundle = Bundle().apply {
                        putString("username", username)
                    }
                    findNavController().navigate(R.id.action_play_to_leaderboard, bundle)
                    ApiService.submitScore(username, timeInSeconds)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "上传成绩失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
    }
}