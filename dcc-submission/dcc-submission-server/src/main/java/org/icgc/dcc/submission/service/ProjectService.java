package org.icgc.dcc.submission.service;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.repository.ProjectRepository;

import com.google.code.morphia.Key;
import com.google.inject.Inject;

@Slf4j
@NoArgsConstructor
@RequiredArgsConstructor(onConstructor = @_({ @Inject }))
public class ProjectService {

	@NonNull
	private ProjectRepository projectRepository;

	public Project find(String projectKey) {
		log.info("Request for Project '{}'", projectKey);
		return projectRepository.find(projectKey);
	}

	public Project findForUser(String projectKey, String username) {
		log.info("Request for Project '{}' for ", projectKey, username);
		return projectRepository.findForUser(projectKey, username);
	}

	public Set<Project> findAll() {
		log.info("Request to find all Projects");
		return projectRepository.findAll();
	}

	public Set<Project> findAllForUser(String username) {
		log.info("Request to find Projects for User '{}'", username);
		return projectRepository.findAllForUser(username);
	}

	public Key<Project> add(Project project) {
		log.info("Adding Project '{}'", project);

		return projectRepository.upsert(project);
	}

	public Key<Project> update(Project project) {
		log.info("Updating Project '{}'", project);

		return projectRepository.upsert(clean(project));
	}

	public Project clean(Project dirty) {
		log.info("Cleaning Project '{}'", dirty);
		val clean = new Project(dirty.getKey(), dirty.getName());
		clean.setAlias(dirty.getAlias());

		log.info("Returing cleaned Project '{}'", clean);
		return clean;
	}

	public Set<Submission> extractSubmissions(Set<Release> releases,
			String projectKey) {
		Set<Submission> submissions = newHashSet();

		for (val release : releases) {
			try {
				submissions.add(release.getSubmission(projectKey));
			} catch (ReleaseException e) {
				log.info(
						"Submission for Project '{}' not found in Release '{}'",
						projectKey, release.getName());
			}
		}

		return submissions;
	}

}
