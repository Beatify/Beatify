package beatify.labonappsdevelopment.beatify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.ListView;
import android.widget.TextView;

import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;

public class Tracks extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        PlayerNotificationCallback, ConnectionStateCallback {

    private static final int ACTIVITY_CREATE = 0;

    private MenuItem heartRatMenuItem;
    private MenuItem connectedDeviceMenuItem;

    private ListView mListView;
    private TrackListAdapter mTrackListAdapter;

    private PlaylistSimple mPlaylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Utils.setupFloatingActionButtons(Tracks.this, this);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        heartRatMenuItem = navigationView.getMenu().findItem(R.id.nav_heart_rate);
        connectedDeviceMenuItem = navigationView.getMenu().findItem(R.id.nav_connected_device);
        if(DeviceScanActivity.mConnected)
            connectedDeviceMenuItem.setTitle(DeviceScanActivity.mDeviceName);

        registerReceiver(mGattUpdateReceiver, DeviceScanActivity.makeGattUpdateIntentFilter());

        Utils.displaySpoitfyUserInfo(navigationView);
        mListView = (ListView) findViewById(R.id.track_list);

        Utils.setNavigationViewIcons(this, navigationView);

        TextView emptyText = (TextView)findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyText);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayTracks();
        Utils.displayCurrentTrackInfo(this);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_songs) {
            Intent i = new Intent(this, MainActivity.class);
            startActivityForResult(i, ACTIVITY_CREATE);
        } else if (id == R.id.nav_connected_device) { /* stay at this activity. */ }
        else if (id == R.id.nav_devices) {
            Intent i = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(i, ACTIVITY_CREATE);
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
        TextView trackName;
        TextView trackAlbum;
        TextView trackArtists;
        TextView trackDuration;
    }

    private class TrackListAdapter extends BaseAdapter {

        private List<PlaylistTrack> mTracks;
        private LayoutInflater mInflator;

        public TrackListAdapter() {
            super();
            mTracks = new ArrayList<PlaylistTrack>();
            mInflator = Tracks.this.getLayoutInflater();
        }

        public void addTracks(PlaylistTrack track) {
            if (!mTracks.contains(track))
                mTracks.add(track);
        }

        public PlaylistTrack getTrack(int position) {
            return mTracks.get(position);
        }

        public void clear() {
            mTracks.clear();
        }

        @Override
        public int getCount() {
            return mTracks.size();
        }

        @Override
        public Object getItem(int position) {
            return mTracks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;

            view = mInflator.inflate(R.layout.listitem_tracks, null);
            viewHolder = new ViewHolder();
            viewHolder.trackName = (TextView) view.findViewById(R.id.track_li_name);
            viewHolder.trackAlbum = (TextView) view.findViewById(R.id.track_li_album);
            viewHolder.trackDuration = (TextView) view.findViewById(R.id.track_li_duration);
            viewHolder.trackArtists = (TextView) view.findViewById(R.id.track_li_artists);
            view.setTag(viewHolder);

            PlaylistTrack track = mTracks.get(position);
            final String name = track.track.name;
            if (name != null && name.length() > 0)
                viewHolder.trackName.setText(name);
            else
                viewHolder.trackName.setText(R.string.track_name_unknown);

            final String album = track.track.album.name;
            if (album != null && album.length() > 0)
                viewHolder.trackAlbum.setText(album);
            else
                viewHolder.trackAlbum.setText(R.string.track_album_unknown);

            final Long duration = track.track.duration_ms;
            Date date = new Date(duration);
            DateFormat formatter = new SimpleDateFormat("mm:ss");
            String dateFormatted = formatter.format(date);
            if (duration != null)
                viewHolder.trackDuration.setText(dateFormatted);
            else
                viewHolder.trackDuration.setText(R.string.track_duration_unknown);

            StringBuilder artists = new StringBuilder();
            for(ArtistSimple artist : track.track.artists) {
                if (artists.length() > 0) artists.append(", ");
                artists.append(artist.name);
            }
            if (artists.toString() != null)
                viewHolder.trackArtists.setText(artists.toString());
            else
                viewHolder.trackArtists.setText(R.string.track_artists_unknown);

            return view;
        }
    }

    private void displayTracks () {

        mTrackListAdapter = new TrackListAdapter();
        String playlist_id = getIntent().getStringExtra("playlist_id");
        mPlaylist = Utils.getPlaylistById(playlist_id);
        for (PlaylistTrack t: Utils.userPlaylistsTracks.get(playlist_id)) {
            mTrackListAdapter.addTracks(t);
            mTrackListAdapter.notifyDataSetChanged();
        }


        mListView.setAdapter(mTrackListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BeatifyPlayer.beatifyPlayer =
                        new BeatifyPlayer(
                                mPlaylist,
                                mTrackListAdapter.getTrack(position).track.name);
                ((FloatingActionButton)Tracks.this.findViewById(R.id.play)).performClick();
                Utils.displayCurrentTrackInfo(Tracks.this);
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                /*
                Intent intentTracks = new Intent(view.getContext(), Tracks.class);
                startActivityForResult(intentTracks, ACTIVITY_CREATE);
                */
                return true;
            }
        });
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
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }
}
