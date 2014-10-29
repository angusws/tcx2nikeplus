package com.awsmithson.tcx2nikeplus.convert;


import com.awsmithson.tcx2nikeplus.http.Geonames;
import com.awsmithson.tcx2nikeplus.nike.RunJson;
import com.awsmithson.tcx2nikeplus.util.Log;
import com.garmin.xmlschemas.trackpointextension.v1.TrackPointExtensionT;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceCalculator;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.distance.GeodesicSphereDistCalc;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.impl.PointImpl;
import com.topografix.gpx._1._1.*;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

public class GpxToRunJson implements Converter<GpxType, RunJson> {

	private static final Log logger = Log.getInstance();

	private static final @Nonnull List<RunJson.Summary.DeviceConfig> DEFAULT_DEVICE_CONFIGS = ImmutableList.of(new RunJson.Summary.DeviceConfig(new RunJson.Summary.DeviceConfig.Component("iphone", "device")));
	private static final @Nonnull String METRIC_DATA_POINT = "dataPoint";

	/*
	Required output for json:
	    distance - km
	    duration - ms
	    starttime - ms since epoch.
	    timeZoneId - Europe/London
	    type - run
	 */
	@Override
	public RunJson convert(@Nonnull GpxType gpxDocument) throws ParserConfigurationException, SAXException, IOException {
		Preconditions.checkNotNull(gpxDocument, "gpxDocument argument is null.");

		logger.out("Converting GpxType to RunJson");
		SplineFunctions splineFunctions = generateSplines(gpxDocument);

		ImmutableList.Builder<RunJson.Detail> runJsonDetailBuilder = ImmutableList.<RunJson.Detail>builder().add(
				generateDetail("distance", "time", "sec", 0L, 10L, "dataStream", splineFunctions.durationToDistance, 3)
		);
		if (splineFunctions.durationToHeartRate != null) {
			runJsonDetailBuilder.add(
					generateDetail("heartrate", "time", "sec", 0L, 10L, "dataStream", splineFunctions.durationToHeartRate, 0)
			);
		}

		RunJson.Summary runJsonSummary = new RunJson.Summary(
				ImmutableList.of(
						generateSnaphot("mileSplit", splineFunctions.distanceToDuration, DistanceUtils.MILES_TO_KM),
						generateSnaphot("kmSplit", splineFunctions.distanceToDuration, 1)
				),
				DEFAULT_DEVICE_CONFIGS
		);

		BigDecimal maxDistance = new BigDecimal(getMaxX(splineFunctions.distanceToDuration)).setScale(6, RoundingMode.HALF_EVEN);
		BigDecimal maxDuration = new BigDecimal(getMaxX(splineFunctions.durationToDistance)).setScale(6, RoundingMode.HALF_EVEN);

		WptType firstTrkpt = getFirstTrkpt(gpxDocument);
		TimeZone timezone = Geonames.getTimeZone(firstTrkpt.getLon(), firstTrkpt.getLat());
		long startTime = firstTrkpt.getTime().toGregorianCalendar().getTimeInMillis();
		return new RunJson(maxDistance, maxDuration, startTime, timezone.getID(), "run", runJsonDetailBuilder.build(), runJsonSummary);
	}

	private double getMaxX(@Nonnull PolynomialSplineFunction splineFunction) {
		double[] knots = splineFunction.getKnots();
		return knots[knots.length - 1];
	}

	private @Nonnull RunJson.Detail generateDetail(@Nonnull String metricType,
												   @Nonnull String intervalType,
												   @Nonnull String intervalUnit,
												   long startTimeOffset,
												   long intervalMetric,
												   @Nonnull String objType,
												   @Nonnull PolynomialSplineFunction splineFunction,
												   int roundingScale) {
		logger.out(" - generating detail for %s", metricType);
		double[] knots = splineFunction.getKnots();
		double maxValue = knots[knots.length - 1];

		// IntervalMetric is seconds, we have our data stored as milliseconds, so multiply by 1000.
		long loopIncrement = intervalMetric * 1000;

		List<BigDecimal> values = new ArrayList<>();
		for (long i = loopIncrement; i <= maxValue; i += loopIncrement) {
			BigDecimal value = new BigDecimal(splineFunction.value(i));
			values.add(value.setScale(roundingScale, RoundingMode.HALF_EVEN));
		}

		return new RunJson.Detail(metricType, intervalType, intervalUnit, startTimeOffset, String.valueOf(intervalMetric), objType, values);
	}

	private @Nonnull RunJson.Summary.Snapshot generateSnaphot(@Nonnull String name, @Nonnull PolynomialSplineFunction splineFunction, double metricInterval) {
		logger.out(" - generating snapshot for %s", name);
		double[] knots = splineFunction.getKnots();
		int dataSeriesLength = (int) (knots[knots.length - 1] / metricInterval);

		List<RunJson.Summary.Snapshot.DataSeries> dataSeries = new ArrayList<>(dataSeriesLength);

		for (int i = 1; i <= dataSeriesLength; ++i) {
			long value = (long) splineFunction.value(i * metricInterval);
			dataSeries.add(createDataSeries(i, value, METRIC_DATA_POINT));
		}

		return new RunJson.Summary.Snapshot(name, dataSeries);
	}

	private @Nonnull RunJson.Summary.Snapshot.DataSeries createDataSeries(int distance, long duration, @Nonnull String objType) {
		RunJson.Summary.Snapshot.DataSeries.Metrics metric = new RunJson.Summary.Snapshot.DataSeries.Metrics(distance, duration);
		return new RunJson.Summary.Snapshot.DataSeries(metric, objType);
	}

	private @Nonnull WptType getFirstTrkpt(@Nonnull GpxType gpxDocument) {
		for (TrkType trks : gpxDocument.getTrk()) {
			for (TrksegType trkSeg : trks.getTrkseg()) {
				for (WptType trkpt : trkSeg.getTrkpt()) {
					if (isValidTrkpt(trkpt)) {
						return trkpt;
					}
				}
			}
		}

		throw new IllegalStateException("GPX document doesn't have any <trkpt/> elements, which are required.");
	}


	private static boolean isValidTrkpt(@Nonnull WptType trkpt) {
		return trkpt.getLon() != null && trkpt.getLat() != null && trkpt.getTime() != null;
	}

	private static final Predicate<WptType> IS_VALID_TRKPT = new Predicate<WptType>() {
		@Override
		public boolean apply(@Nullable WptType trkpt) {
			return trkpt != null && isValidTrkpt(trkpt);
		}
	};

	@Nonnull SplineFunctions generateSplines(@Nonnull GpxType gpxDocument) {
		Preconditions.checkNotNull(gpxDocument, "gpxDocument argument is null.");
		logger.out(" - generating splines");

		List<Long> durations = Lists.newArrayList(0L);
		List<Double> distances = Lists.newArrayList(0d);
		List<Short> heartRates = new ArrayList<>();

		DistanceCalculator distanceCalculator = new GeodesicSphereDistCalc.Vincenty();

		long totalPausedTime = 0;
		double totalDistance = 0;
		long workoutStartTime = 0;
		long previousDuration = 0;
		Point previousPoint = null;


		for (TrkType trks : gpxDocument.getTrk()) {

			for (TrksegType trkSeg : trks.getTrkseg()) {
				// Each <trkseg /> element  represents a period where the device is not paused.
				// We need at least 2 <trkpt /> elemnts to calculate anything useful.
				if (trkSeg.getTrkpt().size() > 1) {

					Iterator<WptType> trkptIt = Iterators.filter(trkSeg.getTrkpt().iterator(), IS_VALID_TRKPT);

					// Get the first <trkpt /> in this <trkSeg />
					WptType trkpt = trkptIt.next();

					// If this is our very first <trkpt /> in our workout...
					if (previousPoint == null) {
						workoutStartTime = trkpt.getTime().toGregorianCalendar().getTimeInMillis();
						previousPoint = getPoint(trkpt);

						// If we have heart rate data, add it
						Short heartRate = getHeartRate(trkpt);
						if (heartRate != null) {
							heartRates.add(heartRate);
						}
					} else {
						long duration = getMillisSinceWorkoutStart(trkpt.getTime(), workoutStartTime, totalPausedTime);
						long pausedTime = duration - previousDuration;
						totalPausedTime += pausedTime;
						previousPoint = getPoint(trkpt);
					}

					// Iterate through the remaining <trkpt />'s in this <trkseg />, adding the duration/distance for each.
					while (trkptIt.hasNext()) {
						trkpt = trkptIt.next();

						long duration = getMillisSinceWorkoutStart(trkpt.getTime(), workoutStartTime, totalPausedTime);

						if (duration > previousDuration) {
							Point point = getPoint(trkpt);
							if (!point.equals(previousPoint)) {
								durations.add(duration);

								double distanceKm = distanceCalculator.distance(previousPoint, point) * DistanceUtils.DEG_TO_KM;
								totalDistance += distanceKm;
								distances.add(totalDistance);

								// If we have heart rate data, add it
								Short heartRate = getHeartRate(trkpt);
								if (heartRate != null) {
									heartRates.add(heartRate);
								}

								previousDuration = duration;
								previousPoint = point;
							}
						}
					}
				}
			}
		}

		SplineInterpolator interpolator = new SplineInterpolator();
		PolynomialSplineFunction durationToDistanceFunction = interpolator.interpolate(Doubles.toArray(durations), Doubles.toArray(distances));
		PolynomialSplineFunction distanceToDurationFunction = interpolator.interpolate(Doubles.toArray(distances), Doubles.toArray(durations));
		PolynomialSplineFunction durationToHeartRateFunction = (heartRates.size() == durations.size())
				? interpolator.interpolate(Doubles.toArray(durations), Doubles.toArray(heartRates))
				: null;

		return new SplineFunctions(durationToDistanceFunction, distanceToDurationFunction, durationToHeartRateFunction);
	}


	private @Nullable Short getHeartRate(@Nonnull WptType trkpt) {
		ExtensionsType extensions = trkpt.getExtensions();
		if (extensions != null) {
			for (Object xsdAny : extensions.getAny()) {
				if (xsdAny instanceof JAXBElement<?>) {
					Object xsdAnyValue = ((JAXBElement) xsdAny).getValue();
					if (xsdAnyValue instanceof TrackPointExtensionT) {
						TrackPointExtensionT trackPointExtension = (TrackPointExtensionT) xsdAnyValue;
						if (trackPointExtension.getHr() != null) {
							return trackPointExtension.getHr();
						}
					}
				}
			}
		}
		return null;
	}


	private @Nonnull Point getPoint(@Nonnull WptType trkpt) {
		return new PointImpl(trkpt.getLon().doubleValue(), trkpt.getLat().doubleValue(), SpatialContext.GEO);
	}

	private long getMillisSinceWorkoutStart(@Nonnull XMLGregorianCalendar currentTime, long workoutStartTime, long totalTimePaused) {
		return currentTime.toGregorianCalendar().getTimeInMillis() - workoutStartTime - totalTimePaused;
	}


	private static class SplineFunctions {
		private final @Nonnull PolynomialSplineFunction durationToDistance;
		private final @Nonnull PolynomialSplineFunction distanceToDuration;
		private final @Nullable PolynomialSplineFunction durationToHeartRate;

		private SplineFunctions(@Nonnull PolynomialSplineFunction durationToDistance, @Nonnull PolynomialSplineFunction distanceToDuration, @Nullable PolynomialSplineFunction durationToHeartRate) {
			this.durationToDistance = Preconditions.checkNotNull(durationToDistance, "durationToDistance argument is null.");
			this.distanceToDuration = Preconditions.checkNotNull(distanceToDuration, "distanceToDuration argument is null.");
			this.durationToHeartRate = durationToHeartRate;
		}
	}
}
