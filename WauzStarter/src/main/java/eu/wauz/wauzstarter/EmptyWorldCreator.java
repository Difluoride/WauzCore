package eu.wauz.wauzstarter;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

/**
 * Represents various types of options that may be used to create an empty world.
 * 
 * @author Wauzmons
 */
public class EmptyWorldCreator extends WorldCreator {

	/**
	 * Creates an empty world creator for the given world name.
	 * 
	 * @param name The name of the world that will be created.
	 */
	public EmptyWorldCreator(String name) {
		super(name);
		generator(new ChunkGenerator() {
			
			@Override
			public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
				return createChunkData(world);
			}
			
		});
	}

}
