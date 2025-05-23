package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.rosetta.model.lib.ModelSymbolId;
import com.rosetta.util.serialisation.RosettaXMLConfiguration;

import javax.xml.stream.XMLStreamReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;

import com.rosetta.util.serialisation.TypeXMLConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Custom XmlMapper that extends the standard XmlMapper and overrides the base readValue method
 * to delegate to the super class.
 */
public class RosettaXmlMapper extends XmlMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(RosettaXmlMapper.class);
    private static final long serialVersionUID = 1L;
    private RosettaXMLConfiguration config;

    public RosettaXmlMapper(JacksonXmlModule module) {
        super(module);
    }

    public RosettaXmlMapper(RosettaXMLConfiguration config) {
        this((JacksonXmlModule) null);
        this.config = config;
    }

    private <T> T readValueInternal(String content, Class<T> valueType) throws IOException, ParserConfigurationException, SAXException {
        String outermostElementName = getOutermostElementName(content);
        Optional<Class<T>> valueTypeForElement = getValueTypeForElement(outermostElementName);
        if (valueTypeForElement.isPresent()) {
            return super.readValue(content, valueTypeForElement.get());
        }
        throw new IOException();
    }

    private <T> T readValueInternal(String content, JavaType valueType) throws IOException, ParserConfigurationException, SAXException {
        String outermostElementName = getOutermostElementName(content);
        Optional<Class<T>> valueTypeForElement = getValueTypeForElement(outermostElementName);
        if (valueTypeForElement.isPresent()) {
            return super.readValue(content, valueTypeForElement.get());
        }
        throw new IOException();
    }

    @Override
    public <T> T readValue(String content, Class<T> valueType) throws JsonProcessingException {
        try {
            return readValueInternal(content, valueType);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            return super.readValue(content, valueType);
        }
    }

    @Override
    public <T> T readValue(String content, JavaType valueType) throws JsonProcessingException {
        try {
            return readValueInternal(content, valueType);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            LOGGER.error("Failed to parse XML content", e);
            return super.readValue(content, valueType);
        }
    }

//    @Override
//    public <T> T readValue(File src, Class<T> valueType) throws IOException {
//        String content = convertFileToString(src);
//        try {
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(File src, JavaType valueType) throws IOException {
//        String content = convertFileToString(src);
//        try {
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(URL src, Class<T> valueType) throws IOException {
//        try {
//            String content = convertFileToString(new File(src.toURI()));
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException | URISyntaxException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(URL src, JavaType valueType) throws IOException {
//        try {
//            String content = convertFileToString(new File(src.toURI()));
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException | URISyntaxException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//
//    @Override
//    public <T> T readValue(Reader src, Class<T> valueType) throws IOException {
//        try {
//            String content = convertReaderToString(src);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(Reader src, JavaType valueType) throws IOException {
//        try {
//            String content = convertReaderToString(src);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(byte[] src, Class<T> valueType) throws IOException {
//        try {
//            String content = new String(src);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(byte[] src, JavaType valueType) throws IOException {
//        try {
//            String content = new String(src);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(InputStream src, Class<T> valueType) throws IOException {
//        try {
//            String content = convertInputStreamToString(src);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(InputStream src, JavaType valueType) throws IOException {
//        try {
//            String content = convertInputStreamToString(src);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(src, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(XMLStreamReader r, Class<T> valueType) throws IOException {
//        try {
//            String content = convertXMLStreamReaderToString(r);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(r, valueType);
//        }
//    }
//
//    @Override
//    public <T> T readValue(XMLStreamReader r, JavaType valueType) throws IOException {
//        try {
//            String content = convertXMLStreamReaderToString(r);
//            return readValueInternal(content, valueType);
//        } catch (ParserConfigurationException | IOException | SAXException e) {
//            LOGGER.error("Failed to parse XML content", e);
//            return super.readValue(r, valueType);
//        }
//    }

    private String getOutermostElementName(String xmlContent) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
        return document.getDocumentElement().getTagName();
    }

    private <T> Optional<Class<T>> getValueTypeForElement(String elementName) {
        for (Map.Entry<ModelSymbolId, TypeXMLConfiguration> entry : config.getTypeConfigMap().entrySet()) {
            TypeXMLConfiguration typeXMLConfiguration = entry.getValue();
            if (typeXMLConfiguration.getXmlElementName().map(x -> x.equals(elementName)).orElse(false)) {
                ModelSymbolId modelSymbolId = entry.getKey();
                try {
                    Class<T> aClass = loadModelSymbolAsClass(modelSymbolId);
                    return Optional.of(aClass);
                } catch (ClassNotFoundException e) {
                    return Optional.empty();
                }
            }
        }

        return Optional.empty();
    }



    @SuppressWarnings("unchecked")
    private <T> Class<T> loadModelSymbolAsClass(ModelSymbolId modelSymbolId) throws ClassNotFoundException {
        return (Class<T>) getClass().getClassLoader().loadClass(modelSymbolId.getQualifiedName().toString());
    }

    private String convertXMLStreamReaderToString(XMLStreamReader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        try {
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamReader.CHARACTERS:
                    case XMLStreamReader.CDATA:
                        content.append(reader.getText());
                        break;
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to read XML content", e);
        }
        return content.toString();
    }

    public String convertFileToString(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    public String convertReaderToString(Reader reader) throws IOException {
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[1024];
        int numRead;
        while ((numRead = reader.read(buffer)) != -1) {
            content.append(buffer, 0, numRead);
        }
        return content.toString();
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        byte[] buffer = new byte[1024];
        int numRead;
        while ((numRead = inputStream.read(buffer)) != -1) {
            content.append(new String(buffer, 0, numRead));
        }
        return content.toString();
    }


}