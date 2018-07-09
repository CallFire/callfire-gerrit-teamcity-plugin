package com.callfire.teamcity.plugin.gerrit;

import java.security.KeyStore;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jetbrains.buildServer.serverSide.BuildTypeIdentity;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.LoginModuleDescriptor;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.vcs.VcsRoot;

public interface CommitStatusPublisherSettings {

  @NotNull
  String getId();

  @NotNull
  String getName();

  @Nullable
  String getEditSettingsUrl();

  @Nullable
  Map<String, String> getDefaultParameters();

  /**
   * Transforms parameters of the publisher before they are shown in UI
   * @param params parameters to transform
   * @return map of transformed parameters or null if no transformation is needed
   */
  @Nullable
  Map<String, String> transformParameters(@NotNull Map<String, String> params);

  @Nullable
  CommitStatusPublisher createPublisher(@NotNull SBuildType buildType, @NotNull String buildFeatureId,
                                        @NotNull Map<String, String> params);

  @NotNull
  String describeParameters(@NotNull Map<String, String> params);

  @Nullable
  PropertiesProcessor getParametersProcessor();

  @NotNull
  Map<LoginModuleDescriptor, Boolean> getOAuthConnections(final SProject project, final SUser user);

  boolean isEnabled();

  boolean isPublishingForVcsRoot(VcsRoot vcsRoot);

  public boolean isEventSupported(CommitStatusPublisher.Event event);

  boolean isTestConnectionSupported();

  default boolean isFQDNTeamCityUrlRequired() { return false; };

  void testConnection(@NotNull BuildTypeIdentity buildTypeOrTemplate, @NotNull VcsRoot root, @NotNull Map<String, String> params) throws PublisherException;

  @Nullable
  KeyStore trustStore();
}
