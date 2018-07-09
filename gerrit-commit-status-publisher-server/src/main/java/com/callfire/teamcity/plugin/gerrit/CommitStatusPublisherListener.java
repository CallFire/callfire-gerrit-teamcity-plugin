package com.callfire.teamcity.plugin.gerrit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.diagnostic.Logger;

import com.callfire.teamcity.plugin.gerrit.CommitStatusPublisher.Event;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.log.LogUtil;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.BuildHistory;
import jetbrains.buildServer.serverSide.BuildPromotion;
import jetbrains.buildServer.serverSide.BuildRevision;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.serverSide.BuildServerListener;
import jetbrains.buildServer.serverSide.BuildTypeNotFoundException;
import jetbrains.buildServer.serverSide.RunningBuildsManager;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SQueuedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.users.User;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.SVcsRoot;

public class CommitStatusPublisherListener extends BuildServerAdapter {

  private final static Logger LOG = Logger.getInstance(CommitStatusPublisherListener.class.getName());
  private final static String PUBLISHING_ENABLED_PROPERTY_NAME = "teamcity.commitStatusPublisher.enabled";

  private final PublisherManager myPublisherManager;
  private final BuildHistory myBuildHistory;
  private final RunningBuildsManager myRunningBuilds;
  private final CommitStatusPublisherProblems myProblems;

  public CommitStatusPublisherListener(@NotNull EventDispatcher<BuildServerListener> events,
                                       @NotNull PublisherManager voterManager,
                                       @NotNull BuildHistory buildHistory,
                                       @NotNull RunningBuildsManager runningBuilds,
                                       @NotNull CommitStatusPublisherProblems problems) {
    myPublisherManager = voterManager;
    myBuildHistory = buildHistory;
    myRunningBuilds = runningBuilds;
    myProblems = problems;
    LOG.info("registering cfbuildfeature");
    events.addListener(this);
  }

  @Override
  public void changesLoaded(@NotNull final SRunningBuild build) {
    LOG.info("cfbuildfeature: loaded called");
    SBuildType buildType = getBuildType(Event.STARTED, build);
    LOG.info("cfbuildfeature bt: " + buildType);
    if (buildType == null)
      return;

    runForEveryPublisher(Event.STARTED, buildType, build, (publisher, revision) -> publisher.buildStarted(build, revision));
  }

  @Override
  public void buildFinished(@NotNull SRunningBuild build) {
    LOG.info("cfbuildfeature: finished called");
    SBuildType buildType = getBuildType(Event.FINISHED, build);
    if (buildType == null)
      return;

    final SFinishedBuild finishedBuild = myBuildHistory.findEntry(build.getBuildId());
    if (finishedBuild == null) {
      LOG.debug("Event: " + Event.FINISHED + ", cannot find finished build for build " + LogUtil.describe(build));
      return;
    }

    runForEveryPublisher(Event.FINISHED, buildType, build,
            (publisher, revision) -> publisher.buildFinished(finishedBuild, revision));
  }

  @Override
  public void buildCommented(@NotNull final SBuild build, @Nullable final User user, @Nullable final String comment) {
    SBuildType buildType = getBuildType(Event.COMMENTED, build);
    if (buildType == null)
      return;
    runForEveryPublisher(Event.COMMENTED, buildType, build,
            (publisher, revision) -> publisher.buildCommented(build, revision, user, comment, isBuildInProgress(build)));
  }

  @Override
  public void buildInterrupted(@NotNull SRunningBuild build) {
    SBuildType buildType = getBuildType(Event.INTERRUPTED, build);
    if (buildType == null)
      return;

    final SFinishedBuild finishedBuild = myBuildHistory.findEntry(build.getBuildId());
    if (finishedBuild == null) {
      LOG.debug("Event: " + Event.INTERRUPTED.getName() + ", cannot find finished build for build " + LogUtil.describe(build));
      return;
    }

    runForEveryPublisher(Event.INTERRUPTED, buildType, build,
            (publisher, revision) -> publisher.buildInterrupted(finishedBuild, revision));
  }


  @Override
  public void buildChangedStatus(@NotNull final SRunningBuild build, Status oldStatus, Status newStatus) {
    if (oldStatus.isFailed() || !newStatus.isFailed()) // we are supposed to report failures only
      return;

    SBuildType buildType = getBuildType(Event.FAILURE_DETECTED, build);
    if (buildType == null)
      return;

    runForEveryPublisher(Event.FAILURE_DETECTED, buildType, build, new PublishTask() {
      @Override
      public boolean run(@NotNull CommitStatusPublisher publisher, @NotNull BuildRevision revision) throws PublisherException {
        return publisher.buildFailureDetected(build, revision);
      }
    });
  }


  @Override
  public void buildProblemsChanged(@NotNull final SBuild build, @NotNull final List<BuildProblemData> before, @NotNull final List<BuildProblemData> after) {
    SBuildType buildType = getBuildType(Event.MARKED_AS_SUCCESSFUL, build);
    if (buildType == null)
      return;

    if (!before.isEmpty() && after.isEmpty()) {
      runForEveryPublisher(Event.MARKED_AS_SUCCESSFUL, buildType, build, new PublishTask()  {
        @Override
        public boolean run(@NotNull CommitStatusPublisher publisher, @NotNull BuildRevision revision) throws PublisherException {
          return publisher.buildMarkedAsSuccessful(build, revision, isBuildInProgress(build));
        }
      });
    }
  }

  @Override
  public void buildTypeAddedToQueue(@NotNull final SQueuedBuild build) {
    SBuildType buildType = getBuildType(Event.QUEUED, build);
    if (buildType == null)
      return;

    runForEveryPublisherQueued(Event.QUEUED, buildType, build, (publisher, revision) -> publisher.buildQueued(build, revision));
  }

  @Override
  public void buildRemovedFromQueue(@NotNull final SQueuedBuild build, final User user, final String comment) {
    SBuildType buildType = getBuildType(Event.REMOVED_FROM_QUEUE, build);
    if (buildType == null)
      return;

    if (user == null)
      return;

    runForEveryPublisherQueued(Event.REMOVED_FROM_QUEUE, buildType, build,
            (publisher, revision) -> publisher.buildRemovedFromQueue(build, revision, user, comment));
  }

  private boolean isPublishingDisabled(SBuildType buildType) {
    String publishingEnabledParam = buildType.getParameterValue(PUBLISHING_ENABLED_PROPERTY_NAME);
    return "false".equals(publishingEnabledParam)
           || !(TeamCityProperties.getBooleanOrTrue(PUBLISHING_ENABLED_PROPERTY_NAME)
                || "true".equals(publishingEnabledParam));
  }

  private void logStatusNotPublished(@NotNull Event event, @NotNull String buildDescription, @NotNull CommitStatusPublisher publisher, @NotNull String message) {
    LOG.info(String.format("Event: %s, build %s, publisher %s: %s", event.getName(), buildDescription, publisher.toString(), message));
  }

  private void runForEveryPublisher(@NotNull Event event, @NotNull SBuildType buildType, @NotNull SBuild build, @NotNull PublishTask task) {
//    if  (!myServerResponsibility.isResponsibleForBuild(build)) {
//      LOG.debug("Current node is not responsible for build " + LogUtil.describe(build) + ", skip processing event " + event);
//      return;
//    }
    if (build.isPersonal()) {
      for(SVcsModification change: build.getBuildPromotion().getPersonalChanges()) {
        if (change.isPersonal()) {
          LOG.info("PERSONAL CFBUILD change???");
          return;
        }
      }
    }
    Map<String, CommitStatusPublisher> publishers = getPublishers(buildType);
    LOG.info("Event: " + event.getName() + ", build " + LogUtil.describe(build) + ", publishers: " + publishers.values());
    for (Map.Entry<String, CommitStatusPublisher> pubEntry : publishers.entrySet()) {
      CommitStatusPublisher publisher = pubEntry.getValue();
      if (!publisher.isEventSupported(event))
        continue;
      if (isPublishingDisabled(buildType)) {
        logStatusNotPublished(event, LogUtil.describe(build), publisher, "commit status publishing is disabled");
        continue;
      }
      List<BuildRevision> revisions = getBuildRevisionForVote(publisher, build);
      if (revisions.isEmpty()) {
        logStatusNotPublished(event, LogUtil.describe(build), publisher, "no compatible revisions found");
        continue;
      }
      myProblems.clearProblem(publisher);
      for (BuildRevision revision: revisions) {
        runTask(event, build.getBuildPromotion(), LogUtil.describe(build), task, publisher, revision);
      }
    }
    myProblems.clearObsoleteProblems(buildType, publishers.keySet());
  }

  private void runForEveryPublisherQueued(@NotNull Event event, @NotNull SBuildType buildType, @NotNull SQueuedBuild build, @NotNull PublishTask task) {
//    if  (!myServerResponsibility.canManageBuilds()) {
//      LOG.debug("Current node is not responsible for build " + LogUtil.describe(build) + ", skip processing event " + event);
//      return;
//    }
    if (build.isPersonal()) {
      for(SVcsModification change: build.getBuildPromotion().getPersonalChanges()) {
        if (change.isPersonal())
          return;
      }
    }
    Map<String, CommitStatusPublisher> publishers = getPublishers(buildType);
    LOG.info("Event: " + event.getName() + ", build " + LogUtil.describe(build) + ", publishers: " + publishers.values());
    for (Map.Entry<String, CommitStatusPublisher> pubEntry : publishers.entrySet()) {
      CommitStatusPublisher publisher = pubEntry.getValue();
      if (!publisher.isEventSupported(event))
        continue;
      if (isPublishingDisabled(buildType)) {
        logStatusNotPublished(event, LogUtil.describe(build), publisher, "commit status publishing is disabled");
        continue;
      }
      List<BuildRevision> revisions = getQueuedBuildRevisionForVote(buildType, publisher, build);
      if (revisions.isEmpty()) {
        logStatusNotPublished(event, LogUtil.describe(build), publisher, "no compatible revisions found");
        continue;
      }
      myProblems.clearProblem(publisher);
      for (BuildRevision revision: revisions) {
        runTask(event, build.getBuildPromotion(), LogUtil.describe(build), task, publisher, revision);
      }
    }
    myProblems.clearObsoleteProblems(buildType, publishers.keySet());
  }

  private void runTask(@NotNull Event event,
                       @NotNull BuildPromotion promotion,
                       @NotNull String buildDescription,
                       @NotNull PublishTask task,
                       @NotNull CommitStatusPublisher publisher,
                       @NotNull BuildRevision revision) {
    try {
      task.run(publisher, revision);
    } catch (Throwable t) {
      myProblems.reportProblem(String.format("Commit Status Publisher has failed to publish %s status", event.getName()), publisher, buildDescription, null, t, LOG);
    }
  }

  @NotNull
  private Map<String, CommitStatusPublisher> getPublishers(@NotNull SBuildType buildType) {
    Map<String, CommitStatusPublisher> publishers = new LinkedHashMap<String, CommitStatusPublisher>();
    for (SBuildFeatureDescriptor buildFeatureDescriptor : buildType.getResolvedSettings().getBuildFeatures()) {
      BuildFeature buildFeature = buildFeatureDescriptor.getBuildFeature();
      if (buildFeature instanceof CommitStatusPublisherFeature) {
        String featureId = buildFeatureDescriptor.getId();
        CommitStatusPublisher publisher = myPublisherManager.createPublisher(buildType, featureId, buildFeatureDescriptor.getParameters());
        if (publisher != null)
          publishers.put(featureId, publisher);
      }
    }
    return publishers;
  }

  @NotNull
  private List<BuildRevision> getBuildRevisionForVote(@NotNull CommitStatusPublisher publisher, @NotNull SBuild build) {

    if (build.getBuildPromotion().isFailedToCollectChanges()) return Collections.emptyList();

    String vcsRootId = publisher.getVcsRootId();
    if (vcsRootId == null) {
      List<BuildRevision> revisions = new ArrayList<BuildRevision>();
      for (BuildRevision revision : build.getRevisions()) {
        if (publisher.isPublishingForRevision(revision)) {
          revisions.add(revision);
        }
      }
      return revisions;
    }

    for (BuildRevision revision : build.getRevisions()) {
      SVcsRoot root = revision.getRoot().getParent();
      if (vcsRootId.equals(root.getExternalId()) || vcsRootId.equals(String.valueOf(root.getId())))
        return Arrays.asList(revision);
    }

    return Collections.emptyList();
  }

  @NotNull
  private List<BuildRevision> getQueuedBuildRevisionForVote(@NotNull SBuildType buildType,
                                                            @NotNull CommitStatusPublisher publisher,
                                                            @NotNull SQueuedBuild build) {
    BuildPromotion p = build.getBuildPromotion();
    SBuild b = p.getAssociatedBuild();
    if (b != null) {
      List<BuildRevision> revisions = getBuildRevisionForVote(publisher, b);
      if (!revisions.isEmpty())
        return revisions;
    }
    return Collections.emptyList();
  }

  @NotNull
  private String getBranchName(@NotNull BuildPromotion p) {
    Branch b = p.getBranch();
    if (b == null)
      return Branch.DEFAULT_BRANCH_NAME;
    return b.getName();
  }

  @Nullable
  private SBuildType getBuildType(@NotNull CommitStatusPublisher.Event event, @NotNull SBuild build) {
    SBuildType buildType = build.getBuildType();
    if (buildType == null)
      LOG.debug("Event: " + event.getName() + ", cannot find buildType for build " + LogUtil.describe(build));
    return buildType;
  }

  @Nullable
  private SBuildType getBuildType(@NotNull CommitStatusPublisher.Event event, @NotNull SQueuedBuild build) {
    try {
      return build.getBuildType();
    } catch (BuildTypeNotFoundException e) {
      LOG.debug("Event: " + event.getName() + ", cannot find buildType for build " + LogUtil.describe(build));
      return null;
    }
  }

  private interface PublishTask {
    boolean run(@NotNull CommitStatusPublisher publisher, @NotNull BuildRevision revision) throws PublisherException;
  }

  private boolean isBuildInProgress(SBuild build) {
    return myRunningBuilds.findRunningBuildById(build.getBuildId()) != null;
  }

  private boolean shouldFailBuild(@NotNull SBuildType buildType) {
    return Boolean.valueOf(buildType.getParameters().get("teamcity.commitStatusPublisher.failBuildOnPublishError"));
  }
}
