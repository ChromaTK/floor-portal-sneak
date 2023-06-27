package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.delves.DelvesModifier;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class WeeklyMission {
	//Which week of the pass the mission is active
	public int mWeek;
	//XP granted for completing the mission
	public int mMP;
	//Type of mission - content, kills, etc
	public @Nullable WeeklyMissionType mType;
	//Times needed to do the thing (content clears, kills, room number, etc.)
	//This is what is tracked on the scoreboard
	public int mAmount;

	//Description shown in GUI
	public @Nullable String mDescription;

	//Unique fields for specific missions
	//Which piece of content to clear (matters for CONTENT or DISTANCE types)
	public @Nullable List<MonumentaContent> mContent;
	//Delve mission fields
	public @Nullable List<DelvesModifier> mDelveModifiers;
	public int mRotatingModifiersAmount;
	public int mModifierRank;
	public int mDelvePoints;
	//Region of content to clear (includes dungeon, strike, boss)
	public int mRegion;

	public WeeklyMission() {

	}

}
