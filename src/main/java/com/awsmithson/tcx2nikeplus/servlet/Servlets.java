package com.awsmithson.tcx2nikeplus.servlet;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

public class Servlets {

	static @Nonnull String getRequiredParameter(@Nonnull HttpServletRequest request, @Nonnull String parameterKey) {
		Preconditions.checkNotNull(request, "request argument is null.");
		Preconditions.checkNotNull(parameterKey, "parameterKey argument is null.");
		return Preconditions.checkNotNull(request.getParameter(parameterKey), "%s parameter is missing", parameterKey);
	}

	static boolean requestParameterEquals(@Nonnull HttpServletRequest request, @Nonnull String parameterKey, @Nonnull String requiredValue) {
		Preconditions.checkNotNull(request, "request argument is null.");
		Preconditions.checkNotNull(parameterKey, "parameterKey argument is null.");
		Preconditions.checkNotNull(requiredValue, "requiredValue argument is null.");
		String value = request.getParameter(parameterKey);
		return ((value != null) && (value.equals(requiredValue)));
	}

	/**
	 * Split the string to obtain the activity-id in case the user
	 * enters the full url "http://connect.garmin.com/activity/23512599"
	 * instead of just the activityid "23512599".
	 * @param input
	 * @return
	 */
	static int getGarminActivityId(@Nonnull String input) {
		Preconditions.checkNotNull(input, "input argument is null.");
		return Integer.parseInt(input.substring(input.lastIndexOf('/') + 1));
	}
}
