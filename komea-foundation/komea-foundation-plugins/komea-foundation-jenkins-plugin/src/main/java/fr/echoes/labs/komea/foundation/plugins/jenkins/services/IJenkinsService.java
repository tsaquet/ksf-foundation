package fr.echoes.labs.komea.foundation.plugins.jenkins.services;

import java.util.List;

import fr.echoes.labs.komea.foundation.plugins.jenkins.JenkinsExtensionException;


/**
 * @author dcollard
 *
 */
public interface IJenkinsService {

	/**
	 * Creates a new project.
	 *
	 * @param projectName the project name.
	 * @return
	 */
	public void createProject(String projectName) throws JenkinsExtensionException;

	/**
	 * Deletes the given project.
	 *
	 * @param projectName the project key.
	 * @return
	 */
	public void deleteProject(String projectName) throws JenkinsExtensionException;

	/**
	 * Returns the build history.
	 *
	 * @param projectName the project name.
	 * @return a list of {@link JenkinsBuildInfo}. Is never {@code null}.
	 * @throws JenkinsExtensionException
	 */
	public List<JenkinsBuildInfo> getBuildInfo(String projectName) throws JenkinsExtensionException;

	/**
	 * Creates a new Jenkins job for the given release.
	 *
	 * @param projectName the project name
	 * @param releaseVersion the release version
	 * @throws JenkinsExtensionException
	 */
	public void createRelease(String name, String releaseVersion) throws JenkinsExtensionException;

	/**
	 * Creates a new Jenkins job for the given feature.
	 *
	 * @param projectName the project name
	 * @param featureId the feature ID
	 * @param featureSubject the feature subject
	 * @throws JenkinsExtensionException
	 */	
	public void createFeature(String projectName, String featureId, String featureSubject) throws JenkinsExtensionException;
	
	/**
	 * Returns the Jenkins job ID.
	 * 
	 * @param projectName the project name.
	 * @return the job ID.
	 * @throws JenkinsExtensionException
	 */
	public String getJobId(String projectName) throws JenkinsExtensionException;
}
