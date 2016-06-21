package fr.echoes.labs.komea.foundation.plugins.jenkins.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.client.JenkinsHttpClient;
import com.offbytwo.jenkins.client.util.EncodingUtils;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;

import fr.echoes.labs.komea.foundation.plugins.jenkins.JenkinsExtensionException;
import fr.echoes.labs.komea.foundation.plugins.jenkins.utils.DashboardJobNameFormatter;
import fr.echoes.labs.komea.foundation.plugins.jenkins.utils.JenkinsUtils;
import fr.echoes.labs.ksf.cc.extensions.gui.ProjectExtensionConstants;
import fr.echoes.labs.ksf.cc.extensions.services.project.ProjectUtils;
import fr.echoes.labs.ksf.extensions.projects.ProjectDto;



/**
 * Spring Service for working with the foreman API.
 *
 * @author dcollard
 *
 */
@Service
public class JenkinsService implements IJenkinsService {

	private static final String DEVELOP = "develop";

	private static final String MASTER = "master";

	private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsService.class);

	@Autowired
	private JenkinsConfigurationService configurationService;

	@Override
	public void createProject(ProjectDto projectDTO)
			throws JenkinsExtensionException {

		final JenkinsServer jenkins;
		final String projectName = projectDTO.getName();
		final String masterJob = getJobName(projectName, MASTER);
		final String developJob = getJobName(projectName, DEVELOP);

		try {
			final JenkinsHttpClient jenkinsHttpClient = new JenkinsHttpClient(getJenkinsUri());

			jenkins = createJenkinsClient(jenkinsHttpClient);

			final String scmUrl = getProjectScmUrl(projectName);

			if (useFolder()) {

				createFolder(jenkinsHttpClient, projectName);

				final FolderJob projectFolder = getProjectParentFolder(jenkins, projectName);

				String resolvedXmlConfig = createConfigXml(projectName, scmUrl, MASTER);
				jenkins.createJob(projectFolder, masterJob, resolvedXmlConfig, false);

				resolvedXmlConfig = createConfigXml(projectName, scmUrl, DEVELOP);
				jenkins.createJob(projectFolder, developJob, resolvedXmlConfig, false);

			} else {

				String resolvedXmlConfig = createConfigXml(projectName, scmUrl, MASTER);
				jenkins.createJob(masterJob, resolvedXmlConfig, false);

				resolvedXmlConfig = createConfigXml(projectName, scmUrl, DEVELOP);
				jenkins.createJob(developJob, resolvedXmlConfig, false);
			}

			final DashboardJobNameFormatter dashboardNameFormatter = new DashboardJobNameFormatter();
			final List<String> jobs = Lists.newArrayList(
					projectName,
					dashboardNameFormatter.format(masterJob, projectName),
					dashboardNameFormatter.format(developJob, projectName)
			);
			projectDTO.getOtherAttributes().put(ProjectExtensionConstants.CI_JOBS_KEY, jobs);

		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to create Jenkins job", e);
		}
	}

	private void createFolder(JenkinsHttpClient jenkinsHttpClient,
			String folderName) throws IOException {
		LOGGER.info("[jenkins] creating folder '{}'", folderName);
        final ImmutableMap<String, String> params = ImmutableMap.of(
                "mode", "com.cloudbees.hudson.plugins.folder.Folder",
                "name", folderName,
                "from", "",
                "Submit", "OK");
        jenkinsHttpClient.post_form( "/" + "createItem?" , params , false);
        LOGGER.info("[jenkins] folder '{}' created", folderName);
	}

	private URI getJenkinsUri() throws URISyntaxException {
		return new URI(this.configurationService.getUrl());
	}

	private FolderJob getProjectParentFolder(final JenkinsServer jenkins, String projectName) throws IOException {
		LOGGER.info("[jenkins] getting job folder: {}", projectName);
		final JobWithDetails root = jenkins.getJob(projectName);
		final Optional<FolderJob> projectFolder = jenkins.getFolderJob(root);
		final FolderJob folderJob = projectFolder.get();
		return folderJob;
	}

	private String getJobName(String projectName, String branchName) {
		final Map<String, String> variables = new HashMap<String, String>(2);
		variables.put("projectName", projectName);
		variables.put("branchName", branchName);
		return replaceVariables(this.configurationService.getJobNamePattern(), variables);

	}

	private JenkinsServer createJenkinsClient(JenkinsHttpClient jenkinsHttpClient) throws URISyntaxException {
		return new JenkinsServer(jenkinsHttpClient);
	}

	private String createConfigXml(String projectName, String scmUrl, String branchName) throws IOException {

		final Map<String, String> variables = new HashMap<String, String>();

		variables.put("scmUrl", scmUrl);
		variables.put("branchName", branchName);
		variables.put("buildScript", this.configurationService.getBuildScript());
		variables.put("publishScript", this.configurationService.getPublishScript());
		variables.put("projectKey", ProjectUtils.createIdentifier(projectName));

		final URL url = com.google.common.io.Resources.getResource(this.configurationService.getTemplateName());
		return substituteText(url, variables);
	}

	/**
	 * Replaces all the occurrences of variables with their matching values.
	 *
	 * @param url
	 * @param variables the map with the variables' values, can be null.
	 * @return
	 * @throws IOException
	 */
	private String substituteText(URL url, Map<String, String> variables) throws IOException {
		final String templateXml = Resources.toString(url, Charsets.UTF_8);
		final StrSubstitutor sub = new StrSubstitutor(variables);
		final String resolvedXml = sub.replace(templateXml);
		return resolvedXml;
	}

	@Override
	public void deleteProject(String projectName)
			throws JenkinsExtensionException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<JenkinsBuildInfo> getBuildInfo(String projectName)
			throws JenkinsExtensionException {
		final JenkinsServer jenkins;
		try {

			jenkins = createJenkinsClient();

			final boolean useFolder = useFolder();

			final int builsdPerJobLimit = this.configurationService.getBuilsdPerJobLimit();

			final List<Build> builds;
			if (useFolder) {
				builds = getFolderBuilds(jenkins, projectName, builsdPerJobLimit);
			} else {
				builds = new ArrayList<Build>();
				builds.addAll(getJobBuilds(jenkins, projectName, getJobName(projectName, DEVELOP), useFolder, builsdPerJobLimit));
				builds.addAll(getJobBuilds(jenkins, projectName, getJobName(projectName, MASTER), useFolder, builsdPerJobLimit));
			}

			final List<JenkinsBuildInfo> buildsInfo = new ArrayList<JenkinsBuildInfo>(builds.size());
			for (final Build build : builds) {

				buildsInfo.add(createJenkinsBuildInfo(build, projectName));
			}
			return buildsInfo;
		} catch (final JenkinsExtensionException e) {
			throw e;
		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to retrieve build history", e);
		}
	}

	private JenkinsServer createJenkinsClient() throws URISyntaxException {
		final JenkinsHttpClient jenkinsHttpClient = new JenkinsHttpClient(getJenkinsUri());
		return createJenkinsClient(jenkinsHttpClient);
	}

	private List<Build> getJobBuilds(JenkinsServer jenkins, String projectName, String branchName, boolean useFolder, int builsdPerJobLimit)
			throws IOException, JenkinsExtensionException {

		final JobWithDetails root = getJobWithDetails(jenkins, projectName, branchName,
				useFolder);

		final List<Build> builds = root.getBuilds();
		if (builds.size() > builsdPerJobLimit) {
			return  new ArrayList<Build>(builds.subList(0, builsdPerJobLimit));
		}
		return builds;
	}

	private List<Build> getFolderBuilds(JenkinsServer jenkins, String projectName, int builsdPerJobLimit)
			throws IOException, JenkinsExtensionException {

		final List<Build> builds = new ArrayList<Build>();
		final FolderJob projectFolder = getProjectParentFolder(jenkins, projectName);
		final Map<String, Job> jobs = projectFolder.getJobs();
		for (final Map.Entry<String, Job> entry : jobs.entrySet()) {
			final Job job = entry.getValue();
			final JobWithDetails details = job.details();
			final List<Build> jobBuilds = details.getBuilds();
			if (jobBuilds.size() > builsdPerJobLimit) {
				builds.addAll(new ArrayList<Build>(jobBuilds.subList(0, builsdPerJobLimit)));
			} else {
				builds.addAll(jobBuilds);
			}
		}
		return builds;
	}

	private JobWithDetails getJobWithDetails(JenkinsServer jenkins, String projectName,
			String jobName, boolean useFolder) throws IOException,
			JenkinsExtensionException {
		final JobWithDetails root;
		if (useFolder) {
			final FolderJob projectFolder = getProjectParentFolder(jenkins, projectName);
			root = jenkins.getJob(projectFolder, jobName);
		} else {
			root = jenkins.getJob(jobName);
		}

		if (root == null) {
			throw new JenkinsExtensionException("The job \"" + projectName +"\" doesn't exist");
		}
		return root;
	}

	private JenkinsBuildInfo createJenkinsBuildInfo(Build build, String jobName) throws IOException {
		final int buildNumber = build.getNumber();
		final String buildUrl = build.getUrl();
		final BuildWithDetails details = build.details();
		final long timestamp = details.getTimestamp();
		final int duration = details.getDuration();
		final String result = details.getResult().name();
		return new JenkinsBuildInfo(buildNumber, details.getFullDisplayName(), timestamp, duration,  buildUrl, result);
	}

	@Override
	public void createRelease(final ProjectDto project, String releaseVersion) throws JenkinsExtensionException {
		final String projectName = project.getName();
		final String jobName = getReleaseJobName(projectName, releaseVersion);
		final String gitBranchName = getGitReleaseBranchName(projectName, releaseVersion);
		createJob(projectName, jobName, gitBranchName);
		JenkinsUtils.registerJob(project, jobName);
	}

	@Override
	public void createFeature(final ProjectDto project, String featureId, String featureSubject) throws JenkinsExtensionException {
		final String projectName = project.getName();
		final String jobName = getFeatureJobName(projectName, featureId, featureSubject);
		final String gitBranchName = getGitFeatureBranchName(projectName, featureId, featureSubject);
		createJob(projectName, jobName, gitBranchName);
		JenkinsUtils.registerJob(project, jobName);
	}

	private void createJob(String projectName, String jobName, String gitBranchName) throws JenkinsExtensionException {
		final JenkinsServer jenkins;
		try {
			jenkins = createJenkinsClient();

			final String scmUrl = getProjectScmUrl(projectName);

			if (useFolder()) {
				final FolderJob projectFolder = getProjectParentFolder(jenkins, projectName);

				final String resolvedXmlConfig = createConfigXml(projectName, scmUrl, gitBranchName);
				jenkins.createJob(projectFolder, jobName, resolvedXmlConfig, false);
			} else {
				final String resolvedXmlConfig = createConfigXml(projectName, scmUrl, gitBranchName);
				jenkins.createJob(jobName, resolvedXmlConfig, false);
			}

		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to create Jenkins job", e);
		}
	}

	private void deleteJob(String projectName, String branchName) throws JenkinsExtensionException {

		try {
			final JenkinsHttpClient jenkinsHttpClient = new JenkinsHttpClient(new URI(this.configurationService.getUrl()));

			final String path = "/job/" + EncodingUtils.encode(projectName) + "/job/" + EncodingUtils.encode(branchName) + "/doDelete";

			jenkinsHttpClient.post(path, false); // crumbFlag is set to false to avoid "org.apache.http.client.HttpResponseException: Not Found",
			                                     // see https://github.com/jenkinsci/java-client-api/issues/56

		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to delete Jenkins job", e);
		}
	}

	private boolean useFolder() {
		return this.configurationService.useFolders();
	}

	private String getReleaseJobName(String projectName, String releaseVersion) {
		final Map<String, String> variables = new HashMap<String, String>(2);
		variables.put("projectName", projectName);
		variables.put("releaseVersion", releaseVersion);
		return replaceVariables(this.configurationService.getJobReleasePattern(), variables);
	}

	private String getFeatureJobName(String projectName, String featureId,
			String featureSubject) {
		final Map<String, String> variables = new HashMap<String, String>(3);
		variables.put("projectName", projectName);
		variables.put("featureId", featureId);
		variables.put("featureDescription", featureSubject);
		return replaceVariables(this.configurationService.getJobFeaturePattern(), variables);
	}

	@Override
	public String getJobId(String projectName) throws JenkinsExtensionException {

		Objects.requireNonNull(projectName);

		try {

			final JenkinsServer jenkinsClient = createJenkinsClient();
			final Map<String, Job> jobs = jenkinsClient.getJobs();
			for (final Map.Entry<String, Job> entry : jobs.entrySet()) {
				final Job job = entry.getValue();
				if (projectName.equals(job.getName())) {
					return entry.getKey();
				}
			}

		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to retrieve build history", e);
		}

		return null;
	}

	private String getGitReleaseBranchName(String projectName, String releaseVersion) {
		final Map<String, String> variables = new HashMap<String, String>(1);
		variables.put("releaseVersion", releaseVersion);
		return ProjectUtils.createIdentifier(replaceVariables(this.configurationService.getGitReleaseBranchPattern(), variables));
	}

	private String getGitFeatureBranchName(String projectName, String featureId, String featureDescription) {
		final Map<String, String> variables = new HashMap<String, String>(2);
		variables.put("featureId", featureId);
		variables.put("featureDescription", featureDescription);
		return ProjectUtils.createIdentifier(replaceVariables(this.configurationService.getGitFeatureBranchPattern(), variables));
	}

	private String getProjectScmUrl(String projectName) {
		final Map<String, String> variables = new HashMap<String, String>(2);
		variables.put("scmUrl", this.configurationService.getScmUrl());
		variables.put("projectName", projectName);
		variables.put("projectKey", ProjectUtils.createIdentifier(projectName));
		return replaceVariables(this.configurationService.getProjectScmUrlPattern(), variables);
	}

	private String replaceVariables(String str, Map<String, String> variables) {
		final StrSubstitutor sub = new StrSubstitutor(variables);
		sub.setVariablePrefix("%{");
		return sub.replace(str);
	}

	private JenkinsBuildInfo getJobLastBuildInfo(String projectName, String jobName) throws JenkinsExtensionException {
		try {

			final JenkinsServer jenkins = createJenkinsClient();

			final boolean useFolder = useFolder();

			final JobWithDetails jobWithDetails = getJobWithDetails(jenkins, projectName, jobName, useFolder);
			final Build lastBuild = jobWithDetails.getLastBuild();

			return lastBuild == null ? null : createJenkinsBuildInfo(lastBuild, projectName);

		} catch (final JenkinsExtensionException e) {
			throw e;
		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to retrieve build history", e);
		}
	}



	@Override
	public JenkinsBuildInfo getFeatureStatus(String projectName, String featureId, String featureSubject) throws JenkinsExtensionException {
		final String featureJobName = getFeatureJobName(projectName, featureId, featureSubject);
		return getJobLastBuildInfo(projectName, featureJobName);
	}

	@Override
	public JenkinsBuildInfo getReleaseStatus(String projectName, String releaseVersion) throws JenkinsExtensionException {
		final String releaseJobName = getReleaseJobName(projectName, releaseVersion);
		return getJobLastBuildInfo(projectName, releaseJobName);
	}


	@Override
	public void deleteFeatureJob(String projectName, String featureId,
			String featureSubject) throws JenkinsExtensionException {
		final String featureJobName = getFeatureJobName(projectName, featureId, featureSubject);
		deleteJob(projectName, featureJobName);
	}

	@Override
	public void deleteReleaseJob(String projectName, String releaseName)
			throws JenkinsExtensionException {
		final String releaseJobName = getReleaseJobName(projectName, releaseName);
		deleteJob(projectName, releaseJobName);
	}


}
