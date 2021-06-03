package com.leobeliik.extremesoundmuffler.network;

import com.leobeliik.extremesoundmuffler.interfaces.IAnchorList;
import com.leobeliik.extremesoundmuffler.utils.Anchor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class PacketDataClient implements IAnchorList {

    private final CompoundNBT data;

    PacketDataClient(PacketBuffer buf) {
        data = buf.readCompoundTag();
    }

    public PacketDataClient(CompoundNBT data) {
        this.data = data;
    }

    void toBytes(PacketBuffer buf) {
        buf.writeCompoundTag(data);
    }

    @SuppressWarnings("SameReturnValue")
    boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            IntStream.rangeClosed(0, 9).forEach(i -> {
                if (!data.contains("anchor" + i)) {
                    anchorList.add(i, new Anchor(i, "Anchor: " + i));
                } else {
                    anchorList.add(i, deserializeNBT(data.getCompound("anchor" + i)));
                }
            });
        });
        return true;
    }

    public static Anchor deserializeNBT(CompoundNBT nbt) {
        SortedMap<String, Float> muffledSounds = new TreeMap<>();
        CompoundNBT muffledNBT = nbt.getCompound("MUFFLED");

        for (String key : muffledNBT.keySet()) {
            muffledSounds.put(key, muffledNBT.getFloat(key));
        }

        if (!nbt.contains("POS")) {
            return new Anchor(nbt.getInt("ID"), nbt.getString("NAME"));
        } else {
            return new Anchor(nbt.getInt("ID"),
                    nbt.getString("NAME"),
                    NBTUtil.readBlockPos(nbt.getCompound("POS")),
                    new ResourceLocation(nbt.getString("DIM")),
                    nbt.getInt("RAD"),
                    muffledSounds);
        }
    }
}