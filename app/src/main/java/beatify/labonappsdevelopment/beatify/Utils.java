package beatify.labonappsdevelopment.beatify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;
import com.joanzapata.iconify.fonts.FontAwesomeModule;
import com.spotify.sdk.android.player.Player;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by mpreis on 04/12/15.
 */
public class Utils {
    protected static SpotifyApi api;
    protected static SpotifyService spotify;

    //store spotify data
    protected static UserPrivate userData;
    protected static List<PlaylistSimple> userPlaylists;
    protected static HashMap<String, HashMap<Integer, List<PlaylistTrack>>> userPlaylistsTracks;

    protected static final String CLIENT_ID = "e0350925a3624229875cb15856fb7567";
    protected static final int REQUEST_CODE = 1337;
    protected static String accessToken;

    protected static final int ACTIVITY_CREATE = 0;
    private static JSONObject song = null;


    protected static void displaySpoitfyUserInfo (NavigationView nv) {
        TextView spotify_user_name = (TextView) nv.getHeaderView(0).findViewById(R.id.spotify_user_name);
        WebView spotify_user_img = (WebView) nv.getHeaderView(0).findViewById(R.id.spotify_user_img);
        ImageView spotify_user_img_dummy = (ImageView) nv.getHeaderView(0).findViewById(R.id.spotify_user_img_dummy);

        if(userData != null) {
            if (userData.display_name != null && userData.display_name.length() > 0)
                spotify_user_name.setText(userData.display_name);
            else
                spotify_user_name.setText(userData.id);


            if (userData.images.size() > 0)
                spotify_user_img.loadUrl(userData.images.get(0).url);
            else {
                spotify_user_img.setVisibility(View.GONE);
                spotify_user_img_dummy.setVisibility(View.VISIBLE);
            }
        }

    }


    protected static void setupFloatingActionButtons(final Context ctx, final Activity a) {
        Iconify.with(new FontAwesomeModule());

        final FloatingActionButton play = (FloatingActionButton) a.findViewById(R.id.play);
        if(BeatifyPlayer.beatifyPlayer != null) {
            if(BeatifyPlayer.beatifyPlayer.isPaused) {
                play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_play)
                        .colorRes(R.color.colorWhite).actionBarSize());
            } else {
                play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_pause)
                        .colorRes(R.color.colorWhite).actionBarSize());
            }
        } else {
            play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_play)
                    .colorRes(R.color.colorWhite).actionBarSize());
        }
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BeatifyPlayer.beatifyPlayer != null) {

                    if( ! BeatifyPlayer.beatifyPlayer.tracksLoaded()) {
                        Toast.makeText(ctx, "Data is loading", Toast.LENGTH_SHORT).show();
                    } else {

                        if (BeatifyPlayer.beatifyPlayer.isPaused) {
                            BeatifyPlayer.beatifyPlayer.play();
                            Toast.makeText(ctx, ctx.getString(R.string.play), Toast.LENGTH_SHORT).show();
                            play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_pause)
                                    .colorRes(R.color.colorWhite).actionBarSize());
                        } else {
                            BeatifyPlayer.beatifyPlayer.pause();
                            Toast.makeText(ctx, ctx.getString(R.string.pause), Toast.LENGTH_SHORT).show();
                            play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_play)
                                    .colorRes(R.color.colorWhite).actionBarSize());
                        }
                        displayCurrentTrackInfo(a);
                    }
                } else {
                    Toast.makeText(ctx, "Please select a playlist.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        FloatingActionButton next = (FloatingActionButton) a.findViewById(R.id.next);
        next.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_step_forward)
                .colorRes(R.color.colorWhite).actionBarSize());
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BeatifyPlayer.beatifyPlayer != null) {
                    if( ! BeatifyPlayer.beatifyPlayer.tracksLoaded()) {
                        Toast.makeText(ctx, "Data is loading", Toast.LENGTH_SHORT).show();
                    } else {
                        BeatifyPlayer.beatifyPlayer.next();
                        Toast.makeText(ctx, ctx.getString(R.string.next), Toast.LENGTH_SHORT).show();
                        displayCurrentTrackInfo(a);
                    }
                } else
                    Toast.makeText(ctx, ctx.getString(R.string.select_playlist), Toast.LENGTH_SHORT).show();
            }
        });
    }


    protected static void setNavigationViewIcons(Activity mthis, NavigationView nv) {
        Iconify.with(new FontAwesomeModule());

        MenuItem heart_rate = (MenuItem) nv.getMenu().findItem(R.id.nav_heart_rate);
        heart_rate.setIcon(new IconDrawable(mthis, FontAwesomeIcons.fa_heartbeat)
                .colorRes(R.color.colorAccent).actionBarSize());

        MenuItem playlists = (MenuItem) nv.getMenu().findItem(R.id.nav_songs);
        playlists.setIcon(new IconDrawable(mthis, FontAwesomeIcons.fa_tasks)
                .colorRes(R.color.colorAccent).actionBarSize());

        MenuItem devices = (MenuItem) nv.getMenu().findItem(R.id.nav_devices);
        devices.setIcon(new IconDrawable(mthis, FontAwesomeIcons.fa_bluetooth_b)
                .colorRes(R.color.colorAccent).actionBarSize());

        MenuItem current_device = (MenuItem) nv.getMenu().findItem(R.id.nav_connected_device);
        current_device.setIcon(new IconDrawable(mthis, FontAwesomeIcons.fa_exchange)
                .colorRes(R.color.colorAccent).actionBarSize());

        MenuItem about = (MenuItem) nv.getMenu().findItem(R.id.nav_about);
        about.setIcon(new IconDrawable(mthis, FontAwesomeIcons.fa_info)
                .colorRes(R.color.colorAccent).actionBarSize());
    }


    protected static void displayCurrentTrackInfo(Activity a) {
        if(BeatifyPlayer.beatifyPlayer != null
            && BeatifyPlayer.beatifyPlayer.existsCurrentTrack()) {
            RelativeLayout rl = (RelativeLayout) a.findViewById(R.id.track_info);
            WebView track_img = (WebView) a.findViewById(R.id.track_img);
            TextView track_name = (TextView) a.findViewById(R.id.track_name);
            TextView track_artists = (TextView) a.findViewById(R.id.track_artists);
            TextView track_bpm = (TextView) a.findViewById(R.id.track_bpm);

            rl.setVisibility(LinearLayout.VISIBLE);
            track_name.setText(BeatifyPlayer.beatifyPlayer.getCurrentTrackName());
            track_artists.setText(BeatifyPlayer.beatifyPlayer.getCurrentTrackArtists());
            track_bpm.setText(BeatifyPlayer.beatifyPlayer.getCurrentTrackBpm());
            track_img.loadUrl(BeatifyPlayer.beatifyPlayer.getCurrentTrackImg());
        }
    }


    protected static PlaylistSimple getPlaylistById(String id) {
        for(PlaylistSimple pl : Utils.userPlaylists)
            if(pl.id.equals(id))
                return pl;
        return null;
    }



    protected static void getSpotifyData(final Activity a, final Intent intent) {
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
                                    for (PlaylistTrack plTrack : tracks) {
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
                        a.startActivityForResult(intent, ACTIVITY_CREATE);
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



    private static void fetchBPM(final PlaylistTrack plTrack, final String playList, String artistName, final String songName) throws IOException {
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

    private static void fetchBPMWithId(PlaylistTrack plTrack, String playList, String id, final String songName){
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

    private static String fetchResponse(HttpURLConnection conn) throws IOException {
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