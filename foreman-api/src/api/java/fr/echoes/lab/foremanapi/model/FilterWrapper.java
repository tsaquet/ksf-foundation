package fr.echoes.lab.foremanapi.model;

import org.codehaus.jackson.annotate.JsonProperty;

import fr.echoes.lab.foremanapi.NewFilter;

public class FilterWrapper {

	@JsonProperty("filter")
	public NewFilter filter;

	public NewFilter getFilter() {
		return this.filter;
	}

	public void setFilter(NewFilter filter) {
		this.filter = filter;
	}
}
