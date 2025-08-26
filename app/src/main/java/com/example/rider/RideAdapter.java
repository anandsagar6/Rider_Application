package com.example.rider;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

        // Pickup
        String pickup = safe(r.getPickupName());
        holder.tvPickup.setText("\uD83D\uDCCD From: " + pickup);
        holder.tvPickup.setMaxLines(1);
        holder.tvPickup.setEllipsize(TextUtils.TruncateAt.END);

        // Drop
        String drop = safe(r.getDrop());
        holder.tvDrop.setText("🎯 To: " + drop);

        // Date + time
        String date = !TextUtils.isEmpty(r.getBookingDate()) ? r.getBookingDate() : "";
        String time = !TextUtils.isEmpty(r.getBookingTime()) ? r.getBookingTime() : "";
        String dateTime = (date + (date.isEmpty() || time.isEmpty() ? "" : "  ") + time).trim();
        holder.tvDateTime.setText(!dateTime.isEmpty() ? dateTime : "—");

        // Price
        String price = safe(r.getPrice());
        holder.tvPrice.setText(price);

        // Type
        String type = safe(r.getRideType());
        holder.tvType.setText(!type.equals("—") ? capitalize(type) : "—");

        // Status
        String status = safe(r.getStatus()).toLowerCase();
        holder.tvStatus.setText(!status.equals("—") ? status.replace('_', ' ') : "—");

        int colorRes;
        switch (status) {
            case "cancelled_by_driver":
            case "cancelled_by_you":
            case "cancelled":
                colorRes = android.R.color.holo_red_dark;
                break;
            case "waiting":
            case "pending":
            case "searching":
                colorRes = android.R.color.holo_orange_light;
                break;
            case "completed":
            case "finished":
            case "done":
                colorRes = android.R.color.holo_green_dark;
                break;
            default:
                colorRes = android.R.color.black;
        }
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, colorRes));

        // Item click
        holder.itemView.setOnClickListener(v -> {
            String rideId = r.getRideId();
            if (!TextUtils.isEmpty(rideId)) {
                try {
                    Intent i = new Intent(context, Address_Activity.class);
                    i.putExtra("rideId", rideId);
                    context.startActivity(i);
                } catch (Exception ex) {
                    Toast.makeText(context, "Ride: " + rideId, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Ride details unavailable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return rides == null ? 0 : rides.size();
    }

    static class RideViewHolder extends RecyclerView.ViewHolder {
        TextView tvPickup, tvDrop, tvDateTime, tvPrice, tvStatus, tvType;

        RideViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPickup = itemView.findViewById(R.id.tvPickup);
            tvDrop = itemView.findViewById(R.id.tvDrop);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvType = itemView.findViewById(R.id.tvType);
        }
    }

    // helper
    private static String safe(String s) {
        if (s == null) return "—";
        String t = s.trim();
        return t.isEmpty() ? "—" : t;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}
