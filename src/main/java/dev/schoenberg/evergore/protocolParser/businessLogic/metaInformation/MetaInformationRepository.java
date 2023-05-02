package dev.schoenberg.evergore.protocolParser.businessLogic.metaInformation;

import java.util.*;

public interface MetaInformationRepository {
	List<MetaInformation> get(String key);

	void add(List<MetaInformation> meta);
}
