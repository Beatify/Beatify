package beatify.labonappsdevelopment.beatify;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.NavigationView;
import android.util.Log;
import android.util.Pair;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;

/**
 * Created by mpreis on 05/12/15.
 */
public class BeatifyPlayer {
    protected static BeatifyPlayer beatifyPlayer;

    // TODO: Set current Track!!!
    private Track currentTrack;
    private PlaylistSimple playlist;
    private Random rand;
    private HashMap<Integer, List<PlaylistTrack>> tracksByBpm;
    private HashMap<String, Track> tracksByUri;
    protected static Player player;
    protected Boolean isPaused;

    private final Integer MIN_SONGS = 3;
    private final Integer INTERVAL_BOUND = 100;
    private final Integer INTERVAL_STEPS = 10;


    public BeatifyPlayer(PlaylistSimple pl) { this(pl, null); }

    public BeatifyPlayer(PlaylistSimple pl, final String trackUri) {
        playlist = pl;
        isPaused = true;
        currentTrack = null;
        rand = new Random();

        tracksByBpm =
                (Utils.userPlaylistsTracks.get(playlist.id) != null)
                        ? Utils.userPlaylistsTracks.get(playlist.id)
                        : new HashMap<Integer, List<PlaylistTrack>>();

        tracksByUri = new HashMap<String, Track>();
        for(Integer bpmKey : tracksByBpm.keySet())
            for (PlaylistTrack track : tracksByBpm.get(bpmKey)) {
                Track tmp = new Track(bpmKey, track);
                tracksByUri.put(tmp.uri, tmp);
            }

        player.clearQueue();
        if(trackUri != null)
            player.queue(trackUri);
        else
            for(String tUri : getMoreTracks())
                player.queue(tUri);

        player.skipToNext();
        player.pause();
    }

    private List<String> getMoreTracks() {
        if (tracksByBpm.isEmpty()) return new ArrayList<String>();

        Integer heartRate = DeviceScanActivity.mData;
        ArrayList<Pair<Integer,PlaylistTrack>> tracksInInterval = new ArrayList<>();

        if (heartRate == null || heartRate < 40)
            heartRate = (new Random()).nextInt(70) + 50;

        if (tracksByBpm.containsKey(heartRate)) {
            while (tracksInInterval.size() < Math.min(MIN_SONGS, tracksByBpm.get(heartRate).size())) {
                Pair<Integer, PlaylistTrack> newTrack = new Pair<Integer, PlaylistTrack>(
                        heartRate,
                        tracksByBpm.get(heartRate).get(rand.nextInt(tracksByBpm.get(heartRate).size())));

                if(!tracksInInterval.contains(newTrack))
                    tracksInInterval.add(newTrack);
            }
        }

        for (int interval = 0, lastInterval = 0;
             tracksInInterval.size() < MIN_SONGS && interval < INTERVAL_BOUND;
             interval+=INTERVAL_STEPS)
        {
            for (int i = lastInterval; i < interval; i++)
                if (tracksByBpm.containsKey(heartRate - i)) {
                    for (int j = 0 ; j < tracksByBpm.get(heartRate - i).size(); j++){
                        tracksInInterval.add(new Pair<Integer,PlaylistTrack>(
                                heartRate - i,
                                tracksByBpm.get(heartRate - i).get(j)));
                    }
                } else if (tracksByBpm.containsKey(heartRate + i)) {
                    for (int j = 0 ; j < tracksByBpm.get(heartRate + i).size(); j++){
                        tracksInInterval.add(new Pair<Integer,PlaylistTrack>(
                                heartRate + i,
                                tracksByBpm.get(heartRate + i).get(j)));
                    }
                }

            lastInterval = interval;
        }

        List<String>nextTracks = new ArrayList<String>();
        // add at least MIN_SONGS tracks to the play queue
        for(int i = 0; i < MIN_SONGS && i < tracksInInterval.size()
                && ! tracksInInterval.isEmpty(); i++)
            nextTracks.add(tracksInInterval.remove(i).second.track.uri);

        // if there are still no tracks to play select a random song
        if(nextTracks.isEmpty()) {
            Set<Integer> keySet = tracksByBpm.keySet();
            Integer[] keys = new Integer[keySet.size()];
            keySet.toArray(keys);
            Integer key = keys[rand.nextInt(keys.length)];
            List<PlaylistTrack> plTracks = tracksByBpm.get(key);
            nextTracks.add(plTracks.get(rand.nextInt(plTracks.size())).track.uri);
        }

        return nextTracks;
    }

    public void play() { player.resume();}
    public void next() { player.skipToNext();}
    public void pause(){ player.pause(); }

    public String getCurrentTrackName() { return currentTrack.name; }
    public String getCurrentTrackArtists () { return currentTrack.artist; }
    public Integer getCurrentTrackBpm() { return currentTrack.bpm; }
    public String getCurrentTrackImg() { return currentTrack.imgUrl; }
    public boolean existsCurrentTrack() { return currentTrack != null; }
    public boolean tracksLoaded() { return tracksByBpm != null; }
    public void setPlayState(PlayerState state) { isPaused = !state.playing; }

    public void addNextTrack() {
        for(String uri : getMoreTracks())
            player.queue(uri);
    }

    public void setCurrentTrack(String uri) {
        currentTrack = tracksByUri.get(uri);
    }


    protected static void setupPlayer(Activity a,
                                      final ConnectionStateCallback csc,
                                      final PlayerNotificationCallback pnc) {
        Config playerConfig = new Config(a, Utils.accessToken, Utils.CLIENT_ID);
        Spotify.getPlayer(playerConfig, a, new Player.InitializationObserver() {
            @Override
            public void onInitialized(Player p) {
                BeatifyPlayer.player = p;
                BeatifyPlayer.player.addConnectionStateCallback(csc);
                BeatifyPlayer.player.addPlayerNotificationCallback(pnc);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
            }
        });
    }



    /// ///
    /// Helper class: Represents a track as one object.
    //////
    private class Track {
        private String name;
        private String artist;
        private String imgUrl;
        private String uri;
        private Integer bpm;

        Track () {
            name = "Unknown track";
            artist = "Unknown artist";
            imgUrl = null;
            uri = null;
            bpm = Utils.DEFAULT_BPM;
        }

        Track(Integer bpm, PlaylistTrack plTrack) {
            this.bpm = bpm;
            name = plTrack.track.name;
            uri = plTrack.track.uri;

            imgUrl = (plTrack.track.album.images.size() > 2)
                    ? plTrack.track.album.images.get(2).url
                    : null;


            StringBuilder artists = new StringBuilder();
            for(ArtistSimple artist : plTrack.track.artists) {
                if (artists.length() > 0) artists.append(", ");
                artists.append(artist.name);
            }
            artist = artists.toString();

        }

        Track(String name, String artist, String imgUrl, String uri, Integer bpm) {
            this.name = name;
            this.artist = artist;
            this.imgUrl = imgUrl;
            this.uri = uri;
            this.bpm = bpm;
        }
    }
}
