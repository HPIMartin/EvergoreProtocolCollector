package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.util.List;

public interface MetaInformationRepository {
	MetaInformationSnapshot snapshot();

	void add(List<? extends MetaInformation<?>> meta);
}
