package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RosettaCsvObjectMapper extends CsvMapper  {
    private final CsvSchema defaultSchema;

    public RosettaCsvObjectMapper() {
        this.defaultSchema = CsvSchema.emptySchema().withHeader();
    }

    public List<Map<String, String>> readCsv(String csv) throws IOException {
        try (MappingIterator<Map<String, String>> iterator = this.readerFor(Map.class).with(defaultSchema).readValues(csv)) {
            return iterator.readAll();
        }
    }

    public String writeCsv(Object value) throws IOException {
        return this.writer(defaultSchema).writeValueAsString(value);
    }

    public static RosettaCsvObjectMapper createCsvObjectMapper() {
        return (RosettaCsvObjectMapper) RosettaObjectMapperCreator.forCSV().create();
    }
}
