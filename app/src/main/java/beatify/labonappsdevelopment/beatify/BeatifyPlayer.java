package beatify.labonappsdevelopment.beatify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;

/**
 * Created by mpreis on 05/12/15.
 */
public class BeatifyPlayer {
    protected static BeatifyPlayer beatifyPlayer;

    private Stack<Pair<Integer, Integer>> playedTracks;
    private Pair<Integer, Integer> currentTrack; // first: bpm second: track id
    private PlaylistSimple playlist;
    private HashMap<Integer, List<PlaylistTrack>> tracks;
    protected static Player player;
    protected Boolean isPaused;


    public BeatifyPlayer(PlaylistSimple pl) {
        playlist = pl;
        tracks = Utils.userPlaylistsTracks.get(playlist.id);
        isPaused = true;
        playedTracks = new Stack<Pair<Integer, Integer>>();
    }

    public BeatifyPlayer(PlaylistSimple pl, String trackName) {
        this(pl);

        Set<Integer> keys = tracks.keySet();
        for (int key : keys){
            for (int i = 0; i < tracks.get(key).size(); i++){
                if (tracks.get(key).get(i).track.name.equals(trackName)) {
                    currentTrack = new Pair<>(key, i);
                    return;
                }
            }
        }
    }

    private String nextTrack() {
        if (currentTrack != null){
            playedTracks.push(currentTrack);
        }

        Integer heartRate = DeviceScanActivity.mData;

        if (tracks == null){ return null; }

        ArrayList<PlaylistTrack> tracksInInterval = new ArrayList<>();

        if (heartRate == null || heartRate < 40)
            heartRate = (new Random()).nextInt(70) + 50;

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
        if (tracksInInterval.size() != 0)
            return tracksInInterval.get((new Random()).nextInt(tracksInInterval.size())).track.uri;
        else return null;
    }

    public void play() {
        if(isPaused && existsCurrentTrack()) player.resume();
        else {
            String nt = nextTrack();
            if(nt != null) player.play(nt);
        }
        isPaused = false;
    }

    public void next() {
        String nt = nextTrack();
        if(nt != null) player.play(nt);
    }
    public void pause(){ player.pause(); isPaused = true; }

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

    public String getCurrentTrackBpm() {
        return currentTrack.first.toString();
    }

    public String getCurrentTrackImg() {
        return tracks.get(currentTrack.first).get(
                (Integer) currentTrack.second).track.album.images.get(2).url;
    }

    public boolean existsCurrentTrack() {
        return currentTrack != null;
    }

    public boolean tracksLoaded() { return tracks != null; }
}
