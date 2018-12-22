package de.hsb.smarthome.util.json;

import java.util.EnumSet;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Models a smarthome controllable device. Each device should be of
 * a valid type declared in this class' Type enum. 
 *
 */
public class Device {
	
	/**
	 * Declares all supported device types.
	 *
	 */
	public enum Type{
		RASPBERRY,
		SOCKET,
		CAMERA;
	}

	public Device() {
		//no-arg constructor
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}
	
	public String getAid() {
		return this.aid;
	}

	public void setAid(final String aid) {
		this.aid = aid;
	}

	public Type getType() {
		return type;
	}

	public void setType(final Type type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(final boolean isConnected) {
		this.isConnected = isConnected;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(final int status) {
		this.status = status;
	}

	public Float getTemperature() {
		return temperature;
	}

	public void setTemperature(final Float temperature) {
		this.temperature = temperature;
	}
	
	public Float getPower() {
		return this.power;
	}

	public void setPower(final Float power) {
		this.power = power;
	}

	public Float getEnergy() {
		return this.energy;
	}

	public void setEnergy(final Float energy) {
		this.energy = energy;
	}

	public List<Cycle> getCycles() {
		return cycles;
	}

	public void setCycles(final List<Cycle> cycles) {
		this.cycles = cycles;
	}
	
	/**
	 * Validates whether the given string matches the required time format.
	 * The accepted format is 'HH:mm:ss', e.g. 13:05:00
	 * 
	 * @param s The string to validate
	 * @return true if the given string conforms to the required time format,
	 * 		   false otherwise
	 */
	public static boolean isValidTimeFormat(final String s) {
		return s.matches(Cycle.TIME_PATTERN);
	}

	@Override
	public String toString(){
		return getName() + " (" + getId() + ")";
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Device){
			final Device dev = (Device) obj;
			if(dev.getId() == this.id){
				return true;
			}
		}
		return false;
	}

	private int id;
	private transient String aid;
	private Type type;
	private String name;
	@SerializedName("connected")
	private boolean isConnected;
	private int status;
	@SerializedName("temp")
	private Float temperature;
	private Float power;
	private Float energy;
	private List<Cycle> cycles;

	/**
	 * Models an operation cycle of a smarthome device. A cycle is defined by
	 * a startup-time, i.e. the time of day the device should turn on, and a 
	 * shutdown-time respectively, i.e. the time of day the device should
	 * turn itself off. You may also construct a cycle defined by temperature values.
	 *
	 */
	public class Cycle {
		
		public static final transient String TIME_PATTERN = "^\\d\\d:\\d\\d:\\d\\d$";
		public static final transient String CYCLETYPE_UNKNOWN = "UNKNOWN";
		public static final transient String CYCLETYPE_TIME = "TIME";
		public static final transient String CYCLETYPE_TEMPERATURE = "TEMPERATURE";
		public static final transient String CYCLETYPE_HUMIDITY = "HUMIDITY";
		
		/**
		 * Standard no-arg constructor initialising the cycle as CYCLETYPE_UNKNOWN.
		 * You must call setCycletype(CYCLETYPE) in order to construct a usable cycle.
		 * Alternatively, pass one of the CYCLETYPE_*-constants to the constructor.
		 * By default a cycle is active on all weekdays
		 */
		public Cycle(){
			//no-arg constructor
		}
		
		/**
		 * Instantiates a new Cycle with the defined cycletype. The argument must be
		 * one of the CYCLETYPE_*-constants. By default a cycle is active on all weekdays
		 * 
		 * @param CYCLETYPE the type of this cycle
		 */
		public Cycle(final String CYCLETYPE){
			this.cycletype = CYCLETYPE;
		}

		/**
		 * Gets the start point of this cycle as a string representation
		 * 
		 * @return The start point of this cycle
		 */
		public String getStart() {
			return this.start;
		}
		
		/**
		 * Gets the cycletype of this cycle which is one of the CYCLETYPE_*-constants
		 * 
		 * @return The type of this cycle
		 */
		public String getCycletype() {
			return this.cycletype;
		}

		/**
		 * Sets the cycletype for this cycle which must be one of the CYCLETYPE_*-constants
		 * 
		 * @param CYCLETYPE the type to set this cycle to
		 */
		public void setCycletype(final String CYCLETYPE) {
			this.cycletype = CYCLETYPE;
		}
		

		/**
		 * Gets the days of the week this cycle is activated on
		 * 
		 * @return The EnumSet containing the Weekday objects of this cycle
		 */
		public EnumSet<Weekday> getDays() {
			return this.days;
		}

		/**
		 * Sets the days of the week this cycle should be active on
		 * 
		 * @param days an EnumSet containing the Weekday objects of this cycle
		 */
		public void setDays(final EnumSet<Weekday> days) {
			this.days = days;
		}
		
		/**
		 * Gets the human readable name of this cycle
		 * 
		 * @return The name of this cycle
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Sets the human readable name of this cycle
		 * 
		 * @param name the name of this cycle
		 */
		public void setName(final String name) {
			this.name = name;
		}

		/**
		 * Sets the startup-time of this device's cycle. The argument must have 
		 * the format 'HH:mm:ss'
		 * 
		 * @param start The string representation of the startup-time
		 * @throws IllegalArgumentException If the given time has not the required format
		 * @throws UnsetCycleException If this cycle's type is not TIME
		 */
		public void setStartTime(final String start) throws IllegalArgumentException, UnsetCycleException {
			if(!this.cycletype.equals(CYCLETYPE_TIME)) {
				throw new UnsetCycleException(String.format("Invalid cycletype. Was expecting %s but found %s",
						CYCLETYPE_TIME, this.cycletype));
			}
			if(!isValidTimeFormat(start)) {
				throw new IllegalArgumentException("Invalid time format");
			}
			this.start = start;
		}

		/**
		 * 
		 * Gets the stop point of this cycle as a string representation
		 * 
		 * @return The stop point of this cycle
		 */
		public String getStop() {
			return this.stop;
		}

		/**
		 * Sets the shutdown-time of this device's cycle. The argument must have 
		 * the format 'HH:mm:ss'
		 * 
		 * @param stop The string representation of the shutdown-time
		 * @throws IllegalArgumentException If the given time has not the required format
		 * @throws UnsetCycleException If this cycle's type is not TIME
		 */
		public void setStopTime(final String stop) throws IllegalArgumentException, UnsetCycleException {
			if(!this.cycletype.equals(CYCLETYPE_TIME)) {
				throw new UnsetCycleException(String.format("Invalid cycletype. Was expecting %s but found %s",
						CYCLETYPE_TIME, this.cycletype));
			}
			if(!isValidTimeFormat(stop)) {
				throw new IllegalArgumentException("Invalid time format");
			}
			this.stop = stop;
		}
		
		/**
		 * Sets the startup-temperature of this device's cycle
		 * 
		 * @param start the temperature at which this cycle starts
		 * @throws UnsetCycleException If this cycle's type is not TEMPERATURE
		 */
		public void setStartTemperature(final float start) throws UnsetCycleException {
			if(!this.cycletype.equals(CYCLETYPE_TEMPERATURE)) {
				throw new UnsetCycleException(String.format("Invalid cycletype. Was expecting %s but found %s",
						CYCLETYPE_TEMPERATURE, this.cycletype));
			}
			this.start = String.valueOf(start);
		}
		
		/**
		 * Sets the shutdown-temperature of this device's cycle
		 * 
		 * @param stop the temperature at which this cycle stops
		 * @throws UnsetCycleException If this cycle's type is not TEMPERATURE
		 */
		public void setStopTemperature(final float stop) throws UnsetCycleException {
			if(!this.cycletype.equals(CYCLETYPE_TEMPERATURE)) {
				throw new UnsetCycleException(String.format("Invalid cycletype. Was expecting %s but found %s",
						CYCLETYPE_TEMPERATURE, this.cycletype));
			}
			this.stop = String.valueOf(stop);
		}
		
		/**
		 * Sets the startup-humidity level of this device's cycle
		 * 
		 * @param start the air humidity level at which this cycle starts
		 * @throws IllegalArgumentException If the argument does not range between 0-100
		 * @throws UnsetCycleException If this cycle's type is not HUMIDITY
		 */
		public void setStartHumidity(final int start) throws IllegalArgumentException, UnsetCycleException {
			if(!this.cycletype.equals(CYCLETYPE_HUMIDITY)) {
				throw new UnsetCycleException(String.format("Invalid cycletype. Was expecting %s but found %s",
						CYCLETYPE_HUMIDITY, this.cycletype));
			}
			if((start < 0) || (start > 100)) {
				throw new IllegalArgumentException("Humidity must range between 0-100");
			}
			this.start = String.valueOf(start);
		}
		
		/**
		 * Sets the shutdown-humidity level of this device's cycle
		 * 
		 * @param stop the air humidity level at which this cycle stops
		 * @throws IllegalArgumentException If the argument does not range between 0-100
		 * @throws UnsetCycleException If this cycle's type is not HUMIDITY
		 */
		public void setStopHumidity(final int stop) throws IllegalArgumentException, UnsetCycleException {
			if(!this.cycletype.equals(CYCLETYPE_HUMIDITY)) {
				throw new UnsetCycleException(String.format("Invalid cycletype. Was expecting %s but found %s",
						CYCLETYPE_HUMIDITY, this.cycletype));
			}
			if((stop < 0) || (stop > 100)) {
				throw new IllegalArgumentException("Humidity must range between 0-100");
			}
			this.stop = String.valueOf(stop);
		}
		
		/**
		 * Adds a Weekday to this cycle
		 * 
		 * @param day the day to add to the cycle
		 * @return True, if the given day was actually added to the set. False if it already existed
		 */
		public boolean addWeekday(final Weekday day) {
			return this.days.add(day);
		}
		
		/**
		 * Removes a Weekday from this cycle
		 * 
		 * @param day the day to remove from the cycle
		 * @return True, if the given day was actually removed from the set. False if it didn't exist to begin with
		 */
		public boolean removeWeekday(final Weekday day) {
			return this.days.remove(day);
		}
		
		@Override
		public String toString(){
			return this.getName();
		}
		
		@SerializedName("start")
		private String start;
		@SerializedName("stop")
		private String stop;
		@SerializedName("type")
		private String cycletype = CYCLETYPE_UNKNOWN;
		@SerializedName("days")
		private EnumSet<Weekday> days = EnumSet.allOf(Weekday.class);
		@SerializedName("name")
		private String name;
		
	}
	
}