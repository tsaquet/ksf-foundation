package fr.echoes.lab.ksf.cc.plugins.redmine.extensions;

import org.springframework.beans.factory.annotation.Autowired;

import fr.echoes.lab.ksf.cc.extensions.gui.project.dashboard.IProjectDashboardExtension;
import fr.echoes.lab.ksf.cc.extensions.gui.project.dashboard.IProjectDashboardWidgets;
import fr.echoes.lab.ksf.extensions.annotations.Extension;

@Extension
public class RedmineProjectDashboardExtension implements IProjectDashboardExtension {

	@Autowired
	public RedmineProjectDashboardWidget projectDashboardWidget;

	@Override
	public void reclaimProjectDashboardWidget(final IProjectDashboardWidgets widgets) {
		widgets.addWidget(this.projectDashboardWidget);
	}

	@Override
	public String toString() {
		return "Redmine Dashboard Widget";
	}
}
