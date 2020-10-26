package com.leobeliik.extremesoundmuffler.gui;

import com.leobeliik.extremesoundmuffler.Config;
import com.leobeliik.extremesoundmuffler.SoundMuffler;
import com.leobeliik.extremesoundmuffler.gui.buttons.MuffledSlider;
import com.leobeliik.extremesoundmuffler.gui.buttons.PlaySoundButton;
import com.leobeliik.extremesoundmuffler.utils.Anchor;
import com.leobeliik.extremesoundmuffler.utils.ISoundLists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class MainScreen extends Screen implements ISoundLists {

    private static final Minecraft minecraft = Minecraft.getInstance();
    public static final ResourceLocation GUI = new ResourceLocation(SoundMuffler.MODID, "textures/gui/sm_gui.png");
    private static final List<Anchor> anchors = new ArrayList<>();
    private static final List<Button> filteredButtons = new ArrayList<>();
    private static boolean isMuffling = true;
    private static String screenTitle = "";
    private static ITextComponent toggleSoundsListMessage;
    private final int xSize = 256;
    private final int ySize = 200;
    private final int colorWhite = 0xffffff;
    private final boolean isAnchorsDisabled = Config.getDisableAchors();
    private final ITextComponent emptyText = StringTextComponent.EMPTY;
    private final String mainTitle = "ESM - Main Screen";
    private int minYButton;
    private int maxYButton;
    private Button btnToggleMuffled;
    private Button btnDelete;
    private Button btnToggleSoundsList;
    private Button btnSetCoord;
    private Button btnEnableTitleEdit;
    private Button btnAccept;
    private Button btnCancel;
    private TextFieldWidget searchBar;
    private TextFieldWidget editTitleBar;
    private MuffledSlider volumeSlider;
    private Anchor anchor;

    private MainScreen() {
        super(new StringTextComponent(""));
    }

    private static void open(String title, ITextComponent message) {
        toggleSoundsListMessage = message;
        screenTitle = title;
        minecraft.displayGuiScreen(new MainScreen());
    }

    public static void open() {
        open("ESM - Main Screen", ITextComponent.getTextComponentOrEmpty("Recent"));
    }

    public static boolean isMuffled() {
        return isMuffling;
    }

    public static List<Anchor> getAnchors() {
        return anchors;
    }

    public static void setAnchor(Anchor anchor) {
        anchors.add(anchor);
    }

    @Nullable
    private static Anchor getAnchorByName(String name) {
        return anchors.stream().filter(a -> a.getName().equals(name)).findFirst().orElse(null);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        minecraft.getTextureManager().bindTexture(GUI);
        this.blit(matrix, getX(), getY(), 0, 0, xSize, ySize); //Main screen bounds
        drawCenteredString(matrix, font, screenTitle, getX() + 128, getY() + 8, colorWhite); //Screen title
        renderButtonsTextures(matrix, mouseX, mouseY, partialTicks);
        super.render(matrix, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        minYButton = getY() + 44;
        maxYButton = getY() + 164;

        addListener(btnToggleSoundsList = new Button(getX() + 23, getY() + 181, 43, 13, toggleSoundsListMessage, b -> {
            if (btnToggleSoundsList.getMessage().equals(ITextComponent.getTextComponentOrEmpty("Recent"))) {
                toggleSoundsListMessage = ITextComponent.getTextComponentOrEmpty("All");
            } else {
                toggleSoundsListMessage = ITextComponent.getTextComponentOrEmpty("Recent");
            }
            btnToggleSoundsList.setMessage(toggleSoundsListMessage);
            buttons.clear();
            open(screenTitle, toggleSoundsListMessage);
        }));

        addSoundButtons();

        addAnchors();

        addButton(btnToggleMuffled = new Button(getX() + 229, getY() + 179, 17, 17, emptyText, b -> isMuffling = !isMuffling)).setAlpha(0);

        addButton(btnDelete = new Button(getX() + 205, getY() + 179, 17, 17, emptyText, b -> {
                    anchor = getAnchorByName(screenTitle);
                    if (screenTitle.equals(mainTitle)) {
                        muffledSounds.clear();
                        open(mainTitle, btnToggleSoundsList.getMessage());
                    } else {
                        if (anchor == null) {
                            return;
                        }
                        anchor.getMuffledSounds().clear();
                        anchor.setAnchorPos(null);
                        anchor.setName("Anchor " + anchor.getId());
                        buttons.clear();
                        open(anchor.getName(), btnToggleSoundsList.getMessage());
                    }
                })
        ).setAlpha(0);

        addButton(btnSetCoord = new Button(getX() + 260, getY() + 42, 10, 10, emptyText, b ->
                Objects.requireNonNull(getAnchorByName(screenTitle)).setAnchorPos(getPlayerPos())));
        btnSetCoord.setAlpha(0);

        addButton(editTitleBar = new TextFieldWidget(font, getX() + 258, getY() + 59, 84, 13, emptyText));
        editTitleBar.visible = false;

        addButton(btnAccept = new Button(getX() + 259, getY() + 75, 40, 20, ITextComponent.getTextComponentOrEmpty("Accept"), b -> {
            anchor = getAnchorByName(screenTitle);
            if (!editTitleBar.getText().isEmpty() && anchor != null) {
                anchor.setName(editTitleBar.getText());
                screenTitle = editTitleBar.getText();
                editTitle();
            }
        })).visible = false;

        addButton(btnCancel = new Button(getX() + 300, getY() + 75, 40, 20, ITextComponent.getTextComponentOrEmpty("Cancel"), b -> editTitle())).visible = false;

        addButton(btnEnableTitleEdit = new Button(getX() + 274, getY() + 42, 10, 10, emptyText, b -> editTitle())).setAlpha(0);

        if (screenTitle.equals(mainTitle)) {
            btnSetCoord.visible = false;
            btnEnableTitleEdit.visible = false;
        }

        addButton(searchBar = new TextFieldWidget(font, getX() + 74, getY() + 183, 119, 13, emptyText));
        searchBar.setEnableBackgroundDrawing(false);

    }

    private void addSoundButtons() {
        int buttonH = minYButton;
        anchor = getAnchorByName(screenTitle);

        if (!screenTitle.equals(mainTitle) && anchor == null) {
            return;
        }

        if (btnToggleSoundsList.getMessage().equals(ITextComponent.getTextComponentOrEmpty("Recent"))) {
            if (screenTitle.equals(mainTitle) && !muffledSounds.isEmpty()) {
                soundsList.addAll(muffledSounds.keySet());
            } else if (anchor != null && !anchor.getMuffledSounds().isEmpty()) {
                soundsList.addAll(anchor.getMuffledSounds().keySet());
            }
        } else {
            soundsList.clear();
        }

        if (soundsList.isEmpty()) {
            return;
        }

        for (ResourceLocation sound : soundsList) {
            double volume = muffledSounds.get(sound) == null ? 1D : muffledSounds.get(sound);
            volumeSlider = new MuffledSlider(getX() + 11, buttonH + 2, 205, 11, volume, sound, screenTitle, anchor);

            boolean muffledAnchor = anchor != null && screenTitle.equals(anchor.getName()) && !anchor.getMuffledSounds().isEmpty() && anchor.getMuffledSounds().containsKey(sound);
            boolean muffledScreen = screenTitle.equals(mainTitle) && !muffledSounds.isEmpty() && muffledSounds.containsKey(sound);

            if (muffledAnchor || muffledScreen) {
                volumeSlider.setFGColor(0xffff00);
            }

            buttonH += volumeSlider.getHeightRealms() + 2;
            volumeSlider.visible = volumeSlider.y <= maxYButton;
            //btnPlaySound.visible = btnPlaySound.y <= maxYButton;
            //volumeSlider.visible = btnPlaySound.y <= maxYButton;

            //soundButtonList.put(btnToggleSound, btnPlaySound);
            //addButton(btnToggleSound).setAlpha(0);
            addButton(volumeSlider);
            addListener(volumeSlider.getBtnToggleSound());
            addListener(volumeSlider.getBtnPlaySound());

        }
        //soundButtonList.forEach((v, k) -> addButton(k));
    }

    private void addAnchors() {
        int buttonW = getX() + 30;
        for (int i = 0; i <= 9; i++) {
            Button btnAnchor;
            if (isAnchorsDisabled) {
                String[] disabledMsg = {"-", "D", "i", "s", "a", "b", "l", "e", "d", "-"};
                btnAnchor = new Button(buttonW, getY() + 24, 16, 16, ITextComponent.getTextComponentOrEmpty(String.valueOf(i)), b -> {
                });
                btnAnchor.setMessage(ITextComponent.getTextComponentOrEmpty(disabledMsg[i]));
                btnAnchor.active = false;
            } else {
                int finalI = i;
                btnAnchor = new Button(buttonW, getY() + 24, 16, 16, ITextComponent.getTextComponentOrEmpty(String.valueOf(i)), b -> {
                    anchor = anchors.get(finalI);
                    if (anchor == null) return;
                    if (screenTitle.equals(anchor.getName())) {
                        screenTitle = mainTitle;
                    } else {
                        screenTitle = anchor.getName();
                    }
                    buttons.clear();
                    open(screenTitle, btnToggleSoundsList.getMessage());
                });
                int colorGreen = 3010605;
                if (!anchors.isEmpty()) {
                    btnAnchor.setFGColor(anchors.get(Integer.parseInt(btnAnchor.getMessage().getString())).getAnchorPos() != null ? colorGreen : colorWhite);
                }
            }
            addButton(btnAnchor).setAlpha(0);
            buttonW += 20;
        }
    }

    private void renderButtonsTextures(MatrixStack matrix, double mouseX, double mouseY, float partialTicks) {
        int x; //start x point of the button
        int y; //start y point of the button
        int i = 0;
        float v; //start x point of the texture
        String message; //Button message
        int stringW; //text width
        int darkBG = -1325400064; //background color for Screen::fill()

        //Mute sound buttons and play sound buttons; Sound names
        if (buttons.size() < soundsList.size()) {
            return;
        }

/*
        for (Widget btn : soundButtonList.keySet()) {

            if (!btn.visible) {
                continue;
            }

            ResourceLocation rs = (ResourceLocation) soundsList.toArray()[i];
            i++;
            x = getX() + 14;
            y = btn.y + 4;
            stringW = 205;
            message = font.func_238412_a_(rs.getPath() + ":" + rs.getNamespace(), stringW); //trim to width
            String fullMessage = rs.getPath() + ":" + rs.getNamespace();
            //draws the name of the sound; sound name : mod name
            //font.drawString(matrix, message, x, y, btn.getFGColor());
            minecraft.getTextureManager().bindTexture(GUI);

            //if muffled
            v = btn.getFGColor() == colorViolet ? 213F : 202F;
            minecraft.getTextureManager().bindTexture(GUI);

            blit(matrix, btn.x + 12, btn.y + 2, 32F, 202F, 11, 11, xSize, xSize); //play button
            volumeSlider.renderGradient(matrix, volumeSlider.x, btn.y + 2, btn.x - 2, btn.y + 13); //Slider bg

            //render full names for the trim sound names when hovered
            */
/*if (font.getStringWidth(fullMessage) < stringW) {
                continue;
            }
            if (mouseX > x && mouseX < x + stringW && mouseY > y && mouseY < y + font.FONT_HEIGHT) {
                fill(matrix, x - 2, y - 2, x + font.getStringWidth(fullMessage) + 2, y + font.FONT_HEIGHT + 2, -16777215);
                font.drawString(matrix, fullMessage, x, y, btn.getFGColor());
            }*//*

        }
*/

        //Delete button
        x = btnDelete.x + 8;
        y = btnDelete.y;
        message = screenTitle.equals(mainTitle) ? "Delete Muffled List" : "Delete Anchor";
        stringW = font.getStringWidth(message) / 2;
        if (btnDelete.isHovered()) {
            fill(matrix, x - stringW - 2, y + 20, x + stringW + 2, y + 31, darkBG);
            drawCenteredString(matrix, font, message, x, y + 22, colorWhite);
        }


        //toggle muffled button
        x = btnToggleMuffled.x + 8;
        y = btnToggleMuffled.y;
        minecraft.getTextureManager().bindTexture(GUI);

        if (isMuffling) {
            blit(matrix, x - 8, y, 54F, 202F, 17, 17, xSize, xSize); //muffle button
        }

        message = isMuffling ? "Stop Muffling" : "Start Muffling";
        stringW = font.getStringWidth(message) / 2;
        if (btnToggleMuffled.isHovered()) {
            fill(matrix, x - stringW - 2, y + 20, x + stringW + 2, y + 31, darkBG);
            drawCenteredString(matrix, font, message, x, y + 22, colorWhite);
        }

        //Anchor coordinates and set coord button
        Anchor anchor = getAnchorByName(screenTitle);
        x = btnSetCoord.x;
        y = btnSetCoord.y;

        if (anchor != null) {
            int xW = font.getStringWidth(anchor.getX()) + font.getStringWidth("X: ");
            int yW = font.getStringWidth(anchor.getY()) + font.getStringWidth("Y: ");
            int zW = font.getStringWidth(anchor.getZ()) + font.getStringWidth("Z: ");
            stringW = Math.max(Math.max(xW, yW), Math.max(zW, 22));
            fill(matrix, x - 5, y - 36, x + stringW + 6, y + 16, darkBG);
            drawString(matrix, font, "X: " + anchor.getX(), x + 1, y - 30, colorWhite);
            drawString(matrix, font, "Y: " + anchor.getY(), x + 1, y - 20, colorWhite);
            drawString(matrix, font, "Z: " + anchor.getZ(), x + 1, y - 10, colorWhite);
            minecraft.getTextureManager().bindTexture(GUI);
            blit(matrix, x, y, 0, 69.45F, 11, 11, 88, 88); //set coordinates button
            blit(matrix, btnEnableTitleEdit.x, btnEnableTitleEdit.y, 32F, 213F, 11, 11, xSize, xSize); //change title button
        }

        if (btnSetCoord.isHovered() && !editTitleBar.visible) {
            fill(matrix, x - 5, y + 16, x + 62, y + 40, darkBG);
            font.drawString(matrix, "Set", x, y + 20, colorWhite);
            font.drawString(matrix, "coordinates", x, y + 30, colorWhite);
        }

        message = "Edit title";
        stringW = font.getStringWidth(message) + 2;

        if (btnEnableTitleEdit.isHovered() && !editTitleBar.visible) {
            fill(matrix, x - 5, y + 16, x + stringW + 2, y + 29, darkBG);
            font.drawString(matrix, message, x, y + 18, colorWhite);
        }

        //draw anchor buttons tooltip
/*
        for (int j = 0; j <= 9; j++) {
            Widget btn = buttons.get(soundsList.size() * 2 + j);
            x = btn.x + 8;
            y = btn.y;
            message = isAnchorsDisabled ? "Anchors are disabled" : anchors.get(j).getName();
            stringW = font.getStringWidth(message) / 2;

            if (btn.isHovered()) {
                fill(matrix, x - stringW - 2, y - 2, x + stringW + 2, y - 13, darkBG);
                drawCenteredString(matrix, font, message, x, y - 11, colorWhite);
            }
        }
*/

        //Toggle List button draw message
        x = btnToggleSoundsList.x;
        y = btnToggleSoundsList.y;
        message = btnToggleSoundsList.getMessage().getString();
        int centerText = x + (btnToggleSoundsList.getWidth() / 2) - (font.getStringWidth(message) / 2);
        font.drawString(matrix, message, centerText, y + 3, 0);
        String text = "Show " + message + " sounds";
        int textW = font.getStringWidth(text);
        int textX = x + (btnToggleSoundsList.getWidth() / 2) - (textW / 2) + 6;

        if (mouseX > x && mouseX < x + 43 && mouseY > y && mouseY < y + 13) {
            fill(matrix, textX - 2, y + 20, textX + textW + 2, y + 22 + font.FONT_HEIGHT, darkBG);
            font.drawString(matrix, text, textX, y + 22, colorWhite);
        }

        //Edit title background
        x = editTitleBar.x;
        y = editTitleBar.y;
        if (editTitleBar.visible) {
            fill(matrix, x - 2, y - 4, x + editTitleBar.getWidth() + 3, btnAccept.y + 22, darkBG);
        }

        //Draw Searchbar prompt text
        x = searchBar.x;
        y = searchBar.y;
        ITextComponent searchHint = (new TranslationTextComponent("gui.recipebook.search_hint")).mergeStyle(TextFormatting.ITALIC).mergeStyle(TextFormatting.GRAY); //Stolen from Vanilla ;)
        if (!this.searchBar.isFocused() && this.searchBar.getText().isEmpty()) {
            drawString(matrix, font, searchHint, x + 1, y, -1);
        }
    }

    private void editTitle() {
        btnAccept.visible = !btnAccept.visible;
        btnCancel.visible = !btnCancel.visible;
        editTitleBar.setText(screenTitle);
        editTitleBar.visible = !editTitleBar.visible;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double direction) {
        if (searchBar.getText().length() > 0) {
            return filteredScroll(filteredButtons.size(), direction);
        } else {
            return normalScroll(soundsList.size(), direction);
        }
    }

    private boolean normalScroll(int size, double direction) {
        if (size <= 10) {
            return false; //enough empty screen, no need for scroll
        }
        if (buttons.get(0).y >= minYButton && direction > 0f) {
            return false; //first button is on the top
        }
        if (buttons.get(size - 1).y <= maxYButton && direction < 0f) {
            return false; //last button is on the bottom
        }

/*
        soundButtonList.forEach((b, psb) -> {
            if (direction > 0) {
                b.y = b.y + (b.getHeightRealms() + 1);
                psb.y = psb.y + (b.getHeightRealms() + 1);
            } else {
                b.y = b.y - (b.getHeightRealms() + 1);
                psb.y = psb.y - (b.getHeightRealms() + 1);
            }

            if (b.y >= minYButton && b.y <= maxYButton) {
                b.visible = true;
                psb.visible = true;
            } else {
                b.visible = false;
                psb.visible = false;
            }
        });
*/
        return true;
    }

    private boolean filteredScroll(int size, double direction) {
        if (size <= 10) {
            return false; //enough empty screen, no need for scroll
        }
        if (filteredButtons.get(0).y >= minYButton && direction > 0f) {
            return false; //first button is on the top
        }
        if (filteredButtons.get(size - 1).y <= maxYButton && direction < 0f) {
            return false; //last button is on the bottom
        }

/*
        soundButtonList.forEach((b, psb) -> {
            if (direction > 0) {
                b.y = b.y + (b.getHeightRealms() + 1);
                psb.y = psb.y + (b.getHeightRealms() + 1);
            } else {
                b.y = b.y - (b.getHeightRealms() + 1);
                psb.y = psb.y - (b.getHeightRealms() + 1);
            }

            if (b.y >= minYButton && b.y <= maxYButton) {
                b.visible = true;
                psb.visible = true;
            } else {
                b.visible = false;
                psb.visible = false;
            }
        });
*/
        return true;
    }

    private void updateText() {
        int buttonH = getY() + 46;
        int i = 0;
        Object[] soundName = soundsList.toArray();
        filteredButtons.clear();
/*
            for (Button b : soundButtonList.keySet()) {
            Widget psb = soundButtonList.get(b);

            if (soundName[i].toString().contains(searchBar.getText())) {
                //b.active = true;
                b.y = buttonH;
                b.visible = b.y >= minYButton && b.y <= maxYButton;
                //psb.active = true;
                psb.y = buttonH;
                psb.visible = b.y >= minYButton && b.y <= maxYButton;
                filteredButtons.add(b);
                buttonH += b.getHeightRealms() + 1;
            } else {
                //b.active = true;
                b.visible = false;
                //psb.active = true;
                psb.visible = false;
            }
            i++;
        }
*/
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        //pressed "backspace" inside search bar
        if (keyCode == 259 && searchBar.isFocused()) {
            updateText();
            return super.keyReleased(keyCode, scanCode, modifiers);
        }
        //Type inside the search bar
        if (searchBar != null && searchBar.isFocused()) {
            updateText();
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int key1, int key2, int key3) {
        //Search bar & Edit title bar looses focus when pressed "Enter" or "Intro"
        if (key1 == 257 || key1 == 335) {
            searchBar.setFocused2(false);
            editTitleBar.setFocused2(false);
            return true;
        }
        //Close screen when press "E" or the mod hotkey outside the search bar or edit title bar
        if (!searchBar.isFocused() && !editTitleBar.isFocused() && (key1 == 69 || key1 == SoundMuffler.getHotkey())) {
            closeScreen();
            return true;
        }
        return super.keyPressed(key1, key2, key3);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (searchBar.isFocused()) {
                searchBar.setText("");
                updateText();
            }
            if (editTitleBar.isFocused()) {
                editTitleBar.setText("");
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        MuffledSlider.showSlider = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int getX() {
        return (this.width - xSize) / 2;
    }

    private int getY() {
        return (this.height - ySize) / 2;
    }

    private BlockPos getPlayerPos() {
        BlockPos player = Objects.requireNonNull(minecraft.player).getPosition();
        return new BlockPos(player.getX(), player.getY(), player.getZ());
    }

}