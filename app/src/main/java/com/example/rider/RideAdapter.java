package com.example.rider;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RideAdapter extends RecyclerView.Adapter<RideAdapter.RideViewHolder> {

    private final Context context;
    private final List<HistoryRideModel> rides;

    public RideAdapter(Context context, List<HistoryRideModel> rides) {
        this.context = context;
        this.rides = rides;
    }

    @NonNull
    @Override
    public RideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_ride, parent, false);
        return new RideViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RideViewHolder holder, int position) {
        HistoryRideModel r = rides.get(position);

        // Pickup & Drop with bold labels
        setBoldLabel(holder.tvPickup, "From: ", safe(r.getPickupName()));
        setBoldLabel(holder.tvDrop, "To: ", safe(r.getDropAddress()));


        setBoldLabel(holder.tvDriverName, "Driver: ", safe(r.getDriverName()));


        // Booking Date + Time
        String dateTime = (safe(r.getBookingDate()) +
                (r.getBookingTime() != null && !r.getBookingTime().isEmpty() ? "  " + r.getBookingTime() : "")).trim();
        holder.tvDateTime.setText(!dateTime.isEmpty() ? dateTime : "—");

        // Price
        setBoldLabel(holder.tvPrice, "Price: ",  safe(r.getPrice()));




        // Status with color
        String status = safe(r.getStatus()).toLowerCase();
        holder.tvStatus.setText(status.replace('_', ' '));
        int colorRes;
        switch (status) {
            case "cancelled_by_driver":
            case "cancelled_by_system":
                colorRes = android.R.color.holo_red_dark;
                break;
            case "accepted":
            case "waiting":
                colorRes = android.R.color.holo_orange_light;
                break;
            case "completed":
                colorRes = android.R.color.holo_green_dark;
                break;
            default:
                colorRes = android.R.color.black;
        }
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, colorRes));

        // Item click
        holder.itemView.setOnClickListener(v -> {
            // handle click if needed
        });
    }

    @Override
    public int getItemCount() {
        return rides == null ? 0 : rides.size();
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickup, tvDrop, tvDateTime, tvPrice, tvStatus, tvDriverName;

        RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDrop = itemView.findViewById(R.id.tvDrop);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);

        }
    }

    // Helper: safe string
    private static String safe(String s) {
        if (s == null) return "—";
        String t = s.trim();
        return t.isEmpty() ? "—" : t;
    }

    // Helper: set single bold label
    private void setBoldLabel(TextView tv, String label, String value) {
        SpannableString spannable = new SpannableString(label + value);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, label.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.setText(spannable);
    }

    // Helper: append another bold label to existing TextView
    private void appendBoldLabel(TextView tv, String label, String value) {
        SpannableString spannable = new SpannableString(label + value);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, label.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        tv.append(spannable);
    }



}
