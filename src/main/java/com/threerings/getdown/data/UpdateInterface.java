package com.threerings.getdown.data;

import com.samskivert.text.MessageUtil;
import com.samskivert.util.StringUtil;

import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.threerings.getdown.Log.log;

/** Used to communicate information about the UI displayed when updating the application. */
public class UpdateInterface
{

    /** The human readable name of this application. */
    public String name;

    /** A background color, just in case. */
    public Color background = Color.white;

    /** Background image specifiers for {@link com.threerings.getdown.launcher.RotatingBackgrounds}. */
    public String[] rotatingBackgrounds;

    /** The error background image for {@link com.threerings.getdown.launcher.RotatingBackgrounds}. */
    public String errorBackground;

    /** The paths (relative to the appdir) of images for the window icon. */
    public String[] iconImages;

    /** The path (relative to the appdir) to a single background image. */
    public String backgroundImage;

    /** The path (relative to the appdir) to the progress bar image. */
    public String progressImage;

    /** The dimensions of the progress bar. */
    public Rectangle progress = new Rectangle(5, 5, 300, 15);

    /** The color of the progress text. */
    public Color progressText = Color.black;

    /** The color of the progress bar. */
    public Color progressBar = new Color(0x6699CC);

    /** The dimensions of the status display. */
    public Rectangle status = new Rectangle(5, 25, 500, 100);

    /** The color of the status text. */
    public Color statusText = Color.black;

    /** The color of the text shadow. */
    public Color textShadow;

    /** Where to point the user for help with install errors. */
    public String installError;

    /** The dimensions of the patch notes button. */
    public Rectangle patchNotes = new Rectangle(5, 50, 112, 26);

    /** The patch notes URL. */
    public String patchNotesUrl;

    /** The dimensions of the play again button. */
    public Rectangle playAgain;

    /** The path (relative to the appdir) to a single play again image. */
    public String playAgainImage;

    /** The global percentages for each step. A step may have more than one, and
     * the lowest reasonable one is used if a step is revisited. */
    public Map<Step, List<Integer>> stepPercentages =
        new EnumMap<Step, List<Integer>>(Step.class);

    public static UpdateInterface create(Configuration config) {
        // parse and return our application config
        UpdateInterface ui = new UpdateInterface();
        ui.name = config.getString("ui.name");
        ui.progress = config.getRectangle("ui.progress", ui.progress);
        ui.progressText = config.getColor("ui.progress_text", ui.progressText);
        ui.progressBar = config.getColor("ui.progress_bar", ui.progressBar);
        ui.status = config.getRectangle("ui.status", ui.status);
        ui.statusText = config.getColor("ui.status_text", ui.statusText);
        ui.textShadow = config.getColor("ui.text_shadow", ui.textShadow);
        ui.backgroundImage = config.getString("ui.background_image");
        if (ui.backgroundImage == null) { // support legacy format
            ui.backgroundImage = config.getString("ui.background");
        }

        // and now ui.background can refer to the background color, but fall back to black
        // or white, depending on the brightness of the progressText
        Color defaultBackground = (.5f < Color.RGBtoHSB(
                ui.progressText.getRed(), ui.progressText.getGreen(), ui.progressText.getBlue(),
                null)[2])
            ? Color.BLACK
            : Color.WHITE;
        ui.background = config.getColor("ui.background", defaultBackground);
        ui.progressImage = config.getString("ui.progress_image");
        ui.rotatingBackgrounds = config.getStringArray("ui.rotating_background");
        ui.iconImages = config.getStringArray("ui.icon");
        ui.errorBackground = config.getString("ui.error_background");

        // On an installation error, where do we point the user.
        String installError = config.getUrl("ui.install_error", null);
        ui.installError = (installError == null) ?
            "m.default_install_error" : MessageUtil.taint(installError);

        // the patch notes bits
        ui.patchNotes = config.getRectangle("ui.patch_notes", ui.patchNotes);
        ui.patchNotesUrl = config.getUrl("ui.patch_notes_url", null);

        // the play again bits
        ui.playAgain = config.getRectangle("ui.play_again", ui.playAgain);
        ui.playAgainImage = config.getString("ui.play_again_image");

        // step progress percentages
        for (Step step : Step.values()) {
            String spec = config.getString("ui.percents." + step.name());
            if (spec != null) {
                try {
                    ui.stepPercentages.put(step, Configuration.intsToList(StringUtil.parseIntArray(spec)));
                } catch (Exception e) {
                    log.warning("Failed to parse percentages for " + step + ": " + spec);
                }
            }
        }

        return ui;
    }

    /** Generates a string representation of this instance. */
    @Override
    public String toString ()
    {
        return "[name=" + name + ", bg=" + background + ", bg=" + backgroundImage +
            ", pi=" + progressImage + ", prect=" + progress + ", pt=" + progressText +
            ", pb=" + progressBar + ", srect=" + status + ", st=" + statusText +
            ", shadow=" + textShadow + ", err=" + installError + ", nrect=" + patchNotes +
            ", notes=" + patchNotesUrl + ", stepPercentages=" + stepPercentages +
            ", parect=" + playAgain + ", paimage=" + playAgainImage + "]";
    }

    /** Initializer */
    {
        for (Step step : Step.values()) {
            stepPercentages.put(step, step.defaultPercents);
        }
    }
}
