package com.leobeliik.extremesoundmuffler.utils;

import com.leobeliik.extremesoundmuffler.interfaces.ISoundLists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

@SuppressWarnings("WeakerAccess")
public class Anchor {

    private final int id;
    private BlockPos anchorPos;
    private String name;
    private ResourceLocation dimension;
    private int Radius;
    private SortedMap<String, Double> muffledSounds = new TreeMap<>();

    public Anchor(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Anchor(int id, String name, BlockPos anchorPos, ResourceLocation dimension, int Radius, SortedMap<String, Double> muffledSounds) {
        this.id = id;
        this.name = name;
        this.anchorPos = anchorPos;
        this.dimension = dimension;
        this.Radius = Radius;
        this.muffledSounds = muffledSounds;
    }

    public BlockPos getAnchorPos() {
        return anchorPos;
    }

    private void setAnchorPos(BlockPos anchorPos) {
        this.anchorPos = anchorPos;
    }

    public int getAnchorId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRadius() {
        return Radius;
    }

    public void setRadius(int Radius) {
        this.Radius = Radius;
    }

    private void setName(String name) {
        this.name = name;
    }

    public SortedMap<ResourceLocation, Double> getMuffledSounds() {
        SortedMap<ResourceLocation, Double> temp = new TreeMap<>();
        this.muffledSounds.forEach((R, D) -> temp.put(new ResourceLocation(R), D));
        return temp;
    }

    public void setMuffledSounds(SortedMap<ResourceLocation, Double> muffledSounds) {
        muffledSounds.forEach((R, D) -> this.muffledSounds.put(R.toString(), D));
    }

    public void addSound(ResourceLocation sound, double volume) {
        muffledSounds.put(sound.toString(), volume);
    }

    public void replaceSound(ResourceLocation sound, double volume) {
        muffledSounds.replace(sound.toString(), volume);
    }

    public String getX() {
        return anchorPos != null ? String.valueOf(anchorPos.getX()) : "";
    }

    public String getY() {
        return anchorPos != null ? String.valueOf(anchorPos.getY()) : "";
    }

    public String getZ() {
        return anchorPos != null ? String.valueOf(anchorPos.getZ()) : "";
    }

    public ResourceLocation getDimension() {
        return dimension;
    }

    private void setDimension(ResourceLocation dimension) {
        this.dimension = dimension;
    }

    public void removeSound(ResourceLocation sound) {
        muffledSounds.remove(sound.toString());
    }

    public void setAnchor() {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        setAnchorPos(player.blockPosition());
        setDimension(player.clientLevel.dimension().location());
        setRadius(this.getRadius() == 0 ? 32 : this.getRadius());
    }

    public void deleteAnchor() {
        setName("Anchor " + this.getAnchorId());
        setAnchorPos(null);
        setDimension(null);
        setRadius(0);
        muffledSounds.clear();
    }

    public void editAnchor(String title, int Radius) {
        setName(title);
        setRadius(Radius);
    }

    public static Anchor getAnchor(SoundInstance sound) {
        BlockPos soundPos = new BlockPos((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
        for (Anchor anchor : ISoundLists.anchorList) {
            ClientLevel world = Minecraft.getInstance().level;
            if (anchor.getAnchorPos() != null
                    && world != null
                    && world.dimension().location().equals(anchor.getDimension())
                    && soundPos.closerThan(anchor.getAnchorPos(), anchor.getRadius())
                    && anchor.getMuffledSounds().containsKey(sound.getLocation())) {
                return anchor;
            }
        }
        return null;
    }
}