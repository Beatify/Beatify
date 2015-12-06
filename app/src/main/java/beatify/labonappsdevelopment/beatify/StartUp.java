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
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
    private JSONObject song = null;

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
        Utils.userPlaylistsTracks = new HashMap<>();

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
                                            for (PlaylistTrack plTrack : tracks ) {
                                                try {
                                                    fetchBPM(plTrack, response.getUrl().split("/")[7], plTrack.track.artists.get(0).name, plTrack.track.name);
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }

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

    private void fetchBPM(final PlaylistTrack plTrack, final String playList, String artistName, final String songName) throws IOException {
        final String[] artistNameArr = artistName.split(" ");
        final String[] songNameArr = songName.split(" ");
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    URL url = null;
                    HttpURLConnection conn = null;

                    String urlString = "http://developer.echonest.com/api/v4/song/search?api_key=KSTXY4LPAIV0FHCNU&artist=";
                    for (int i = 0; i < artistNameArr.length; i++) {
                        urlString += artistNameArr[i].toLowerCase() + "%20";
                    }
                    urlString = urlString.substring(0, urlString.length() - 3);
                    urlString += "&title=";
                    for (int i = 0; i < songNameArr.length; i++) {
                        urlString += songNameArr[i].toLowerCase() + "%20";
                    }
                    urlString = urlString.substring(0, urlString.length() - 3);

                    url = new URL(urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);
                    int responseCode = conn.getResponseCode();
                    //        conn.disconnect();
                    System.out.println("\nSending 'GET' request to URL : " + url);
                    System.out.println("Response Code : " + responseCode);

                    if (responseCode == 200) {
                        String response = fetchResponse(conn);

                        song = new JSONObject(response);
                        JSONArray arr = song.getJSONObject("response").getJSONArray("songs");
                        if (arr.length() == 0) {
                            if (Utils.userPlaylistsTracks.containsKey(playList)) {
                                if (Utils.userPlaylistsTracks.get(playList).containsKey(0)) {
                                    Utils.userPlaylistsTracks.get(playList).get(0).add(plTrack);
                                } else {
                                    ArrayList<PlaylistTrack> pl = new ArrayList<>();
                                    pl.add(plTrack);
                                    Utils.userPlaylistsTracks.get(playList).put(0, pl);
                                }
                            } else {
                                HashMap<Integer, List<PlaylistTrack>> map = new HashMap<>();
                                ArrayList<PlaylistTrack> pl = new ArrayList<>();
                                pl.add(plTrack);
                                map.put(0, pl);
                                Utils.userPlaylistsTracks.put(playList, map);
                            }
                        } else {
                            String id = ((JSONObject) arr.get(0)).getString("id");
                            fetchBPMWithId(plTrack, playList, id, songName);
                        }
                    } else {
                        //            Toast.makeText(mContext, "connection error", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }            }
        });
        thread.start();

    }

    private void fetchBPMWithId(PlaylistTrack plTrack, String playList, String id, final String songName){
        try {
            String bpmCheckURL = "http://developer.echonest.com/api/v4/song/profile?api_key=" +
                    "KSTXY4LPAIV0FHCNU&id=" + id + "&bucket=audio_summary";
            URL url = new URL(bpmCheckURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            int responseCode = conn.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);

            if (responseCode == 200) {
                String response = fetchResponse(conn);


                JSONObject songDetails = new JSONObject(response);
                JSONArray arrDetails = songDetails.getJSONObject("response").getJSONArray("songs");
                double bpm = ((JSONObject) arrDetails.get(0)).getJSONObject("audio_summary").getDouble("tempo");
                if (Utils.userPlaylistsTracks.containsKey(playList)){
                    if (Utils.userPlaylistsTracks.get(playList).containsKey((int)bpm)){
                        Utils.userPlaylistsTracks.get(playList).get((int)bpm).add(plTrack);
                    }
                    else{
                        ArrayList<PlaylistTrack> pl = new ArrayList<>();
                        pl.add(plTrack);
                        Utils.userPlaylistsTracks.get(playList).put((int)bpm, pl);
                    }
                }
                else{
                    HashMap<Integer, List<PlaylistTrack>> map = new HashMap<>();
                    ArrayList<PlaylistTrack> pl = new ArrayList<>();
                    pl.add(plTrack);
                    map.put((int) bpm, pl);
                    Utils.userPlaylistsTracks.put(playList, map);
                }
            }
            else{
     //           Toast.makeText(mContext, "connection error", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String fetchResponse(HttpURLConnection conn) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));

        StringBuffer response = new StringBuffer();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }


}
