package com.continuuity.gateway.runtime;

import com.continuuity.common.conf.CConfiguration;
import com.continuuity.common.conf.Constants;
import com.continuuity.common.runtime.RuntimeModule;
import com.continuuity.common.utils.Networks;
import com.continuuity.gateway.auth.GatewayAuthModules;
import com.continuuity.gateway.handlers.AppFabricGatewayModules;
import com.continuuity.gateway.handlers.GatewayHandlerModules;
import com.continuuity.logging.gateway.handlers.LogHandlerModules;
import com.continuuity.metrics.guice.MetricsQueryModule;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.name.Named;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Guice modules for Gateway.
 */
public class GatewayModules extends RuntimeModule {

  @Override
  public Module getInMemoryModules() {
    return getCommonModules();
  }

  @Override
  public Module getSingleNodeModules() {
    return getCommonModules();
  }

  @Override
  public Module getDistributedModules() {
    return getCommonModules();
  }

  private Module getCommonModules() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        install(new GatewayAuthModules());
        install(new GatewayHandlerModules());
        install(new AppFabricGatewayModules());
        install(new MetricsQueryModule());
        install(new LogHandlerModules());
      }

      @Provides
      @Named(Constants.Gateway.ADDRESS)
      public final InetAddress providesHostname(CConfiguration cConf) {
        return Networks.resolve(cConf.get(Constants.Gateway.ADDRESS),
                                new InetSocketAddress("localhost", 0).getAddress());
      }
    };
  }
}
