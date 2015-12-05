package beatify.labonappsdevelopment.beatify;

import android.content.Context;
import android.util.Log;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.Spotify;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;

/**
 * Created by mpreis on 05/12/15.
 */
public class BeatifyPlayer {
    protected static BeatifyPlayer beatifyPlayer;

    private Stack<Integer> playedTracks;
    private Integer currentTrack;
    private PlaylistSimple playlist;
    private List<PlaylistTrack> tracks;
    private Boolean isPaused;
    protected static Player player;


    private BeatifyPlayer() {}
    public BeatifyPlayer(PlaylistSimple pl) {
        playlist = pl;
        tracks = Utils.userPlaylistsTracks.get(playlist.id);
        isPaused = false;
        playedTracks = new Stack<Integer>();
    }

    private String nextTrack() {
        if(currentTrack != null)
            playedTracks.push(currentTrack);

        currentTrack = (new Random()).nextInt(tracks.size());
        return tracks.get(currentTrack).track.uri;
    }

    private String previousTrack() {
        if(playedTracks.isEmpty())
            return null;

        currentTrack = playedTracks.pop();
        return tracks.get(currentTrack).track.uri;
    }

    public void play() {
        if(isPaused) {
            isPaused = false;
            player.resume();
        } else {
            player.play(nextTrack());
        }

    }

    public void next() { player.play(nextTrack()); }
    public boolean prev() {
        String prevTrack = previousTrack();
        if(prevTrack == null)
            return false;

        player.play(prevTrack);
        return true;
    }
    public void pause(){ player.pause(); isPaused = true; }
}
