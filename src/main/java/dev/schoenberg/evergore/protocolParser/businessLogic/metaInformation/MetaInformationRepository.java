package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.util.List;
import java.util.Optional;

public interface MetaInformationRepository {
	<T> Optional<T> get(MetaInformationKey<T> key);

	<T> void add(List<MetaInformation<T>> meta);
}
