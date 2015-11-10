package fr.echoes.lab.ksf.cc.extensions.gui.project.dashboard;

import fr.echoes.lab.ksf.extensions.api.IExtension;

/**
 * This extension points allows a plugin to add a new widget to the project
 * dashboard.
 *
 * @author sleroy
 *
 */
public interface IProjectDashboardExtension  extends IExtension{

	void reclaimProjectDashboardWidget(IProjectDashboardWidgets _widgets);
}
