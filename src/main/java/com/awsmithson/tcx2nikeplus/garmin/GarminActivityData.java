package com.awsmithson.tcx2nikeplus.garmin;

import com.google.common.base.Preconditions;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


public class GarminActivityData {
	private @Nonnull Document tcxDocument;
	private @Nullable Document gpxDocument;

	public GarminActivityData(@Nonnull Document tcxDocument, @Nullable Document gpxDocument) {
		this.tcxDocument = Preconditions.checkNotNull(tcxDocument, "tcxDocument argument was null.");
		this.gpxDocument = gpxDocument;
	}

	public @Nonnull Document getTcxDocument() {
		return tcxDocument;
	}

	public @Nullable Document getGpxDocument() {
		return gpxDocument;
	}
}
