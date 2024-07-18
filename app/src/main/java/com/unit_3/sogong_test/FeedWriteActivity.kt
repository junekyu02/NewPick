package com.unit_3.sogong_test

import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.unit_3.sogong_test.databinding.ActivityFeedWriteBinding
import java.text.SimpleDateFormat
import java.util.Locale

class FeedWriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedWriteBinding
    private val TAG = "FeedWriteActivity"
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feed_write)

        binding.registerBtn.setOnClickListener {
            val title = binding.titleEditText.text.toString()
            val content = binding.contentEditText.text.toString()
            Log.d(TAG, title)
            Log.d(TAG, content)
            val userId = currentUser!!.uid
            val time = getTime()
            val feedRef = database.getReference("feeds")

            // Create a unique ID for the new post
            val newPostRef = feedRef.push()
            val postId = newPostRef.key

            // Ensure the postId is not null
            if (postId != null) {
                val feed = FeedModel(postId, userId, title, time, content,0,0)
                newPostRef.setValue(feed)
            }

            finish()
        }
    }

    fun getTime(): String {
        val currentDateTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyy.MM.dd(E)HH:mm", Locale.KOREAN).format(currentDateTime)
        return dateFormat
    }
}
