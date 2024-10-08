package fragments

import KeywordRVAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database
import com.unit_3.sogong_test.KeywordModel
import com.unit_3.sogong_test.MapViewActivity
import com.unit_3.sogong_test.R
import com.unit_3.sogong_test.databinding.FragmentMyKeywordBinding

class MyKeywordFragment : Fragment() {

    private lateinit var binding: FragmentMyKeywordBinding
    private lateinit var popupWindow: PopupWindow

    private val database = Firebase.database
    val myRef = database.getReference("keyword")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize PopupWindow
        val popupView = layoutInflater.inflate(R.layout.keyword_popup, null)
        popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Set PopupWindow background and animations
        popupWindow.setBackgroundDrawable(null)
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Apply elevation to PopupWindow
        ViewCompat.setElevation(popupView, 8f) // Adjust elevation as needed

        // Show PopupWindow when the help button is touched
        binding.helpImageView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Show PopupWindow
                    popupWindow.showAsDropDown(v, 0, 0)
                    true
                }
                else -> false
            }
        }

        // Dismiss the PopupWindow when touched outside
        binding.root.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (popupWindow.isShowing) {
                    popupWindow.dismiss()
                }
            }
            false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_keyword, container, false)

        // Bottom navigation setup
        binding.bottomNavigationLocal.setOnClickListener {
            checkUserLocation()
        }
        binding.bottomNavigationHome.setOnClickListener {
            it.findNavController().navigate(R.id.action_myKeywordFragment_to_homeFragment)
        }
        binding.bottomNavigationMyPage.setOnClickListener {
            it.findNavController().navigate(R.id.action_myKeywordFragment_to_myPageFragment)
        }
        binding.bottomNavigationFeed.setOnClickListener {
            it.findNavController().navigate(R.id.action_myKeywordFragment_to_feedFragment)
        }

        binding.addKeywordBtn.setOnClickListener {
            val dialogFragment = AddKeywordDialogFragment()
            dialogFragment.show(childFragmentManager, "AddKeywordDialogFragment")
        }

        // RecyclerView setup
        val rv: RecyclerView = binding.rv
        val items = ArrayList<KeywordModel>()
        val rvAdapter = KeywordRVAdapter(items)
        rv.adapter = rvAdapter
        rv.layoutManager = LinearLayoutManager(requireContext())

        // Fetch data from Firebase
        myRef.child(Firebase.auth.currentUser!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    items.clear()
                    for (keyword in snapshot.children) {
                        try {
                            val getKeyword = keyword.getValue(KeywordModel::class.java)!!
                            getKeyword.url = keyword.key!!
                            items.add(getKeyword)
                        } catch (e: Exception) {
                            // Handle exception if any
                        }
                    }
                    rvAdapter.notifyDataSetChanged()

                    // Check if there are any keywords
                    if (items.isEmpty()) {
                        binding.noKeywordTextview.visibility = View.VISIBLE
                        rv.visibility = View.GONE
                    } else {
                        binding.noKeywordTextview.visibility = View.GONE
                        rv.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error if any
                }
            })

        return binding.root
    }

    private fun checkUserLocation() {
        val currentUserId = Firebase.auth.currentUser?.uid
        currentUserId?.let {
            val locationRef = Firebase.database.getReference("location").child(it)
            locationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.childrenCount > 0) {
                        // Location exists, navigate to MapNewsFragment
                        view?.findNavController()
                            ?.navigate(R.id.action_myKeywordFragment_to_mapNewsFragment)
                    } else {
                        // No location, navigate to MapViewActivity
                        val intent = Intent(requireContext(), MapViewActivity::class.java)
                        startActivity(intent)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MyKeywordFragment", "Database error: ${error.message}")
                }
            })
        }
    }
}
