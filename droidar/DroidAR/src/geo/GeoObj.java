package geo;

import gl.MeshComponent;
import gl.MeshGroup;
import gui.MetaInfos;
import system.EventManager;
import util.EfficientList;
import util.HasDebugInformation;
import util.Vec;
import worldData.Entity;
import worldData.Obj;
import worldData.Visitor;
import android.location.Address;
import android.location.Location;
import android.util.Log;

public class GeoObj extends Obj implements HasDebugInformation {

	public interface GeoObjUpdateListener {

		void updateToNewPosition(int i, int j);

	}

	// TODO move somewhere else:
	public static final GeoObj a1 = new GeoObj(50.769118, 6.097568, 0, "A1");
	public static final GeoObj a2 = new GeoObj(50.769328, 6.097514, 0, "A2");
	public static final GeoObj a3 = new GeoObj(50.769159, 6.097986, 0, "A3");
	public static final GeoObj n1 = new GeoObj(50.769444, 6.095191, 0, "N1");
	public static final GeoObj n2 = new GeoObj(50.769617, 6.09481, 0, "N2");
	public static final GeoObj n3 = new GeoObj(50.769174, 6.095156, 0, "N3");
	public static final GeoObj rwthI9 = new GeoObj(50.778393, 6.060886, 0, "I9");
	public static final GeoObj iPark1 = new GeoObj(50.778771, 6.061074, 0, "P1");
	public static final GeoObj iPark2 = new GeoObj(50.778661, 6.060497, 0, "P2");
	public static final GeoObj iPark3 = new GeoObj(50.779134, 6.060202, 0, "P3");
	public static final GeoObj iPark4 = new GeoObj(50.779242, 6.060787, 0, "P4");
	public static final GeoObj p = new GeoObj(50.781161, 6.078752, 0, "Ponttor");

	private static final String LOG_TAG = "GeoObj";

	/**
	 * if this is true the {@link GeoObj} will try to calculate its virtual
	 * position in the world assuming the camera is moving relative to the
	 * 0-point in the world. whenever a new mesh is assigned to this object and
	 * therefor a new wrapper {@link MeshGroup} is created this flag is checked.
	 */
	private boolean autoCalcVirtualPos = true;

	/**
	 * this is the average radius of the earth in meters. polar-radius is
	 * 6356800 meters and equatorial-radius is 6378100 meters
	 */
	public static final int EARTH_RADIUS = 6371000;

	// MeshComponent myMesh;
	private double myLatitude = 0;
	private double myLongitude = 0;
	private double myAltitude = 0;

	/**
	 * is needed for the dijsktra algorithm, dont use it anywhere else, it will
	 * change when using dijsktra!
	 */
	public int dijkstraId;
	private MeshGroup mySurroundGroup;
	private GeoObjUpdateListener myUpdateListener;
	/**
	 * this flag is used in the {@link CustomItemizedOverlay}-class to
	 * synchronize it with a parallel created {@link EfficientList} instance of
	 * {@link GeoObjWrapper}s
	 */
	private boolean isDeleted = false;

	// Vec myPosition=new Vec();

	private GeoObj(double lati, double longi, double alti,
			MeshComponent meshToSurround, boolean calcVirtulPos) {
		// a geoObj itself should not have a color so null:
		setMyLatitude(lati);
		setMyLongitude(longi);
		setMyAltitude(alti);
		autoCalcVirtualPos = calcVirtulPos;
		setComp(meshToSurround);

	}

	public void setUpdateListener(GeoObjUpdateListener myUpdateListener) {
		this.myUpdateListener = myUpdateListener;
	}

	public GeoObj() {
		super();
		setComp(loadDefaultMesh());
	}

	public GeoObj(boolean autoCalcVirtualPos) {
		this.autoCalcVirtualPos = autoCalcVirtualPos;
		setComp(loadDefaultMesh());
	}

	/**
	 * @return the {@link MeshGroup} where all {@link MeshComponent} will be
	 *         inserted to and which will wrap these objects and recalculate the
	 *         virtual position of the {@link GeoObj}
	 */
	public MeshGroup getMySurroundGroup() {
		if (mySurroundGroup == null)
			if (autoCalcVirtualPos) {
				mySurroundGroup = new MeshGroup(null, getVirtualPosition());
			} else {
				mySurroundGroup = new MeshGroup();
			}
		return mySurroundGroup;
	}

	@Override
	public void setComp(Entity comp) {
		if (comp instanceof MeshComponent) {
			MeshGroup g = getMySurroundGroup();
			g.clear();
			g.add((MeshComponent) comp);
			setMyGraphicsComponent(g);
			/*
			 * if the surround-group was not jet added to the GeoObj it will be
			 * added now:
			 */
			if (getMyComponents().contains(g) == -1) {
				getMyComponents().add(g);
			}
		} else
			super.setComp(comp);
	}

	public GeoObj(double lati, double longi, double alti,
			MeshComponent meshToSurround) {
		this(lati, longi, alti, meshToSurround, true);
	}

	public GeoObj(GeoObj positionSource, MeshComponent meshToSurround) {
		this(positionSource.getLatitude(), positionSource.getLongitude(),
				positionSource.getAltitude(), meshToSurround);
	}

	public GeoObj(GeoObj positionSource) {

		this(positionSource.getLatitude(), positionSource.getLongitude(),
				positionSource.getAltitude());
	}

	// public boolean addConnection(GeoObj connectedObject) {
	// return true;
	// }

	public GeoObj(double lati, double longi, double alti) {
		this(lati, longi, alti, loadDefaultMesh());
	}

	public GeoObj(Address a) {
		this(a.getLatitude(), a.getLongitude(), 0, loadDefaultMesh());
		getInfoObject().extractInfos(a);
	}

	public GeoObj(Location l) {
		this(l.getLatitude(), l.getLongitude(), l.getAltitude(),
				loadDefaultMesh());
	}

	public GeoObj(double lati, double longi, int alti, String name) {
		this(lati, longi, alti, loadDefaultMesh());
		getInfoObject().setShortDescr(name);
	}

	/**
	 * @param l
	 *            if l is null the GPS coordinates will be set to 0
	 * @param b
	 *            if false the virtual position wont be calculated!
	 */
	public GeoObj(Location l, boolean b) {
		this((l != null) ? l.getLatitude() : null, (l != null) ? l
				.getLongitude() : null, (l != null) ? l.getAltitude() : null,
				loadDefaultMesh(), false);
	}

	public GeoObj(double latitude, double longitude) {
		this(latitude, longitude, 0);
	}

	/**
	 * this can be overwritten by subclasses of {@link GeoObj} to set a default
	 * mesh to a newly created {@link GeoObj}
	 * 
	 * @return
	 */
	public static MeshComponent loadDefaultMesh() {
		return null;
	}

	public Location toLocation() {
		Location x = new Location("customCreated");
		x.setLatitude(getLatitude());
		x.setLongitude(getLongitude());
		x.setAltitude(getAltitude());
		return x;
	}

	public double getLatitude() {
		return myLatitude;
	}

	public double getLongitude() {
		return myLongitude;
	}

	public double getAltitude() {
		return myAltitude;
	}

	public void setMyLatitude(double latitude) {
		this.myLatitude = latitude;

		if (myUpdateListener != null)
			myUpdateListener.updateToNewPosition((int) (getLatitude() * 1E6),
					(int) (getLongitude() * 1E6));
	}

	public void setMyLongitude(double longitude) {
		this.myLongitude = longitude;

		if (myUpdateListener != null)
			myUpdateListener.updateToNewPosition((int) (getLatitude() * 1E6),
					(int) (getLongitude() * 1E6));
	}

	public void setMyAltitude(double altitude) {
		this.myAltitude = altitude;

		if (myUpdateListener != null)
			myUpdateListener.updateToNewPosition((int) (getLatitude() * 1E6),
					(int) (getLongitude() * 1E6));
	}

	/**
	 * calculates the virtual position in the OpenGL-view relative to the
	 * relative zero point (normally the device position)
	 * 
	 * @param zeroLatitude
	 * @param zeroLongitude
	 * @param zeroAltitude
	 * @return the target Vec or a new Vec with the correct virtual position if
	 *         target vec was null
	 */
	public Vec getVirtualPosition(double zeroLatitude, double zeroLongitude,
			double zeroAltitude) {
		/*
		 * The longitude calculation depends on current latitude: The
		 * circumference of a circle at a given latitude is proportional to the
		 * cosine, so the formula is:
		 * 
		 * (myLongitude - zeroLongitude) * 40075017 / 360 * cos(zeroLatitude)
		 * 
		 * earth circumfence through poles is 40008000
		 * 
		 * earth circumfence at equator is 40075017
		 * 
		 * degree to radians: PI/180=0.0174532925
		 * 
		 * TODO check what happens when myLongi positive but zeroLongi negative
		 * for example. this can create problems i think! both have to be
		 * negative or positive otherwise delta value is wrong! this will nearly
		 * never happen, but for people in englang eg it might be a problem when
		 * living near the 0 latitude..
		 */
		Vec position = new Vec();
		position.x = (float) ((myLongitude - zeroLongitude) * 111319.4917 * Math
				.cos(zeroLatitude * 0.0174532925));
		position.y = (float) ((myLatitude - zeroLatitude) * 111133.3333);
		// TODO set z (altitude) too
		return position;
	}

	/**
	 * This method will set the gps position according to the passed virtual
	 * position
	 * 
	 * @param virtualPosition
	 *            the virtual position of the object in meters
	 * @param zeroLocation
	 *            normally this would be the device position, but you can
	 *            specify anything else here too
	 * @return if the virtual position of the mesh could be updated too
	 */
	public boolean calcGPSPosition(Vec virtualPosition, GeoObj zeroLocation) {
		Vec newGPSPos = GeoObj.calcGPSPosition(virtualPosition,
				zeroLocation.getLatitude(), zeroLocation.getLongitude(),
				zeroLocation.getAltitude());
		if (newGPSPos != null) {
			setMyLongitude(newGPSPos.x);
			setMyLatitude(newGPSPos.y);
			setMyAltitude(newGPSPos.z);
		} else {
			Log.i(LOG_TAG, "GeoObj " + this
					+ " did not have a virtual position so it's assumed "
					+ "it should be placed at (0,0,0).");
			setMyLatitude(zeroLocation.getLatitude());
			setMyLongitude(zeroLocation.getLongitude());
			setMyAltitude(zeroLocation.getAltitude());
		}
		return refreshVirtualPosition();
	}

	/**
	 * define the virtual position and the GPS position will be calculated
	 * correctly.
	 * 
	 * @param virtualPosition
	 * @return true if the virtual position was updated correctly
	 */
	public boolean setVirtualPosition(Vec virtualPosition) {
		return calcGPSPosition(virtualPosition, EventManager.getInstance()
				.getZeroPositionLocationObject());
	}

	/**
	 * @param zeroLatitude
	 * @param zeroLongitude
	 * @param zeroAltitude
	 * @return a Vector with x=Longitude, y=Latitude, z=Altitude
	 */
	public static Vec calcGPSPosition(Vec virtualPosition, double zeroLatitude,
			double zeroLongitude, double zeroAltitude) {
		if (virtualPosition != null) {
			/*
			 * same formula as in calcVirtualPos() but resolved for latitude and
			 * longitude:
			 */
			Vec result = new Vec();
			result.x = (float) (virtualPosition.x
					/ (111319.889f * Math.cos(zeroLatitude * 0.0174532925f)) + zeroLongitude);
			result.y = (float) (virtualPosition.y / 111133.3333f + zeroLatitude);
			result.z = (float) (virtualPosition.z + zeroAltitude);
			return result;
		}
		return null;
	}

	/**
	 * This sets all three position values at once (can be useful in combination
	 * with {@link GeoObj}.calcGPSPosition e.g.
	 * 
	 * @param GPSPosition
	 *            a Vector with x=Longitude, y=Latitude, z=Altitude
	 */
	public void setMyPosition(Vec GPSPosition) {
		setMyLongitude(GPSPosition.x);
		setMyLatitude(GPSPosition.y);
		setMyAltitude(GPSPosition.z);
	}

	public int matchesSearchTerm(String searchTerm) {
		return getInfoObject().matchesSearchTerm(searchTerm);
	}

	/**
	 * calculates the distance in meters to the {@link GeoObj} GPS-position
	 * 
	 * @param otherGeoObj
	 * @return positive value in meters
	 */
	public double getDistance(GeoObj otherObj) {
		return distFrom(getLatitude(), getLongitude(), otherObj.getLatitude(),
				otherObj.getLongitude());
	}

	/**
	 * Implementation of the Harvesine function
	 * 
	 * @param lat1
	 *            value in degree
	 * @param lng1
	 *            value in degree
	 * @param lat2
	 *            value in degree
	 * @param lng2
	 *            value in degree
	 * @return the distance in meters
	 */
	private static double distFrom(double lat1, double lng1, double lat2,
			double lng2) {
		final double dLat = Math.toRadians(lat2 - lat1);
		final double dLng = Math.toRadians(lng2 - lng1);
		final double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
				* Math.sin(dLng / 2);
		return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	public boolean hasSameCoordsAs(GeoObj o) {
		if (o.getLatitude() == getLatitude()) {
			if (o.getLongitude() == getLongitude()) {
				return true;
			}
		}
		return false;
	}

	public void setLocation(Location l) {
		if (l != null) {
			setMyLatitude(l.getLatitude());
			setMyLongitude(l.getLongitude());
			setMyAltitude(l.getAltitude());
		}
	}

	// public void setLocation(Location location) {
	// setMyLatitude(location.getLatitude());
	// setMyLongitude(location.getLongitude());
	// setMyAltitude(location.getAltitude());
	// }

	public Vec getVirtualPosition(GeoObj relativeNullPoint) {
		if (relativeNullPoint == null) {
			Log.e(LOG_TAG, "Virtual position can't be calculated if the "
					+ "relative zero position is not known!");
			return null;
		}
		return getVirtualPosition(relativeNullPoint.getLatitude(),
				relativeNullPoint.getLongitude(),
				relativeNullPoint.getAltitude());
	}

	/**
	 * This will use the current gps values and calculate the virtual position
	 * in account to the current device gps position
	 * 
	 * @return the virtual position
	 */
	public Vec getVirtualPosition() {
		return getVirtualPosition(EventManager.getInstance()
				.getZeroPositionLocationObject());
	}

	/**
	 * refresh the virtual position of the object (does only work if the object
	 * has a {@link MeshComponent} jet)
	 * 
	 * @return true if it worked
	 */
	private boolean refreshVirtualPosition() {
		Vec pos = getVirtualPosition();
		if (pos != null) {
			MeshComponent m = getGraphicsComponent();

			if (m != null) {
				m.myPosition = pos;
				return true;
			}
		}
		return false;
	}

	// /**
	// * this is called by {@link DefaultSelectionInterface} objects like the
	// * {@link GMap} or the {@link GeoGraph} to load the default onClick
	// command
	// * and set it to this {@link GeoObj}
	// *
	// * @param s
	// */
	// public void setSelectionCommands(DefaultSelectionInterface s) {
	// if (getOnClickCommand() == null)
	// setOnClickCommand(s.getDefaultOnClickCommand());
	// if (getOnLongClickCommand() == null)
	// setOnLongClickCommand(s.getDefaultOnLongClickCommand());
	// if (getOnMapClickCommand() == null)
	// setOnMapClickCommand(s.getDefaultOnMapClickCommand());
	// }

	@Override
	public boolean accept(Visitor visitor) {
		return visitor.default_visit(this);
	}

	public void setRemoved() {
		isDeleted = true;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	/**
	 * @return a new object with the same location data and the same
	 *         {@link MetaInfos} information
	 */
	public GeoObj copy() {
		GeoObj o = new GeoObj(this);
		o.autoCalcVirtualPos = this.autoCalcVirtualPos;
		o.getInfoObject().setTo(this.getInfoObject());
		return o;
	}

	@Override
	public void showDebugInformation() {
		Log.e(LOG_TAG, "Information about " + this);
		Log.d(LOG_TAG, "mySurroundGroup=" + mySurroundGroup);
		Log.d(LOG_TAG, "mySurroundGroup.myPosition="
				+ mySurroundGroup.myPosition);
		Log.d(LOG_TAG, "mySurroundGroup.myScale=" + mySurroundGroup.myScale);
		Log.d(LOG_TAG, "mySurroundGroup.myRotation="
				+ mySurroundGroup.myRotation);
		Log.d(LOG_TAG, "mesh inside=" + mySurroundGroup.getAllItems().get(0));

	}

}
