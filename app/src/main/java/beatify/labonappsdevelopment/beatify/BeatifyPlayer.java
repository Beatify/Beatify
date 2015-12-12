package beatify.labonappsdevelopment.beatify;

import android.app.Activity;
import android.util.Log;
import android.util.Pair;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.Track;

public class BeatifyPlayer {
    // Holds the only BeatifyPlayer instance of the whole application.
    protected static BeatifyPlayer beatifyPlayer;

    private Pair<Integer, PlaylistTrack> currentTrack;          // Currently selected track (first=bpm, second=track)
    private PlaylistSimple playlist;                            // Currently selected playlist
    private HashMap<Integer, List<PlaylistTrack>> tracksByBpm;  // All tracks of playlist categorized by BPM
    private Boolean isPaused;                                   // Spotify/Beatify player status
    protected static Player player;                             // Spotify player

    private Random rand;

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

        player.clearQueue();
        if(trackUri != null)
            player.queue(trackUri);
        else
            addTracksToQueue();

        player.skipToNext();
        player.pause();
    }

    /**
     * Select tracks to play depending on the current heart rate of the user.
     * @return
     */
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

    /**
     * Start playing or resume current track.
     */
    public void play() { player.resume();}
    
    /**
     * Pause player
     */
    public void pause(){ player.pause(); }

    /**
     * Play next track in queue.
     */
    public void next() {
        addTracksToQueue();
        player.skipToNext();
    }



    /**
     * @return Name of current track.
     */
    public String getCurrentTrackName() {
        return currentTrack.second.track.name;
    }

    /**
     * @return Artists of current track.
     */
    public String getCurrentTrackArtists () {
        StringBuilder artists = new StringBuilder();
        for(ArtistSimple artist : currentTrack.second.track.artists) {
            if (artists.length() > 0) artists.append(", ");
            artists.append(artist.name);
        }
        return artists.toString();
    }

    /**
     * @return Bpm (beats per minute) of current track.
     */
    public Integer getCurrentTrackBpm() {
        return currentTrack.first;
    }

    /**
     * @return Image URL of current track.
     */
    public String getCurrentTrackImg() {
        return (currentTrack.second.track.album.images.size() > 2)
                ? currentTrack.second.track.album.images.get(2).url
                : null;
    }

    /**
     * @return Name of currently selected play list.
     */
    public String getCurrentPlaylistName() {
        return playlist.name;
    }

    /**
     * @return <code>true</code> if there is a current track (playlist selected or explicit track selected),
     *          else return <code>false</code>.
     */
    public boolean existsCurrentTrack() {
        return currentTrack != null;
    }

    /**
     * @return <code>true</code> if all tracks has been loaded, else <code>false</code>.
     */
    public boolean tracksLoaded() {
        return tracksByBpm != null;
    }

    /**
     * The BeatifyPlayer adopts the state of the given PlayerState object.
     * @param state
     */
    public void setPlayState(PlayerState state) { isPaused = !state.playing; }

    /**
     * @return <code>true</code> if player is paused, else <code>false</code>.
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Adds tracks to player queue.
     */
    public void addTracksToQueue() {
        for(String uri : getMoreTracks())
            player.queue(uri);
    }

    /**
     * Set current track by given Spotify URI. If URI is not found dummy values are set.
     * @param uri
     */
    public void setCurrentTrack(String uri) {
        currentTrack = null;
        for(Integer bpm : tracksByBpm.keySet())
            for(PlaylistTrack plt: tracksByBpm.get(bpm))
                if(uri.equals(plt.track.uri)) {
                    currentTrack = new Pair<Integer, PlaylistTrack>(bpm, plt);
                    break;
                }
        if(currentTrack == null)
            currentTrack = getDummyTrack();
    }

    /**
     * Initialize Spotify player.
     * @param a
     * @param csc
     * @param pnc
     */
    protected static void setupPlayer(Activity a,
                                      final ConnectionStateCallback csc,
                                      final PlayerNotificationCallback pnc) {
        if(player == null) {
            Config playerConfig = new Config(a, Utils.accessToken, Utils.CLIENT_ID);
            Spotify.getPlayer(playerConfig, a, new Player.InitializationObserver() {
                @Override
                public void onInitialized(Player p) {
                    player = p;
                    player.addConnectionStateCallback(csc);
                    player.addPlayerNotificationCallback(pnc);
                }

                @Override
                public void onError(Throwable throwable) {
                    Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                }
            });
        }
    }

    /**
     * @return A dummy track.
     */
    private Pair<Integer, PlaylistTrack> getDummyTrack() {

        AlbumSimple dummyAlbum = new AlbumSimple();
        dummyAlbum.name = "Unknown album";
        dummyAlbum.images = new ArrayList<>();

        PlaylistTrack dummyTrack = new PlaylistTrack();
        dummyTrack.track = new Track();
        dummyTrack.track.name = "Unknown track";
        dummyTrack.track.uri = "Unknown uri";
        dummyTrack.track.album = dummyAlbum;
        dummyTrack.track.artists = new ArrayList<>();

        return new Pair<Integer, PlaylistTrack>(
                Utils.DEFAULT_BPM, dummyTrack);
    }
}
