package beatify.labonappsdevelopment.beatify;

import android.app.DownloadManager;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class StartUp extends AppCompatActivity implements
        PlayerNotificationCallback, ConnectionStateCallback{
    // TODO: Replace with your client ID
    private static final String CLIENT_ID = "e0350925a3624229875cb15856fb7567";
    // TODO: Replace with your redirect URI
    private static final String REDIRECT_URI = "beatify-login://callback";
    private static final int REQUEST_CODE = 1337;
    private static final int ACTIVITY_CREATE = 0;

    private static Intent itentMainActivity;


    public StartUp() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);

        Utils.userPlaylists = new LinkedList<PlaylistSimple>();
        Utils.userPlaylistsTracks = new HashMap<String, List<PlaylistTrack>>();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            itentMainActivity = new Intent(this, MainActivity.class);

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Utils.api = new SpotifyApi();
                Utils.api.setAccessToken(response.getAccessToken());
                Utils.spotify = Utils.api.getService();

                //new getUserID().execute();

                //get user_id
                Utils.spotify.getMe(new Callback<UserPrivate>() {
                    @Override
                    public void success(final UserPrivate userPrivate, retrofit.client.Response response) {
                        Log.d("User success", userPrivate.id);
                        Utils.userData = userPrivate;
                        //get playlists
                        Utils.spotify.getPlaylists(userPrivate.id, new Callback<Pager<PlaylistSimple>>() {
                            @Override
                            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                                List<PlaylistSimple> playlists = playlistSimplePager.items;
                                Utils.userPlaylists = playlists;
                                for (PlaylistSimple p : playlists) {
                                    Utils.spotify.getPlaylistTracks(userPrivate.id, p.id, new Callback<Pager<PlaylistTrack>>() {
                                        @Override
                                        public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                                            List<PlaylistTrack> tracks = playlistTrackPager.items;
                                            Utils.userPlaylistsTracks.put(response.getUrl().split("/")[7], tracks);
                                        }
                                        @Override
                                        public void failure(RetrofitError error) {
                                            Log.e("TEST", "Could not get playlist tracks");
                                        }
                                    });
                                }
                                startActivityForResult(itentMainActivity, ACTIVITY_CREATE);
                            }
                            @Override
                            public void failure(RetrofitError error) {
                                Log.e("TEST", "Could not get playlists");
                            }
                        });
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e("TEST", "Could not get userdata");
                    }
                });
            }
        }

    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }
    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}
