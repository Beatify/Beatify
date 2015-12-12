package beatify.labonappsdevelopment.beatify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.PlaylistSimple;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        PlayerNotificationCallback, ConnectionStateCallback  {

    private MenuItem heartRatMenuItem;

    private ListView mListView;
    private PlaylistListAdapter mPlaylistListAdapter;
    protected static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Utils.setupFloatingActionButtons(MainActivity.this, this);

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

        TextView emptyText = (TextView)findViewById(android.R.id.empty);
        mListView = (ListView) findViewById(R.id.playlist_list);
        mListView.setEmptyView(emptyText);

        mContext = this;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

        if(mGattUpdateReceiver != null)
            unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.spotify_reload) {
            Utils.getSpotifyData(this, new Intent(this, MainActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Initializes list view adapter.
        registerReceiver(mGattUpdateReceiver, DeviceScanActivity.makeGattUpdateIntentFilter());
        displayPlaylists();
        BeatifyPlayer.setupPlayer(this, MainActivity.this, MainActivity.this);
        Utils.currentActivity = this;
        Utils.displayCurrentTrackInfo();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_devices) {
            Intent i = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(i, Utils.ACTIVITY_CREATE);

        } else if (id == R.id.nav_about) {
            Intent i = new Intent(this, AboutActivity.class);
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

    static class ViewHolder {
        TextView playListName;
        TextView playListSize;
    }

    private class PlaylistListAdapter extends BaseAdapter {

        private List<PlaylistSimple> mPlaylists;
        private LayoutInflater mInflator;

        public PlaylistListAdapter() {
            super();
            mPlaylists = new ArrayList<PlaylistSimple>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addPlaylist(PlaylistSimple playlist) {
            if (!mPlaylists.contains(playlist))
                mPlaylists.add(playlist);
        }

        public PlaylistSimple getPlaylist(int position) {
            return mPlaylists.get(position);
        }

        public void clear() {
            mPlaylists.clear();
        }

        @Override
        public int getCount() {
            return mPlaylists.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlaylists.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;

            view = mInflator.inflate(R.layout.listitem_playlist, null);
            viewHolder = new ViewHolder();
            viewHolder.playListName = (TextView) view.findViewById(R.id.playlist_name);
            viewHolder.playListSize = (TextView) view.findViewById(R.id.playlist_size);
            view.setTag(viewHolder);

            PlaylistSimple playlist = mPlaylists.get(position);
            final String name = playlist.name;
            if (name != null && name.length() > 0)
                viewHolder.playListName.setText(name);
            else
                viewHolder.playListName.setText(R.string.spotify_unknown_playlist);

            viewHolder.playListSize.setText(
                    getResources().getString(R.string.spotify_nr_tracks) + ":" + playlist.tracks.total);

            return view;
        }
    }

    private void displayPlaylists () {
        mPlaylistListAdapter = new PlaylistListAdapter();
        mListView.setAdapter(mPlaylistListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BeatifyPlayer.beatifyPlayer = new BeatifyPlayer(mPlaylistListAdapter.getPlaylist(position));
                ((FloatingActionButton) MainActivity.this.findViewById(R.id.play)).performClick();
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intentTracks = new Intent(view.getContext(), TracksActivity.class);
                intentTracks.putExtra("playlist_id", mPlaylistListAdapter.getPlaylist(position).id);
                startActivityForResult(intentTracks, Utils.ACTIVITY_CREATE);
                return true;
            }
        });

        if(Utils.userPlaylists != null)
        for (PlaylistSimple pl: Utils.userPlaylists) {
            mPlaylistListAdapter.addPlaylist(pl);
            mPlaylistListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Throwable throwable) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onPlaybackEvent(PlayerNotificationCallback.EventType eventType, PlayerState playerState) {
        BeatifyPlayer.beatifyPlayer.setPlayState(playerState);

        if (PlayerNotificationCallback.EventType.TRACK_CHANGED.equals(eventType)) {
            BeatifyPlayer.beatifyPlayer.addNextTrack();
        } else if(PlayerNotificationCallback.EventType.AUDIO_FLUSH.equals(eventType)) {
            BeatifyPlayer.beatifyPlayer.setCurrentTrack(playerState.trackUri);
            Utils.displayCurrentTrackInfo();
        }

    }

    @Override
    public void onPlaybackError(PlayerNotificationCallback.ErrorType errorType, String s) {

    }
}
