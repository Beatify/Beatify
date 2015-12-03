package beatify.labonappsdevelopment.beatify;

import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.SavedTrack;
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

    private Player mPlayer;
    private final String USER_AGENT = "Mozilla/5.0";
    private UserPrivate uData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming", "playlist-read-collaborative"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                SpotifyApi api = new SpotifyApi();
                api.setAccessToken(response.getAccessToken());
                SpotifyService spotify = api.getService();

                //get UserID
                spotify.getMe(new Callback<UserPrivate>() {
                    @Override
                    public void success(UserPrivate userPrivate, retrofit.client.Response response) {
                        Log.d("User success", userPrivate.id);
                        uData = userPrivate;
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        //TODO: Errorhandling
                    }
                });

                //get all playlists: should replace huetterth with UserID from above
                spotify.getPlaylists("huetterth", new Callback<Pager<PlaylistSimple>>() {
                    @Override
                    public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                        Log.d("TEST", "Got the playlists");
                        List<PlaylistSimple> playlists = playlistSimplePager.items;
                        for (PlaylistSimple p : playlists) {
                            Log.e("TEST", p.name + " - " + p.id);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e("TEST", "Could not get playlists");
                    }
                });

                //get all songs from a playlist: again replace UserID and do it for all playlists
                spotify.getPlaylistTracks("huetterth", "35cPdE04iQQgztYxNyx2mC", new Callback<Pager<PlaylistTrack>>() {
                    @Override
                    public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                        Log.e("TEST", "GOT the tracks in playlist");
                        List<PlaylistTrack> items = playlistTrackPager.items;
                        for (PlaylistTrack pt : items) {
                            Log.e("TEST", pt.track.name + " - " + pt.track.artists.get(0).name + " - " + pt.track.id);
                        }
                    }

                    @Override
                    public void failure(RetrofitError error) {
                        Log.e("TEST", "Could not get playlist tracks");
                    }
                });


                /*Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer = player;
                        mPlayer.addConnectionStateCallback(StartUp.this);
                        mPlayer.addPlayerNotificationCallback(StartUp.this);
                        mPlayer.play("spotify:user:skatygarcia:playlist:36f5MuoelMRwVIet1HUqYC");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });*/
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
