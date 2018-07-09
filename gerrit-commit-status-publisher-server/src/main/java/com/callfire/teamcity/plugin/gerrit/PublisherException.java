package com.callfire.teamcity.plugin.gerrit;
import org.jetbrains.annotations.NotNull;

public class PublisherException extends Exception {

  public PublisherException(@NotNull String message) {
    super(message);
  }

  public PublisherException(@NotNull String message, Throwable cause) {
    super(message, cause);
  }

}
