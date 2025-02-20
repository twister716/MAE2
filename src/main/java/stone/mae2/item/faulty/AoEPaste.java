package stone.mae2.item.faulty;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import stone.mae2.MAE2;
import stone.mae2.util.TransHelper;

public class AoEPaste extends FaultyCardMode {
    private static final String RADIUS = "radius";
    private static final int MAX_RADIUS = (16 + 1) / 2 - 1;
    
    private byte radius;

    public AoEPaste() {
        this.radius = 1;
    }
    
    @Override
    protected FaultyCardMode load(CompoundTag tag) {
    	this.radius = tag.getByte(RADIUS);
    	return this;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag data = super.save(tag);
        data.putByte(RADIUS, this.radius);
        return data;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> onItemUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        radius = (byte) (radius >= MAX_RADIUS ? 1 : radius + 1);
        this.save(stack.getOrCreateTag());
        player.displayClientMessage(Component.translatable(TransHelper.GUI.toKey("faulty", "radius"), 2 * radius + 1), true);
        MAE2.LOGGER.info("Radius: {}/{}", this.radius, MAX_RADIUS);
        return InteractionResultHolder.consume(stack);
    }
    
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof IPartHost partHost) {
            SelectedPart selectedPart = partHost.selectPartWorld(context.getClickLocation());
            if (selectedPart.part != null && selectedPart.side != null && stack.getItem() instanceof IMemoryCard card) {
                CompoundTag data = card.getData(stack);
                // 0 if it's along the normal of the plane of AoE
                // (the methods are accessing the normal variable)
                int x = selectedPart.side.getStepX() != 0 ? 0 : 1;
                int y = selectedPart.side.getStepY() != 0 ? 0 : 1;
                int z = selectedPart.side.getStepZ() != 0 ? 0 : 1;
                BlockPos clicked = context.getClickedPos().immutable();
                // TODO optimize the iteration, maybe something that just iterates radius^2 times and converts the index to coords
                for (int i = -radius * x; i <= radius * x; i++) {
                    for (int j = -radius * y; j <= radius * y; j++) {
                        for (int k = -radius * z; k <= radius * z; k++) {
                            BlockEntity aoeBE = level.getBlockEntity(clicked.offset(i, j, k));
                            MAE2.LOGGER.info("Iterated over Faulty Card!");
                            if (aoeBE instanceof IPartHost aoePartHost) {
                                IPart part = aoePartHost.getPart(selectedPart.side);
                                if (part != null) {
                                    // no idea what the Vec pos argument does here, doesn't seem used in any implementation
                                    part.onActivate(context.getPlayer(), context.getHand(), context.getClickLocation());
                                }
                            }
                        }
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public ResourceLocation getType() {
        return MAE2.toKey("aoe_paste");
    }

    @Override
    public int getTintColor() {
        return 0xFF000000 | 0x111111 * (int) (15 * ((double) radius / MAX_RADIUS));
    }

    @Override
    protected Component getName() {
        return Component.translatable(TransHelper.GUI.toKey("faulty", "aoe"), 2*radius+1);
    }
}
