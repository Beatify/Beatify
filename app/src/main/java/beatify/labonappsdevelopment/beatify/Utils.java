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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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
    protected static HashMap<String, List<PlaylistTrack>> userPlaylistsTracks;

    protected static final String CLIENT_ID = "e0350925a3624229875cb15856fb7567";
    protected static final int REQUEST_CODE = 1337;
    protected static String accessToken;

    protected static final int ACTIVITY_CREATE = 0;

    protected static void displaySpoitfyUserInfo (NavigationView nv) {
        TextView spotify_user_name = (TextView) nv.getHeaderView(0).findViewById(R.id.spotify_user_name);
        WebView spotify_user_img = (WebView) nv.getHeaderView(0).findViewById(R.id.spotify_user_img);
        ImageView spotify_user_img_dummy = (ImageView) nv.getHeaderView(0).findViewById(R.id.spotify_user_img_dummy);

        if(userData != null)
            if(userData.display_name != null && userData.display_name.length() > 0)
                spotify_user_name.setText(userData.display_name);
            else
                spotify_user_name.setText(userData.id);


        if(userData.images.size() > 0)
            spotify_user_img.loadUrl(userData.images.get(0).url);
        else {
            spotify_user_img.setVisibility(View.GONE);
            spotify_user_img_dummy.setVisibility(View.VISIBLE);
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
                    BeatifyPlayer.beatifyPlayer.next();
                    Toast.makeText(ctx, ctx.getString(R.string.next), Toast.LENGTH_SHORT).show();
                    displayCurrentTrackInfo(a);
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
                                    Utils.userPlaylistsTracks.put(response.getUrl().split("/")[7], tracks);
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

}