package com.awsmithson.tcx2nikeplus.jaxb;

import com.garmin.xmlschemas.trackpointextension.v1.TrackPointExtensionT;
import com.garmin.xmlschemas.trainingcenterdatabase.v2.TrainingCenterDatabaseT;
import com.google.common.base.Preconditions;
import com.topografix.gpx._1._1.GpxType;
import org.w3c.dom.Document;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringWriter;

public enum JAXBObject {
	GPX_TYPE {
		@Override
		@Nonnull Class[] getClassesToBeBound() {
			return new Class[] { GpxType.class, TrackPointExtensionT.class };
		}
	},

	TRAINING_CENTER_DATABASE {
		@Override
		@Nonnull Class[] getClassesToBeBound() {
			return new Class[] { TrainingCenterDatabaseT.class };
		}
	};

	abstract @Nonnull Class[] getClassesToBeBound();

	private final JAXBContext JAXB_CONTEXT = createJAXBContect();
	private @Nonnull JAXBContext createJAXBContect() {
		try {
			return JAXBContext.newInstance(getClassesToBeBound());
		} catch (JAXBException je) {
			throw new ExceptionInInitializerError(je);
		}
	}

	private final @Nonnull ThreadLocal<Marshaller> MARSHALLER = new ThreadLocal<Marshaller>() {
		protected synchronized Marshaller initialValue() {
			try {
				Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
				marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
				return marshaller;
			} catch (JAXBException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
	};

	private final @Nonnull ThreadLocal<Unmarshaller> UNMARSHALLER = new ThreadLocal<Unmarshaller>() {
		protected synchronized Unmarshaller initialValue() {
			try {
				return JAXB_CONTEXT.createUnmarshaller();
			} catch (JAXBException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
	};

	private @Nonnull Marshaller getMarshaller() {
		return MARSHALLER.get();
	}

	private @Nonnull Unmarshaller getUnmarshaller() {
		return UNMARSHALLER.get();
	}

	public void marshal(@Nonnull JAXBElement<?> jaxbElement, @Nonnull StringWriter stringWriter) throws JAXBException {
		Preconditions.checkNotNull(jaxbElement, "jaxbElement argument is null.");
		Preconditions.checkNotNull(stringWriter, "stringWriter argument is null.");
		getMarshaller().marshal(jaxbElement, stringWriter);
	}

	@Deprecated
	public void marshal(@Nonnull JAXBElement<?> jaxbElement, @Nonnull Document document) throws JAXBException {
		Preconditions.checkNotNull(jaxbElement, "jaxbElement argument is null.");
		Preconditions.checkNotNull(document, "document argument is null.");
		getMarshaller().marshal(jaxbElement, document);
	}

	public @Nonnull <T> T unmarshall(@Nonnull InputStream inputStream) throws JAXBException {
		Preconditions.checkNotNull(inputStream, "inputStream argument is null.");

		//noinspection unchecked
		return (T) getUnmarshaller().unmarshal(new StreamSource(inputStream), getClassesToBeBound()[0]).getValue();
	}
}
