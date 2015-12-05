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
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class StartUp extends AppCompatActivity {
    private static final String REDIRECT_URI = "beatify-login://callback";
    private static final int ACTIVITY_CREATE = 0;
    private static Intent itentMainActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(Utils.CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, Utils.REQUEST_CODE, request);

        Utils.userPlaylists = new LinkedList<PlaylistSimple>();
        Utils.userPlaylistsTracks = new HashMap<String, List<PlaylistTrack>>();

        itentMainActivity = new Intent(this, MainActivity.class);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == Utils.REQUEST_CODE) {

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Utils.accessToken = response.getAccessToken();
                Utils.api = new SpotifyApi();
                Utils.api.setAccessToken(Utils.accessToken);
                Utils.spotify = Utils.api.getService();

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
}
