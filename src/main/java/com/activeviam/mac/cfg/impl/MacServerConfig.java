/*
 * (C) Quartet FS 2013-2015
 * ALL RIGHTS RESERVED. This material is the CONFIDENTIAL and PROPRIETARY
 * property of Quartet Financial Systems Limited. Any unauthorized use,
 * reproduction or transfer of this material is strictly prohibited
 */
package com.activeviam.mac.cfg.impl;

import com.activeviam.mac.cfg.security.impl.CorsConfig;
import com.activeviam.mac.cfg.security.impl.SecurityConfig;
import com.activeviam.mac.cfg.security.impl.UserConfig;
import com.activeviam.properties.SpringAllPropertyResolverConfig;
import com.qfs.pivot.content.impl.DynamicActivePivotContentServiceMBean;
import com.qfs.server.cfg.IActivePivotConfig;
import com.qfs.server.cfg.IDatastoreConfig;
import com.qfs.server.cfg.content.IActivePivotContentServiceConfig;
import com.qfs.server.cfg.i18n.impl.LocalI18nConfig;
import com.qfs.server.cfg.impl.ActivePivotConfig;
import com.qfs.server.cfg.impl.ActivePivotServicesConfig;
import com.qfs.server.cfg.impl.ActiveViamRestServicesConfig;
import com.qfs.server.cfg.impl.ActiveViamWebSocketServicesConfig;
import com.qfs.server.cfg.impl.DatastoreConfig;
import com.qfs.server.cfg.impl.FullAccessBranchPermissionsManagerConfig;
import com.qfs.server.cfg.impl.JwtConfig;
import com.qfs.server.cfg.impl.JwtRestServiceConfig;
import com.quartetfs.fwk.AgentException;
import com.quartetfs.fwk.Registry;
import com.quartetfs.fwk.contributions.impl.ClasspathContributionProvider;
import com.quartetfs.fwk.monitoring.jmx.impl.JMXEnabler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;


/**
 * Spring configuration of the ActivePivot Sandbox application.
 *
 * <p>
 * This is the entry point for the Spring "Java Config" of the entire application. This is
 * referenced in {@link MACWebAppInitializer} to bootstrap the application (as per Spring framework
 * principles).
 *
 * <p>
 * We use {@link PropertySource} annotation(s) to define some .properties file(s), whose content
 * will be loaded into the Spring {@link Environment}, allowing some externally-driven configuration
 * of the application. Parameters can be quickly changed by modifying the {@code sandbox.properties}
 * file.
 *
 * <p>
 * We use {@link Import} annotation(s) to reference additional Spring {@link Configuration} classes,
 * so that we can manage the application configuration in a modular way (split by domain/feature,
 * re-use of core config, override of core config, customized config, etc...).
 *
 * <p>
 * Spring best practices recommends not to have arguments in bean methods if possible. One should
 * rather autowire the appropriate spring configurations (and not beans directly unless necessary),
 * and use the beans from there.
 *
 * @author Quartet FS
 */
@PropertySource(value = "classpath:application.properties")
@Configuration
@Import(
		value = {
				SpringAllPropertyResolverConfig.class,

				JwtRestServiceConfig.class,
				JwtConfig.class,

				DatastoreDescriptionConfig.class,
				ManagerDescriptionConfig.class,

				// Pivot
				ActivePivotConfig.class,
				DatastoreConfig.class,
				NoWriteDatastoreServiceConfig.class,
				FullAccessBranchPermissionsManagerConfig.class,
				ActivePivotServicesConfig.class,
				ActiveViamRestServicesConfig.class,
				ActiveViamWebSocketServicesConfig.class,

				// Content server
				LocalContentServiceConfig.class,
				LocalI18nConfig.class,

				// Specific to monitoring server
				SecurityConfig.class,
				CorsConfig.class,
				UserConfig.class,

				SourceConfig.class,
				MonitoringConnectorConfig.class,

				ActiveUIResourceServerConfig.class
		})
public class MacServerConfig {

	/* Before anything else we statically initialize the Quartet FS Registry. */
	{
		Registry.setContributionProvider(new ClasspathContributionProvider(
				"com.qfs",
				"com.quartetfs",
				"com.activeviam"));
	}

	/** Datastore spring configuration */
	@Autowired
	protected IDatastoreConfig datastoreConfig;

	/** ActivePivot spring configuration */
	@Autowired
	protected IActivePivotConfig apConfig;

	/** ActivePivot content service spring configuration */
	@Autowired
	protected IActivePivotContentServiceConfig apCSConfig;

	@Autowired
	protected SourceConfig sourceConfig;

	/**
	 *
	 * Initialize and start the ActivePivot Manager, after performing all the injections into the
	 * ActivePivot plug-ins.
	 *
	 * @return void
	 * @throws Exception any exception that occurred during the manager's start up
	 */
	@Bean
	public Void startManager() {
		/* *********************************************** */
		/* Initialize the ActivePivot Manager and start it */
		/* *********************************************** */
		try {
			apConfig.activePivotManager().init(null);
			apConfig.activePivotManager().start();
		} catch (AgentException e) {
			throw new IllegalStateException("Cannot start the application", e);
		}

		// Connect the real-time updates
		sourceConfig.watchStatisticDirectory();

		return null;
	}

	/**
	 * Enable JMX Monitoring for the Datastore
	 *
	 * @return the {@link JMXEnabler} attached to the datastore
	 */
	@Bean
	public JMXEnabler JMXDatastoreEnabler() {
		return new JMXEnabler(datastoreConfig.datastore());
	}

	/**
	 * Enable JMX Monitoring for ActivePivot Components
	 *
	 * @return the {@link JMXEnabler} attached to the activePivotManager
	 */
	@Bean
	public JMXEnabler JMXActivePivotEnabler() {
		startManager();

		return new JMXEnabler(apConfig.activePivotManager());
	}

	@Bean
	public JMXEnabler JMXActivePivotContentServiceEnabler() {
		// to allow operations from the JMX bean
		return new JMXEnabler(
				new DynamicActivePivotContentServiceMBean(
						apCSConfig.activePivotContentService(),
						apConfig.activePivotManager()));
	}

}
