package de.robotricker.transportpipes.pipes;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jnbt.StringTag;
import org.jnbt.Tag;

import de.robotricker.transportpipes.pipeitems.PipeItem;
import de.robotricker.transportpipes.pipeutils.PipeColor;
import de.robotricker.transportpipes.pipeutils.PipeDirection;

public class ColoredPipe extends Pipe {

	private PipeColor pipeColor;

	public ColoredPipe(Location blockLoc, PipeColor pipeColor) {
		super(blockLoc);
		this.pipeColor = pipeColor;
	}

	@Override
	public PipeDirection itemArrivedAtMiddle(PipeItem item, PipeDirection before, List<PipeDirection> dirs) {
		return null;
	}

	@Override
	public void destroy(boolean dropPipeItem) {

	}

	public PipeColor getPipeColor() {
		return pipeColor;
	}

	@Override
	public PipeType getPipeType() {
		return PipeType.COLORED;
	}

	@Override
	protected List<ItemStack> getDroppedItems() {
		return null;
	}

	@Override
	public void saveToNBTTag(HashMap<String, Tag> tags) {
		super.saveToNBTTag(tags);
		tags.put("PipeColor", new StringTag("PipeColor", pipeColor.name()));
	}

}