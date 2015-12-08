package beatify.labonappsdevelopment.beatify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private MenuItem heartRatMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Utils.setupFloatingActionButtons(AboutActivity.this, this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        heartRatMenuItem = navigationView.getMenu().findItem(R.id.nav_heart_rate);
        MenuItem connectedDeviceMenuItem = navigationView.getMenu().findItem(R.id.nav_connected_device);
        if(DeviceScanActivity.mConnected)
            connectedDeviceMenuItem.setTitle(DeviceScanActivity.mDeviceName);

        Utils.displaySpoitfyUserInfo(navigationView);
        Utils.setNavigationViewIcons(this, navigationView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Initializes list view adapter.
        registerReceiver(mGattUpdateReceiver, DeviceScanActivity.makeGattUpdateIntentFilter());
        Utils.displayCurrentTrackInfo(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_songs) {
            Intent i = new Intent(this, MainActivity.class);
            startActivityForResult(i, Utils.ACTIVITY_CREATE);
        } else if (id == R.id.nav_devices) {
            Intent i = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(i, Utils.ACTIVITY_CREATE);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null)
                    heartRatMenuItem.setTitle(getResources().getString(R.string.heart_rate) + ": " + data);

            }
        }
    };

}
