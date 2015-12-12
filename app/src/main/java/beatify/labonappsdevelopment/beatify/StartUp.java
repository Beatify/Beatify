package beatify.labonappsdevelopment.beatify;

import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;

public class StartUp extends AppCompatActivity {
    private static final String REDIRECT_URI = "beatify-login://callback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(Utils.CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, Utils.REQUEST_CODE, request);

        Utils.userPlaylists = new LinkedList<PlaylistSimple>();
        Utils.userPlaylistsTracks = new HashMap<String, HashMap<Integer, List<PlaylistTrack>>>();
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
                Utils.getSpotifyData(this, new Intent(this, MainActivity.class));
            }
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(BeatifyPlayer.beatifyPlayer != null)
            BeatifyPlayer.beatifyPlayer.pause();
    }
}
