package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.example.houseclean.R
import com.example.houseclean.model.ServiceRepository
import com.example.houseclean.model.ServiceRequest
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ActivityFragment : Fragment() {

    private val repository = ServiceRepository()
    private lateinit var tabLayout: TabLayout
    private lateinit var tvSection1Title: TextView
    private lateinit var tvSection2Title: TextView
    private lateinit var tvSection3Title: TextView
    private lateinit var layoutSection1: LinearLayout
    private lateinit var layoutSection2: LinearLayout
    private lateinit var layoutSection3: LinearLayout
    private lateinit var tvNoActivity: TextView

    private var currentMode = "REQUESTS" // Default mode

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = view.findViewById(R.id.tabLayout)
        tvSection1Title = view.findViewById(R.id.tvSection1Title)
        tvSection2Title = view.findViewById(R.id.tvSection2Title)
        tvSection3Title = view.findViewById(R.id.tvSection3Title)
        layoutSection1 = view.findViewById(R.id.layoutSection1)
        layoutSection2 = view.findViewById(R.id.layoutSection2)
        layoutSection3 = view.findViewById(R.id.layoutSection3)
        tvNoActivity = view.findViewById(R.id.tvNoActivity)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentMode = tab?.text.toString()
                updateUIForMode()
                fetchActivityData()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        updateUIForMode()
        fetchActivityData()
    }

    private fun updateUIForMode() {
        if (currentMode == "REQUESTS") {
            tvSection1Title.text = "PENDING"
            tvSection1Title.setTextColor(resources.getColor(R.color.black, null))
            tvSection2Title.text = "CONFIRMED"
            tvSection2Title.setTextColor(resources.getColor(R.color.black, null))
            tvSection3Title.text = "DECLINED"
            tvSection3Title.setTextColor(resources.getColor(R.color.black, null))
            tvNoActivity.text = "No requests found"
        } else {
            tvSection1Title.text = "PENDING PAYMENT"
            tvSection1Title.setTextColor(resources.getColor(R.color.black, null))
            tvSection2Title.text = "PAID"
            tvSection2Title.setTextColor(resources.getColor(R.color.black, null))
            tvSection3Title.text = "OVERDUE"
            tvSection3Title.setTextColor(resources.getColor(R.color.black, null))
            tvNoActivity.text = "No payments found"
        }
    }

    private fun fetchActivityData() {
        val currentUid = repository.getCurrentUserUid() ?: return

        repository.getHouseholderRequests(currentUid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                
                layoutSection1.removeAllViews()
                layoutSection2.removeAllViews()
                layoutSection3.removeAllViews()
                
                var totalCount = 0

                for (child in snapshot.children) {
                    val request = child.getValue(ServiceRequest::class.java) ?: continue
                    val requestId = child.key ?: ""
                    val finalRequest = request.copy(requestId = requestId)
                    
                    if (currentMode == "REQUESTS") {
                        when (finalRequest.status) {
                            "PENDING" -> {
                                addActivityCard(finalRequest, layoutSection1)
                                totalCount++
                            }
                            "CONFIRMED", "ACCEPTED" -> {
                                addActivityCard(finalRequest, layoutSection2)
                                totalCount++
                            }
                            "DECLINED" -> {
                                addActivityCard(finalRequest, layoutSection3)
                                totalCount++
                            }
                        }
                    } else {
                        when (finalRequest.paymentStatus) {
                            "PENDING" -> {
                                addPaymentCard(finalRequest, layoutSection1)
                                totalCount++
                            }
                            "PAID" -> {
                                addPaymentCard(finalRequest, layoutSection2)
                                totalCount++
                            }
                            "OVERDUE" -> {
                                addPaymentCard(finalRequest, layoutSection3)
                                totalCount++
                            }
                        }
                    }
                }
                
                tvNoActivity.visibility = if (totalCount == 0) View.VISIBLE else View.GONE
                
                // Also hide section titles if there's no activity at all in this mode? 
                // The user specifically asked for "No requests found" text.
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ActivityFragment", "Error: ${error.message}")
            }
        })
    }

    private fun addActivityCard(request: ServiceRequest, parent: LinearLayout) {
        val view = layoutInflater.inflate(R.layout.item_activity_card, parent, false)
        view.findViewById<TextView>(R.id.tvDateTime).text = request.startDate
        view.findViewById<TextView>(R.id.tvStatus).text = request.status
        view.findViewById<TextView>(R.id.tvDate).text = request.startDate
        view.findViewById<TextView>(R.id.tvService).text = request.serviceType
        view.findViewById<TextView>(R.id.tvHousekeeper).text = request.housekeeperName
        view.findViewById<TextView>(R.id.tvPayment).text = "PHP ${request.totalPrice.toInt()}"
        
        view.findViewById<View>(R.id.btnViewDetails).setOnClickListener {
            openDetails(request.requestId)
        }
        
        parent.addView(view)
    }

    private fun addPaymentCard(request: ServiceRequest, parent: LinearLayout) {
        val view = layoutInflater.inflate(R.layout.item_payment_card_vertical, parent, false)
        
        view.findViewById<TextView>(R.id.tvAmount).text = "PHP ${request.totalPrice.toInt()}"
        view.findViewById<TextView>(R.id.tvStatus).text = request.paymentStatus
        view.findViewById<TextView>(R.id.tvTime).text = request.startDate
        view.findViewById<TextView>(R.id.tvServicePay).text = request.serviceType
        view.findViewById<TextView>(R.id.tvHousekeeperPay).text = request.housekeeperName
        
        view.findViewById<View>(R.id.btnViewDetailsPay).setOnClickListener {
            openDetails(request.requestId)
        }
        
        parent.addView(view)
    }

    private fun openDetails(requestId: String) {
        val intent = Intent(activity, ServiceDetailsActivity::class.java)
        intent.putExtra("requestId", requestId)
        startActivity(intent)
    }
}
