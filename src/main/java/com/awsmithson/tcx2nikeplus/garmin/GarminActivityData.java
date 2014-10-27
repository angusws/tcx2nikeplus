package com.awsmithson.tcx2nikeplus.garmin;

import com.google.common.base.Preconditions;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Deprecated
public class GarminActivityData {
	private final @Nonnull Document tcxDocument;
	private final @Nullable Document gpxDocument;

	public GarminActivityData(@Nonnull Document tcxDocument, @Nullable Document gpxDocument) {
		this.tcxDocument = Preconditions.checkNotNull(tcxDocument, "tcxDocument argument is null.");
		this.gpxDocument = gpxDocument;
	}

	public @Nonnull Document getTcxDocument() {
		return tcxDocument;
	}

	public @Nullable Document getGpxDocument() {
		return gpxDocument;
	}
}
