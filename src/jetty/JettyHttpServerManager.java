package jetty;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

import servlet.HelloServlet;

import com.sun.jdmk.comm.HtmlAdaptorServer;

public class JettyHttpServerManager
{
	private static HtmlAdaptorServer adaptorServer = null;
	private static javax.management.remote.JMXConnectorServer cs = null;
	private static Logger logger = null;
	private static boolean isHtmlJmx = false;

	private static Server server = null;

	private static ExecutorThreadPool executorThreadPool = null;

	private static void initHtmlJMX() throws Exception
	{
		MBeanServer server = MBeanServerFactory.createMBeanServer();

		ObjectName helloName = new ObjectName("jmxBean:name=stopper");
		server.registerMBean(new Stopper(), helloName);

		ObjectName adapterName = new ObjectName("HelloAgent:name=htmladapter,port=8081");
		adaptorServer = new HtmlAdaptorServer();
		server.registerMBean(adaptorServer, adapterName);

		adaptorServer.start();

		logger.info("start jmx html server");
	}

	private static int initProtogenesisJMX() throws Exception
	{
		String port1Str = System.getProperty("com.jmxport1");
		String port2Str = System.getProperty("com.jmxport2");
		if (port1Str == null || port2Str == null)
		{
			logger.error("jmx端口未通过系统属性设置");
			return -1;
		}
		final int port1 = Integer.valueOf(port1Str);
		final int port2 = Integer.valueOf(port2Str);
		System.setProperty("java.rmi.server.randomIDs", "true");
		try
		{
			LocateRegistry.createRegistry(port2);
		}
		catch (java.rmi.server.ExportException ex)
		{
			logger.error("err", ex);
			return -1;
		}
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		java.util.HashMap<String, Object> env = new java.util.HashMap<String, Object>();
		env.put("jmx.remote.x.password.file", "jmxremote.password");
		env.put("jmx.remote.x.access.file", "jmxremote.access");

		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://127.0.0.1:" + port1 + "/jndi/rmi://127.0.0.1:" + port2
				+ "/jmxrmi");
		cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

		try
		{
			cs.start();
		}
		catch (java.net.BindException ex)
		{
			logger.error("端口已被占用", ex);
			return -2;
		}

		MBeanServer server = java.lang.management.ManagementFactory.getPlatformMBeanServer();

		ObjectName helloName = new ObjectName("jmxBean:name=stopper");

		server.registerMBean(new Stopper(), helloName);

		return 0;
	}

	public static void exitJMX() throws IOException
	{
		if (isHtmlJmx)
		{
			exitHtmlJMX();
		}
		else
		{
			exitProtogenesisJMX();
		}
	}

	private static void initLogger()
	{
		System.out.println("configuring log4j with log4j.xml");
		DOMConfigurator.configure("log4j.xml");
		logger = Logger.getLogger("root");
	}

	public static void exitHtmlJMX()
	{
		adaptorServer.stop();
	}

	public static void exitProtogenesisJMX() throws IOException
	{
		cs.stop();
	}

	private static void initHttpServer() throws Exception
	{
		server = new Server();

		// 实例化
		final SelectChannelConnector connector = new SelectChannelConnector();
		// 设置主机地址
		connector.setHost("localhost");
		// 设置端口号
		connector.setPort(40404);

		connector.setMaxIdleTime(30000);

		executorThreadPool = new ExecutorThreadPool(25, 50, 30000);
		connector.setThreadPool(executorThreadPool);

		WebAppContext webAppContext = new WebAppContext();

		webAppContext.setResourceBase(".");

		webAppContext.setClassLoader(Thread.currentThread().getContextClassLoader());

		webAppContext.addServlet(new ServletHolder(new HelloServlet()), "/hellohandler");

		server.setHandler(webAppContext);

		server.addConnector(connector);

		server.start();
	}

	public static void exitHttpServer() throws Exception
	{
		server.stop();

		executorThreadPool.stop();
	}

	public static void initJmx() throws Exception
	{
		if (isHtmlJmx)
		{
			initHtmlJMX();
		}
		else
		{
			initProtogenesisJMX();
		}
	}

	public static void init() throws Exception
	{
		initLogger();

		initJmx();

		initHttpServer();
	}

	public static void exit() throws Exception
	{
		exitHttpServer();

		exitJMX();
	}

	public static void main(String[] args) throws Exception
	{
		System.setProperty("com.jmxport1", String.valueOf(7000));
		System.setProperty("com.jmxport2", String.valueOf(7001));

		init();
	}

	public interface StopperMBean
	{
		void stop() throws Exception;
	}

	public static class Stopper implements StopperMBean
	{
		public void stop() throws Exception
		{
			JettyHttpServerManager.exit();
		}
	}
}
