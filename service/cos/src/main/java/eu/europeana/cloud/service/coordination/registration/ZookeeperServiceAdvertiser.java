package eu.europeana.cloud.service.coordination.registration;

import com.google.common.base.Throwables;
import eu.europeana.cloud.service.coordination.ServiceProperties;
import eu.europeana.cloud.service.coordination.ZookeeperService;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceInstanceBuilder;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Registers services as available in Zookeeper.
 * <p/>
 * ZooKeeper is used for service discovery:
 * <p/>
 * Services are registered on a common znode, and any client can query Zookeeper
 * for a list of available services.
 *
 * @author emmanouil.koufakis@theeuropeanlibrary.org
 */
public final class ZookeeperServiceAdvertiser implements
        EcloudServiceAdvertiser {

    @Autowired
    ServletContext servletContext;

    /**
     * Logging
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ZookeeperServiceAdvertiser.class);

    /**
     * List of properties that allow a client to connect to this UIS REST
     * Service.
     */
    private ServiceProperties currentlyAdvertisedServiceProperties;

    /**
     * Id of currenly advertised service.
     * Autogenerated at runtime.
     * <p/>
     * Example: "1b96c813-0ec2-4038-ab49-1ef6a1a73083"
     */
    private String currentlyAdvertisedServiceID;

    /**
     * Used to serialize the {@link ServiceProperties} before sending them to
     * Zookeeper.
     */
    private final InstanceSerializer<ServiceProperties> serializer = new JsonInstanceSerializer<ServiceProperties>(
            ServiceProperties.class);

    /**
     * Service that actually performs the advertisement.
     */
    private final ServiceDiscovery<ServiceProperties> discovery;

    /**
     * List of properties that allow a client to connect to some UIS REST
     * Service.
     */
    private ServiceProperties serviceProperties;

    ZookeeperServiceAdvertiser(final ZookeeperService zookeeper,
                               final String discoveryPath,
                               final ServiceProperties serviceProperties) {

        LOGGER.info("ZookeeperServiceAdvertiser starting...");

        this.serviceProperties = serviceProperties;

        discovery = ServiceDiscoveryBuilder.builder(ServiceProperties.class)
                .basePath(zookeeper.getZookeeperPath() + discoveryPath)
                .client(zookeeper.getClient()).serializer(serializer).build();

        try {
            discovery.start();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        LOGGER.info("ZookeeperServiceAdvertiser started successfully.");
    }

    /**
     * Registers this service as available.
     * <p/>
     * Other clients querying Zookeeper for available services will receive this
     * service on their list.
     *
     * @param serviceProperties List of properties required to connect to this Service.
     */
    @Override
    public void startAdvertising(final ServiceProperties serviceProperties) {

        LOGGER.info("ZookeeperServiceAdvertiser starting advertising process '{}' ...",
                serviceProperties);

        try {
            ServiceInstance<ServiceProperties> propertiesToBeRegisteredInZoo = convert(serviceProperties);
            discovery.registerService(propertiesToBeRegisteredInZoo);
            this.currentlyAdvertisedServiceProperties = serviceProperties;
            this.currentlyAdvertisedServiceID = propertiesToBeRegisteredInZoo.getId();
            LOGGER.info("ZookeeperServiceAdvertiser has advertised the service successfully.");

        } catch (final Exception e) {
            this.currentlyAdvertisedServiceProperties = null;
            this.currentlyAdvertisedServiceID = null;
            LOGGER.error(e.getMessage());
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void stopAdvertising() {

        LOGGER.info("ZookeeperServiceAdvertiser unregistering process '{}' ...",
                serviceProperties);

        try {
            discovery.unregisterService(convert(this.currentlyAdvertisedServiceProperties));

            this.currentlyAdvertisedServiceProperties = null;
            this.currentlyAdvertisedServiceID = null;

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw Throwables.propagate(e);
        }
    }

    private static ServiceInstance<ServiceProperties> convert(
            final ServiceProperties p) throws Exception {

        return ServiceInstance.<ServiceProperties>builder()
                .name(p.getServiceName()).payload(p)
                .address(p.getListenAddress()).build();
    }

    /**
     * Used if no IP address is specified by the user.
     *
     * @return tries to autodetect server's ip address using ServiceInstanceBuilder.getAllLocalIPs()
     */
    private String getAutoListenAddress() throws UnknownHostException, IOException {

        Collection<InetAddress> localIps = ServiceInstanceBuilder.getAllLocalIPs();
        Iterator<InetAddress> localIpsIter = localIps.iterator();
        LOGGER.info(
                "ZookeeperServiceAdvertiser: autodetecting ip address.. found {} localIpAddresses",
                localIps.size());

        while (localIpsIter.hasNext()) {
            InetAddress localIp = localIpsIter.next();
            if (localIp.isReachable(5000)) {
                String address = localIp.getLocalHost().getHostAddress();
                LOGGER.info("ZookeeperServiceAdvertiser: autodetected {} as listen address",
                        address);
                return address;
            }
        }
        return null;
    }

    /**
     * @return Servlet's context path.
     * <p/>
     * (Appended to the ip address, to construct the listen address for this service)
     */
    private String getServletContextPath() {

        String servletPath = servletContext.getContextPath();
        LOGGER.info("ZookeeperServiceAdvertiser detecting ContextPath: {}", servletPath);
        return servletPath;
    }

    /**
     * TODO
     * <p/>
     * Used if no port is specified by the user.
     *
     * @return Currently hardcoded port number.
     * <p/>
     * How this port is configured will depend
     * on how the services are going to be deployed in the future.
     */
    private String getPort() {
        return "8080";
    }

    @PostConstruct
    public void postConstruct() {

        try {
            if (this.serviceProperties.getListenAddress().isEmpty()) {
                String autodectedIpaddress = getAutoListenAddress();
                if (autodectedIpaddress != null) {
                    this.serviceProperties.setListenAddress("http://"
                            + getAutoListenAddress() + ":" + getPort()
                            + getServletContextPath());
                }
            } else {
                this.serviceProperties.setListenAddress(serviceProperties.getListenAddress()
                        + getServletContextPath());
            }
        } catch (Exception e) {
            LOGGER.warn("ZookeeperServiceAdvertiser: Error while setting service address.. {}",
                    e.getMessage());
        }

        this.startAdvertising(serviceProperties);
        LOGGER.info("ZookeeperServiceAdvertiser advertising process successfull.");
    }

    @Override
    public String getCurrentlyAdvertisedServiceID() {
        return currentlyAdvertisedServiceID;
    }

    @Override
    public String getCurrentlyAdvertisedServiceAddress() {

        if (currentlyAdvertisedServiceProperties != null) {
            return currentlyAdvertisedServiceProperties.getListenAddress();
        }

        return null;
    }

    public ServiceProperties getServiceProperties() {
        return serviceProperties;
    }
}
