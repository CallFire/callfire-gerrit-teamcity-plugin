package com.callfire.teamcity.plugin.gerrit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jetbrains.buildServer.serverSide.BuildRevision;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SQueuedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.users.User;

public interface CommitStatusPublisher {

  boolean buildQueued(@NotNull SQueuedBuild build, @NotNull BuildRevision revision) throws PublisherException;

  boolean buildRemovedFromQueue(@NotNull SQueuedBuild build, @NotNull BuildRevision revision, @Nullable User user,
                                @Nullable String comment) throws PublisherException;

  boolean buildStarted(@NotNull SRunningBuild build, @NotNull BuildRevision revision) throws PublisherException;

  boolean buildFinished(@NotNull SFinishedBuild build, @NotNull BuildRevision revision) throws PublisherException;

  boolean buildCommented(@NotNull SBuild build, @NotNull BuildRevision revision, @Nullable User user, @Nullable String comment,
                         boolean buildInProgress) throws PublisherException;

  boolean buildInterrupted(@NotNull SFinishedBuild build, @NotNull BuildRevision revision) throws PublisherException;

  boolean buildFailureDetected(@NotNull SRunningBuild build, @NotNull BuildRevision revision) throws PublisherException;

  boolean buildMarkedAsSuccessful(@NotNull SBuild build, @NotNull BuildRevision revision, boolean buildInProgress) throws PublisherException;

  @NotNull
  String getBuildFeatureId();

  @NotNull
  SBuildType getBuildType();

  @Nullable
  String getVcsRootId();

  @NotNull
  String toString();

  @NotNull
  String getId();

  @NotNull
  CommitStatusPublisherSettings getSettings();

  boolean isPublishingForRevision(@NotNull BuildRevision revision);

  void setConnectionTimeout(int timeout);

  boolean isEventSupported(Event event);

  enum Event {
    STARTED("buildStarted"), FINISHED("buildFinished"),
    QUEUED("buildQueued"), REMOVED_FROM_QUEUE("buildRemovedFromQueue"),
    COMMENTED("buildCommented"), INTERRUPTED("buildInterrupted"),
    FAILURE_DETECTED("buildFailureDetected"), MARKED_AS_SUCCESSFUL("buildMarkedAsSuccessful");

    private final String myName;

    Event(String name) {
      myName = name;
    }

    public String getName() {
      return myName;
    }
  }
}
