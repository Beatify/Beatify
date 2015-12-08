package beatify.labonappsdevelopment.beatify;

import android.util.Pair;
import com.spotify.sdk.android.player.Player;

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

    private Pair<Integer, PlaylistTrack> currentTrack;      // first: bpm second: track id
    private Integer currentTrackBpm;
    private String currentTrackName;
    private String currentTrackArtists;
    private String currentTrackImg;
    private String currentTrackUri;
    private Queue<Pair<Integer, PlaylistTrack>> nextTracks;   // pair first: bpm second: track id
    private PlaylistSimple playlist;
    private Random rand;
    private HashMap<Integer, List<PlaylistTrack>> tracks;
    protected static Player player;
    protected Boolean isPaused;

    private final Integer MIN_SONGS = 3;
    private final Integer INTERVAL_BOUND = 100;
    private final Integer INTERVAL_STEPS = 10;


    public BeatifyPlayer(PlaylistSimple pl) {
        playlist = pl;
        tracks = Utils.userPlaylistsTracks.get(playlist.id);
        isPaused = true;
        currentTrack = null;
        nextTracks = new LinkedList<Pair<Integer, PlaylistTrack>>();
        rand = new Random();
        player.clearQueue();


        currentTrackBpm = Utils.DEFAULT_BPM;
        currentTrackName = "";
        currentTrackArtists = "";
        currentTrackImg = null;
        currentTrackUri = "";
    }

    public BeatifyPlayer(PlaylistSimple pl, String trackName) {
        this(pl);

        Set<Integer> keys = tracks.keySet();
        for (int key : keys)
            for (PlaylistTrack t : tracks.get(key))
                if (t.track.name.equals(trackName))
                    nextTracks.add(new Pair<Integer, PlaylistTrack>(key, t));
    }

    private String nextTrack() {
        if( ! nextTracks.isEmpty() && nextTracks.size() > 1 ) {
            currentTrack = nextTracks.remove();
            setCurrentTrack(currentTrack);
            return currentTrackUri;
        }

        // else compute next tracks
        addMoreTracks();

        currentTrack = nextTracks.remove();
        setCurrentTrack(currentTrack);
        return currentTrackUri;
    }

    private void addMoreTracks() {
        if (tracks == null) return;


        Integer heartRate = DeviceScanActivity.mData;
        ArrayList<Pair<Integer,PlaylistTrack>> tracksInInterval = new ArrayList<>();

        if (heartRate == null || heartRate < 40)
            heartRate = (new Random()).nextInt(70) + 50;

        if (tracks.containsKey(heartRate)) {
            while (tracksInInterval.size() < Math.min(MIN_SONGS, tracks.get(heartRate).size())) {
                Pair<Integer, PlaylistTrack> newTrack = new Pair<Integer, PlaylistTrack>(
                        heartRate,
                        tracks.get(heartRate).get(rand.nextInt(tracks.get(heartRate).size())));

                if(!tracksInInterval.contains(newTrack))
                    tracksInInterval.add(newTrack);
            }
        }

        for (int interval = 0, lastInterval = 0;
             tracksInInterval.size() < MIN_SONGS && interval < INTERVAL_BOUND;
             interval+=INTERVAL_STEPS)
        {
            for (int i = lastInterval; i < interval; i++)
                if (tracks.containsKey(heartRate - i)) {
                    for (int j = 0 ; j < tracks.get(heartRate - i).size(); j++){
                        tracksInInterval.add(new Pair<Integer,PlaylistTrack>(
                                heartRate - i,
                                tracks.get(heartRate - i).get(j)));
                    }
                } else if (tracks.containsKey(heartRate + i)) {
                    for (int j = 0 ; j < tracks.get(heartRate + i).size(); j++){
                        tracksInInterval.add(new Pair<Integer,PlaylistTrack>(
                                heartRate + i,
                                tracks.get(heartRate + i).get(j)));
                    }
                }

            lastInterval = interval;
        }

        // add at least MIN_SONGS tracks to the play queue
        for(int i = 0; i < MIN_SONGS && i < tracksInInterval.size()
                && ! tracksInInterval.isEmpty(); i++)
            nextTracks.add(tracksInInterval.remove(i));

        // if there are still no tracks to play select a random song
        if(nextTracks.isEmpty()) {
            Set<Integer> keySet = tracks.keySet();
            Integer[] keys = new Integer[keySet.size()];
            keySet.toArray(keys);
            Integer key = keys[rand.nextInt(keys.length)];
            List<PlaylistTrack> plTracks = tracks.get(key);
            nextTracks.add(new Pair<Integer, PlaylistTrack>(
                    key,
                    plTracks.get(rand.nextInt(plTracks.size()))
            ));
        }
    }


    public void play() {
        if(isPaused && existsCurrentTrack()) {
            player.resume();
            isPaused = false;
        }
        else {
            String nt = nextTrack();
            if(nt != null) {
                player.play(nt);
                isPaused = false;
            }
            else
                isPaused = true;
        }
    }

    public void next() {
        String nt = nextTrack();
        if(nt != null) {
            player.play(nt);
            isPaused = false;
        }
    }
    public void pause(){ player.pause(); isPaused = true; }

    public String getCurrentTrackName() { return currentTrackName; }

    public String getCurrentTrackArtists () { return currentTrackArtists; }
    public Integer getCurrentTrackBpm() { return currentTrackBpm; }
    public String getCurrentTrackImg() { return currentTrackImg; }
    public boolean existsCurrentTrack() { return currentTrack != null; }
    public boolean tracksLoaded() { return tracks != null; }

    public void addNextTrack() {
        addMoreTracks();
        // peek required; element is remove when play is performed.
        player.queue(nextTracks.peek().second.track.uri);
    }

    private void setCurrentTrack(Pair<Integer, PlaylistTrack> t) {
        currentTrackBpm = currentTrack.first;
        currentTrackName = currentTrack.second.track.name;
        currentTrackUri = currentTrack.second.track.uri;

        currentTrackImg = (currentTrack.second.track.album.images.size() > 2)
            ? currentTrack.second.track.album.images.get(2).url
            : null;


        StringBuilder artists = new StringBuilder();
        for(ArtistSimple artist :currentTrack.second.track.artists) {
            if (artists.length() > 0) artists.append(", ");
            artists.append(artist.name);
        }
        currentTrackArtists = artists.toString();

    }
}
