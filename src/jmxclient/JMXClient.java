package jmxclient;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class JMXClient
{
	private static Logger logger = null;

	private static void initLogger()
	{
		System.out.println("configuring log4j with log4j.xml");
		DOMConfigurator.configure("log4j.xml");
		logger = Logger.getLogger("root");
	}

	public static void main(String[] args) throws Exception
	{
		initLogger();

		if ((args.length < 6) || (args.length % 2 != 0))
		{
			logger.error("Usage: java  [host] [port] [username] [password] [bean name] [method] <<parametertype value> ...>");
			return;
		}
		// 服务ip地址
		final String host = args[0];

		// 服务端口
		final int rmiPort = Integer.valueOf(args[1]).intValue();

		// jmx用户名
		final String username = args[2];

		// jmx密码
		final String password = args[3];

		// bean 对应的ObjectName
		final ObjectName objectName = new ObjectName(args[4]);

		// 对应bean注册类的一个方法
		final String methodName = args[5];

		// 方法参数
		final HashMap<String, String[]> jmxParamsHashMap = new HashMap<String, String[]>();

		final String[] usernameAndPassword =
		{ username, password };

		jmxParamsHashMap.put("jmx.remote.credentials", usernameAndPassword);
		final String serviceUrl = new StringBuilder().append("service:jmx:rmi:///jndi/rmi://").append(host).append(":")
				.append(rmiPort).append("/jmxrmi").toString();
		final JMXServiceURL jmxServiceURL = new JMXServiceURL(serviceUrl);

		final JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, jmxParamsHashMap);
		if (jmxConnector == null)
		{
			logger.error(new StringBuilder().append("connect to jmx failed, url=").append(jmxServiceURL).toString());
			return;
		}

		logger.error(new StringBuilder().append("JMXConnector=").append(jmxConnector.toString()).toString());

		Object[] paramsValue = null;
		String[] paramsClassName = null;

		final MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();

		final Object localObject = mBeanServerConnection.invoke(objectName, methodName, paramsValue, paramsClassName);

		logger.error(new StringBuilder().append("invoke method success, name=").append(objectName).append(", operation=")
				.append(methodName).append(", retvalue=").append(localObject == null ? "void" : localObject.toString())
				.toString());
	}

	static void log(String paramString)
	{
		logger.error(new StringBuilder()
				.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss : ").format(Long.valueOf(System.currentTimeMillis())))
				.append(paramString).toString());
	}

	static void logErr(String paramString)
	{
		logger.error(new StringBuilder()
				.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss : ").format(Long.valueOf(System.currentTimeMillis())))
				.append(paramString).toString());
	}
}
