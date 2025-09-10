package fr.univ_lille.gitlab.classrooms.gitlab;

import org.gitlab4j.api.GitLabApiException;

public class GitLabAuthenticationException extends RuntimeException {

    public GitLabAuthenticationException(String message) {
        super(message);
    }

    public GitLabAuthenticationException(String message, GitLabApiException cause) {
        super(message, cause);
    }
}
