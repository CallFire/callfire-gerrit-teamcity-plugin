package com.callfire.teamcity.plugin.gerrit;

import javax.servlet.http.HttpServletRequest;

import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.serverSide.crypt.RSACipher;

/**
 * @author inekrashevych
 */
public class PluginPropertiesUtil {
    private final static String PROPERTY_PREFIX = "prop:";
    private static final String ENCRYPTED_PROPERTY_PREFIX = "prop:encrypted:";

    private PluginPropertiesUtil() {}

    public static void bindPropertiesFromRequest(HttpServletRequest request, BasePropertiesBean bean) {
        bindPropertiesFromRequest(request, bean, false);
    }

    public static void bindPropertiesFromRequest(HttpServletRequest request, BasePropertiesBean bean, boolean includeEmptyValues) {
        request.getParameterMap().keySet().stream().filter(it -> it.startsWith(PROPERTY_PREFIX)).forEach(param ->
                {
                    if (param.startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
                        setEncryptedProperty(param, request.getParameter(param), bean, includeEmptyValues);
                    } else {
                        setStringProperty(param, request.getParameter(param), bean, includeEmptyValues);
                    }
                }
        );
    }
    private static void setStringProperty(final String paramName, final String propertyValue,
                                          final BasePropertiesBean bean, final boolean includeEmptyValues) {
        String propName = paramName.substring(PROPERTY_PREFIX.length());
        if (includeEmptyValues || propertyValue.length() > 0) {
            bean.setProperty(propName, toUnixLineFeeds(propertyValue));
        }
    }
    private static void setEncryptedProperty(final String paramName, final String value,
                                             final BasePropertiesBean bean, final boolean includeEmptyValues) {
        String propName = paramName.substring(ENCRYPTED_PROPERTY_PREFIX.length());
        String propertyValue = RSACipher.decryptWebRequestData(value);
        if (propertyValue != null && (includeEmptyValues || propertyValue.length() > 0)) {
            bean.setProperty(propName, toUnixLineFeeds(propertyValue));
        }
    }
    private static String toUnixLineFeeds(final String str) {
        return str.replace("\r", "");
    }
}