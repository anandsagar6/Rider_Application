package com.example.rider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Service_Fragment extends Fragment {

    private TextView address, dropAddress;
    private LinearLayout searchBar;

    public Service_Fragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_service, container, false);

        // Find views
        address = view.findViewById(R.id.address);
        dropAddress = view.findViewById(R.id.drop_address);
        searchBar = view.findViewById(R.id.search_bar);

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottom_navigation);

        // On click → move to EnterAddress_Fragment via bottom nav
        View.OnClickListener goToAddress = v -> {
            // TODO: Navigation code
        };

        address.setOnClickListener(goToAddress);
        searchBar.setOnClickListener(goToAddress);

        // ✅ DropAddress click (moved inside onCreateView)
        dropAddress.setOnClickListener(v -> {
            String dropText = dropAddress.getText().toString();

            Bundle bundle = new Bundle();
            bundle.putString("lastDrop", dropText);

            EnterAddress_Fragment fragment = new EnterAddress_Fragment();
            fragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment) // your FrameLayout id in activity
                    .addToBackStack(null)
                    .commit();
        });

        // 🔹 Fetch and display last ride
        fetchPreviousRide();

        return view;
    }

    private void fetchPreviousRide() {
        String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ridesRef = FirebaseDatabase.getInstance()
                .getReference("Customers")
                .child(customerId)
                .child("rides");

        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String lastDrop = "";

                    // Get the latest ride (last child)
                    for (DataSnapshot rideSnap : snapshot.getChildren()) {
                        String drop = rideSnap.child("Drop").getValue(String.class);
                        if (drop != null) {
                            lastDrop = drop; // overwrite so last one remains
                        }
                    }

                    if (!lastDrop.isEmpty()) {
                        dropAddress.setText(lastDrop);   // ✅ Only drop location
                    } else {
                        dropAddress.setText("No previous rides");
                    }

                } else {
                    dropAddress.setText("No previous rides found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dropAddress.setText("Error: " + error.getMessage());
            }
        });
    }
}
