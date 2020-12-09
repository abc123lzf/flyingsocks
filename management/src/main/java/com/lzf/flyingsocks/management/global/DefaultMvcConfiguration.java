package com.lzf.flyingsocks.management.global;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Log4j2
@Configuration
class DefaultMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(false);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new FastJSONMessageConverter());
        converters.removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);
        for (HttpMessageConverter<?> converter : converters) {
            log.info("MessageConverter: {}", converter.getClass());
        }
    }
}
