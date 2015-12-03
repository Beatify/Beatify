package beatify.labonappsdevelopment.beatify;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
    //needed for request of the kaees spotify wrapper
    SpotifyApi api;
    SpotifyService spotify;

    //store spotify data
    private UserPrivate userData;
    private List<PlaylistSimple> userPlaylists;
    private HashMap<String, List<PlaylistTrack>> userPlaylistsTracks = new HashMap<String, List<PlaylistTrack>>();


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
                api = new SpotifyApi();
                api.setAccessToken(response.getAccessToken());
                spotify = api.getService();

                //new getUserID().execute();

                //get user_id
                spotify.getMe(new Callback<UserPrivate>() {
                    @Override
                    public void success(final UserPrivate userPrivate, retrofit.client.Response response) {
                        Log.d("User success", userPrivate.id);
                        userData = userPrivate;
                        //get playlists
                        spotify.getPlaylists(userPrivate.id, new Callback<Pager<PlaylistSimple>>() {
                            @Override
                            public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                                List<PlaylistSimple> playlists = playlistSimplePager.items;
                                userPlaylists = playlists;
                                for (PlaylistSimple p : playlists) {
                                    //Log.e("TEST", p.name + " - " + p.id);
                                    //get tracks of playlists
                                    spotify.getPlaylistTracks(userPrivate.id, p.id, new Callback<Pager<PlaylistTrack>>() {
                                        @Override
                                        public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                                            //Log.e("TEST", "GOT the tracks in playlist");
                                            List<PlaylistTrack> tracks = playlistTrackPager.items;
                                            userPlaylistsTracks.put(response.getUrl().split("/")[7], tracks);
                                            //for (PlaylistTrack pt : tracks) {
                                                //Log.e("TEST", pt.track.name + " - " + pt.track.artists.get(0).name + " - " + pt.track.id);
                                            //}
                                        }

                                        @Override
                                        public void failure(RetrofitError error) {
                                            Log.e("TEST", "Could not get playlist tracks");
                                        }
                                    });
                                }
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

/*
    private class getUserID extends AsyncTask<Void, Void, UserPrivate> {

        @Override
        protected UserPrivate doInBackground(Void... params) {
            Log.d("async:", "start");
            try {
                UserPrivate userPrivate = spotify.getMe();
                return userPrivate;
            } catch (RetrofitError error) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                // handle error
            }
            return null;
        }

        @Override
        protected void onPostExecute(UserPrivate up) {
            Log.d("ID:", up.id);
            userData = up;
            new getAllPlaylists().execute(userData.id);
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    private class getAllPlaylists extends AsyncTask<String, Void, Pager<PlaylistSimple>> {

        @Override
        protected Pager<PlaylistSimple> doInBackground(String... params) {
            Log.d("async:", "start playlist");
            try {
                Pager<PlaylistSimple> ppls = spotify.getPlaylists(params[0]);
                return ppls;
            } catch (RetrofitError error) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                // handle error
            }
            return null;
        }

        @Override
        protected void onPostExecute(Pager<PlaylistSimple> ppls) {
            List<PlaylistSimple> plList = ppls.items;
            userPlaylists = plList;
            for (PlaylistSimple p: plList) {
                Log.e("TEST", p.name + " - " + p.id);
                //get infos for all playlists
                new getPlaylistSongs().execute(userData.id, p.id);
            }
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    private class getPlaylistSongs extends AsyncTask<String, Void, Pager<PlaylistTrack>> {

        @Override
        protected Pager<PlaylistTrack> doInBackground(String... params) {
            Log.d("async:", "start playlistsongs");
            try {
                Pager<PlaylistTrack> pplst = spotify.getPlaylistTracks(params[0], params[1]);
                return pplst;
            } catch (RetrofitError error) {
                SpotifyError spotifyError = SpotifyError.fromRetrofitError(error);
                // handle error
            }
            return null;
        }

        @Override
        protected void onPostExecute(Pager<PlaylistTrack> pplst) {
            List<PlaylistTrack> pltList = pplst.items;
            for (PlaylistTrack pt: pltList) {
                Log.e("TEST", pt.track.name + " - " + pt.track.artists.get(0).name + " - " + pt.track.id);
            }
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
*/

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
