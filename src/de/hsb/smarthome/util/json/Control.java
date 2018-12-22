package de.hsb.smarthome.util.json;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Models a smarthome control unit. Used to hold and retrieve information about such.
 *
 */
public class Control {

	public Control() {
		//no-arg constructor
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(final String info) {
		this.info = info;
	}

	public List<Processor> getCpu() {
		return cpu;
	}

	public void setCpu(final List<Processor> cpu) {
		this.cpu = cpu;
	}

	public Memory getMemory() {
		return this.memory;
	}

	public final void setMem(final Memory memory) {
		this.memory = memory;
	}

	private String info;
	private List<Processor> cpu;
	@SerializedName("mem")
	private Memory memory;

	/**
	 * Models a single Processor of the smarthome control unit CPU. 
	 *
	 */
	public class Processor {
		
		public Processor() {
			//no-arg constructor
		}

		public int getCore() {
			return core;
		}

		public void setCore(final int core) {
			this.core = core;
		}

		public float getWork() {
			return work;
		}

		public void setWork(final float work) {
			this.work = work;
		}

		public float getTemperature() {
			return temperature;
		}

		public void setTemperature(final float temperature) {
			this.temperature = temperature;
		}

		private int core;
		private float work;
		@SerializedName("temp")
		private float temperature;
	}

	/**
	 * Wrapper class to model memory information of a smarthome control unit.
	 *
	 */
	public class Memory {

		public Memory() {
			//no-arg constructor
		}

		public long getUsed() {
			return used;
		}

		public void setUsed(final long used) {
			this.used = used;
		}

		public long getFree() {
			return free;
		}

		public void setFree(final long free) {
			this.free = free;
		}

		private long used;
		private long free;
	}
}