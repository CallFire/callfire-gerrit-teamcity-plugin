package com.callfire.teamcity.plugin.gerrit.summary;

import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.controllers.BuildDataExtensionUtil;
import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PlaceId;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.SimplePageExtension;
import jetbrains.buildServer.web.openapi.WebControllerManager;

public class BuildSummaryLinkExtension extends SimplePageExtension {
    public static final String GERRIT_LINK = "gerrit.link";
    public static final String SONAR_LINK = "sonar.link";
    public static final String SONAR_PROJECT_KEY = "sonar.project.key";
    @NotNull private final SBuildServer myServer;

    public BuildSummaryLinkExtension(@NotNull WebControllerManager manager,
                                     @NotNull PluginDescriptor pluginDescriptor,
                                     @NotNull SBuildServer server) {
        super(manager, PlaceId.BUILD_SUMMARY, pluginDescriptor.getPluginName(), "buildSummary.jsp");
        myServer = server;
        register();
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model,
                          @NotNull HttpServletRequest request) {
        SBuild build = BuildDataExtensionUtil.retrieveBuild(request, myServer);
        if (build == null) {
            return;
        }
        String gerritLink = build.getParametersProvider().get(GERRIT_LINK);
        Branch branch = build.getBranch();
        model.put("gerrit_url", gerritLink + substringAfter(branch.getName(), "/"));
        if (StringUtils.isNotBlank(build.getParametersProvider().get(SONAR_PROJECT_KEY))) {
            model.put("sonar_url", build.getParametersProvider().get(SONAR_LINK) + "dashboard?id=" + build.getParametersProvider()
                    .get(SONAR_PROJECT_KEY));
        }
        super.fillModel(model, request);
    }

    @Override
    public boolean isAvailable(@NotNull final HttpServletRequest request) {
        SBuild build = BuildDataExtensionUtil.retrieveBuild(request, myServer);
        if (build == null) {
            return false;
        }
        return build.getBranch() != null && StringUtils.isNotEmpty(build.getParametersProvider().get(GERRIT_LINK));
    }
}
