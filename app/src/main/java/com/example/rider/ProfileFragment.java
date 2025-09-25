package com.example.rider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    Button backToDash, account, personal_info, review, notification, logoutBtn;
    Button privacyPolicyBtn, termsConditionsBtn,about_us;  // New buttons
    TextView profileName, profileEmail;
    FirebaseAuth mAuth;
    DatabaseReference userRef;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Firebase Database reference
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // User not logged in → send to login screen
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        } else {
            // Use "Customers" node
            userRef = FirebaseDatabase.getInstance()
                    .getReference("Customers")
                    .child(user.getUid());
        }

        // Buttons
        backToDash = view.findViewById(R.id.profiletodashboard);
        account = view.findViewById(R.id.account_setting);
        review = view.findViewById(R.id.review);
        notification = view.findViewById(R.id.notification);
        about_us = view.findViewById(R.id.About_us);
        personal_info = view.findViewById(R.id.profile_info);
        logoutBtn = view.findViewById(R.id.logoutBtn);
        privacyPolicyBtn = view.findViewById(R.id.privacy_policy);          // New
        termsConditionsBtn = view.findViewById(R.id.terms_conditions);     // New

        // TextViews
        profileName = view.findViewById(R.id.profileName);
        profileEmail = view.findViewById(R.id.profileEmail);

        // --- Fetch user details ---
        if (user != null) {
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        // Get name and email
                        String name = snapshot.child("name").getValue(String.class);
                        String emailFromDB = snapshot.child("email").getValue(String.class);

                        if (name != null && !name.isEmpty()) {
                            profileName.setText(name);
                        } else {
                            profileName.setText("No Name Found");
                        }

                        if (emailFromDB != null && !emailFromDB.isEmpty()) {
                            profileEmail.setText(emailFromDB);
                        } else if (user.getEmail() != null) {
                            profileEmail.setText(user.getEmail());
                        } else {
                            profileEmail.setText("No Email Found");
                        }

                    } else {
                        Toast.makeText(getContext(), "User data not found in DB", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // --- Logout Button ---
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        // Other button clicks
        notification.setOnClickListener(v ->
                Toast.makeText(getContext(), "Notification", Toast.LENGTH_SHORT).show());

        review.setOnClickListener(v ->
                Toast.makeText(getContext(), "My Review", Toast.LENGTH_SHORT).show());


        personal_info.setOnClickListener(v ->
                Toast.makeText(getContext(), "Personal info", Toast.LENGTH_SHORT).show());

        account.setOnClickListener(v ->
                Toast.makeText(getContext(), "Account Setting", Toast.LENGTH_SHORT).show());

        backToDash.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), DashBoard.class);
            startActivity(intent);
        });

        about_us.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutUsActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        });

        privacyPolicyBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PrivacyPolicyActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        });

        termsConditionsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), TermsConditionsActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out);
        });


        return view;
    }
}
