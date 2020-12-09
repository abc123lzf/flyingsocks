package com.lzf.flyingsocks.management.global;

import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class FastJSONMessageConverter extends FastJsonHttpMessageConverter {

    private static final Charset charset = StandardCharsets.UTF_8;

    public FastJSONMessageConverter() {
        super();
        FastJsonConfig config = new FastJsonConfig();
        config.setCharset(charset);
        config.setSerializerFeatures(SerializerFeature.DisableCircularReferenceDetect,
                SerializerFeature.BrowserCompatible,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteMapNullValue);
        SerializeConfig sc = SerializeConfig.globalInstance;
        sc.put(BigInteger.class, ToStringSerializer.instance);
        sc.put(BigDecimal.class, ToStringSerializer.instance);
        sc.put(Long.class, ToStringSerializer.instance);
        sc.put(long.class, ToStringSerializer.instance);
        config.setSerializeConfig(sc);

        List<MediaType> media = new ArrayList<>();
        media.add(MediaType.APPLICATION_JSON);
        media.add(MediaType.APPLICATION_JSON_UTF8);
        media.add(MediaType.APPLICATION_ATOM_XML);
        media.add(MediaType.APPLICATION_FORM_URLENCODED);
        media.add(MediaType.APPLICATION_OCTET_STREAM);
        media.add(MediaType.APPLICATION_PDF);
        media.add(MediaType.APPLICATION_RSS_XML);
        media.add(MediaType.APPLICATION_XHTML_XML);
        media.add(MediaType.APPLICATION_XML);
        media.add(MediaType.IMAGE_GIF);
        media.add(MediaType.IMAGE_JPEG);
        media.add(MediaType.IMAGE_PNG);
        media.add(MediaType.TEXT_EVENT_STREAM);
        media.add(MediaType.TEXT_HTML);
        media.add(MediaType.TEXT_MARKDOWN);
        media.add(MediaType.TEXT_PLAIN);
        media.add(MediaType.TEXT_XML);

        setSupportedMediaTypes(media);
        super.setFastJsonConfig(config);
    }

}
