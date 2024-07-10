package fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.contentValuesOf
import androidx.fragment.app.DialogFragment
import com.unit_3.sogong_test.BuildConfig
import com.unit_3.sogong_test.R
import com.unit_3.sogong_test.WebViewActivity
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

class SummaryFragment : DialogFragment() {

    private val client = OkHttpClient()
    private var url: String? = null

    val clientId = BuildConfig.Naver_Clova_Client_ID
    val clientSecret = BuildConfig.Naver_Clova_Client_Secret

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            url = it.getString(ARG_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_summary, container, false)

        val summaryTextView = view.findViewById<TextView>(R.id.summaryTextView)
        val cancelBtn = view.findViewById<Button>(R.id.cancelBtn)
        val toWebViewBtn = view.findViewById<Button>(R.id.toWebViewBtn)

        url?.let {
            getArticleContent(it) { content ->
                Log.d("SummaryFragment", "Article content: $content")
                getSummary(content) { summary ->
                    activity?.runOnUiThread {
                        summaryTextView.text = summary
                    }
                }
            }
        }

        cancelBtn.setOnClickListener {
            dismiss()
        }
        toWebViewBtn.setOnClickListener {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra("link", url)
            startActivity(intent)
        }


        return view
    }

    private fun getArticleContent(url: String, callback: (String) -> Unit) {
        Thread {
            try {
                Log.d("SummaryFragment", "Fetching article content from URL: $url")
                val document = Jsoup.connect(url).get()

                // 기사 제목 추출
                val title = document.title()
                Log.d("SummaryFragment", "Title: $title")


                // 기사 본문을 추출하는 로직
                var content = document.select("article#dic_area").text()
                Log.d("SummaryFragment", "article#dic_area : $content")
                if(content.isBlank()){
                    content = document.select("div._article_content").text()
                    Log.d("SummaryFragment", "div._article_content : $content")
                }
                if (content.isBlank()) {
                    content = document.select("div.article-body").text()
                    Log.d("SummaryFragment", "div.article-body : $content")
                }
                if (content.isBlank()) {
                    content = document.select("span.article_p").text()
                    Log.d("SummaryFragment", "span.article_p : $content")
                }
                if (content.isBlank()) {
                    content = document.select("div._article_content").text()
                    Log.d("SummaryFragment", "div._article_content : $content")
                }
                if (content.isBlank()) {
                    content = document.select("div#newsEndContents").text()
                    Log.d("SummaryFragment", "div#newsEndContents : $content")
                }


                Log.d("SummaryFragment", "Fetched content: $content")
                callback(content)
            } catch (e: IOException) {
                Log.e("SummaryFragment", "Failed to retrieve article content", e)
                callback("Failed to retrieve article content")
            }
        }.start()
    }

    private fun getSummary(content: String, callback: (String) -> Unit) {
        val apiUrl = "https://naveropenapi.apigw.ntruss.com/text-summary/v1/summarize"

        val json = JSONObject().apply {
            put("document", JSONObject().apply {
                put("content", content)
            })
            put("option", JSONObject().apply {
                put("language", "ko")
                put("model", "news")
                put("tone", 0)
                put("summaryCount", 3)
            })
        }

        val body = RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"), json.toString())

        val request = Request.Builder()
            .url(apiUrl)
            .post(body)
            .addHeader("X-NCP-APIGW-API-KEY-ID", clientId)
            .addHeader("X-NCP-APIGW-API-KEY", clientSecret)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SummaryFragment", "Failed to summarize", e)
                callback("Failed to summarize")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val jsonResponse = JSONObject(responseBody)
                    val summary = jsonResponse.getString("summary")
                    Log.d("SummaryFragment", "Summary: $summary")
                    callback(summary)
                } else {
                    Log.e("SummaryFragment", "Error: ${response.message()}")
                    callback("Error: ${response.message()}")
                }
            }
        })
    }

    companion object {
        private const val ARG_URL = "url"

        @JvmStatic
        fun newInstance(url: String) =
            SummaryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_URL, url)
                }
            }
    }
}
