package fr.echoes.labs.komea.foundation.plugins.git.extensions;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.tocea.corolla.products.dao.IProjectDAO;

import fr.echoes.labs.komea.foundation.plugins.git.services.GitErrorHandlingService;
import fr.echoes.labs.ksf.extensions.annotations.Extension;
import fr.echoes.labs.ksf.extensions.projects.IProjectLifecycleExtension;
import fr.echoes.labs.ksf.extensions.projects.ProjectDto;
import fr.echoes.labs.ksf.users.security.api.ICurrentUserService;

/**
 * @author dcollard
 *
 */
@Extension
public class GitProjectLifeCycleExtension implements IProjectLifecycleExtension {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitProjectLifeCycleExtension.class);

	@Autowired
	private GitErrorHandlingService errorHandler;

	@Autowired
	private IProjectDAO projectDAO;

	@Autowired
	private ApplicationContext applicationContext;

	private ICurrentUserService currentUserService;

	public void init() {

		if (this.currentUserService == null) {
			this.currentUserService = this.applicationContext.getBean(ICurrentUserService.class);
		}
	}

	@Override
	public void notifyCreatedProject(ProjectDto project) {

		init();

		final String logginName = this.currentUserService.getCurrentUserLogin();

		if (StringUtils.isEmpty(logginName)) {
			LOGGER.error("[Git] No user found. Aborting project creation in Git module");
			return;
		}

		LOGGER.info("[Git] project {} creation detected [demanded by: {}]", project.getKey(), logginName);
	}

	@Override
	public void notifyDeletedProject(ProjectDto project) {

	}

	@Override
	public void notifyDuplicatedProject(ProjectDto _project) {
		// TODO Auto-generated method stub

	}

	@Override
	public void notifyUpdatedProject(ProjectDto _project) {
		// TODO Auto-generated method stub

	}

}
