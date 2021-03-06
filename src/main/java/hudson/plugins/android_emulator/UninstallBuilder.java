package hudson.plugins.android_emulator;

import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.plugins.android_emulator.builder.AbstractBuilder;
import hudson.plugins.android_emulator.sdk.AndroidSdk;
import hudson.plugins.android_emulator.util.Utils;
import hudson.tasks.Builder;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class UninstallBuilder extends AbstractBuilder {

    /** Package ID of the APK to be uninstalled. */
    private final String packageId;

    /** Whether to fail the build if uninstallation isn't successful. */
    private final boolean failOnUninstallFailure;

    @DataBoundConstructor
    @SuppressWarnings("hiding")
    public UninstallBuilder(String packageId, boolean failOnUninstallFailure) {
        this.packageId = Util.fixEmptyAndTrim(packageId);
        this.failOnUninstallFailure = failOnUninstallFailure;
    }

    public String getPackageId() {
        return packageId;
    }

    public boolean shouldFailBuildOnFailure() {
        return failOnUninstallFailure;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        // Discover Android SDK
        AndroidSdk androidSdk = getAndroidSdk(build, launcher, listener);
        if (androidSdk == null) {
            return false;
        }

        // Check whether a value was provided
        final String packageId = getPackageId();
        if (Util.fixEmptyAndTrim(packageId) == null) {
            AndroidEmulator.log(logger, Messages.PACKAGE_ID_NOT_SPECIFIED());
            return false;
        }

        // Expand package ID value
        String expandedPackageId = Utils.expandVariables(build, listener, packageId);

        // Execute uninstallation
        String deviceIdentifier = getDeviceIdentifier(build, listener);
        boolean success = uninstallApk(build, launcher, logger, androidSdk, deviceIdentifier, expandedPackageId);
        if (!success && failOnUninstallFailure) {
            return false;
        }
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Builder> implements Serializable {

        private static final long serialVersionUID = 1L;

        public DescriptorImpl() {
            super(UninstallBuilder.class);
            load();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) {
            save();
            return true;
        }

        @Override
        public String getHelpFile() {
            return Functions.getResourcePath() + "/plugin/ATAF-plugin/help-uninstallPackage.html";
        }

        @Override
        public String getDisplayName() {
            return Messages.UNINSTALL_ANDROID_PACKAGE();
        }

    }

}
