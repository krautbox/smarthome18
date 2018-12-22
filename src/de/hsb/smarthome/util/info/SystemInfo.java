package de.hsb.smarthome.util.info;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

/**
 * Collects and returns information (OS, CPU, MEM) about the system.
 * 
 * @author Fabian Mangels
 *
 */
public class SystemInfo {

	public SystemInfo() {
		mRuntime = Runtime.getRuntime();
		mOS = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	}

	/**
	 * Returns information about the operating system.
	 * 
	 * @return String - Collection of operating system information (os.arch,
	 *         os.name, os.version)
	 */
	public String getOsInfo() {
		return "Operating system architecture: " + System.getProperty("os.arch") + " Operating system name: "
				+ System.getProperty("os.name") + " Operating system version: " + System.getProperty("os.version");
	}

	/**
	 * Returns the count of availableProcessors.
	 * 
	 * @return int - count of availableProcessors
	 */
	public int getAvailableProcessors() {
		return mRuntime.availableProcessors();
	}

	/**
	 * Returns information about the SystemCpuLoad.
	 * 
	 * @return float - SystemCpuLoad
	 */
	public float getSystemCpuLoad() {
		return (float) mOS.getSystemCpuLoad();
	}

	/**
	 * Returns information about the FreePhysicalMemorySize.
	 * 
	 * @return long - FreePhysicalMemorySize
	 */
	public long getFreePhysicalMemorySize() {
		return (long) mOS.getFreePhysicalMemorySize();
	}

	/**
	 * Returns information about the TotalPhysicalMemorySize.
	 * 
	 * @return long - TotalPhysicalMemorySize
	 */
	public long getTotalPhysicalMemorySize() {
		return (long) mOS.getTotalPhysicalMemorySize();
	}

	private Runtime mRuntime;
	private OperatingSystemMXBean mOS;
}