package com.awsmithson.tcx2nikeplus.convert;


import com.garmin.xmlschemas.trainingcenterdatabase.v2.ActivityT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.util.Date;

public class ConvertTcxV2 {

	public static @Nonnull Date getActivityStartTime(@Nonnull ActivityT activity) {
		Preconditions.checkNotNull(activity, "activity argument is null.");

		// We need to accept a timezone, or figure one out in here.

		return activity.getId().toGregorianCalendar().getTime();
	}
}
