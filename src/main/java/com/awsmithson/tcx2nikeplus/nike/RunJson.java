package com.awsmithson.tcx2nikeplus.nike;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Utility class for (de)serializing objects to/from JSON format, using the GSON library.
 */
public class RunJson {
	private final @Nonnull BigDecimal distance;
	private final @Nonnull BigDecimal duration;
	private final long startTime;
	private final @Nonnull String timeZoneId;
	private final @Nonnull String type;
	private final @Nonnull List<Detail> detail;
	private final @Nonnull Summary summary;

	public RunJson(@Nonnull BigDecimal distance, @Nonnull BigDecimal duration, long startTime, @Nonnull String timeZoneId, @Nonnull String type, @Nonnull List<Detail> detail, @Nonnull Summary summary) {
		this.distance = Preconditions.checkNotNull(distance, "distance argument is null.");
		this.duration = Preconditions.checkNotNull(duration, "duration argument is null.");
		this.startTime = startTime;
		this.timeZoneId = Preconditions.checkNotNull(timeZoneId, "timeZoneId argument is null.");
		this.type = Preconditions.checkNotNull(type, "type argument is null.");
		this.detail = Preconditions.checkNotNull(detail, "detail argument is null.");
		this.summary = Preconditions.checkNotNull(summary, "summary argument is null.");
	}

	public static class Detail {
		private final @Nonnull String metricType;
		private final @Nonnull String intervalType;
		private final @Nonnull String intervalUnit;
		private final long startTimeOffset;
		private final @Nonnull String intervalMetric;
		private final @Nonnull String objType;
		private final @Nonnull List<BigDecimal> value;

		public Detail(@Nonnull String metricType,
					  @Nonnull String intervalType,
					  @Nonnull String intervalUnit,
					  long startTimeOffset,
					  @Nonnull String intervalMetric,
					  @Nonnull String objType,
					  @Nonnull List<BigDecimal> value) {
			this.metricType = Preconditions.checkNotNull(metricType, "metricType argument is null.");
			this.intervalType = Preconditions.checkNotNull(intervalType, "intervalType argument is null.");
			this.intervalUnit = Preconditions.checkNotNull(intervalUnit, "intervalUnit argument is null.");
			this.startTimeOffset = startTimeOffset;
			this.intervalMetric = Preconditions.checkNotNull(intervalMetric, "intervalMetric argument is null.");
			this.objType = Preconditions.checkNotNull(objType, "objType argument is null.");
			this.value = Preconditions.checkNotNull(value, "value argument is null.");
		}
	}

	public static class Summary {
		private final @Nonnull List<Snapshot> snapshots;
		private final @Nonnull List<DeviceConfig> deviceConfig;

		public Summary(@Nonnull List<Snapshot> snapshots, @Nonnull List<DeviceConfig> deviceConfig) {
			this.snapshots = Preconditions.checkNotNull(snapshots, "value argument is null.");
			this.deviceConfig = Preconditions.checkNotNull(deviceConfig, "value argument is null.");
		}

		public static class Snapshot {
			private final @Nonnull String name;
			private final @Nonnull List<DataSeries> dataSeries;

			public Snapshot(@Nonnull String name, @Nonnull List<DataSeries> dataSeries) {
				this.name = Preconditions.checkNotNull(name, "value argument is null.");
				this.dataSeries = Preconditions.checkNotNull(dataSeries, "dataSeries argument is null.");
			}
			
			public static class DataSeries {
				private final @Nonnull Metrics metrics;
				private final @Nonnull String objType;

				public DataSeries(@Nonnull Metrics metrics, @Nonnull String objType) {
					this.metrics = Preconditions.checkNotNull(metrics, "metrics argument is null.");
					this.objType = Preconditions.checkNotNull(objType, "objType argument is null.");
				}

                public static class Metrics {
                    private final int distance;
                    private final long duration;

                    public Metrics(int distance, long duration) {
                        this.distance = distance;
                        this.duration = duration;
                    }
                }
			}
		}

		public static class DeviceConfig {
			private final @Nonnull Component component;

			public DeviceConfig(@Nonnull Component component) {
				this.component = Preconditions.checkNotNull(component, "component argument is null.");
			}

			public static class Component {
				private final @Nonnull String type;
				private final @Nonnull String category;

				public Component(@Nonnull String type, @Nonnull String category) {
					this.type = Preconditions.checkNotNull(type, "type argument is null.");;
					this.category = Preconditions.checkNotNull(category, "category argument is null.");;
				}
			}
		}
	}
}
