package com.example.ca

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.ca.databinding.FragmentLeaderboardBinding
import kotlinx.coroutines.launch

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var username: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
        arguments?.let {
            username = it.getString("username") ?: "Unknown"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            try {
                val scores = ApiService.getLeaderboard()
                Log.d("Leaderboard", "Received scores: $scores")
                binding.leaderboardContainer.removeAllViews()
                scores.forEachIndexed { index, item ->
                    val row = TextView(requireContext()).apply {
                        text = "${index + 1}. ${item.username} - ${item.usertime}s"
                        textSize = 18f
                        setPadding(8, 8, 8, 8)
                    }
                    binding.leaderboardContainer.addView(row)
                }
            } catch (e: Exception) {
                Log.e("Leaderboard", "Error: ${e.message}")
            }
        }
        binding.apply {
            closeButton.setOnClickListener {
                root.findNavController()
                    .navigate(R.id.action_leaderboard_to_fetch)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}