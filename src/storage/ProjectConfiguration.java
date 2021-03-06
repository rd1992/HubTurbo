package storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstractions for the contents of the project configuration file.
 */
public class ProjectConfiguration {
	
	private List<String> nonInheritedLabels = new ArrayList<String>();
	private List<String> openStatusLabels = new ArrayList<String>();
	private List<String> closedStatusLabels = new ArrayList<String>();
	
	public ProjectConfiguration() {

		// Defaults for every repository
		nonInheritedLabels.add("status.");
		openStatusLabels.add("status.open");
		closedStatusLabels.add("status.closed");
	}

	private List<String> getOpenStatusLabels() {
		return Collections.unmodifiableList(openStatusLabels);
	}
	
	private List<String> getClosedStatusLabels() {
		return Collections.unmodifiableList(closedStatusLabels);
	}
	
	public List<String> getStatusLabels() {
		List<String> statusLabels = new ArrayList<String>();
		statusLabels.addAll(getOpenStatusLabels());
		statusLabels.addAll(getClosedStatusLabels());
		return statusLabels;
	}

	public boolean isNonInheritedLabel(String label) {
		for (String nonInherited : nonInheritedLabels) {
			if (label.contains(nonInherited)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isStatusLabel(String label){
		return isOpenStatusLabel(label) || isClosedStatusLabel(label);
	}

	public boolean isOpenStatusLabel(String label) {
		assert openStatusLabels != null;
		for (String openLabel : openStatusLabels) {
			if (label.equalsIgnoreCase(openLabel)) {
				return true;
			}
		}
		return false;
	}

	public boolean isClosedStatusLabel(String label) {
		assert closedStatusLabels != null;
		for (String closedLabel : closedStatusLabels) {
			if (label.equalsIgnoreCase(closedLabel)) {
				return true;
			}
		}
		return false;
	}
}
