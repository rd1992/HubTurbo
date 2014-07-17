package service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.OrganizationService;
import org.eclipse.egit.github.core.service.RepositoryService;

public class RepositoryServiceExtended extends RepositoryService{
	private GitHubClientExtended ghClient;
	private OrganizationService orgService;
	
	public RepositoryServiceExtended(GitHubClientExtended ghClient){
		this.ghClient = ghClient;
		orgService = new OrganizationService(this.ghClient);
	}
	
	private List<User> getOrganisations() throws IOException{
		return orgService.getOrganizations();
	}
	
	private List<Repository> getOrganisationRepos(User org){
		try {
			return getOrgRepositories(org.getLogin());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<Repository>();
	}
	
	public List<Repository> getOrganisationRepositories() throws IOException{
		List<User> orgs = getOrganisations();
		return orgs.stream()
			.map(org -> getOrganisationRepos(org))
			.reduce(new ArrayList<Repository>(), 
							(l, item) -> {l.addAll(item); 
										  return l;});
	}
	
	/**
	 * Returns a list of all repositories the user owns/contributes 
	 * and repositories belonging to organisations the user is a member of
	 * */
	
	public List<Repository> getAllRepositories() throws IOException{
		HashSet<Repository> result = new HashSet<Repository>(getRepositories());
		result.addAll(getOrganisationRepositories());
		return new ArrayList<Repository>(result);
	}
	
	public List<String> getAllRepositoriesNames() throws IOException{
		return getAllRepositories().stream()
									   .map(repo -> repo.getName())
									   .collect(Collectors.toList());
	}
	
	public List<String> getRepositoriesNames() throws IOException{
		return getRepositories().stream()
								.map(repo -> repo.getName())
								.collect(Collectors.toList());
	}
	
}
