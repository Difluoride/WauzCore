package eu.wauz.wauzcore.system;

import java.io.File;

import org.bukkit.entity.Player;

import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;

import eu.wauz.wauzcore.WauzCore;
import eu.wauz.wauzcore.players.WauzPlayerData;
import eu.wauz.wauzcore.players.WauzPlayerDataPool;

/**
 * Used for playing note block studio background songs to players.
 * 
 * @author Wauzmons
 */
public class WauzNoteBlockPlayer {
	
	/**
	 * A direct reference to the main class.
	 */
	private static WauzCore core = WauzCore.getInstance();
	
	/**
	 * Plays the soundtrack of their current world to the player.
	 * Specified by the "soundtrack.nbs" file in the world folder.
	 * 
	 * @param player The player that should hear the music.
	 */
	public static void play(Player player) {
		play(player, new File(player.getWorld().getWorldFolder().getAbsolutePath() + "/soundtrack.nbs"));
	}
	
	/**
	 * Plays a specific soundtrack to the player.
	 * Specified by the according ".nbs" file in the /Wauzcore/Music folder.
	 * 
	 * @param player The player that should hear the music.
	 * @param songName The name of the song, without ".nbs".
	 */
	public static void play(Player player, String songName) {
		play(player, new File(core.getDataFolder(), "Music/" + songName + ".nbs"));
	}
	
	/**
	 * Plays the given ".nbs" file to the player.
	 * If there is already a song runnig for the player, it is destroyed.
	 * 
	 * @param player The player that should hear the music.
	 * @param soundtrackFile The ".nbs" file.
	 */
	private static void play(Player player, File soundtrackFile) {
		WauzPlayerData playerData = WauzPlayerDataPool.getPlayer(player);
		if(playerData == null) {
			return;
		}
		
		SongPlayer currentSongPlayer = playerData.getSongPlayer();
		if(currentSongPlayer != null) {
			currentSongPlayer.destroy();
		}
		
		if(soundtrackFile.exists()) {
			Song song = NBSDecoder.parse(soundtrackFile);
			SongPlayer songPlayer = new RadioSongPlayer(song);
			songPlayer.setAutoDestroy(true);
			songPlayer.setRepeatMode(RepeatMode.ALL);
			songPlayer.setVolume((byte) 30);
			songPlayer.addPlayer(player);
			songPlayer.setPlaying(true);
			
			WauzDebugger.log(player, "Loaded Soundtrack: " + soundtrackFile.getAbsolutePath());
			playerData.setSongPlayer(songPlayer);
		}
		else {
			WauzDebugger.log(player, "No Soundtrack found: " + soundtrackFile.getAbsolutePath());
			playerData.setSongPlayer(null);
		}
	}

}
