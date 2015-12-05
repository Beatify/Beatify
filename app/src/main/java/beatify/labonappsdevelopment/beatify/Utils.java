package beatify.labonappsdevelopment.beatify;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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


    protected static void setupFloatingActionButtons(final Context ctx, Activity a) {
        FloatingActionButton prev = (FloatingActionButton) a.findViewById(R.id.prev);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(BeatifyPlayer.beatifyPlayer != null) {
                    if(BeatifyPlayer.beatifyPlayer.prev())
                        Toast.makeText(ctx, ctx.getString(R.string.prev), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(ctx, ctx.getString(R.string.noprev), Toast.LENGTH_SHORT).show();
                }
                else Toast.makeText(ctx, ctx.getString(R.string.select_playlist), Toast.LENGTH_SHORT).show();
            }
        });

        final FloatingActionButton play = (FloatingActionButton) a.findViewById(R.id.play);
        play.setOnClickListener(new View.OnClickListener() {
            boolean isPlaying = false;

            @Override
            public void onClick(View view) {
                if (BeatifyPlayer.beatifyPlayer != null) {
                    isPlaying = !isPlaying;
                    if (isPlaying) {
                        BeatifyPlayer.beatifyPlayer.pause();
                        Toast.makeText(ctx, ctx.getString(R.string.pause), Toast.LENGTH_SHORT).show();
                    } else {
                        BeatifyPlayer.beatifyPlayer.play();
                        Toast.makeText(ctx, ctx.getString(R.string.play), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ctx, "Please select a playlist.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        FloatingActionButton next = (FloatingActionButton) a.findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BeatifyPlayer.beatifyPlayer != null) {
                    BeatifyPlayer.beatifyPlayer.next();
                    Toast.makeText(ctx, ctx.getString(R.string.next), Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(ctx, ctx.getString(R.string.select_playlist), Toast.LENGTH_SHORT).show();
            }
        });
    }
}