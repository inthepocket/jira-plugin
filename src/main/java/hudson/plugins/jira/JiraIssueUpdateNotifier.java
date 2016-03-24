package hudson.plugins.jira;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Notifier that will mass-update all issues matching a JQL query, using the specified workflow
 * action name (e.g., "Resolve Issue", "Close Issue").
 *
 * @author Thomas Mons thomas.mons@inthepocket.mobi
 */
public class JiraIssueUpdateNotifier extends Notifier {

    private final String jqlSearch;
    private final String workflowActionName;
    private final String comment;

    @DataBoundConstructor
    public JiraIssueUpdateNotifier(String jqlSearch, String workflowActionName, String comment) {
        this.jqlSearch = Util.fixEmptyAndTrim(jqlSearch);
        this.workflowActionName = Util.fixEmptyAndTrim(workflowActionName);
        this.comment = Util.fixEmptyAndTrim(comment);

    }

    /**
     * @return the jql
     */
    public String getJqlSearch() {
        return jqlSearch;
    }

    /**
     * @return the workflowActionName
     */
    public String getWorkflowActionName() {
        return workflowActionName;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {


        String realComment = Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(comment));
        String realJql = Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(jqlSearch));
        String realWorkflowActionName = Util.fixEmptyAndTrim(build.getEnvironment(listener).expand(workflowActionName));

        JiraSite site = JiraSite.get(build.getParent());

        if (site == null) {
            listener.getLogger().println(Messages.Updater_NoJiraSite());
            build.setResult(Result.FAILURE);
        }

        if (StringUtils.isNotEmpty(realWorkflowActionName)) {
            listener.getLogger().println(Messages.JiraIssueUpdateBuilder_UpdatingWithAction(realWorkflowActionName));
        }

        listener.getLogger().println("[JIRA] JQL: " + realJql);

        try {
            if (!site.progressMatchingIssues(realJql, realWorkflowActionName, realComment, listener.getLogger())) {
                listener.getLogger().println(Messages.JiraIssueUpdateBuilder_SomeIssuesFailed());
                build.setResult(Result.UNSTABLE);
            }
        } catch (IOException e) {
            listener.getLogger().println(Messages.JiraIssueUpdateBuilder_Failed());
            e.printStackTrace(listener.getLogger());
        }
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(JiraIssueUpdateNotifier.class);
        }

        /*@Override
        public JiraIssueUpdateNotifier newInstance(StaplerRequest req,
                                                     JSONObject formData) throws FormException {
            return req.bindJSON(JiraIssueUpdateNotifier.class, formData);
        }*/

        public FormValidation doCheckJqlSearch(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error(Messages.JiraIssueUpdateBuilder_NoJqlSearch());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckWorkflowActionName(@QueryParameter String value) {
            if (Util.fixNull(value).trim().length() == 0) {
                return FormValidation.warning(Messages.JiraIssueUpdateBuilder_NoWorkflowAction());
            }

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraIssueUpdateBuilder_DisplayName();
        }


    }
}
