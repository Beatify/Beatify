package beatify.labonappsdevelopment.beatify;

import android.support.design.widget.NavigationView;
import android.widget.TextView;

import com.spotify.sdk.android.player.Player;

import java.util.HashMap;
import java.util.List;

import beatify.labonappsdevelopment.beatify.R;
import beatify.labonappsdevelopment.beatify.StartUp;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.UserPrivate;

/**
 * Created by mpreis on 04/12/15.
 */
public class Utils {

    private Player mPlayer;

    protected static SpotifyApi api;
    protected static SpotifyService spotify;

    //store spotify data
    protected static UserPrivate userData;
    protected static List<PlaylistSimple> userPlaylists;
    protected static HashMap<String, List<PlaylistTrack>> userPlaylistsTracks;


    protected static void displaySpoitfyUserInfo (NavigationView nv) {
        TextView spotify_id = (TextView) nv.getHeaderView(0).findViewById(R.id.spotify_id);
        TextView spotify_displayname = (TextView) nv.getHeaderView(0).findViewById(R.id.spotify_displayname);
        spotify_id.setText(userData.id);
        spotify_displayname.setText(userData.display_name);
    }
}
