package com.example.ca

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class FetchFragment : Fragment() {

    private lateinit var urlInput: EditText
    private lateinit var fetchBtn: Button
    private lateinit var startGameBtn: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var photoGrid: GridLayout
    private lateinit var selectText: TextView
    private lateinit var username: String

    private val IMGURL_REG = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>"
    private val maxImages = 20
    private var fetchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_fetch, container, false)

        urlInput = view.findViewById(R.id.urlInput)
        fetchBtn = view.findViewById(R.id.fetchBtn)
        startGameBtn = view.findViewById(R.id.startGameBtn)
        progressBar = view.findViewById(R.id.progressBar)
        progressText = view.findViewById(R.id.progressText)
        photoGrid = view.findViewById(R.id.photoGrid)
        selectText = view.findViewById(R.id.selectText)

        username = arguments?.getString("username") ?: "Unknown"

        fetchBtn.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isEmpty()) {
                showToast("请输入网址")
            } else {
                fetchJob?.cancel() // 取消旧任务
                fetchJob = fetchImagesFromUrl(if (url.startsWith("http")) url else "https://$url")
            }
        }

        startGameBtn.setOnClickListener {
            val selected = photoGrid.children
                .filter { it.tag is Bitmap && it.alpha == 0.5f }
                .map { it.tag as Bitmap }
                .take(6)
                .toList()

            if (selected.size < 6) {
                showToast("请选择 6 张图片")
                return@setOnClickListener
            }

            saveBitmapsToCache(selected)

            val bundle = Bundle().apply {
                putString("username", username)
            }
            findNavController().navigate(R.id.action_fetch_to_play, bundle)
            showToast("已选择 ${selected.size} 张图片，准备跳转")
        }

        return view
    }

    private fun fetchImagesFromUrl(url: String): Job {
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        photoGrid.visibility = View.GONE
        startGameBtn.visibility = View.INVISIBLE

        return lifecycleScope.launch {
            val bitmaps = withContext(Dispatchers.IO) {
                val result = mutableListOf<Bitmap>()
                try {
                    val html = URL(url).readText()
                    val pattern = Pattern.compile(IMGURL_REG)
                    val matcher = pattern.matcher(html)

                    val imageUrls = mutableListOf<String>()
                    while (matcher.find()) {
                        val src = matcher.group(1)
                        val absoluteUrl = URL(URL(url), src).toString()
                        imageUrls.add(absoluteUrl)
                        if (imageUrls.size >= maxImages) break
                    }

                    for ((index, imgUrl) in imageUrls.withIndex()) {
                        ensureActive() // 支持取消
                        try {
                            val bitmap = downloadBitmap(imgUrl)
                            if (bitmap != null) {
                                result.add(bitmap)
                                withContext(Dispatchers.Main) {
                                    progressText.text = "下载中：${index + 1}/${imageUrls.size}"
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FetchFragment", "下载失败: $imgUrl", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FetchFragment", "解析失败", e)
                }
                result
            }

            progressBar.visibility = View.INVISIBLE
            progressText.visibility = View.INVISIBLE
            photoGrid.visibility = View.VISIBLE

            if (bitmaps.isEmpty()) {
                showToast("未找到图片")
                return@launch
            }

            for (i in 0 until photoGrid.childCount) {
                val imageView = photoGrid.getChildAt(i) as ImageView
                if (i < bitmaps.size) {
                    imageView.setImageBitmap(bitmaps[i])
                    imageView.visibility = View.VISIBLE
                    imageView.tag = bitmaps[i]
                    imageView.alpha = 1f
                    imageView.setOnClickListener {
                        it.alpha = if (it.alpha == 1f) 0.5f else 1f
                    }
                } else {
                    imageView.setImageDrawable(null)
                    imageView.visibility = View.INVISIBLE
                    imageView.tag = null
                }
            }

            startGameBtn.visibility = View.VISIBLE
        }
    }

    private fun downloadBitmap(url: String): Bitmap? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        connection.doInput = true
        connection.connect()
        val input: InputStream = connection.inputStream
        return BitmapFactory.decodeStream(input)
    }

    private fun saveBitmapsToCache(bitmaps: List<Bitmap>){
        val cacheDir = requireContext().cacheDir


        cacheDir.listFiles { _, name -> name.startsWith("selected_image_") }?.forEach { it.delete() }

        bitmaps.forEachIndexed { index, bitmap ->
            val file = File(cacheDir, "selected_image_$index.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
