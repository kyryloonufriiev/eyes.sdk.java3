package com.applitools.eyes;

import com.applitools.eyes.config.Configuration;
import com.applitools.utils.ArgumentGuard;

public abstract class EyesBaseConfig {
    // Will be checked <b>before</b> any argument validation. If true,
    // all method will immediately return without performing any action.
    protected boolean isDisabled;

    /**
     * @param isDisabled If true, all interactions with this API will be
     *                   silently ignored.
     */
    public void setIsDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    /**
     * @return Whether eyes is disabled.
     */
    public boolean getIsDisabled() {
        return isDisabled;
    }

    /**
     * @return The viewport size of the AUT.
     */
    protected abstract RectangleSize getViewportSize();

    /**
     * @param size The required viewport size.
     */
    protected abstract Configuration setViewportSize(RectangleSize size);


     protected String getTestName() {
        return getConfiguration().getTestName();
    }

    /**
     * @param appName The name of the application under test.
     */
    public Configuration setAppName(String appName) {
        this.getConfiguration().setAppName(appName);
        return getConfiguration();
    }

    /**
     * @return The name of the application under test.
     */
    protected String getAppName() {
        return getConfiguration().getAppName();
    }

    public Boolean isSendDom() {
        return getConfiguration().isSendDom();
    }

    public Configuration setSendDom(boolean isSendDom) {
        this.getConfiguration().setSendDom(isSendDom);
        return getConfiguration();
    }

    public void setIsDisabled(Boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public FailureReports getFailureReports() {
        return getConfiguration().getFailureReports();
    }

    public Configuration setFailureReports(FailureReports failureReports) {
        getConfiguration().setFailureReports(failureReports);
        return getConfiguration();
    }

    /**
     * @return The currently set batch info.
     */
    public BatchInfo getBatch() {
        return getConfiguration().getBatch();
    }

    /**
     * Updates the match settings to be used for the session.
     * @param defaultMatchSettings The match settings to be used for the session.
     */
    public Configuration setDefaultMatchSettings(ImageMatchSettings
                                                defaultMatchSettings) {
        ArgumentGuard.notNull(defaultMatchSettings, "defaultMatchSettings");
        getConfiguration().setDefaultMatchSettings(defaultMatchSettings);
        return getConfiguration();
    }

    /**
     * @return The match settings used for the session.
     */
    public ImageMatchSettings getDefaultMatchSettings() {
        return getConfiguration().getDefaultMatchSettings();
    }

    /**
     * This function is deprecated. Please use {@link #setDefaultMatchSettings} instead.
     * <p>
     * The test-wide match level to use when checking application screenshot
     * with the expected output.
     * @param matchLevel The match level setting.
     * @return The match settings used for the session.
     * @see MatchLevel
     */
    public Configuration setMatchLevel(MatchLevel matchLevel) {
        getConfiguration().getDefaultMatchSettings().setMatchLevel(matchLevel);
        return getConfiguration();
    }

    public Configuration setIgnoreDisplacements(boolean isIgnoreDisplacements) {
        getConfiguration().setIgnoreDisplacements(isIgnoreDisplacements);
        return getConfiguration();
    }

    protected abstract Configuration getConfiguration();

    /**
     * @return The test-wide match level.
     * @deprecated Please use{@link #getDefaultMatchSettings} instead.
     */
    public MatchLevel getMatchLevel() {
        return getConfiguration().getDefaultMatchSettings().getMatchLevel();
    }

    public boolean getIgnoreDisplacements() {
        return getConfiguration().getIgnoreDisplacements();
    }

    /**
     * @return Whether to ignore or the blinking caret or not when comparing images.
     */
    public boolean getIgnoreCaret() {
        Boolean ignoreCaret = getConfiguration().getDefaultMatchSettings().getIgnoreCaret();
        return ignoreCaret == null ? true : ignoreCaret;
    }

    /**
     * Sets the ignore blinking caret value.
     * @param value The ignore value.
     */
    public Configuration setIgnoreCaret(boolean value) {
        getConfiguration().getDefaultMatchSettings().setIgnoreCaret(value);
        return getConfiguration();
    }

    /**
     * Returns the stitching overlap in pixels.
     */
    public int getStitchOverlap() {
        return getConfiguration().getStitchOverlap();
    }

    /**
     * Sets the stitching overlap in pixels.
     * @param pixels The width (in pixels) of the overlap.
     */
    public Configuration setStitchOverlap(int pixels) {
        this.getConfiguration().setStitchOverlap(pixels);
        return getConfiguration();
    }

    /**
     * @param hostOS The host OS running the AUT.
     */
    public Configuration setHostOS(String hostOS) {

        if (hostOS == null || hostOS.isEmpty()) {
            this.getConfiguration().setHostOS(null);
        } else {
            getConfiguration().setHostOS(hostOS.trim());
        }
        return getConfiguration();
    }

    /**
     * @return get the host OS running the AUT.
     */
    public String getHostOS() {
        return getConfiguration().getHostOS();
    }

    /**
     * @param hostApp The application running the AUT (e.g., Chrome).
     */
    public Configuration setHostApp(String hostApp) {
        if (hostApp == null || hostApp.isEmpty()) {
            this.getConfiguration().setHostApp(null);
        } else {
            this.getConfiguration().setHostApp(hostApp.trim());
        }
        return getConfiguration();
    }

    /**
     * @return The application name running the AUT.
     */
    public String getHostApp() {
        return getConfiguration().getHostApp();
    }

    /**
     * @param baselineName If specified, determines the baseline to compare
     *                     with and disables automatic baseline inference.
     * @deprecated Only available for backward compatibility. See {@link #setBaselineEnvName(String)}.
     */
    public void setBaselineName(String baselineName) {
        setBaselineEnvName(baselineName);
    }

    /**
     * @return The baseline name, if specified.
     * @deprecated Only available for backward compatibility. See {@link #getBaselineEnvName()}.
     */
    @SuppressWarnings("UnusedDeclaration")
    public String getBaselineName() {
        return getBaselineEnvName();
    }

    /**
     * If not {@code null}, determines the name of the environment of the baseline.
     * @param baselineEnvName The name of the baseline's environment.
     */
    public Configuration setBaselineEnvName(String baselineEnvName) {

        if (baselineEnvName == null || baselineEnvName.isEmpty()) {
            this.getConfiguration().setBaselineEnvName(null);
        } else {
            this.getConfiguration().setBaselineEnvName(baselineEnvName.trim());
        }
        return getConfiguration();
    }

    public Configuration setEnvironmentName(String environmentName) {
        return getConfiguration();
    }

    protected String getBaselineEnvName() {
        return getConfiguration().getBaselineEnvName();
    }

    /**
     * Sets the branch in which the baseline for subsequent test runs resides.
     * If the branch does not already exist it will be created under the
     * specified parent branch (see {@link #setParentBranchName}).
     * Changes to the baseline or model of a branch do not propagate to other
     * branches.
     * @param branchName Branch name or {@code null} to specify the default branch.
     */
    public Configuration setBranchName(String branchName) {
        this.getConfiguration().setBranchName(branchName);
        return getConfiguration();
    }

    /**
     * @return The current branch (see {@link #setBranchName(String)}).
     */
    public String getBranchName() {
        return getConfiguration().getBranchName();
    }

    /**
     * Sets the branch under which new branches are created. (see {@link
     * #setBranchName(String)}.
     * @param branchName Branch name or {@code null} to specify the default branch.
     */
    public Configuration setParentBranchName(String branchName) {
        this.getConfiguration().setParentBranchName(branchName);
        return getConfiguration();
    }

    /**
     * @return The name of the current parent branch under which new branches
     * will be created. (see {@link #setParentBranchName(String)}).
     */
    public String getParentBranchName() {
        return getConfiguration().getParentBranchName();
    }

    /**
     * Automatically save differences as a baseline.
     * @param saveDiffs Sets whether to automatically save differences as baseline.
     */
    public Configuration setSaveDiffs(Boolean saveDiffs) {
        this.getConfiguration().setSaveDiffs(saveDiffs);
        return getConfiguration();
    }

    /**
     * Returns whether to automatically save differences as a baseline.
     * @return Whether to automatically save differences as baseline.
     */
    public Boolean getSaveDiffs() {
        return this.getConfiguration().getSaveDiffs();
    }

    /**
     * Sets the maximum time (in ms) a match operation tries to perform a match.
     * @param ms Total number of ms to wait for a match.
     */
    public Configuration setMatchTimeout(int ms) {
        final int MIN_MATCH_TIMEOUT = 500;
        if (getIsDisabled()) {
            return getConfiguration();
        }

        if ((ms != 0) && (MIN_MATCH_TIMEOUT > ms)) {
            throw new IllegalArgumentException("Match timeout must be set in milliseconds, and must be > " +
                    MIN_MATCH_TIMEOUT);
        }

        this.getConfiguration().setMatchTimeout(ms);

        return getConfiguration();
    }

    /**
     * @return The maximum time in ms
     * (RegionProvider, String, boolean, int)} waits for a match.
     */
    public int getMatchTimeout() {
        return this.getConfiguration().getMatchTimeout();
    }

    /**
     * Set whether or not new tests are saved by default.
     * @param saveNewTests True if new tests should be saved by default. False otherwise.
     */
    public Configuration setSaveNewTests(boolean saveNewTests) {
        this.getConfiguration().setSaveNewTests(saveNewTests);
        return getConfiguration();
    }

    /**
     * @return True if new tests are saved by default.
     */
    public boolean getSaveNewTests() {
        return getConfiguration().getSaveNewTests();
    }

    /**
     * Set whether or not failed tests are saved by default.
     * @param saveFailedTests True if failed tests should be saved by default, false otherwise.
     */
    public Configuration setSaveFailedTests(boolean saveFailedTests) {
        this.getConfiguration().setSaveFailedTests(saveFailedTests);
        return getConfiguration();
    }

    /**
     * @return True if failed tests are saved by default.
     */
    public boolean getSaveFailedTests() {
        return getConfiguration().getSaveFailedTests();
    }

    public Configuration setAgentId(String agentId) {
        return getConfiguration().setAgentId(agentId);
    }

    public String getAgentId() {
        return getConfiguration().getAgentId();
    }

    /**
     * Sets the batch in which context future tests will run or {@code null}
     * if tests are to run standalone.
     * @param batch The batch info to set.
     */
    public Configuration setBatch(BatchInfo batch) {
        if (getIsDisabled()) {
            return getConfiguration();
        }
        this.getConfiguration().setBatch(batch);
        return getConfiguration();
    }

    public String getEnvironmentName() {
        return null;
    }

    /**
     * If not {@code null} specifies a name for the environment in which the application under test is running.
     * @param envName The name of the environment of the baseline.
     */
    public void setEnvName(String envName) {
        if (envName == null || envName.isEmpty()) {
            this.getConfiguration().setEnvironmentName(null);
        } else {
            this.getConfiguration().setEnvironmentName(envName.trim());
        }
    }

    /**
     * If not {@code null} specifies a name for the environment in which the application under test is running.
     * @return The name of the environment of the baseline, or {@code null} if no such name was set.
     */
    public String getEnvName() {
        return getConfiguration().getEnvironmentName();
    }
}
