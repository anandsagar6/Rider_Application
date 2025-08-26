package com.example.rider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Activity_Fragment extends Fragment {

    private ProgressBar progressBar;
    private TextView emptyText;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rideRecyclerView;

    private RideAdapter adapter;
    private List<HistoryRideModel> rideList = new ArrayList<>();
    private DatabaseReference ridesRef;

    public Activity_Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_activity_, container, false);

        progressBar = root.findViewById(R.id.progressBar);
        emptyText = root.findViewById(R.id.emptyText);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rideRecyclerView = root.findViewById(R.id.rideRecyclerView);

        adapter = new RideAdapter(getContext(), rideList);
        rideRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rideRecyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> fetchRides());

        // initial load
        fetchRides();

        return root;
    }

    private void fetchRides() {
        progressBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        rideRecyclerView.setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Not logged in
            progressBar.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            emptyText.setText("Not logged in");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        String uid = user.getUid();
        // Using Customers/{uid}/rides as per your DB structure
        ridesRef = FirebaseDatabase.getInstance().getReference()
                .child("Customers")
                .child(uid)
                .child("rides");

        // read once (change to addValueEventListener if you want live updates)
        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                rideList.clear();

                for (DataSnapshot rideSnap : snapshot.getChildren()) {
                    HistoryRideModel r = rideSnap.getValue(HistoryRideModel.class);
                    if (r != null) {
                        // If rideId isn't set in child-to-object mapping (sometimes push key),
                        // ensure rideId field is consistent (fallback to key)
                        if (r.getRideId() == null || r.getRideId().isEmpty()) {
                            r.setRideId(rideSnap.getKey());
                        }
                        rideList.add(r);
                    }
                }

                // show latest first (optional)
                Collections.reverse(rideList);

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (rideList.isEmpty()) {
                    emptyText.setText("No rides yet");
                    emptyText.setVisibility(View.VISIBLE);
                    rideRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    rideRecyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                emptyText.setText("Failed to load rides");
                emptyText.setVisibility(View.VISIBLE);
            }
        });
    }
}
