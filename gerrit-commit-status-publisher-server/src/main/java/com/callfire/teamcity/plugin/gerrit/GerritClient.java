package com.callfire.teamcity.plugin.gerrit;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.jcraft.jsch.JSchException;

/**
 * This interface does not declare full gerrit functionality, but only the methods required
 * by Commit Status Publisher.
 */
interface GerritClient {

  void review(@NotNull GerritConnectionDetails connectionDetails, @Nullable final String label, @NotNull String vote,
              @NotNull String message, @NotNull String revision) throws JSchException, IOException;

  void testConnection(@NotNull GerritConnectionDetails connectionDetails) throws JSchException, IOException, PublisherException;

  String runCommand(@NotNull GerritConnectionDetails connectionDetails, @NotNull String command) throws JSchException, IOException;

}
