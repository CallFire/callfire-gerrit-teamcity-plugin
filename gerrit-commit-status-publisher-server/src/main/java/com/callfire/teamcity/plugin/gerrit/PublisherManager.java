package com.callfire.teamcity.plugin.gerrit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jetbrains.buildServer.serverSide.SBuildType;

public class PublisherManager {

  private final Map<String, CommitStatusPublisherSettings> myPublisherSettings;

  public PublisherManager(@NotNull Collection<CommitStatusPublisherSettings> settings) {
    myPublisherSettings = new HashMap<String, CommitStatusPublisherSettings>();
    for (CommitStatusPublisherSettings s : settings) {
      myPublisherSettings.put(s.getId(), s);
    }
  }

  @Nullable
  public CommitStatusPublisher createPublisher(@NotNull SBuildType buildType, @NotNull String buildFeatureId, @NotNull Map<String, String> params) {
    String publisherId = params.get(Constants.PUBLISHER_ID_PARAM);
    if (publisherId == null)
      return null;
    CommitStatusPublisherSettings settings = myPublisherSettings.get(publisherId);
    if (settings == null)
      return null;
    return settings.createPublisher(buildType, buildFeatureId, params);
  }

  @Nullable
  public CommitStatusPublisherSettings findSettings(@NotNull String publisherId) {
    return myPublisherSettings.get(publisherId);
  }

  @NotNull
  List<CommitStatusPublisherSettings> getAllPublisherSettings() {
    List<CommitStatusPublisherSettings> settings = new ArrayList<CommitStatusPublisherSettings>();
    for (CommitStatusPublisherSettings s : myPublisherSettings.values()) {
      if (s.isEnabled())
        settings.add(s);
    }
    Collections.sort(settings, new Comparator<CommitStatusPublisherSettings>() {
      public int compare(CommitStatusPublisherSettings o1, CommitStatusPublisherSettings o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    return settings;
  }
}
