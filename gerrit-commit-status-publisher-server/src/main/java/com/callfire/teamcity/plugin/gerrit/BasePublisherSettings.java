package com.callfire.teamcity.plugin.gerrit;

import java.security.KeyStore;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;

import com.callfire.teamcity.plugin.gerrit.CommitStatusPublisher.Event;

import jetbrains.buildServer.serverSide.BuildTypeIdentity;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.serverSide.auth.LoginModuleDescriptor;
import jetbrains.buildServer.serverSide.executors.ExecutorServices;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.ssl.SSLTrustStoreProvider;
import jetbrains.buildServer.vcs.VcsRoot;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public abstract class BasePublisherSettings implements CommitStatusPublisherSettings {

  protected final PluginDescriptor myDescriptor;
  protected final WebLinks myLinks;
  protected final ExecutorServices myExecutorServices;
  protected CommitStatusPublisherProblems myProblems;
  private SSLTrustStoreProvider myTrustStoreProvider;
  protected final Gson myGson = new Gson();

  public BasePublisherSettings(@NotNull final ExecutorServices executorServices,
                               @NotNull PluginDescriptor descriptor,
                               @NotNull WebLinks links,
                               @NotNull CommitStatusPublisherProblems problems,
                               @NotNull SSLTrustStoreProvider trustStoreProvider) {
    myDescriptor = descriptor;
    myLinks= links;
    myExecutorServices = executorServices;
    myProblems = problems;
    myTrustStoreProvider = trustStoreProvider;
  }

  @Nullable
  public Map<String, String> getDefaultParameters() {
    return null;
  }

  @Nullable
  @Override
  public Map<String, String> transformParameters(@NotNull Map<String, String> params) {
    return null;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull final Map<String, String> params) {
    return String.format("Post commit status to %s", getName());
  }

  @NotNull
  @Override
  public Map<LoginModuleDescriptor, Boolean> getOAuthConnections(final SProject project, final SUser user) {
    return Collections.emptyMap();
  }

  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean isPublishingForVcsRoot(final VcsRoot vcsRoot) {
    return true;
  }

  @Override
  public boolean isEventSupported(Event event) {
    return getSupportedEvents().contains(event);
  }

  @Override
  public boolean isTestConnectionSupported() { return false; }

  @Override
  public void testConnection(@NotNull BuildTypeIdentity buildTypeOrTemplate, @NotNull VcsRoot root, @NotNull Map<String, String> params) throws PublisherException {
    throw new UnsupportedOperationException(String.format("Test connection functionality is not supported by %s publisher", getName()));
  }

  @Nullable
  @Override
  public KeyStore trustStore() {
    return myTrustStoreProvider.getTrustStore();
  }

  protected Set<Event> getSupportedEvents() {
    return Collections.emptySet();
  }
}
