package com.rate.boardcastmod.config;

import com.rate.boardcastmod.BoardcastMod;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

@Config(name = BoardcastMod.MOD_ID)
public class BoardcastConfig implements ConfigData {

    // ---------------------------------------------------------------------
    // CSV export (Scoreboard Helper compatible, event-driven + polling)
    // ---------------------------------------------------------------------

    @ConfigEntry.Category("export")
    @ConfigEntry.Gui.Tooltip
    public boolean exportEnabled = true;

    @ConfigEntry.Category("export")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 1200)
    @ConfigEntry.Gui.Tooltip
    public int exportIntervalTicks = 1;

    @ConfigEntry.Category("export")
    @ConfigEntry.Gui.Tooltip
    @FilePath
    public String exportCsvPath = "config/boardcastmod/scoreboard.csv";

    @ConfigEntry.Category("export")
    @ConfigEntry.Gui.Tooltip
    public boolean exportAllObjectives = true;

    @ConfigEntry.Category("export")
    @ConfigEntry.Gui.Tooltip
    public boolean exportOnlySidebarEntries = true;

    @ConfigEntry.Category("export")
    public boolean exportIncludeHeader = true;

    @ConfigEntry.Category("export")
    @ConfigEntry.Gui.Tooltip
    public boolean exportHelperCsv = true;

    @ConfigEntry.Category("export")
    @ConfigEntry.Gui.Tooltip
    @FilePath
    public String exportHelperCsvPath = "config/boardcastmod/scoreboard_helper.csv";

    // ---------------------------------------------------------------------
    // Sidebar rendering (Scoreboard Overhaul style)
    // ---------------------------------------------------------------------

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.Gui.Tooltip
    public boolean showSidebar = true;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.Gui.Tooltip
    public boolean useCustomStyle = true;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.Gui.Tooltip
    public boolean obsOverlayAutoHide = true;

    @ConfigEntry.Category("sidebar")
    public boolean showTitle = true;

    @ConfigEntry.Category("sidebar")
    public String sidebarTitleOverride = "";

    @ConfigEntry.Category("sidebar")
    public boolean showScoreHolder = true;

    @ConfigEntry.Category("sidebar")
    public boolean showScoreValue = true;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.Gui.Tooltip
    public boolean compactScoreValues = true;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.Gui.Tooltip
    public boolean truncateScoreNames = true;

    @ConfigEntry.Category("sidebar")
    public boolean showTeamAffixes = true;

    @ConfigEntry.Category("sidebar")
    public boolean renderBackground = true;

    @ConfigEntry.Category("sidebar")
    public boolean sidebarTextShadow = true;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
    public int maxRows = 15;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = 8, max = 20)
    public int rowHeight = 10;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = -1000, max = 1000)
    public int sidebarXOffset = 0;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = -1000, max = 1000)
    public int sidebarYOffset = 0;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 400)
    public int sidebarRightMargin = 4;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 400)
    public int sidebarTopMargin = 4;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 800)
    public int sidebarMinWidth = 0;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 2000)
    public int sidebarMaxWidth = 0;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.BoundedDiscrete(min = 25, max = 400)
    @ConfigEntry.Gui.Tooltip
    public int sidebarScale = 100;

    @ConfigEntry.Category("sidebar")
    public String titleColor = "#FF5555";

    @ConfigEntry.Category("sidebar")
    public String scoreColor = "#FF5555";

    @ConfigEntry.Category("sidebar")
    public String holderColor = "#FFFFFF";

    @ConfigEntry.Category("sidebar")
    public String backgroundColor = "#66000000";

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HealthDisplayMode healthDisplay = HealthDisplayMode.AUTO;

    @ConfigEntry.Category("sidebar")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HeartDisplayStyle heartDisplayStyle = HeartDisplayStyle.ICONS;

    @ConfigEntry.Category("sidebar")
    public String fullHeartSymbol = "♥";

    @ConfigEntry.Category("sidebar")
    public String halfHeartSymbol = "♥";

    @ConfigEntry.Category("sidebar")
    public String fullHeartColor = "#FF5555";

    @ConfigEntry.Category("sidebar")
    public String halfHeartColor = "#555555";

    // ---------------------------------------------------------------------
    // Timer scoreboard support
    // ---------------------------------------------------------------------

    @ConfigEntry.Category("timer")
    public boolean timerEnabled = true;

    @ConfigEntry.Category("timer")
    @ConfigEntry.Gui.Tooltip
    public boolean timerUseSeconds = false;

    @ConfigEntry.Category("timer")
    @ConfigEntry.Gui.Tooltip
    public boolean timerAssumeTicks = true;

    @ConfigEntry.Category("timer")
    @ConfigEntry.BoundedDiscrete(min = 1, max = 200)
    public int timerTicksPerSecond = 20;

    @ConfigEntry.Category("timer")
    @ConfigEntry.Gui.Tooltip
    public boolean timerUseClockFormat = false;

    @ConfigEntry.Category("timer")
    @ConfigEntry.Gui.Tooltip
    public List<String> timerObjectiveKeywords = new ArrayList<>(List.of(
            "timer", "time", "clock", "计时", "倒计时", "cd"
    ));

    // ---------------------------------------------------------------------
    // Chat capture
    // ---------------------------------------------------------------------

    @ConfigEntry.Category("chat")
    public boolean chatCaptureEnabled = true;

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    public List<String> chatPatterns = new ArrayList<>(List.of(".*"));

    @ConfigEntry.Category("chat")
    public boolean chatRegexCaseInsensitive = true;

    @ConfigEntry.Category("chat")
    public boolean chatCaptureGameMessages = true;

    @ConfigEntry.Category("chat")
    public boolean chatCaptureChatMessages = true;

    @ConfigEntry.Category("chat")
    public boolean chatCaptureSystemMessages = true;

    @ConfigEntry.Category("chat")
    public boolean chatCaptureOutgoingMessages = true;

    @ConfigEntry.Category("chat")
    public boolean chatIncludeKindPrefix = true;

    @ConfigEntry.Category("chat")
    public boolean chatIncludeTimestamp = true;

    @ConfigEntry.Category("chat")
    @ConfigEntry.Gui.Tooltip
    @FilePath
    public String chatCapturePath = "config/boardcastmod/chat_capture.txt";

    @ConfigEntry.Category("chat")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1048576)
    @ConfigEntry.Gui.Tooltip
    public int chatMaxFileSizeKb = 1024;
}
