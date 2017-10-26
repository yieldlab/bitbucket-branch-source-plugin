package com.cloudbees.jenkins.plugins.bitbucket;

import com.google.common.base.Charsets;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Jackson based JSON parser
 *
 * @author Vivek Pandey
 */
@Restricted(NoExternalUse.class)
public final class JsonParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonParser.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final ObjectMapper mapper = createObjectMapper();

    public static <T> T toJava(String data, Class<T> type) throws IOException {
        return toJava(new StringReader(data), type);
    }

    public static <T> T toJava(InputStream data, Class<T> type) throws IOException {
        return toJava(new InputStreamReader(data, Charsets.UTF_8), type);
    }

    public static <T> T toJava(Reader data, Class<T> type) throws IOException{
        return mapper.readValue(data, type);
    }

    public static String toJson(Object value) throws IOException {
        return mapper.writeValueAsString(value);
    }

    private static ObjectMapper createObjectMapper(){
        ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        mapper.setDateFormat(format);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
