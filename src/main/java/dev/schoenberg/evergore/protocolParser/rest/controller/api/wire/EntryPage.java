package dev.schoenberg.evergore.protocolParser.rest.controller.api.wire;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EntryPage<T>(@JsonProperty("page") int page, @JsonProperty("size") int size, @JsonProperty("totalCount") long totalCount, @JsonProperty("items") List<T> items) {}
