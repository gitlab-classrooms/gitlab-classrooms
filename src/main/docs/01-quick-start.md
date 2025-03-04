# Quick Start

This guide will help start a GitLab Classrooms instance quickly.

## With Docker

!!! info

    Pre-requisites :

    * Docker installed on your system
    * a running PostgreSQL database
    * access to a GitLab instance

Run the following command :

```shell
docker run -d \
  --name gitlab-classrooms-app \
  -p 8080:8080 \
  -e POSTGRESQL_ADDON_HOST=<your-postgresql-host> \
  -e POSTGRESQL_ADDON_PORT=<your-postgresql-port> \
  -e POSTGRESQL_ADDON_DB=<your-database-name> \
  -e POSTGRESQL_ADDON_USER=<your-database-user> \
  -e POSTGRESQL_ADDON_PASSWORD=<your-database-password> \
  -e GITLAB_URL=<your-gitlab-url> \
  -e GITLAB_CLIENT_ID=<your-gitlab-client-id> \
  -e GITLAB_CLIENT_SECRET=<your-gitlab-client-secret> \
  gitlabclassrooms/app:latest
```

Replace the following placeholders with your actual PostgreSQL database details:

* `<your-postgresql-host>`: The hostname or IP address of your PostgreSQL server
* `<your-postgresql-port>`: The port number on which PostgreSQL is running (usually 5432)
* `<your-database-name>`: The name of the database for GitLab Classrooms
* `<your-database-user>`: The username for accessing the database
* `<your-database-password>`: The password for the database user

See [Database configuration](./configuration/01-configure-database.md) for database configuration details.

Replace the following placeholders with your GitLab instance details:

* `<your-gitlab-url>`: The URL of your GitLab instance (e.g., https://gitlab.com)
* `<your-gitlab-client-id>`: The Application ID from your GitLab OAuth application
* `<your-gitlab-client-secret>`: The Secret from your GitLab OAuth application

See [GitLab configuration](./configuration/02-configure-gitlab-connectivity.md) for GitLab connectivity configuration details.

GitLab Classrooms should be available at [http://localhost:8080](http://localhost:8080).