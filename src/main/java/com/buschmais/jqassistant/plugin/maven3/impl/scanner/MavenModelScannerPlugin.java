package com.buschmais.jqassistant.plugin.maven3.impl.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.AbstractDependencyDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ArrayValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.PropertyDescriptor;
import com.buschmais.jqassistant.plugin.common.api.model.ValueDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.maven3.api.artifact.ArtifactResolver;
import com.buschmais.jqassistant.plugin.maven3.api.artifact.Coordinates;
import com.buschmais.jqassistant.plugin.maven3.api.artifact.DependencyCoordinates;
import com.buschmais.jqassistant.plugin.maven3.api.artifact.ModelCoordinates;
import com.buschmais.jqassistant.plugin.maven3.api.artifact.ParentCoordinates;
import com.buschmais.jqassistant.plugin.maven3.api.artifact.PluginCoordinates;
import com.buschmais.jqassistant.plugin.maven3.api.model.BaseProfileDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.ConfigurableDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.EffectiveDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenActivationFileDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenActivationOSDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenArtifactDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenConfigurationDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenContributorDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenDependencyDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenDependentDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenDeveloperDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenExcludesDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenExecutionGoalDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenLicenseDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenModuleDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenOrganizationDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenParticipantRoleDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenPluginDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenPluginExecutionDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenPomDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenProfileActivationDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenProfileDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenProjectParticipantDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenRepositoryDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.MavenScmDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.PluginDependsOnDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.PomDeclaresDependencyDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.PomManagesDependencyDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.ProfileDeclaresDependencyDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.model.ProfileManagesDependencyDescriptor;
import com.buschmais.jqassistant.plugin.maven3.api.scanner.EffectiveModel;
import com.buschmais.jqassistant.plugin.maven3.api.scanner.MavenRepositoryResolver;
import com.buschmais.jqassistant.plugin.maven3.impl.scanner.artifact.MavenArtifactResolver;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.apache.maven.model.RepositoryPolicy;
import org.apache.maven.model.Scm;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

/**
 * Scans Maven model instances.
 *
 * This plugin requires an instance of {@link MavenPomDescriptor} in the scanner
 * context which will be enriched.
 *
 * @author ronald.kunzmann@buschmais.com
 */
public class MavenModelScannerPlugin extends AbstractScannerPlugin<Model, MavenPomDescriptor> {

    @Override
    protected void configure() {
        getScannerContext().push(ArtifactResolver.class, new MavenArtifactResolver());
    }

    @Override
    public Class<? extends Model> getType() {
        return Model.class;
    }

    @Override
    public Class<MavenPomDescriptor> getDescriptorType() {
        return MavenPomDescriptor.class;
    }

    @Override
    public boolean accepts(Model item, String path, Scope scope) {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public MavenPomDescriptor scan(Model model, String path, Scope scope, Scanner scanner) {
        MavenPomDescriptor pomDescriptor = createMavenPomDescriptor(model, scanner);
        ScannerContext scannerContext = scanner.getContext();
        Store store = scannerContext.getStore();
        addParent(pomDescriptor, model, scannerContext);
        addProfiles(pomDescriptor, model, scannerContext);
        addProperties(pomDescriptor, model.getProperties(), store);
        addModules(pomDescriptor, model.getModules(), store);
        addDependencies(pomDescriptor, PomDeclaresDependencyDescriptor.class, PomManagesDependencyDescriptor.class, model, scannerContext);
        addManagedPlugins(pomDescriptor, model.getBuild(), scannerContext);
        addPlugins(pomDescriptor, model.getBuild(), scannerContext);
        addLicenses(pomDescriptor, model, store);
        addDevelopers(pomDescriptor, model, store);
        addContributors(pomDescriptor, model, store);
        addOrganization(pomDescriptor, model, store);
        addRepository(of(pomDescriptor), model.getRepositories(), store);
        addScmInformation(pomDescriptor, model.getScm(), store);
        return pomDescriptor;
    }

    private void addScmInformation(MavenPomDescriptor pomDescriptor, Scm scmInformation, Store store) {
        ofNullable(scmInformation).ifPresent(scm -> {
            MavenScmDescriptor scmDescriptor = store.create(MavenScmDescriptor.class);

            ofNullable(scm.getConnection()).ifPresent(scmDescriptor::setConnection);
            ofNullable(scm.getDeveloperConnection()).ifPresent(scmDescriptor::setDeveloperConnection);
            ofNullable(scm.getTag()).ifPresent(scmDescriptor::setTag);
            ofNullable(scm.getUrl()).ifPresent(scmDescriptor::setUrl);

            pomDescriptor.setScm(scmDescriptor);
        });
    }

    private void addOrganization(MavenPomDescriptor pomDescriptor, Model model, Store store) {
        Organization mavenOrganization = model.getOrganization();

        if (null != mavenOrganization) {
            MavenOrganizationDescriptor organization = store.create(MavenOrganizationDescriptor.class);
            organization.setName(mavenOrganization.getName());
            organization.setUrl(mavenOrganization.getUrl());

            pomDescriptor.setOrganization(organization);
        }
    }

    private void addContributors(MavenPomDescriptor pomDescriptor, Model model, Store store) {
        List<Contributor> contributors = model.getContributors();

        for (Contributor contributor : contributors) {
            MavenContributorDescriptor contributorDescriptor = store.create(MavenContributorDescriptor.class);

            addCommonParticipantAttributes(contributorDescriptor, contributor, store);

            pomDescriptor.getContributors().add(contributorDescriptor);
        }
    }

    private void addCommonParticipantAttributes(MavenProjectParticipantDescriptor participant, Contributor contributor, Store store) {
        participant.setName(contributor.getName());

        participant.setEmail(contributor.getEmail());
        participant.setUrl(contributor.getUrl());
        participant.setOrganization(contributor.getOrganization());
        participant.setOrganizationUrl(contributor.getOrganizationUrl());
        participant.setTimezone(contributor.getTimezone());

        if (contributor.getRoles() != null) {
            for (String role : contributor.getRoles()) {
                MavenParticipantRoleDescriptor developerRoleDescriptor = store.create(MavenParticipantRoleDescriptor.class);
                developerRoleDescriptor.setName(role);
                participant.getRoles().add(developerRoleDescriptor);
            }
        }

    }

    /**
     * Create the descriptor and set base information.
     *
     * @param model
     *            The model.
     * @param scanner
     *            The scanner.
     * @return The descriptor.
     */
    protected MavenPomDescriptor createMavenPomDescriptor(Model model, Scanner scanner) {
        ScannerContext context = scanner.getContext();
        MavenPomDescriptor pomDescriptor = context.peek(MavenPomDescriptor.class);
        if (model instanceof EffectiveModel) {
            context.getStore().addDescriptorType(pomDescriptor, EffectiveDescriptor.class);
        }
        pomDescriptor.setName(model.getName());
        pomDescriptor.setGroupId(model.getGroupId());
        pomDescriptor.setArtifactId(model.getArtifactId());
        pomDescriptor.setPackaging(model.getPackaging());
        pomDescriptor.setVersion(model.getVersion());
        pomDescriptor.setUrl(model.getUrl());
        Coordinates artifactCoordinates = new ModelCoordinates(model);
        MavenArtifactDescriptor artifact = context.peek(ArtifactResolver.class).resolve(artifactCoordinates, context);
        pomDescriptor.getDescribes().add(artifact);
        return pomDescriptor;
    }

    /**
     * Adds activation information for the given profile.
     *
     * @param mavenProfileDescriptor
     *            The profile descriptor.
     * @param activation
     *            The activation information.
     * @param store
     *            The database.
     */
    private void addActivation(MavenProfileDescriptor mavenProfileDescriptor, Activation activation, Store store) {
        if (null == activation) {
            return;
        }
        MavenProfileActivationDescriptor profileActivationDescriptor = store.create(MavenProfileActivationDescriptor.class);
        mavenProfileDescriptor.setActivation(profileActivationDescriptor);

        profileActivationDescriptor.setJdk(activation.getJdk());
        profileActivationDescriptor.setActiveByDefault(activation.isActiveByDefault());

        ActivationFile activationFile = activation.getFile();
        if (null != activationFile) {
            MavenActivationFileDescriptor activationFileDescriptor = store.create(MavenActivationFileDescriptor.class);
            profileActivationDescriptor.setActivationFile(activationFileDescriptor);
            activationFileDescriptor.setExists(activationFile.getExists());
            activationFileDescriptor.setMissing(activationFile.getMissing());
        }
        ActivationOS os = activation.getOs();
        if (null != os) {
            MavenActivationOSDescriptor osDescriptor = store.create(MavenActivationOSDescriptor.class);
            profileActivationDescriptor.setActivationOS(osDescriptor);
            osDescriptor.setArch(os.getArch());
            osDescriptor.setFamily(os.getFamily());
            osDescriptor.setName(os.getName());
            osDescriptor.setVersion(os.getVersion());
        }
        ActivationProperty property = activation.getProperty();
        if (null != property) {
            PropertyDescriptor propertyDescriptor = store.create(PropertyDescriptor.class);
            profileActivationDescriptor.setProperty(propertyDescriptor);
            propertyDescriptor.setName(property.getName());
            propertyDescriptor.setValue(property.getValue());
        }
    }

    /**
     * Adds configuration information.
     *
     * @param configurableDescriptor
     *            The descriptor for the configured element (Plugin,
     *            PluginExecution).
     * @param config
     *            The configuration information.
     * @param store
     *            The database.
     */
    private void addConfiguration(ConfigurableDescriptor configurableDescriptor, Xpp3Dom config, Store store) {
        if (null == config) {
            return;
        }
        MavenConfigurationDescriptor configDescriptor = store.create(MavenConfigurationDescriptor.class);
        configurableDescriptor.setConfiguration(configDescriptor);
        Xpp3Dom[] children = config.getChildren();
        for (Xpp3Dom child : children) {
            configDescriptor.getValues().add(getConfigChildNodes(child, store));
        }
    }

    /**
     * Adds declared and managed dependencies to the given
     * {@link MavenDependentDescriptor}.
     *
     * @param dependentDescriptor
     *            The {@link MavenDependentDescriptor}.
     * @param model
     *            The {@link ModelBase} providing the dependencies.
     * @param scannerContext
     *            The scanner context.
     */
    private void addDependencies(MavenDependentDescriptor dependentDescriptor, Class<? extends AbstractDependencyDescriptor> declaresDependencyType,
            Class<? extends AbstractDependencyDescriptor> managesDependencyType, ModelBase model, ScannerContext scannerContext) {
        dependentDescriptor.getDeclaresDependencies()
                .addAll(getDependencies(dependentDescriptor, model.getDependencies(), declaresDependencyType, scannerContext));
        dependentDescriptor.getManagesDependencies()
                .addAll(addManagedDependencies(dependentDescriptor, model.getDependencyManagement(), scannerContext, managesDependencyType));
    }

    /**
     * Adds information about artifact dependencies.
     *
     * @param dependent
     *            The dependent to add artifacts as dependencies
     * @param dependencies
     *            The dependencies information.
     * @param dependsOnType
     *            The type for creating the
     *            {@link com.buschmais.jqassistant.plugin.common.api.model.DependsOnDescriptor}.
     * @param scannerContext
     *            The scanner context
     * @return The list of {@link MavenDependencyDescriptor}s.
     */
    private <P extends MavenDependentDescriptor, D extends AbstractDependencyDescriptor> List<MavenDependencyDescriptor> getDependencies(P dependent,
            List<Dependency> dependencies, Class<D> dependsOnType, ScannerContext scannerContext) {
        Store store = scannerContext.getStore();
        List<MavenDependencyDescriptor> dependencyDescriptors = new ArrayList<>(dependencies.size());
        // initially collect all artifact descriptors (avoid write flushes to datastore)
        Map<Dependency, MavenArtifactDescriptor> mavenArtifactDescriptors = dependencies.stream()
                .collect(toMap(dependency -> dependency, dependency -> getMavenArtifactDescriptor(dependency, scannerContext)));
        for (Dependency dependency : dependencies) {
            MavenArtifactDescriptor dependencyArtifactDescriptor = mavenArtifactDescriptors.get(dependency);
            // New graph structure supporting exclusions
            MavenDependencyDescriptor dependencyDescriptor = store.create(MavenDependencyDescriptor.class);
            dependencyDescriptor.setToArtifact(dependencyArtifactDescriptor);
            dependencyDescriptor.setOptional(dependency.isOptional());
            dependencyDescriptor.setScope(dependency.getScope());
            for (Exclusion exclusion : dependency.getExclusions()) {
                MavenExcludesDescriptor mavenExcludesDescriptor = store.create(MavenExcludesDescriptor.class);
                mavenExcludesDescriptor.setGroupId(exclusion.getGroupId());
                mavenExcludesDescriptor.setArtifactId(exclusion.getArtifactId());
                dependencyDescriptor.getExclusions().add(mavenExcludesDescriptor);
            }
            dependencyDescriptors.add(dependencyDescriptor);
        }
        return dependencyDescriptors;
    }

    /**
     * Adds information about execution goals.
     *
     * @param executionDescriptor
     *            The descriptor for the execution.
     * @param pluginExecution
     *            The PluginExecution.
     * @param store
     *            The database.
     */
    private void addExecutionGoals(MavenPluginExecutionDescriptor executionDescriptor, PluginExecution pluginExecution, Store store) {
        List<String> goals = pluginExecution.getGoals();
        for (String goal : goals) {
            MavenExecutionGoalDescriptor goalDescriptor = store.create(MavenExecutionGoalDescriptor.class);
            goalDescriptor.setName(goal);
            executionDescriptor.getGoals().add(goalDescriptor);
        }

    }

    /**
     * Adds information about references licenses.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param model
     *            The Maven Model.
     * @param store
     *            The database.
     */
    private void addLicenses(MavenPomDescriptor pomDescriptor, Model model, Store store) {
        List<License> licenses = model.getLicenses();
        for (License license : licenses) {
            MavenLicenseDescriptor licenseDescriptor = store.create(MavenLicenseDescriptor.class);
            licenseDescriptor.setUrl(license.getUrl());
            licenseDescriptor.setComments(license.getComments());
            licenseDescriptor.setName(license.getName());
            licenseDescriptor.setDistribution(license.getDistribution());

            pomDescriptor.getLicenses().add(licenseDescriptor);
        }
    }

    /**
     * Adds information about developers.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param model
     *            The Maven Model.
     * @param store
     *            The database.
     */
    private void addDevelopers(MavenPomDescriptor pomDescriptor, Model model, Store store) {
        List<Developer> developers = model.getDevelopers();
        for (Developer developer : developers) {
            MavenDeveloperDescriptor developerDescriptor = store.create(MavenDeveloperDescriptor.class);
            developerDescriptor.setId(developer.getId());

            addCommonParticipantAttributes(developerDescriptor, developer, store);

            pomDescriptor.getDevelopers().add(developerDescriptor);
        }
    }

    /**
     * Adds dependency management information.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param dependencyManagement
     *            The dependency management information.
     * @param scannerContext
     */
    private List<MavenDependencyDescriptor> addManagedDependencies(MavenDependentDescriptor pomDescriptor, DependencyManagement dependencyManagement,
            ScannerContext scannerContext, Class<? extends AbstractDependencyDescriptor> relationClass) {
        if (dependencyManagement == null) {
            return Collections.emptyList();
        }
        List<Dependency> dependencies = dependencyManagement.getDependencies();
        return getDependencies(pomDescriptor, dependencies, relationClass, scannerContext);
    }

    /**
     * Adds information about managed plugins.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param build
     *            Information required to build the project.
     * @param scannerContext
     *            The scanner context.
     */
    private void addManagedPlugins(BaseProfileDescriptor pomDescriptor, BuildBase build, ScannerContext scannerContext) {
        if (null == build) {
            return;
        }
        PluginManagement pluginManagement = build.getPluginManagement();
        if (null == pluginManagement) {
            return;
        }
        List<MavenPluginDescriptor> pluginDescriptors = createMavenPluginDescriptors(pluginManagement.getPlugins(), scannerContext);
        pomDescriptor.getManagedPlugins().addAll(pluginDescriptors);
    }

    /**
     * Create plugin descriptors for the given plugins.
     *
     * @param plugins
     *            The plugins.
     * @param context
     *            The scanner context.
     * @return The plugin descriptors.
     */
    private List<MavenPluginDescriptor> createMavenPluginDescriptors(List<Plugin> plugins, ScannerContext context) {
        Store store = context.getStore();
        List<MavenPluginDescriptor> pluginDescriptors = new ArrayList<>();
        for (Plugin plugin : plugins) {
            MavenPluginDescriptor mavenPluginDescriptor = store.create(MavenPluginDescriptor.class);
            MavenArtifactDescriptor artifactDescriptor = context.peek(ArtifactResolver.class).resolve(new PluginCoordinates(plugin), context);
            mavenPluginDescriptor.setArtifact(artifactDescriptor);
            mavenPluginDescriptor.setInherited(plugin.isInherited());
            mavenPluginDescriptor.getDeclaresDependencies()
                    .addAll(getDependencies(mavenPluginDescriptor, plugin.getDependencies(), PluginDependsOnDescriptor.class, context));
            addPluginExecutions(mavenPluginDescriptor, plugin, store);
            addConfiguration(mavenPluginDescriptor, (Xpp3Dom) plugin.getConfiguration(), store);
            pluginDescriptors.add(mavenPluginDescriptor);
        }
        return pluginDescriptors;
    }

    /**
     * Adds information about referenced modules.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param modules
     *            The modules.
     * @param store
     *            The database.
     */
    private void addModules(BaseProfileDescriptor pomDescriptor, List<String> modules, Store store) {
        for (String module : modules) {
            MavenModuleDescriptor moduleDescriptor = store.create(MavenModuleDescriptor.class);
            moduleDescriptor.setName(module);
            pomDescriptor.getModules().add(moduleDescriptor);
        }

    }

    /**
     * Adds information about parent POM.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param model
     *            The Maven Model.
     * @param context
     *            The scanner context.
     */
    private void addParent(MavenPomDescriptor pomDescriptor, Model model, ScannerContext context) {
        Parent parent = model.getParent();
        if (null != parent) {
            ArtifactResolver resolver = context.peek(ArtifactResolver.class);
            MavenArtifactDescriptor parentDescriptor = resolver.resolve(new ParentCoordinates(parent), context);
            pomDescriptor.setParent(parentDescriptor);
        }
    }

    /**
     * Adds information about plugin executions.
     *
     * @param mavenPluginDescriptor
     *            The descriptor for the plugin.
     * @param plugin
     *            The Plugin.
     * @param store
     *            The database.
     */
    private void addPluginExecutions(MavenPluginDescriptor mavenPluginDescriptor, Plugin plugin, Store store) {
        List<PluginExecution> executions = plugin.getExecutions();
        for (PluginExecution pluginExecution : executions) {
            MavenPluginExecutionDescriptor executionDescriptor = store.create(MavenPluginExecutionDescriptor.class);
            executionDescriptor.setId(pluginExecution.getId());
            executionDescriptor.setPhase(pluginExecution.getPhase());
            executionDescriptor.setInherited(pluginExecution.isInherited());
            mavenPluginDescriptor.getExecutions().add(executionDescriptor);
            addExecutionGoals(executionDescriptor, pluginExecution, store);
            addConfiguration(executionDescriptor, (Xpp3Dom) pluginExecution.getConfiguration(), store);
        }

    }

    /**
     * Adds information about plugins.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param build
     *            Information required to build the project.
     * @param scannerContext
     *            The scanner context.
     */
    private void addPlugins(BaseProfileDescriptor pomDescriptor, BuildBase build, ScannerContext scannerContext) {
        if (null == build) {
            return;
        }
        List<Plugin> plugins = build.getPlugins();
        List<MavenPluginDescriptor> pluginDescriptors = createMavenPluginDescriptors(plugins, scannerContext);
        pomDescriptor.getPlugins().addAll(pluginDescriptors);
    }

    /**
     * Adds information about defined profile.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param model
     *            The Maven Model.
     * @param scannerContext
     *            The scanner context.
     */
    private void addProfiles(MavenPomDescriptor pomDescriptor, Model model, ScannerContext scannerContext) {
        List<Profile> profiles = model.getProfiles();
        Store store = scannerContext.getStore();
        for (Profile profile : profiles) {
            MavenProfileDescriptor mavenProfileDescriptor = store.create(MavenProfileDescriptor.class);
            pomDescriptor.getProfiles().add(mavenProfileDescriptor);
            mavenProfileDescriptor.setId(profile.getId());
            addProperties(mavenProfileDescriptor, profile.getProperties(), store);
            addModules(mavenProfileDescriptor, profile.getModules(), store);
            addPlugins(mavenProfileDescriptor, profile.getBuild(), scannerContext);
            addManagedPlugins(mavenProfileDescriptor, profile.getBuild(), scannerContext);
            addDependencies(mavenProfileDescriptor, ProfileDeclaresDependencyDescriptor.class, ProfileManagesDependencyDescriptor.class, profile,
                    scannerContext);
            addActivation(mavenProfileDescriptor, profile.getActivation(), store);
            addRepository(of(mavenProfileDescriptor), profile.getRepositories(), store);
        }
    }

    private void addRepository(RepositoryHolder holder, List<Repository> repositories, Store store) {
        for (Repository repo : repositories) {
            MavenRepositoryDescriptor repoDescriptor = MavenRepositoryResolver.resolve(store, repo.getUrl());

            repoDescriptor.setName(repo.getName());
            repoDescriptor.setId(repo.getId());
            repoDescriptor.setLayout(repo.getLayout() != null ? repo.getLayout() : "default");

            WrappedPolicy relPolicy = new WrappedPolicy(repo.getReleases());
            WrappedPolicy snapPolicy = new WrappedPolicy(repo.getSnapshots());

            repoDescriptor.setReleasesChecksumPolicy(relPolicy.getChecksumPolicy());
            repoDescriptor.setReleasesUpdatePolicy(relPolicy.getUpdatePolicy());
            repoDescriptor.setReleasesEnabled(relPolicy.isEnabled());

            repoDescriptor.setSnapshotsChecksumPolicy(snapPolicy.getChecksumPolicy());
            repoDescriptor.setSnapshotsUpdatePolicy(snapPolicy.getUpdatePolicy());
            repoDescriptor.setSnapshotsEnabled(snapPolicy.isEnabled());

            holder.getRepositories().add(repoDescriptor);
        }
    }

    /**
     * Adds information about defined properties.
     *
     * @param pomDescriptor
     *            The descriptor for the current POM.
     * @param properties
     *            The properties information.
     * @param store
     *            The database.
     */
    private void addProperties(BaseProfileDescriptor pomDescriptor, Properties properties, Store store) {
        Set<Entry<Object, Object>> entrySet = properties.entrySet();
        for (Entry<Object, Object> entry : entrySet) {
            PropertyDescriptor propertyDescriptor = store.create(PropertyDescriptor.class);
            propertyDescriptor.setName(entry.getKey().toString());
            propertyDescriptor.setValue(entry.getValue().toString());
            pomDescriptor.getProperties().add(propertyDescriptor);
        }

    }

    /**
     * Creates a MavenArtifactDescriptor and fills it with all information from
     * given dependency.
     *
     * @param dependency
     *            Dependency.
     * @param context
     *            The scanner context.
     * @return The MavenArtifactDescriptor.
     */
    private MavenArtifactDescriptor getMavenArtifactDescriptor(Dependency dependency, ScannerContext context) {
        DependencyCoordinates coordinates = new DependencyCoordinates(dependency);
        return context.peek(ArtifactResolver.class).resolve(coordinates, context);
    }

    /**
     * Returns information about child config entries.
     *
     * @param node
     *            Current config node.
     * @param store
     *            The database.
     * @return Child config information.
     */
    private ValueDescriptor<?> getConfigChildNodes(Xpp3Dom node, Store store) {
        Xpp3Dom[] children = node.getChildren();
        if (children.length == 0) {
            PropertyDescriptor propertyDescriptor = store.create(PropertyDescriptor.class);
            propertyDescriptor.setName(node.getName());
            propertyDescriptor.setValue(node.getValue());
            return propertyDescriptor;
        }
        ArrayValueDescriptor childDescriptor = store.create(ArrayValueDescriptor.class);
        childDescriptor.setName(node.getName());
        for (Xpp3Dom child : children) {
            childDescriptor.getValue().add(getConfigChildNodes(child, store));
        }
        return childDescriptor;
    }

    private static RepositoryHolder of(final MavenProfileDescriptor profileDescriptor) {
        return new RepositoryHolder() {
            @Override
            public List<MavenRepositoryDescriptor> getRepositories() {
                return profileDescriptor.getRepositories();
            }
        };
    }

    private static RepositoryHolder of(final MavenPomDescriptor pomDescriptor) {
        return new RepositoryHolder() {
            @Override
            public List<MavenRepositoryDescriptor> getRepositories() {
                return pomDescriptor.getRepositories();
            }
        };
    }

    protected interface RepositoryHolder {
        List<MavenRepositoryDescriptor> getRepositories();
    }

    static class WrappedPolicy {
        RepositoryPolicy original;

        private WrappedPolicy(RepositoryPolicy policy) {
            original = policy;
        }

        public String getChecksumPolicy() {
            if (original != null) {
                return firstNonNull(original.getChecksumPolicy(), "warn");
            }

            return "warn";
        }

        public String getUpdatePolicy() {
            if (original != null) {
                return firstNonNull(original.getUpdatePolicy(), "daily");
            }

            return "daily";
        }

        public boolean isEnabled() {
            boolean result = true;

            if (original != null) {
                result = firstNonNull(original.isEnabled(), true);
            }

            return result;
        }
    }

}
