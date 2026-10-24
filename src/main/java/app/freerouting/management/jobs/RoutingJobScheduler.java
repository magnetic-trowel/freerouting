package app.freerouting.management.jobs;

import app.freerouting.board.actions.ItemIdGenerator;
import app.freerouting.core.RoutingJob;
import app.freerouting.core.RoutingJobState;
import app.freerouting.core.Session;
import app.freerouting.core.StoppableThread;
import app.freerouting.io.FileFormat;
import app.freerouting.io.specctra.SesImportSummary;
import app.freerouting.io.specctra.SesReader;
import app.freerouting.logger.FRLogger;
import app.freerouting.management.CompositeBoardInput;
import app.freerouting.management.HeadlessBoardManager;
import app.freerouting.management.sessions.SessionManager;
import app.freerouting.settings.GlobalSettings;
import app.freerouting.settings.RouterSettings;
import app.freerouting.settings.SettingsMerger;
import app.freerouting.settings.sources.ApiSettings;
import app.freerouting.settings.sources.DefaultSettings;
import app.freerouting.settings.sources.DsnFileSettings;
import app.freerouting.util.TextManager;
import app.freerouting.util.gson.GsonProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * This singleton class is responsible for managing the jobs that will be processed by the router.
 * The jobs are stored in a priority queue where the jobs with the highest priority are processed
 * first. There is only one instance of this class in the Freerouting process.
 */
public final class RoutingJobScheduler {

  private static final int MAX_QUEUED_JOBS = 5_000;

  /**
   * Default for {@code api_server.max_finished_jobs}. A finished job without its board holds its
   * input and output files only (usually well under a few MB each), so 200 of them stay within a
   * few hundred MB, while a client normally downloads its result within seconds of completion.
   */
  static final int DEFAULT_MAX_FINISHED_JOBS = 200;

  /**
   * Smallest default for {@code api_server.max_finished_job_boards}. A routed board with its search
   * trees can take tens of MB, so not every finished job keeps one; the default is twice the number
   * of parallel jobs, and at least this, so that a client running as many jobs as the server runs
   * at once can still poll, download and check each result after the others finish.
   */
  static final int DEFAULT_MAX_FINISHED_JOB_BOARDS = 4;

  private static final RoutingJobScheduler instance = new RoutingJobScheduler();
  public final LinkedList<RoutingJob> jobs = new LinkedList<>();

  // Private constructor to prevent instantiation
  private RoutingJobScheduler() {
    // start a loop to process the jobs on another thread
    Thread loopThread =
        new Thread(
            () -> {
              while (true) {
                try {
                  // loop through jobs with the READY_TO_START state, order them according to
                  // their priority and start them up to the maximum number of parallel jobs
                  while (jobs.stream().count() > 0) {
                    RoutingJob[] jobsArray;
                    synchronized (jobs) {
                      // Remove any null entries that could have been introduced by concurrent
                      // access
                      jobs.removeIf(Objects::isNull);
                      // sort the jobs by priority
                      Collections.sort(jobs);

                      jobsArray = jobs.toArray(RoutingJob[]::new);
                    }

                    boolean startedAny = false;
                    // start the jobs up to the maximum number of parallel jobs (and make a copy of
                    // the list to avoid concurrent modification)
                    for (RoutingJob job : jobsArray) {
                      if (job.state == RoutingJobState.READY_TO_START) {
                        int parallelJobs =
                            (int)
                                jobs.stream()
                                    .filter(j -> j.state == RoutingJobState.RUNNING)
                                    .count();

                        if (parallelJobs < getMaxParallelJobs()) {
                          if ((job.input == null) || (job.input.getData() == null)) {
                            FRLogger.warn("RoutingJob input is null, it is skipped.");
                            job.state = RoutingJobState.INVALID;
                            continue;
                          }

                          boolean isDsn = job.input.format == FileFormat.DSN;
                          boolean isJson = job.input.format == FileFormat.KICAD_DESIGN_JSON;

                          // load the board from the input into a RoutingBoard object
                          if (isDsn || isJson) {
                            try {
                              // The board load fills board defaults into job.routerSettings;
                              // merge only what the caller set (see CompositeBoardInput).
                              final RouterSettings callerSettings =
                                  job.routerSettings != null ? job.routerSettings.clone() : null;
                              HeadlessBoardManager boardManager = new HeadlessBoardManager(job);
                              boardManager.holdPostLoadProcessing();
                              if (isDsn) {
                                boardManager.loadFromSpecctraDsn(
                                    job.input.getData(), null, new ItemIdGenerator());
                              } else {
                                boardManager.loadFromKiCadJson(
                                    job.input.getData(), null, new ItemIdGenerator());
                              }
                              job.board = boardManager.getRoutingBoard();

                              GlobalSettings gs = globalSettings();
                              var settingsMerger =
                                  gs != null && gs.settingsMergerProtype != null
                                      ? gs.settingsMergerProtype.clone()
                                      : new SettingsMerger(new DefaultSettings());

                              if (isDsn) {
                                settingsMerger.addOrReplaceSources(
                                    new DsnFileSettings(
                                        job.input.getData(), job.input.getFilename()));
                              }

                              // Rules file from job.rules, CLI initialRulesFile, or adjacent
                              // <designName>.rules
                              byte[] rulesData = null;
                              String rulesFilename = null;
                              if (job.rules != null && job.rules.getData() != null) {
                                rulesData = job.rules.getData().readAllBytes();
                                rulesFilename = job.rules.getFilename();
                              } else if (gs != null && gs.initialRulesFile != null) {
                                java.io.File rf = new java.io.File(gs.initialRulesFile);
                                if (rf.exists()) {
                                  try {
                                    rulesData = Files.readAllBytes(rf.toPath());
                                    rulesFilename = rf.getName();
                                  } catch (IOException e) {
                                    FRLogger.warn(
                                        "Failed to read rules file: "
                                            + rf.getPath()
                                            + ": "
                                            + e.getMessage());
                                  }
                                }
                              } else if (isDsn && job.input.getDirectoryPath() != null) {
                                String baseName = job.input.getFilename();
                                if (baseName.lastIndexOf('.') > 0) {
                                  baseName = baseName.substring(0, baseName.lastIndexOf('.'));
                                }
                                java.io.File autoRules =
                                    new java.io.File(
                                        job.input.getDirectoryPath(), baseName + ".rules");
                                if (autoRules.exists()) {
                                  try {
                                    rulesData = Files.readAllBytes(autoRules.toPath());
                                    rulesFilename = autoRules.getName();
                                  } catch (IOException e) {
                                    FRLogger.warn(
                                        "Failed to read adjacent rules file: "
                                            + autoRules.getPath()
                                            + ": "
                                            + e.getMessage());
                                  }
                                }
                              }

                              // The rules apply onto the board here, and their autoroute settings
                              // merge at RULES-file priority, below the environment, the command
                              // line and the caller.
                              if (rulesData != null) {
                                settingsMerger.addOrReplaceSources(
                                    CompositeBoardInput.readRulesOntoBoard(
                                        rulesData,
                                        rulesFilename,
                                        job.name != null ? job.name : "board",
                                        job.board));
                              }

                              // Keep per-job overrides (e.g. tests toggling fanout/optimizer) by
                              // applying the job's current settings as highest-priority API
                              // settings.
                              if (callerSettings != null) {
                                settingsMerger.addOrReplaceSources(new ApiSettings(callerSettings));
                              }

                              // Apply the final merged settings to the job and optimize them for
                              // the board
                              job.routerSettings = settingsMerger.merge();

                              job.routerSettings.applyBoardSpecificOptimizations(job.board);
                              job.routerSettings.applyNetClassExclusions(job.board);

                              // Load session file if specified in job or globally
                              byte[] sessionBytesToLoad = null;
                              String sessionFilenameToLoad = null;

                              if (job.initialSession != null
                                  && job.initialSession.getData() != null) {
                                sessionBytesToLoad = job.initialSession.getData().readAllBytes();
                                sessionFilenameToLoad = job.initialSession.getFilename();
                              } else if (gs != null && gs.designSessionFilename != null) {
                                java.io.File sessionFile =
                                    new java.io.File(gs.designSessionFilename);
                                if (sessionFile.exists()) {
                                  try {
                                    sessionBytesToLoad = Files.readAllBytes(sessionFile.toPath());
                                    sessionFilenameToLoad = sessionFile.getName();
                                  } catch (IOException e) {
                                    FRLogger.warn(
                                        "Failed to read session file: " + sessionFile.getPath());
                                  }
                                } else {
                                  FRLogger.warn(
                                      "Session file not found: " + gs.designSessionFilename);
                                }
                              }

                              if (sessionBytesToLoad != null && job.board != null) {
                                try {
                                  boolean isJsonSession =
                                      (sessionFilenameToLoad != null
                                              && sessionFilenameToLoad
                                                  .toLowerCase(Locale.ROOT)
                                                  .endsWith(".json"))
                                          || RoutingJob.getFileFormat(sessionBytesToLoad)
                                              == FileFormat.KICAD_DESIGN_JSON;

                                  if (isJsonSession) {
                                    FRLogger.info(
                                        "Loading KiCad JSON session data: "
                                            + sessionFilenameToLoad);
                                    try (java.io.Reader jsonReader =
                                        new java.io.InputStreamReader(
                                            new ByteArrayInputStream(sessionBytesToLoad),
                                            StandardCharsets.UTF_8)) {
                                      app.freerouting.io.kicad.KiCadJsonReader.importSession(
                                          jsonReader, job.board);
                                      FRLogger.info("KiCad JSON session loaded successfully");
                                    }
                                  } else {
                                    FRLogger.info(
                                        "Loading SES session data: " + sessionFilenameToLoad);
                                    SesImportSummary summary =
                                        SesReader.read(
                                            new ByteArrayInputStream(sessionBytesToLoad),
                                            job.board);
                                    FRLogger.info(
                                        "SES session loaded: "
                                            + summary.wiresImported()
                                            + " wires, "
                                            + summary.viasImported()
                                            + " vias imported"
                                            + (summary.errorsEncountered() > 0
                                                ? " (" + summary.errorsEncountered() + " errors)"
                                                : ""));
                                  }
                                } catch (Exception e) {
                                  FRLogger.error("Failed to load session data", e);
                                }
                              }

                              // The rules and the session are on the board: scan it now.
                              boardManager.releasePostLoadProcessing();

                              // All pre-checks look fine, start the routing process on a new thread
                              StoppableThread routerThread =
                                  new RoutingJobSchedulerActionThread(job);
                              // Loading took a while; a cancel or timeout in the meantime wins
                              // (cancelJob changes the state under the same lock).
                              boolean stillReady;
                              synchronized (jobs) {
                                stillReady = job.state == RoutingJobState.READY_TO_START;
                                if (stillReady) {
                                  job.thread = routerThread;
                                  job.state = RoutingJobState.RUNNING;
                                }
                              }
                              if (stillReady) {
                                routerThread.start();
                                startedAny = true;
                              } else {
                                FRLogger.info(
                                    "Routing job '"
                                        + job.shortName
                                        + "' left the ready state ("
                                        + job.state
                                        + ") while it was being set up; not started.");
                              }
                            } catch (Throwable e) {
                              // An Error too (an OutOfMemoryError while loading a large board):
                              // only this job fails, and its partly loaded board is let go.
                              FRLogger.error(
                                  "Failed to set up routing job '"
                                      + job.id
                                      + "', it will be terminated.",
                                  e);
                              job.board = null;
                              job.state = RoutingJobState.TERMINATED;
                            }
                          } else {
                            FRLogger.warn("Only DSN and JSON formats are supported as an input.");
                            job.state = RoutingJobState.INVALID;
                            continue;
                          }
                        } else {
                          break;
                        }
                      }
                    }

                    if (!startedAny) {
                      break;
                    }
                  }

                  // Boards are released on this thread only, so never during the set-up above.
                  enforceRetention();

                  // wait for a short time before checking the queue again
                  Thread.sleep(250);
                } catch (InterruptedException e) {
                  FRLogger.error("RoutingJobScheduler thread was interrupted.", e);
                } catch (Throwable e) {
                  // Nothing else restarts this thread: if an Error ended it, no job would ever
                  // start again.
                  FRLogger.error(
                      "RoutingJobScheduler thread encountered an unexpected error and will "
                          + "continue.",
                      e);
                }
              }
            });

    loopThread.setDaemon(true);
    loopThread.start();
  }

  /**
   * Returns the singleton instance of the RoutingJobScheduler.
   *
   * @return The singleton instance.
   */
  public static RoutingJobScheduler getInstance() {
    return instance;
  }

  /**
   * Returns the default maximum number of routing jobs that may run concurrently, calculated
   * dynamically as {@code max(1, CPU cores - 1)}.
   *
   * @return The default maximum number of parallel jobs.
   */
  public static int defaultMaxParallelJobs() {
    return Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
  }

  /**
   * Returns the maximum number of routing jobs that may run concurrently, taken from the
   * api_server.max_parallel_jobs setting. Falls back to {@link #defaultMaxParallelJobs()} when the
   * settings are not loaded yet or the configured value is not positive.
   *
   * @return The maximum number of parallel jobs.
   */
  private static GlobalSettings globalSettings() {
    return GlobalSettings.current();
  }

  public int getMaxParallelJobs() {
    GlobalSettings gs = globalSettings();
    if ((gs != null)
        && (gs.apiServerSettings != null)
        && (gs.apiServerSettings.maxParallelJobs != null)
        && (gs.apiServerSettings.maxParallelJobs > 0)) {
      return gs.apiServerSettings.maxParallelJobs;
    }
    return defaultMaxParallelJobs();
  }

  /**
   * Returns how many finished jobs the scheduler keeps, from {@code api_server.max_finished_jobs}.
   * Values below 1 and unloaded settings use {@link #DEFAULT_MAX_FINISHED_JOBS}.
   */
  int getMaxFinishedJobs() {
    GlobalSettings gs = globalSettings();
    Integer configured =
        gs != null && gs.apiServerSettings != null ? gs.apiServerSettings.maxFinishedJobs : null;
    return configured != null && configured > 0 ? configured : DEFAULT_MAX_FINISHED_JOBS;
  }

  /**
   * Returns how many of the most recently finished jobs keep their board, from {@code
   * api_server.max_finished_job_boards}. Negative values and unloaded settings use twice the number
   * of parallel jobs, at least {@link #DEFAULT_MAX_FINISHED_JOB_BOARDS}.
   */
  int getMaxFinishedJobBoards() {
    GlobalSettings gs = globalSettings();
    Integer configured =
        gs != null && gs.apiServerSettings != null
            ? gs.apiServerSettings.maxFinishedJobBoards
            : null;
    if (configured != null && configured >= 0) {
      return configured;
    }
    return Math.max(DEFAULT_MAX_FINISHED_JOB_BOARDS, 2 * getMaxParallelJobs());
  }

  /**
   * Applies the retention limits: removes the oldest finished jobs beyond {@link
   * #getMaxFinishedJobs()} and releases the boards of the finished jobs older than the newest
   * {@link #getMaxFinishedJobBoards()}. Queued and running jobs, jobs whose routing thread is still
   * alive and the jobs of the primary (GUI) session are never touched. The scheduler thread calls
   * it after each start-up pass; releasing a board elsewhere could race with the set-up of a job
   * that was cancelled while it loaded.
   */
  void enforceRetention() {
    synchronized (jobs) {
      evictFinishedJobs(getMaxFinishedJobs());
      List<RoutingJob> finished = finishedJobsNewestFirst();
      int released = 0;
      for (int i = getMaxFinishedJobBoards(); i < finished.size(); i++) {
        RoutingJob job = finished.get(i);
        if (!job.keepBoardWhenFinished && job.board != null) {
          job.releaseBoard();
          released++;
        }
      }
      if (released > 0) {
        FRLogger.debug("Released the routing boards of " + released + " finished jobs.");
      }
    }
  }

  /**
   * Removes the oldest finished jobs so that at most {@code keep} of them remain. The caller holds
   * the lock of {@link #jobs}.
   *
   * @return the number of finished jobs that remain
   */
  private int evictFinishedJobs(int keep) {
    List<RoutingJob> finished = finishedJobsNewestFirst();
    if (finished.size() <= keep) {
      return finished.size();
    }
    Set<RoutingJob> evicted = Collections.newSetFromMap(new IdentityHashMap<>());
    evicted.addAll(finished.subList(keep, finished.size()));
    this.jobs.removeIf(evicted::contains);
    forgetLogs(evicted);
    FRLogger.debug("Removed the " + evicted.size() + " oldest finished routing jobs.");
    return keep;
  }

  /**
   * Drops the in-memory log entries of removed jobs: the job log endpoints answer 404 for them, and
   * the console and file logs keep the messages.
   */
  private static void forgetLogs(Iterable<RoutingJob> removedJobs) {
    for (RoutingJob job : removedJobs) {
      FRLogger.getLogEntries().removeTopic(job.id);
    }
  }

  /**
   * Returns the jobs that retention may remove or strip, newest first. The caller holds the lock of
   * {@link #jobs}.
   */
  private List<RoutingJob> finishedJobsNewestFirst() {
    List<RoutingJob> finished = new ArrayList<>();
    for (RoutingJob job : this.jobs) {
      if (job != null && isFinished(job) && !isPrimarySessionJob(job)) {
        finished.add(job);
      }
    }
    // The list sort is stable, so jobs that ended at the same instant keep their queue order.
    finished.sort(Comparator.comparing(RoutingJobScheduler::endedAt).reversed());
    return finished;
  }

  /** A finished job is in a terminal state and its routing thread, if any, has ended. */
  private static boolean isFinished(RoutingJob job) {
    RoutingJobState state = job.state;
    StoppableThread thread = job.thread;
    return state != null && state.isTerminal() && (thread == null || !thread.isAlive());
  }

  /** The primary session is the GUI's; its job is reused for every run and owns the GUI board. */
  private static boolean isPrimarySessionJob(RoutingJob job) {
    if (job.sessionId == null) {
      return false;
    }
    Session session = SessionManager.getInstance().getSession(job.sessionId.toString());
    return session != null && session.isPrimary;
  }

  /** When the job ended, or when it was created if it never ran. */
  private static Instant endedAt(RoutingJob job) {
    return job.finishedAt != null ? job.finishedAt : job.createdAt;
  }

  /**
   * Returns the sessions that have a job which is queued, starting, running, stopping or paused, or
   * whose routing thread is still alive.
   */
  public Set<UUID> sessionIdsWithActiveJobs() {
    Set<UUID> active = new HashSet<>();
    synchronized (jobs) {
      for (RoutingJob job : this.jobs) {
        if (job != null && job.sessionId != null && !isFinished(job)) {
          active.add(job.sessionId);
        }
      }
    }
    return active;
  }

  /**
   * Removes the finished jobs of a session and their in-memory log entries; jobs that are still
   * active stay.
   *
   * @param sessionId the session identifier
   */
  public void removeFinishedJobs(UUID sessionId) {
    List<RoutingJob> removed = new ArrayList<>();
    synchronized (jobs) {
      Iterator<RoutingJob> queue = this.jobs.iterator();
      while (queue.hasNext()) {
        RoutingJob job = queue.next();
        if (job != null && sessionId.equals(job.sessionId) && isFinished(job)) {
          queue.remove();
          removed.add(job);
        }
      }
    }
    forgetLogs(removed);
  }

  private String uuidToShortCode(UUID uuid) {
    return uuid.toString().substring(0, 6).toUpperCase(Locale.ROOT);
  }

  /**
   * Enqueues a job to be processed by the router.
   *
   * @param job The job to enqueue.
   * @return the job that was enqueued
   */
  public RoutingJob enqueueJob(RoutingJob job) {
    // Get the session object from the SessionManager and user ID from the job
    UUID sessionId = job.sessionId;
    if (sessionId == null) {
      throw new IllegalArgumentException("The job must have a session ID.");
    }

    var session = SessionManager.getInstance().getSession(sessionId.toString());
    if (session == null) {
      throw new IllegalArgumentException("The session does not exist.");
    }

    UUID userId = session.userId;
    if (userId == null) {
      throw new IllegalArgumentException("The session must have a user ID.");
    }

    job.state = RoutingJobState.QUEUED;

    synchronized (jobs) {
      int finishedJobs = evictFinishedJobs(getMaxFinishedJobs());
      int excess = this.jobs.size() + 1 - MAX_QUEUED_JOBS;
      if (excess > 0) {
        // Make room with the oldest remaining finished jobs before refusing the new one.
        evictFinishedJobs(Math.max(0, finishedJobs - excess));
      }
      if (this.jobs.size() >= MAX_QUEUED_JOBS) {
        throw new IllegalStateException(
            "Maximum job queue capacity reached (" + MAX_QUEUED_JOBS + ").");
      }
      this.jobs.add(job);
    }

    GlobalSettings gs = globalSettings();
    if (gs != null && gs.statistics != null) {
      gs.statistics.incrementJobsStarted();
    }

    return job;
  }

  /**
   * Saves a job and its associated files when job persistence is enabled.
   *
   * @param job the job to save
   */
  public void saveJob(RoutingJob job) {
    GlobalSettings gs = globalSettings();
    if (gs != null && gs.featureFlags != null && gs.featureFlags.saveJobs) {
      String sessionIdString = "null";
      String userIdString = "null";

      try {
        Session session = SessionManager.getInstance().getSession(job.sessionId.toString());

        if (session == null) {
          FRLogger.error(
              "Failed to save job in session '%s' to disk, because the session does not exist."
                  .formatted(job.sessionId),
              null);
        }

        sessionIdString = session.id.toString();
        userIdString = session.userId.toString();

        saveJobToDisk(
            "U-" + uuidToShortCode(session.userId), "S-" + uuidToShortCode(session.id), job);
      } catch (IOException e) {
        FRLogger.error(
            "Failed to save job for user '%s' in session '%s' to disk."
                .formatted(userIdString, sessionIdString),
            e);
      }
    }
  }

  private void saveJobToDisk(String userFolder, String sessionFolder, RoutingJob job)
      throws IOException {
    // Create the user's folder if it doesn't exist
    Path userFolderPath = GlobalSettings.getUserDataPath().resolve("data").resolve(userFolder);

    // Make sure that we have the directory structure in place, and create it if it
    // doesn't exist
    Files.createDirectories(userFolderPath);

    // Check if we already have a directory that has a name with the ending of
    // sessionFolder. Streams over directories must be closed to prevent FD leaks.
    Path sessionFolderPath;
    try (Stream<Path> dirs = Files.list(userFolderPath)) {
      sessionFolderPath =
          dirs.filter(Files::isDirectory)
              .filter(p -> p.getFileName().toString().endsWith(sessionFolder))
              .findFirst()
              .orElse(null);
    }

    if (sessionFolderPath == null) {
      // List all directories in the user folder and check if they start with a number
      // If they do, then they are job folders, and we can get the highest number and
      // increment it
      int jobFolderCount;
      try (Stream<Path> dirs = Files.list(userFolderPath)) {
        jobFolderCount =
            dirs.filter(Files::isDirectory)
                .map(Path::getFileName)
                .map(Path::toString)
                .map(s -> s.split("_")[0]) // Extract the numeric prefix before the underscore
                .filter(s -> s.matches("\\d+")) // Ensure it is numeric
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
      }

      sessionFolderPath =
          userFolderPath.resolve("%04d".formatted(jobFolderCount + 1) + "_" + sessionFolder);
    }

    // Create the session's folder if it doesn't exist
    Files.createDirectories(sessionFolderPath);

    // Save the job to the session's folder using ISO standard date and time format
    String jobFilename =
        "FRJ_"
            + TextManager.convertInstantToString(job.createdAt)
            + "__J-"
            + uuidToShortCode(job.id)
            + ".json";
    Path jobFilePath = sessionFolderPath.resolve(jobFilename);

    try (Writer writer = Files.newBufferedWriter(jobFilePath, StandardCharsets.UTF_8)) {
      GsonProvider.GSON.toJson(job, writer);
    } catch (Exception e) {
      FRLogger.error("Failed to save job '%s' to disk.".formatted(job.id), e);
    }

    // Save the input file if the filename is defined and there is data stored in it
    if (job.input != null
        && job.input.getFilename() != null
        && !job.input.getFilename().isEmpty()
        && job.input.getData() != null) {
      Path inputFilePath = resolveInFolder(sessionFolderPath, job.input.getFilename());
      if (inputFilePath != null) {
        Files.write(inputFilePath, job.input.getData().readAllBytes());
      }
    }

    // Save the output file if the filename is defined and there is data stored in
    // it, or remove any stale persisted preview when output is unavailable
    if (job.output != null
        && job.output.getFilename() != null
        && !job.output.getFilename().isEmpty()) {
      Path outputFilePath = resolveInFolder(sessionFolderPath, job.output.getFilename());
      var outputData = job.output.getData();
      if (outputFilePath == null) {
        FRLogger.warn("Not saving the output of job '%s': invalid file name.".formatted(job.id));
      } else if (outputData != null && outputData.available() > 0) {
        Files.write(outputFilePath, outputData.readAllBytes());
      } else {
        Files.deleteIfExists(outputFilePath);
      }
    }
  }

  /**
   * Returns the file {@code filename} directly inside {@code folder}, or {@code null} when the name
   * is not a plain file name (absolute, with a directory, or {@code ..}).
   */
  static Path resolveInFolder(Path folder, String filename) {
    if (filename == null || filename.isBlank()) {
      return null;
    }
    Path name;
    try {
      name = Path.of(filename);
    } catch (InvalidPathException _) {
      return null;
    }
    if (name.isAbsolute() || name.getNameCount() != 1 || "..".equals(name.toString())) {
      return null;
    }
    Path resolved = folder.resolve(name).normalize();
    return folder.normalize().equals(resolved.getParent()) ? resolved : null;
  }

  /**
   * Returns the position of the job in the queue.
   *
   * @param job The job to get the position of.
   * @return The position of the job in the queue or -1 if the job is not in the queue. 0 means the
   *     job is next in line.
   */
  public int getQueuePosition(RoutingJob job) {
    synchronized (jobs) {
      return this.jobs.indexOf(job);
    }
  }

  /** Returns a snapshot of all jobs currently in the scheduler queue. */
  public RoutingJob[] listJobs() {
    synchronized (jobs) {
      return this.jobs.toArray(RoutingJob[]::new);
    }
  }

  /**
   * Returns jobs belonging to one session.
   *
   * @param sessionId the session identifier
   * @return a snapshot of the session's jobs
   */
  public RoutingJob[] listJobs(String sessionId) {
    synchronized (jobs) {
      return this.jobs.stream()
          .filter(j -> j.sessionId.toString().equals(sessionId))
          .toArray(RoutingJob[]::new);
    }
  }

  /**
   * Returns the jobs of all sessions the caller owns (see {@link Session#isOwnedBy}).
   *
   * @param userId the caller's profile id
   * @param apiKeyHash the hash of the caller's validated API key, or {@code null}
   * @return a snapshot of the matching jobs
   */
  public RoutingJob[] listOwnedJobs(UUID userId, String apiKeyHash) {
    List<RoutingJob> result = new LinkedList<>();
    for (Session session : SessionManager.getInstance().getOwnedSessions(userId, apiKeyHash)) {
      result.addAll(List.of(listJobs(session.id.toString())));
    }
    return result.toArray(RoutingJob[]::new);
  }

  /**
   * Finds a queued job by identifier.
   *
   * @param jobId the job identifier
   * @return the matching job, or {@code null} when it is not queued
   */
  public RoutingJob getJob(String jobId) {
    synchronized (jobs) {
      return this.jobs.stream().filter(j -> j.id.toString().equals(jobId)).findFirst().orElse(null);
    }
  }

  /**
   * Removes all queued jobs belonging to a session.
   *
   * @param sessionId the session identifier
   */
  public void clearJobs(String sessionId) {
    synchronized (jobs) {
      this.jobs.removeIf(j -> j.sessionId.toString().equals(sessionId));
    }
  }

  /**
   * Cancels a job.
   *
   * @param job The job to cancel.
   */
  public void cancelJob(RoutingJob job) {
    if (job == null) {
      return;
    }

    synchronized (jobs) {
      if (job.state == RoutingJobState.QUEUED || job.state == RoutingJobState.READY_TO_START) {
        job.state = RoutingJobState.CANCELLED;
        job.setCancelledByUser(true);
        saveJob(job);
      } else if (job.state == RoutingJobState.RUNNING) {
        if (job.thread instanceof RoutingJobSchedulerActionThread action) {
          // The action thread may assign the terminal state at the same time.
          action.cancelIfRunning();
        } else {
          job.state = RoutingJobState.STOPPING;
          job.setCancelledByUser(true);
          if (job.thread != null) {
            job.thread.requestStop();
          }
        }
        saveJob(job);
      } else if (!job.isCancelledByUser()
          && (job.state != RoutingJobState.COMPLETED)
          && (job.state != RoutingJobState.TIMED_OUT)
          && (job.state != RoutingJobState.TERMINATED)) {
        // If the job is in another state (e.g. PAUSED), we can still cancel it
        job.state = RoutingJobState.CANCELLED;
        job.setCancelledByUser(true);
        saveJob(job);
      }
    }
  }
}
