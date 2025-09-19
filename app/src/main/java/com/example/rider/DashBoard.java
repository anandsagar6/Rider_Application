package com.example.rider;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;

public class DashBoard extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dash_board);

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        drawerLayout = findViewById(R.id.drawer_layout);


        // ✅ Set default fragment = EnterAddress_Fragment
        if (savedInstanceState == null) {
            loadFragment(new EnterAddress_Fragment());

            bottomNavigationView.setSelectedItemId(R.id.homee); // highlight home in bottom nav
        }

        // ✅ Bottom Navigation handling
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.homee) {
                    loadFragment(new EnterAddress_Fragment()); // ✅ Home shows EnterAddress
                } else if (itemId == R.id.Services) {
                    loadFragment(new Service_Fragment()); // ✅ Search shows Home_Fragment
                } else if (itemId == R.id.account) {
                    loadFragment(new ProfileFragment());
                } else if (itemId == R.id.Activities) {
                    loadFragment(new Activity_Fragment());
                }
                return true;
            }
        });
    }

    private void loadFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            loadFragment(new EnterAddress_Fragment());
            bottomNavigationView.setSelectedItemId(R.id.homee); // keep Home tab highlighted
        } else if (id == R.id.nav_account) {
            loadFragment(new ProfileFragment());
            bottomNavigationView.setSelectedItemId(R.id.account);
        } else if (id == R.id.nav_share) {
            Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_about) {
            Toast.makeText(this, "About clicked", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}
