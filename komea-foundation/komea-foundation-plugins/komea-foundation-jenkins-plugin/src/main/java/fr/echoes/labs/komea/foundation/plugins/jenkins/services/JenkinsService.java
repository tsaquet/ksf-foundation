package fr.echoes.labs.komea.foundation.plugins.jenkins.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.JobWithDetails;

import fr.echoes.labs.komea.foundation.plugins.jenkins.JenkinsExtensionException;



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
	public void createProject(String projectName)
			throws JenkinsExtensionException {
		final JenkinsServer jenkins;
		try {
			jenkins = createJenkinsClient();

			final String resolvedXmlConfig = createXmlConfig(projectName);

			if (this.configurationService.useFolders()) {
				jenkins.createFolder(projectName);
				final JobWithDetails root = jenkins.getJob(projectName);
				final Optional<FolderJob> projectFolder = jenkins.getFolderJob(root);
				jenkins.createJob(projectFolder.get(), getJobName(projectName, MASTER), resolvedXmlConfig);
				jenkins.createJob(projectFolder.get(), getJobName(projectName, DEVELOP), resolvedXmlConfig);

			} else {
				jenkins.createJob(getJobName(projectName, MASTER), resolvedXmlConfig);
				jenkins.createJob(getJobName(projectName, DEVELOP), resolvedXmlConfig);
			}


		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to create Jenkins job", e);
		}
	}

	private String getJobName(String projectName, String branchName) {
		return projectName + " - " + branchName;
	}

	private JenkinsServer createJenkinsClient() throws URISyntaxException {
		return new JenkinsServer(new URI(this.configurationService.getUrl()));
	}

	private String createXmlConfig(String projectName) throws IOException {
		final URL url = com.google.common.io.Resources.getResource(this.configurationService.getTemplateName());

		final String templateXml = Resources.toString(url, Charsets.UTF_8);

		final Map<String, String> variables = new HashMap<String, String>();

		final String scmUrl = this.configurationService.getScmUrl() + '/' + projectName + ".git";

		variables.put("scmUrl", scmUrl);

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
		JenkinsServer jenkins;
		try {
			
			jenkins = createJenkinsClient();
			final JobWithDetails root = jenkins.getJob(projectName);
			if (root == null) {
				throw new JenkinsExtensionException("The job \"" + projectName +"\" doesn't exist");
			}

			final List<Build> builds = root.getBuilds();
			final List<JenkinsBuildInfo> buildsInfo = new ArrayList<JenkinsBuildInfo>(builds.size());
			for (final Build build : builds) {
				
				buildsInfo.add(createJenkinsBuildInfo(build, projectName));
			}
			return buildsInfo;
		} catch (JenkinsExtensionException e) {
			throw e;
		} catch (final Exception e) {
			throw new JenkinsExtensionException("Failed to retrieve build history", e);
		}
	}

	private JenkinsBuildInfo createJenkinsBuildInfo(Build build, String jobName) throws IOException {
		final int buildNumber = build.getNumber();
		final String buildUrl = build.getUrl();
		final BuildWithDetails details = build.details();
		final long timestamp = details.getTimestamp();
		final int duration = details.getDuration();
		final String result = details.getResult().name();
		return new JenkinsBuildInfo(buildNumber, jobName, timestamp, duration,  buildUrl, result);
	}
}
