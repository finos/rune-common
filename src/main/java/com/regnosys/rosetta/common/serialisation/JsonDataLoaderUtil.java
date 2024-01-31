package com.regnosys.rosetta.common.serialisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.reportdata.ExpectedResult;
import com.regnosys.rosetta.common.util.UrlUtils;
import com.rosetta.model.lib.ModelReportId;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public class JsonDataLoaderUtil {

    public static <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(UrlUtils.openURL(url), type);
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to " + type, e);
        }
    }

    public static <U> U readType(Class<U> type, ObjectMapper rosettaObjectMapper, String json) {
        try {
            return rosettaObjectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("JSON cannot be serialised to " + type + "[" + json + "]", e);
        }
    }

    public static <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, URL url) {
        try {
            return rosettaObjectMapper.readValue(UrlUtils.openURL(url), rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(url + " cannot be serialised to list of " + type, e);
        }
    }

    public static <U> List<U> readTypeList(Class<U> type, ObjectMapper rosettaObjectMapper, Reader input) {
        try {
            return rosettaObjectMapper.readValue(input, rosettaObjectMapper.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new RuntimeException(input + " cannot be serialised to list of " + type, e);
        }
    }

    public static <U> U fromObject(Object obj, Class<U> type, ObjectMapper rosettaObjectMapper) {
        try {
            return readType(type, rosettaObjectMapper, rosettaObjectMapper.writeValueAsString(obj));
        } catch (IOException e) {
            throw new RuntimeException(obj.getClass() + " cannot be serialised to " + type + "[" + obj.toString() + "]", e);
        }
    }

    public static Optional<Reader> openURL(URL descriptorUrl) {
        try {
            return Optional.of(UrlUtils.openURL(descriptorUrl));
        } catch (FileNotFoundException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Class<?> loadClass(String type, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for type " + type);
        }
    }

    public static class DataSet {
        private final static String EXPECTED_TYPE = ExpectedResult.class.getName();

        private String dataSetName;

        private String dataSetShortName;
        private String inputType;
        private List<ModelReportId> applicableReports;
        private List<DataItem> data;

        public DataSet(String dataSetName, String dataSetShortName, String inputType, List<ModelReportId> applicableReports, List<DataItem> data) {
            this.dataSetName = dataSetName;

            if(null != dataSetShortName && !dataSetShortName.isEmpty()){
                this.dataSetShortName = dataSetShortName;
            }
            else{
                this.dataSetShortName = dataSetName;
            }
            this.inputType = inputType;
            this.applicableReports = applicableReports;
            this.data = data;
        }

        public DataSet(String dataSetName, String inputType, List<ModelReportId> applicableReports, List<DataItem> data) {
            this.dataSetName = dataSetName;
            this.dataSetShortName = dataSetName;
            this.inputType = inputType;
            this.applicableReports = applicableReports;
            this.data = data;
        }

        public String getDataSetShortName() {
            return dataSetShortName;
        }

        public DataSet() {
        }

        public String getDataSetName() {
            return dataSetName;
        }

        public String getInputType() {
            return inputType;
        }

        public String getExpectedType() {
            return EXPECTED_TYPE;
        }

        public List<ModelReportId> getApplicableReports() {
            return applicableReports;
        }

        public List<DataItem> getData() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataSet that = (DataSet) o;
            return Objects.equals(dataSetName, that.dataSetName) &&
                    Objects.equals(dataSetShortName, that.dataSetShortName) &&
                    Objects.equals(inputType, that.inputType) &&
                    Objects.equals(applicableReports, that.applicableReports) &&
                    Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataSetName, dataSetShortName, inputType, EXPECTED_TYPE, applicableReports, data);
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", DataSet.class.getSimpleName() + "[", "]")
                    .add("dataSetName='" + dataSetName + "'")
                    .add("inputType='" + inputType + "'")
                    .add("expectedType='" + EXPECTED_TYPE + "'")
                    .add("applicableReports=" + applicableReports)
                    .add("data=" + data)
                    .toString();
        }
    }
}
