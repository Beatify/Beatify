package beatify.labonappsdevelopment.beatify;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;

/**
 * Created by mpreis on 05/12/15.
 */
public class BeatifyPlayer {
    protected static BeatifyPlayer beatifyPlayer;

    private Stack<Pair<Integer, Integer>> playedTracks;
    private Pair currentTrack;
    private PlaylistSimple playlist;
    private HashMap<Integer, List<PlaylistTrack>> tracks;
    private Boolean isPaused;
    protected static Player player;


    private BeatifyPlayer() {}
    public BeatifyPlayer(PlaylistSimple pl) {
        playlist = pl;
        tracks = Utils.userPlaylistsTracks.get(playlist.id);
        isPaused = false;
        playedTracks = new Stack<Pair<Integer, Integer>>();
    }

    private String nextTrack() {
        if (currentTrack != null){
            playedTracks.push(currentTrack);
        }
        int heartRate = DeviceScanActivity.mData;

        if (tracks == null){
            Toast.makeText(MainActivity.mContext, "Data is loading", Toast.LENGTH_SHORT);
        }
        ArrayList<PlaylistTrack> tracksInInterval = new ArrayList<>();

        Pair beforeTrack = currentTrack;

        if (tracks.containsKey(heartRate))
            tracksInInterval.addAll(tracks.get(heartRate));
        int interval = 0;
        int lastInterval = interval;
        while (tracksInInterval.size() < 3 && interval < 100) {
            lastInterval = interval;
            interval += 10;
            for (int i = lastInterval; i < interval; i++)
                if (tracks.containsKey(heartRate - i)) {
                    tracksInInterval.addAll(tracks.get(heartRate - i));
                } else if (tracks.containsKey(heartRate + i)) {
                    tracksInInterval.addAll(tracks.get(heartRate + i));
                }
        }
       // currentTrack = new Pair(heartRate, (new Random()).nextInt(tracksInInterval.size()));
        if (tracksInInterval.size() != 0)
            return tracksInInterval.get((new Random()).nextInt(tracksInInterval.size())).track.uri;
        else return null;
    }

/*    private String previousTrack() {
        if(playedTracks.isEmpty())
            return null;

        currentTrack = playedTracks.pop();
        return tracks.get(currentTrack).track.uri;
    }
*/
    public void play() {
        if(isPaused) {
            isPaused = false;
            player.resume();
        } else {
            player.play(nextTrack());
        }

    }

    public void next() { player.play(nextTrack()); }
/*    public boolean prev() {
        String prevTrack = previousTrack();
        if(prevTrack == null)
            return false;

        player.play(prevTrack);
        return true;
    }
*/    public void pause(){ player.pause(); isPaused = true; }

    public String getCurrentTrackName() {
        return tracks.get(currentTrack.first).get((Integer) currentTrack.second).track.name;
    }

    public String getCurrentTrackArtists () {
        StringBuilder artists = new StringBuilder();
        for(ArtistSimple artist :tracks.get(currentTrack.first).get((Integer) currentTrack.second).track.artists) {
            if (artists.length() > 0) artists.append(", ");
            artists.append(artist.name);
        }
        return artists.toString();
    }

    public boolean existsCurrentTrack() {
        return currentTrack != null;
    }
}
