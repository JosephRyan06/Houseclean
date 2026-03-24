package com.example.houseclean.view

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.houseclean.R
import com.example.houseclean.viewmodel.ProfileViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.tabs.TabLayout

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private var isEditModePersonal = false
    private var isEditModeAccount = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val profileName = view.findViewById<TextView>(R.id.profileName)
        val profileEmail = view.findViewById<TextView>(R.id.profileEmail)
        
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutProfile)
        val personalSection = view.findViewById<LinearLayout>(R.id.layoutPersonalSection)
        val accountSection = view.findViewById<LinearLayout>(R.id.layoutAccountSection)

        val inputLastName = view.findViewById<TextInputEditText>(R.id.inputLastName)
        val inputFirstName = view.findViewById<TextInputEditText>(R.id.inputFirstName)
        val inputBarangay = view.findViewById<TextInputEditText>(R.id.inputBarangay)
        val inputHouseStreet = view.findViewById<TextInputEditText>(R.id.inputHouseStreet)
        val inputLandmark = view.findViewById<TextInputEditText>(R.id.inputLandmark)
        
        val inputEmailAccount = view.findViewById<TextInputEditText>(R.id.inputEmailAccount)
        val inputPhoneAccount = view.findViewById<TextInputEditText>(R.id.inputPhoneAccount)
        
        val buttonEditProfile = view.findViewById<Button>(R.id.buttonEditProfile)
        val buttonEditAccount = view.findViewById<Button>(R.id.buttonEditAccount)
        val buttonLogout = view.findViewById<Button>(R.id.buttonLogout)

        val personalFields = listOf(inputLastName, inputFirstName, inputBarangay, inputHouseStreet, inputLandmark)
        val accountFields = listOf(inputEmailAccount, inputPhoneAccount)

        // Tab selection logic
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        personalSection.visibility = View.VISIBLE
                        accountSection.visibility = View.GONE
                    }
                    1 -> {
                        personalSection.visibility = View.GONE
                        accountSection.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        observeViewModel(profileName, profileEmail, inputFirstName, inputLastName, inputBarangay, inputHouseStreet, inputLandmark, inputEmailAccount, inputPhoneAccount, personalFields, accountFields, buttonEditProfile, buttonEditAccount)

        viewModel.fetchUserData()

        buttonEditProfile.setOnClickListener {
            if (!isEditModePersonal) {
                isEditModePersonal = true
                buttonEditProfile.text = "Done"
                personalFields.forEach { it.isEnabled = true }
            } else {
                val firstName = inputFirstName.text.toString().trim()
                val lastName = inputLastName.text.toString().trim()
                val barangay = inputBarangay.text.toString().trim()
                val street = inputHouseStreet.text.toString().trim()
                val landmark = inputLandmark.text.toString().trim()

                if (firstName.isEmpty() || lastName.isEmpty()) {
                    Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateProfile(firstName, lastName, barangay, street, landmark)
                }
            }
        }

        buttonEditAccount.setOnClickListener {
            if (!isEditModeAccount) {
                isEditModeAccount = true
                buttonEditAccount.text = "Done"
                accountFields.forEach { it.isEnabled = true }
            } else {
                val email = inputEmailAccount.text.toString().trim()
                val phone = inputPhoneAccount.text.toString().trim()

                if (email.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateAccountInfo(email, phone)
                }
            }
        }

        buttonLogout.setOnClickListener {
            viewModel.logout()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun observeViewModel(
        profileName: TextView,
        profileEmail: TextView,
        inputFirstName: TextInputEditText,
        inputLastName: TextInputEditText,
        inputBarangay: TextInputEditText,
        inputHouseStreet: TextInputEditText,
        inputLandmark: TextInputEditText,
        inputEmailAccount: TextInputEditText,
        inputPhoneAccount: TextInputEditText,
        personalFields: List<TextInputEditText>,
        accountFields: List<TextInputEditText>,
        buttonEditProfile: Button,
        buttonEditAccount: Button
    ) {
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                if (!isEditModePersonal && !isEditModeAccount) {
                    profileName.text = it.getDisplayName()
                    profileEmail.text = it.email
                    inputFirstName.setText(it.firstName)
                    inputLastName.setText(it.lastName)
                    inputBarangay.setText(it.barangay)
                    inputHouseStreet.setText(it.street)
                    inputLandmark.setText(it.landmark)
                    inputEmailAccount.setText(it.email)
                    inputPhoneAccount.setText(it.phone)
                }
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                
                if (isEditModePersonal) {
                    isEditModePersonal = false
                    buttonEditProfile.text = "Edit"
                    personalFields.forEach { it.isEnabled = false }
                }
                
                if (isEditModeAccount) {
                    isEditModeAccount = false
                    buttonEditAccount.text = "Edit"
                    accountFields.forEach { it.isEnabled = false }
                }
            }.onFailure {
                Toast.makeText(context, "Update Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}