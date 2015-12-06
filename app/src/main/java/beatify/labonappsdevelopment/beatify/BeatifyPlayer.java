package beatify.labonappsdevelopment.beatify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;

/**
 * Created by mpreis on 05/12/15.
 */
public class BeatifyPlayer {
    protected static BeatifyPlayer beatifyPlayer;

    private Stack<Integer> playedTracks;
    private Integer currentTrack;
    private Integer startWithTrack;
    private PlaylistSimple playlist;
    private List<PlaylistTrack> tracks;
    protected Boolean isPaused;
    protected static Player player;


    public BeatifyPlayer(PlaylistSimple pl) {
        playlist = pl;
        tracks = Utils.userPlaylistsTracks.get(playlist.id);
        isPaused = true;
        playedTracks = new Stack<Integer>();
        startWithTrack = null;
    }

    public BeatifyPlayer(PlaylistSimple pl, String trackName) {
        this(pl);
        for(int i = 0; i < tracks.size() && startWithTrack == null; i++)
            if (tracks.get(i).track.name.equals(trackName))
                startWithTrack = i;
    }

    private String nextTrack() {
        if(currentTrack != null)
            playedTracks.push(currentTrack);

        if(startWithTrack != null)
            currentTrack = startWithTrack;
        else
            currentTrack = getNextTrackId();

        return getTrackUriById(currentTrack);
    }

    private String previousTrack() {
        if(playedTracks.isEmpty())
            return null;

        currentTrack = playedTracks.pop();
        return tracks.get(currentTrack).track.uri;
    }

    public void play() {
        if(isPaused && existsCurrentTrack()) player.resume();
        else player.play(nextTrack());
        isPaused = false;
    }

    public void next() { player.play(nextTrack()); }
    public boolean prev() {
        String prevTrack = previousTrack();
        if(prevTrack == null)
            return false;

        player.play(prevTrack);
        return true;
    }
    public void pause() { player.pause(); isPaused = true; }

    public String getCurrentTrackName() {
        return tracks.get(currentTrack).track.name;
    }

    public String getCurrentTrackArtists () {
        StringBuilder artists = new StringBuilder();
        for(ArtistSimple artist :tracks.get(currentTrack).track.artists) {
            if (artists.length() > 0) artists.append(", ");
            artists.append(artist.name);
        }
        return artists.toString();
    }

    public String getCurrentTrackBpm() {
        //TODO implement
        return "17";
    }

    public String getCurrentTrackImg() {
        return tracks.get(currentTrack).track.album.images.get(2).url;
    }

    public boolean existsCurrentTrack() {
        return currentTrack != null;
    }


    public void addNextTrack() {
        player.queue(getTrackUriById(getNextTrackId()));
    }

    private Integer getNextTrackId() {
        return (new Random()).nextInt(tracks.size());
    }

    private String getTrackUriById(Integer id) {
        return tracks.get(id).track.uri;
    }
}
