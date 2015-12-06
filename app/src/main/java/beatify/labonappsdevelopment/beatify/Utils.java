package beatify.labonappsdevelopment.beatify;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
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

import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;

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

    protected static void displaySpoitfyUserInfo (NavigationView nv) {
        TextView spotify_id = (TextView) nv.getHeaderView(0).findViewById(R.id.spotify_id);
        TextView spotify_displayname = (TextView) nv.getHeaderView(0).findViewById(R.id.spotify_displayname);
        spotify_id.setText(userData.id);
        spotify_displayname.setText(userData.display_name);
    }


    protected static void setupFloatingActionButtons(final Context ctx, final Activity a) {
        Iconify.with(new FontAwesomeModule());

        FloatingActionButton prev = (FloatingActionButton) a.findViewById(R.id.prev);
        prev.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_step_backward)
                .colorRes(R.color.colorWhite).actionBarSize());

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BeatifyPlayer.beatifyPlayer != null) {
                    if (BeatifyPlayer.beatifyPlayer.prev()) {
                        displayCurrentTrackInfo(a);
                        Toast.makeText(ctx, ctx.getString(R.string.prev), Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(ctx, ctx.getString(R.string.noprev), Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(ctx, ctx.getString(R.string.select_playlist), Toast.LENGTH_SHORT).show();
            }
        });

        final FloatingActionButton play = (FloatingActionButton) a.findViewById(R.id.play);

        if(BeatifyPlayer.beatifyPlayer != null) {
            if(BeatifyPlayer.beatifyPlayer.isPaused) {
                play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_play_circle)
                        .colorRes(R.color.colorWhite).actionBarSize());
            } else {
                play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_pause_circle)
                        .colorRes(R.color.colorWhite).actionBarSize());
            }
        } else {
            play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_play_circle)
                    .colorRes(R.color.colorWhite).actionBarSize());
        }
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BeatifyPlayer.beatifyPlayer != null) {
                    if(BeatifyPlayer.beatifyPlayer.isPaused) {
                        BeatifyPlayer.beatifyPlayer.play();
                        Toast.makeText(ctx, ctx.getString(R.string.play), Toast.LENGTH_SHORT).show();
                        play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_pause_circle)
                                .colorRes(R.color.colorWhite).actionBarSize());
                    } else {
                        BeatifyPlayer.beatifyPlayer.pause();
                        Toast.makeText(ctx, ctx.getString(R.string.pause), Toast.LENGTH_SHORT).show();
                        play.setImageDrawable(new IconDrawable(a, FontAwesomeIcons.fa_play_circle)
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
}