package com.callfire.teamcity.plugin.gerrit;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.serverSide.Branch;
import jetbrains.buildServer.serverSide.BuildRevision;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.serverSide.WebLinks;
import jetbrains.buildServer.ssh.ServerSshKeyManager;

class GerritPublisher extends BaseCommitStatusPublisher {
  private final static com.intellij.openapi.diagnostic.Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(GerritPublisher.class.getName());
  private final WebLinks myLinks;
  private final GerritClient myGerritClient;

  GerritPublisher(@NotNull CommitStatusPublisherSettings settings,
                  @NotNull SBuildType buildType, @NotNull String buildFeatureId,
                  @NotNull GerritClient gerritClient,
                  @NotNull WebLinks links,
                  @NotNull Map<String, String> params,
                  @NotNull CommitStatusPublisherProblems problems) {
    super(settings, buildType, buildFeatureId, params, problems);
    myLinks = links;
    myGerritClient = gerritClient;
  }

  @NotNull
  public String toString() {
    return "gerrit";
  }

  @NotNull
  @Override
  public String getId() {
    return Constants.GERRIT_PUBLISHER_ID;
  }

  @Override
  public boolean buildInterrupted(@NotNull SFinishedBuild build, @NotNull BuildRevision revision) throws PublisherException {
    Branch branch = build.getBranch();
    if (branch == null || branch.isDefaultBranch())
      return false;
    return post(build, revision, getFailureVote());
  }

  private boolean post(@NotNull SBuild build, @NotNull BuildRevision revision, String vote) throws PublisherException {
    String msg = build.getFullName() +
            " #" + build.getBuildNumber() +
            ": " + build.getStatusDescriptor().getText() +
            " " + myLinks.getViewResultsUrl(build);
    try {
      SBuildType bt = build.getBuildType();
      if (null == bt) return false;

      myGerritClient.review(
              new GerritConnectionDetails(bt.getProject(), getGerritProject(), getGerritServer(), getUsername(),
                      myParams.get(ServerSshKeyManager.TEAMCITY_SSH_KEY_PROP)),
              getGerritLabel(), vote, msg, revision.getRevision()
      );
      return true;
    } catch (Exception e) {
      throw new PublisherException("Cannot publish status to Gerrit for VCS root " +
              revision.getRoot().getName() + ": " + e.toString(), e);
    }
  }

  @Override
  public boolean buildStarted(@NotNull SRunningBuild build, @NotNull BuildRevision revision) throws PublisherException {
    Branch branch = build.getBranch();
    LOG.info("publishing geerit started " + build + " branch: " + branch);
    if (branch == null || branch.isDefaultBranch())
      return false;

    String vote = getNoVote();
    return post(build, revision, vote);
  }

  @Override
  public boolean buildFinished(@NotNull SFinishedBuild build, @NotNull BuildRevision revision) throws PublisherException {
    Branch branch = build.getBranch();
    if (branch == null || branch.isDefaultBranch())
      return false;

    String vote = build.getBuildStatus().isSuccessful() ? getSuccessVote() : getFailureVote();
    return post(build, revision, vote);
  }

  private String getGerritServer() {
    return myParams.get(Constants.GERRIT_SERVER);
  }

  private String getGerritProject() {
    return myParams.get(Constants.GERRIT_PROJECT);
  }

  private String getGerritLabel() {
    return myParams.get(Constants.GERRIT_LABEL);
  }

  private String getUsername() {
    return myParams.get(Constants.GERRIT_USERNAME);
  }

  private String getSuccessVote() {
    return myParams.get(Constants.GERRIT_SUCCESS_VOTE);
  }

  private String getFailureVote() {
    return myParams.get(Constants.GERRIT_FAILURE_VOTE);
  }

  private String getNoVote() {
    return myParams.get(Constants.GERRIT_NO_VOTE);
  }
}
