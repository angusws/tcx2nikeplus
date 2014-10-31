package com.awsmithson.tcx2nikeplus.servlet;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Servlets {

	private static final @Nonnull Pattern DIGIT_MATCHER = Pattern.compile("([0-9]+)");

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
	 * Extract the last integer we can find in the input String.  For eaxmple, all of these return {@code}34448379{@code}:
	 * <ul>
	 *     <li>{@code}34448379#{@code}</li>
	 *     <li>{@code}https://connect.garmin.com/activity/34448379{@code}</li>
	 *     <li>{@code}https://connect.garmin.com/activity/34448379?foo=bar{@code}</li>
	 *     <li>{@code}https://10.11.12.13/activity/34448379?foo=bar{@code}</li>
	 * </ul>
	 * @param input input to extract ID from.
	 * @return garmin activity-id.
	 */
	static int getGarminActivityId(@Nonnull String input) throws IOException {
		Preconditions.checkNotNull(input, "input argument is null.");

		// If it ends in hash, take that off - or maybe just grab the last set of numbers?
		Matcher matcher = DIGIT_MATCHER.matcher(input);
		if (matcher.find()) {
			matcher.reset();
			String lastDigits = "";
			while (matcher.find()) {
				lastDigits = matcher.group();
			}
			return Integer.parseInt(lastDigits);
		} else {
			throw new IOException(String.format("Unable to obtain garmin activity-id from '%s'", input));
		}

	}
}
