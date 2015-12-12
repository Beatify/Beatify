package beatify.labonappsdevelopment.beatify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
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

public class Utils {
    protected static SpotifyApi api;
    protected static SpotifyService spotify;

    protected static final Integer DEFAULT_BPM = 0;

    //store Spotify data
    protected static UserPrivate userData;
    protected static List<PlaylistSimple> userPlaylists;
    protected static HashMap<String, HashMap<Integer, List<PlaylistTrack>>> userPlaylistsTracks;

    // Constants of the Spotify connection.
    protected static final String CLIENT_ID = "e0350925a3624229875cb15856fb7567";
    protected static final int REQUEST_CODE = 1337;
    protected static String accessToken;

    protected static final int ACTIVITY_CREATE = 0;

    // Holds the currently displayed activity, needed to display track information.
    protected static Activity currentActivity;

    /**
     * Display user information in given NavigationView.
     * @param nv
     */
    protected static void displaySpoitfyUserInfo (NavigationView nv) {
        TextView spotify_user_name = (TextView) nv.getHeaderView(0).findViewById(R.id.spotify_user_name);
        WebView spotify_user_img = (WebView) nv.getHeaderView(0).findViewById(R.id.spotify_user_img);
        ImageView spotify_user_img_dummy = (ImageView) nv.getHeaderView(0).findViewById(R.id.spotify_user_img_dummy);

        if(userData != null) {
            if (userData.display_name != null && userData.display_name.length() > 0)
                spotify_user_name.setText(userData.display_name);
            else
                spotify_user_name.setText(userData.id);

            if (userData.images.size() > 0){
                spotify_user_img.setVisibility(View.VISIBLE);
                spotify_user_img_dummy.setVisibility(View.GONE);
                spotify_user_img.loadUrl(userData.images.get(0).url);
            } else {
                spotify_user_img.setVisibility(View.GONE);
                spotify_user_img_dummy.setVisibility(View.VISIBLE);
            }
        }

    }


    /**
     * Setup FloatingActionButton (play and next buttons) for given activity and corresponding context.
     * @param ctx
     * @param a
     */
    protected static void setupFloatingActionButtons(final Context ctx, final Activity a) {
        Iconify.with(new FontAwesomeModule());

        final FloatingActionButton play = (FloatingActionButton) a.findViewById(R.id.play);
        if(BeatifyPlayer.beatifyPlayer != null) {
            if(BeatifyPlayer.beatifyPlayer.isPaused()) {
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

                    if (!BeatifyPlayer.beatifyPlayer.tracksLoaded()) {
                        Toast.makeText(ctx, "Data is loading", Toast.LENGTH_SHORT).show();
                    } else {

                        if (BeatifyPlayer.beatifyPlayer.isPaused()) {
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
                    if (!BeatifyPlayer.beatifyPlayer.tracksLoaded()) {
                        Toast.makeText(ctx, "Data is loading", Toast.LENGTH_SHORT).show();
                    } else {
                        if(BeatifyPlayer.beatifyPlayer.isPaused()){
                            FloatingActionButton ply = (FloatingActionButton)a.findViewById(R.id.play);
                            ply.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_pause)
                                    .colorRes(R.color.colorWhite).actionBarSize());
                        }
                        BeatifyPlayer.beatifyPlayer.next();
                        Toast.makeText(ctx, ctx.getString(R.string.next), Toast.LENGTH_SHORT).show();

                    }
                } else
                    Toast.makeText(ctx, ctx.getString(R.string.select_playlist), Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Replace default icons of the navigation by fancy AwesomeFont icons.
     * @param mthis
     * @param nv
     */
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


    /**
     * Display information about the currenlty playing track, if there is such one.
     */
    protected static void displayCurrentTrackInfo() {
        if(BeatifyPlayer.beatifyPlayer != null
            && BeatifyPlayer.beatifyPlayer.existsCurrentTrack())
        {
            RelativeLayout rl = (RelativeLayout) currentActivity.findViewById(R.id.track_info);
            WebView track_img = (WebView) currentActivity.findViewById(R.id.track_img);
            TextView track_name = (TextView) currentActivity.findViewById(R.id.track_name);
            TextView track_artists = (TextView) currentActivity.findViewById(R.id.track_artists);
            TextView track_bpm = (TextView) currentActivity.findViewById(R.id.track_bpm);
            TextView playlist_name = (TextView) currentActivity.findViewById(R.id.track_playlist_name);

            rl.setVisibility(LinearLayout.VISIBLE);
            track_name.setText(BeatifyPlayer.beatifyPlayer.getCurrentTrackName());
            track_artists.setText(BeatifyPlayer.beatifyPlayer.getCurrentTrackArtists());
            track_img.loadUrl(BeatifyPlayer.beatifyPlayer.getCurrentTrackImg());
            playlist_name.setText(BeatifyPlayer.beatifyPlayer.getCurrentPlaylistName());

            if(BeatifyPlayer.beatifyPlayer.getCurrentTrackBpm() != DEFAULT_BPM)
                track_bpm.setText(BeatifyPlayer.beatifyPlayer.getCurrentTrackBpm().toString());
            else
                track_bpm.setText(Utils.currentActivity.getResources().getText(R.string.no_data));

        }
    }

    /**
     * Request all information needed from Spotify (user data, playlists, tracks) and
     * fetch BMP of tracks.
     * @param a
     * @param intent
     */
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
                                            fetchBPM(plTrack,
                                                    response.getUrl().split("/")[7],
                                                    plTrack.track.artists.get(0).name,
                                                    plTrack.track.name);
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
                        a.finish();
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

    /**
     * Fetch BPM of given track (by track name and artist).
     * @param plTrack
     * @param playlist
     * @param artistName
     * @param songName
     * @throws IOException
     */
    private static void fetchBPM(final PlaylistTrack plTrack, final String playlist, String artistName, final String songName)
            throws IOException {

        final String encodedArtistName = URLEncoder.encode(artistName, "UTF-8");
        final String encodedSongName = URLEncoder.encode(songName, "UTF-8");

        Thread threadFetch = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    HttpURLConnection conn = null;
                    StringBuilder url = new StringBuilder();
                    url.append("http://developer.echonest.com/api/v4/song/search?api_key=KSTXY4LPAIV0FHCNU");
                    url.append("&artist=" + encodedArtistName);
                    url.append("&title=" + encodedSongName);

                    conn = (HttpURLConnection) new URL(url.toString()).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setDoInput(true);

                    JSONObject song = new JSONObject(fetchResponse(conn));
                    JSONArray arrDetails = song.getJSONObject("response").getJSONArray("songs");

                    Integer bpm  = DEFAULT_BPM;
                    if(conn.getResponseCode() == 200 && arrDetails.length() > 0)
                        bpm = fetchBPMWithId(((JSONObject) arrDetails.get(0)).getString("id"));

                    addSpotifyBpmDataToTrackList(playlist, plTrack, bpm);

                } catch (Exception e) {
                    addSpotifyBpmDataToTrackList(playlist, plTrack, DEFAULT_BPM);
                    e.printStackTrace();
                }
            }
        });

        // start fetching data
        threadFetch.start();

    }

    /**
     * Fetch BPM of track by Echonest id.
     * @param id
     * @return
     */
    private static Integer fetchBPMWithId(String id) {
        Integer bpm = DEFAULT_BPM;
        try {
            String bpmCheckURL = "http://developer.echonest.com/api/v4/song/profile?api_key=" +
                    "KSTXY4LPAIV0FHCNU&id=" + id + "&bucket=audio_summary";

            HttpURLConnection conn = (HttpURLConnection) (new URL(bpmCheckURL)).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            String response = fetchResponse(conn);
            JSONObject songDetails = new JSONObject(response);
            JSONArray arrDetails = songDetails.getJSONObject("response").getJSONArray("songs");
            JSONObject audioSummary = ((JSONObject) arrDetails.get(0)).getJSONObject("audio_summary");
            Integer value = audioSummary.getInt("tempo");

            bpm = (conn.getResponseCode() == 200)
                ? value
                : DEFAULT_BPM;

        } catch (Exception e) { e.printStackTrace(); }
        finally { return bpm;}
    }

    /**
     * Get response of connection.
     * @param conn
     * @return
     * @throws IOException
     */
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

    /**
     * Build main Beatify data structure.
     * @param playlist
     * @param track
     * @param bpm
     */
    private static void addSpotifyBpmDataToTrackList(String playlist, PlaylistTrack track,  Integer bpm) {
        HashMap<Integer, List<PlaylistTrack>> playListEntry =
                Utils.userPlaylistsTracks.containsKey(playlist)
                        ? Utils.userPlaylistsTracks.get(playlist)
                        : new HashMap<Integer, List<PlaylistTrack>>();

        List<PlaylistTrack> trackList =
                playListEntry.containsKey(bpm)
                        ? playListEntry.get(bpm)
                        : new ArrayList<PlaylistTrack>();

        trackList.add(track);
        playListEntry.put(bpm, trackList);
        Utils.userPlaylistsTracks.put(playlist, playListEntry);
    }
}