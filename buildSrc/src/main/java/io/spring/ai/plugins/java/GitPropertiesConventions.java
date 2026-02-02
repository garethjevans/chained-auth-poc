package io.spring.ai.plugins.java;

import com.gorylenko.GitPropertiesPlugin;
import com.gorylenko.GitPropertiesPluginExtension;
import org.gradle.api.Project;

import java.util.List;

class GitPropertiesConventions {

    public void apply(Project project) {
        project.getPluginManager().apply(GitPropertiesPlugin.class);
        GitPropertiesPluginExtension git = project.getExtensions().getByType(GitPropertiesPluginExtension.class);
        git.setFailOnNoGitDirectory(false);
        git.setKeys(
                List.of(
                        "git.branch",
                        "git.build.version",
                        "git.closest.tag.commit.count",
                        "git.closest.tag.name",
                        "git.commit.id",
                        "git.commit.id.abbrev",
                        "git.commit.id.describe",
                        "git.commit.time",
                        "git.dirty",
                        "git.tags"
                )
        );

        // setting git root directory is required as of version 2.5.0 of the plugin
        var dotGitDir = project.getRootProject().getLayout().getProjectDirectory().dir(".git");
        git.getDotGitDirectory().set(dotGitDir);

        project.getLogger().info("GitProperties Plugin configured");
    }
}
