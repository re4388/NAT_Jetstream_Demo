package com.ben.nat_jetstream_demo.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

@Configuration
public class I18nConfig {

    @Bean
    public MessageSource messageSource() {
        YamlMessageSource messageSource = new YamlMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    /**
     * 自定義 MessageSource 支援 YAML 格式
     */
    private static class YamlMessageSource extends ResourceBundleMessageSource {
        @Override
        protected ResourceBundle doGetBundle(String basename, Locale locale) {
            return ResourceBundle.getBundle(basename, locale, new YamlResourceBundleControl());
        }
    }

    /**
     * 自定義 ResourceBundle.Control 用於載入 YAML
     */
    private static class YamlResourceBundleControl extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "yml");
            Resource resource = new ClassPathResource(resourceName);
            if (resource.exists()) {
                YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
                factory.setResources(resource);
                Properties properties = factory.getObject();
                return new PropertiesResourceBundle(properties);
            }
            return null;
        }
    }

    private static class PropertiesResourceBundle extends ResourceBundle {
        private final Properties properties;

        public PropertiesResourceBundle(Properties properties) {
            this.properties = properties;
        }

        @Override
        protected Object handleGetObject(String key) {
            return properties.get(key);
        }

        @Override
        public java.util.Enumeration<String> getKeys() {
            return java.util.Collections.enumeration(properties.stringPropertyNames());
        }
    }
}
