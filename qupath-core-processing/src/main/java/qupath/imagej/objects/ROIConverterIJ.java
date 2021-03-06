/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.imagej.objects;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import qupath.lib.awt.common.AwtTools;
import qupath.lib.geom.Point2;
import qupath.lib.images.PathImage;
import qupath.lib.regions.ImagePlane;
import qupath.lib.regions.ImageRegion;
import qupath.lib.roi.EllipseROI;
import qupath.lib.roi.LineROI;
import qupath.lib.roi.AreaROI;
import qupath.lib.roi.PathROIToolsAwt;
import qupath.lib.roi.PointsROI;
import qupath.lib.roi.PolygonROI;
import qupath.lib.roi.PolylineROI;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.RectangleROI;
import qupath.lib.roi.interfaces.PathPoints;
import qupath.lib.roi.interfaces.ROI;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.process.FloatPolygon;

/**
 * Class for converting between PathROIs and ImageJ's own Rois of various types.
 * 
 * @author Pete Bankhead
 *
 */
public class ROIConverterIJ {
	
	public static double convertXtoIJ(double x, double xOrigin, double downsample) {
		return x / downsample + xOrigin;
	}

	public static double convertYtoIJ(double y, double yOrigin, double downsample) {
		return y / downsample + yOrigin;
	}
	
	@Deprecated
	private static <T extends Roi> T setIJRoiProperties(T roi, ROI pathROI) {
////		roi.setStrokeColor(pathROI.getStrokeColor());
////		roi.setStrokeWidth(pathROI.getStrokeWidth());
//		roi.setName(pathROI.getName());
		return roi;
	}

	public static Rectangle2D getTransformedBounds(ROI pathROI, double xOrigin, double yOrigin, double downsampleFactor) {
		Rectangle2D bounds = AwtTools.getBounds2D(pathROI);
		double x1 = convertXtoIJ(bounds.getMinX(), xOrigin, downsampleFactor);
		double y1 = convertYtoIJ(bounds.getMinY(), yOrigin, downsampleFactor);
		double x2 = convertXtoIJ(bounds.getMaxX(), xOrigin, downsampleFactor);
		double y2 = convertYtoIJ(bounds.getMaxY(), yOrigin, downsampleFactor);
		return new Rectangle2D.Double(
				x1, y1, x2-x1, y2-y1);
	}
	
	/**
	 * Convert a collection of points from a ROI into the coordinate space determined from the calibration information.
	 * 
	 * @param points
	 * @param xOrigin
	 * @param yOrigin
	 * @param downsampleFactor
	 * @return float arrays, where result[0] gives the x coordinates and result[1] the y coordinates
	 */
	protected static float[][] getTransformedPoints(Collection<Point2> points, double xOrigin, double yOrigin, double downsampleFactor) {
		float[] xPoints = new float[points.size()];
		float[] yPoints = new float[points.size()];
		int i = 0;
		for (Point2 p : points) {
			xPoints[i] = (float)convertXtoIJ(p.getX(), xOrigin, downsampleFactor);
			yPoints[i] = (float)convertYtoIJ(p.getY(), yOrigin, downsampleFactor);
			i++;
		}
		return new float[][]{xPoints, yPoints};
	}
	

	public static Roi getRectangleROI(RectangleROI pathRectangle, double xOrigin, double yOrigin, double downsampleFactor) {
		Rectangle2D bounds = getTransformedBounds(pathRectangle, xOrigin, yOrigin, downsampleFactor);
		return setIJRoiProperties(new Roi(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()), pathRectangle);
	}

	public static OvalRoi convertToOvalROI(EllipseROI pathOval, double xOrigin, double yOrigin, double downsampleFactor) {
		Rectangle2D bounds = getTransformedBounds(pathOval, xOrigin, yOrigin, downsampleFactor);
		return setIJRoiProperties(new OvalRoi(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight()), pathOval);
	}

	public static Line convertToLineROI(LineROI pathLine, double xOrigin, double yOrigin, double downsampleFactor) {
		return setIJRoiProperties(new Line(convertXtoIJ(pathLine.getX1(), xOrigin, downsampleFactor),
											convertYtoIJ(pathLine.getY1(), yOrigin, downsampleFactor),
											convertXtoIJ(pathLine.getX2(), xOrigin, downsampleFactor),
											convertYtoIJ(pathLine.getY2(), yOrigin, downsampleFactor)), pathLine);		
	}

	public static PointRoi convertToPointROI(PointsROI pathPoints, double xOrigin, double yOrigin, double downsampleFactor) {
		float[][] points = getTransformedPoints(pathPoints.getPointList(), xOrigin, yOrigin, downsampleFactor);
		return setIJRoiProperties(new PointRoi(points[0], points[1]), pathPoints);
	}

	public static PolygonRoi convertToPolygonROI(PolygonROI pathPolygon, double xOrigin, double yOrigin, double downsampleFactor) {
		float[][] points = getTransformedPoints(pathPolygon.getPolygonPoints(), xOrigin, yOrigin, downsampleFactor);
		return setIJRoiProperties(new PolygonRoi(points[0], points[1], Roi.POLYGON), pathPolygon);
	}
	
	public static PolygonRoi convertToPolygonROI(PolylineROI pathPolygon, double xOrigin, double yOrigin, double downsampleFactor) {
		float[][] points = getTransformedPoints(pathPolygon.getPolygonPoints(), xOrigin, yOrigin, downsampleFactor);
		return setIJRoiProperties(new PolygonRoi(points[0], points[1], Roi.POLYLINE), pathPolygon);
	}
	
	/**
	 * Create an ImageJ Roi from a ROI, suitable for displaying on the ImagePlus of an {@code PathImage<ImagePlus>}.
	 * 
	 * @param pathROI
	 * @param pathImage
	 * @return
	 */
	public static <T extends PathImage<ImagePlus>> Roi convertToIJRoi(ROI pathROI, T pathImage) {
		Calibration cal = null;
		double downsampleFactor = 1;
		if (pathImage != null) {
			cal = pathImage.getImage().getCalibration();
			downsampleFactor = pathImage.getDownsampleFactor();
		}
		// TODO: Integrate ROI not supported exception...?
		return convertToIJRoi(pathROI, cal, downsampleFactor);		
	}

	public static <T extends PathImage<ImagePlus>> Roi convertToIJRoi(ROI pathROI, Calibration cal, double downsampleFactor) {
		if (cal != null)
			return convertToIJRoi(pathROI, cal.xOrigin, cal.yOrigin, downsampleFactor);
		else
			return convertToIJRoi(pathROI, 0, 0, downsampleFactor);
	}

	public static <T extends PathImage<ImagePlus>> Roi convertToIJRoi(ROI pathROI, double xOrigin, double yOrigin, double downsampleFactor) {
		if (pathROI instanceof PolygonROI)
			return convertToPolygonROI((PolygonROI)pathROI, xOrigin, yOrigin, downsampleFactor);
		if (pathROI instanceof RectangleROI)
			return getRectangleROI((RectangleROI)pathROI, xOrigin, yOrigin, downsampleFactor);
		if (pathROI instanceof EllipseROI)
			return convertToOvalROI((EllipseROI)pathROI, xOrigin, yOrigin, downsampleFactor);
		if (pathROI instanceof LineROI)
			return convertToLineROI((LineROI)pathROI, xOrigin, yOrigin, downsampleFactor);
		if (pathROI instanceof PolylineROI)
			return convertToPolygonROI((PolylineROI)pathROI, xOrigin, yOrigin, downsampleFactor);
		if (pathROI instanceof PointsROI)
			return convertToPointROI((PointsROI)pathROI, xOrigin, yOrigin, downsampleFactor);
		// If we have any other kind of shape, create a general shape roi
		if (pathROI instanceof AreaROI) { // TODO: Deal with non-AWT area ROIs!
			Shape shape = PathROIToolsAwt.getArea(pathROI);
//			"scaleX", "shearY", "shearX", "scaleY", "translateX", "translateY"
			shape = new AffineTransform(1.0/downsampleFactor, 0, 0, 1.0/downsampleFactor, xOrigin, yOrigin).createTransformedShape(shape);
			return setIJRoiProperties(new ShapeRoi(shape), pathROI);
		}
		// TODO: Integrate ROI not supported exception...?
		return null;		
	}
	
	
	public static double convertXfromIJ(double x, Calibration cal, double downsample) {
		return convertLocationfromIJ(x, cal == null ? 0 : cal.xOrigin, downsample);
	}

	public static double convertYfromIJ(double y, Calibration cal, double downsample) {
		return convertLocationfromIJ(y, cal == null ? 0 : cal.yOrigin, downsample);
	}
	
	/**
	 * Take an x or y coordinate for a pixel in ImageJ, and convert to a full image pixel using the 
	 * Calibration.xOrigin or Calibration.yOrigin.
	 * 
	 * @param xory
	 * @param origin
	 * @param downsample
	 * @return
	 */
	public static double convertLocationfromIJ(double xory, double origin, double downsample) {
		return (xory - origin) * downsample;
	}
	
	/**
	 * Create a ROI from an ImageJ Roi.
	 * 
	 * @param roi
	 * @param pathImage
	 * @return
	 */
	public static <T extends PathImage<? extends ImagePlus>> ROI convertToPathROI(Roi roi, T pathImage) {
		Calibration cal = null;
		double downsampleFactor = 1;
		ImageRegion region = pathImage.getImageRegion();
		if (pathImage != null) {
			cal = pathImage.getImage().getCalibration();
			downsampleFactor = pathImage.getDownsampleFactor();
		}
		return convertToPathROI(roi, cal, downsampleFactor, -1, region.getZ(), region.getT());	
	}
	
	
	public static ROI convertToPathROI(Roi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToPathROI(roi, x, y, downsampleFactor, c, z, t);
	}
	/**
	 * Create a ROI from an ImageJ Roi.
	 * 
	 * @param roi
	 * @param xOrigin
	 * @param yOrigin
	 * @param downsampleFactor
	 * @param c
	 * @param z
	 * @param t
	 * @return
	 */
	public static ROI convertToPathROI(Roi roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
//		if (roi.getType() == Roi.POLYGON || roi.getType() == Roi.TRACED_ROI)
//			return convertToPolygonROI((PolygonRoi)roi, cal, downsampleFactor);
		if (roi.getType() == Roi.RECTANGLE && roi.getCornerDiameter() == 0)
			return getRectangleROI(roi, xOrigin, yOrigin, downsampleFactor, c, z, t);
		if (roi.getType() == Roi.OVAL)
			return convertToEllipseROI(roi, xOrigin, yOrigin, downsampleFactor, c, z, t);
		if (roi instanceof Line)
			return convertToLineROI((Line)roi, xOrigin, yOrigin, downsampleFactor, c, z, t);
		if (roi instanceof PointRoi)
			return convertToPointROI((PolygonRoi)roi, xOrigin, yOrigin, downsampleFactor, c, z, t);
//		if (roi instanceof ShapeRoi)
//			return convertToAreaROI((ShapeRoi)roi, cal, downsampleFactor);
//		// Shape ROIs should be able to handle most eventualities
		if (roi instanceof ShapeRoi)
			return convertToAreaROI((ShapeRoi)roi, xOrigin, yOrigin, downsampleFactor, c, z, t);
		if (roi.isArea())
			return convertToPolygonOrAreaROI(roi, xOrigin, yOrigin, downsampleFactor, c, z, t);
		if (roi instanceof PolygonRoi) {
			if (roi.getType() == Roi.FREELINE || roi.getType() == Roi.POLYLINE)
				return convertToPolylineROI((PolygonRoi)roi, xOrigin, yOrigin, downsampleFactor, c, z, t);
		}
		// TODO: Integrate ROI not supported exception...?
		return null;	
	}

	@Deprecated
	public static ROI convertToPolylineROI(PolygonRoi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToPolylineROI(roi, x, y, downsampleFactor, c, z, t);
	}
	
	public static ROI convertToPolylineROI(PolygonRoi roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
		List<Point2> points = convertToPointsList(roi.getFloatPolygon(), xOrigin, yOrigin, downsampleFactor);
		if (points == null)
			return null;
		return ROIs.createPolylineROI(points, ImagePlane.getPlaneWithChannel(c, z, t));
	}
	
	@Deprecated
	public static ROI convertToPolygonOrAreaROI(Roi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToPolygonOrAreaROI(roi, x, y, downsampleFactor, c, z, t);
	}
	
	public static ROI convertToPolygonOrAreaROI(Roi roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
		Shape shape;
		if (roi instanceof ShapeRoi)
			shape = ((ShapeRoi)roi).getShape();
		else
			shape = new ShapeRoi(roi).getShape();
		AffineTransform transform = new AffineTransform();
		transform.scale(downsampleFactor, downsampleFactor);
		transform.translate(roi.getXBase(), roi.getYBase());
		transform.translate(-xOrigin, -yOrigin);
		return ROIs.createAreaROI(new Area(transform.createTransformedShape(shape)), ImagePlane.getPlaneWithChannel(c, z, t));
//		return setPathROIProperties(new PathAreaROI(transform.createTransformedShape(shape)), roi);
	}
	
	@Deprecated
	public static ROI convertToAreaROI(ShapeRoi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToAreaROI(roi, x, y, downsampleFactor, c, z, t);
	}
	
	public static ROI convertToAreaROI(ShapeRoi roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
		Shape shape = roi.getShape();
		AffineTransform transform = new AffineTransform();
		transform.scale(downsampleFactor, downsampleFactor);
		transform.translate(roi.getXBase(), roi.getYBase());
		transform.translate(-xOrigin, -yOrigin);
//		return setPathROIProperties(PathROIHelpers.getShapeROI(new Area(transform.createTransformedShape(shape)), 0, 0, 0), roi);
		return ROIs.createAreaROI(transform.createTransformedShape(shape), ImagePlane.getPlaneWithChannel(c, z, t));
	}
	
	
	protected static Rectangle2D getTransformedBoundsFromIJ(Roi roi, double xOrigin, double yOrigin, double downsampleFactor) {
		Rectangle2D bounds = roi.getBounds();
		double x1 = convertLocationfromIJ(bounds.getMinX(), xOrigin, downsampleFactor);
		double y1 = convertLocationfromIJ(bounds.getMinY(), yOrigin, downsampleFactor);
		double x2 = convertLocationfromIJ(bounds.getMaxX(), xOrigin, downsampleFactor);
		double y2 = convertLocationfromIJ(bounds.getMaxY(), yOrigin, downsampleFactor);
		return new Rectangle2D.Double(
				x1, y1, x2-x1, y2-y1);
		
//		return new Rectangle2D.Double(
//				convertXfromIJ(bounds.getX(), cal, downsampleFactor),
//				convertYfromIJ(bounds.getY(), cal, downsampleFactor),
//				convertXfromIJ(bounds.getWidth(), null, downsampleFactor),
//				convertYfromIJ(bounds.getHeight(), null, downsampleFactor));
	}
	
	@Deprecated
	public static ROI getRectangleROI(Roi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return getRectangleROI(roi, x, y, downsampleFactor, c, z, t);
	}
	
	public static ROI getRectangleROI(Roi roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
		Rectangle2D bounds = getTransformedBoundsFromIJ(roi, xOrigin, yOrigin, downsampleFactor);
		return ROIs.createRectangleROI(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), ImagePlane.getPlaneWithChannel(c, z, t));
	}
	
	@Deprecated
	public static ROI convertToEllipseROI(Roi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToEllipseROI(roi, x, y, downsampleFactor, c, z, t);
	}

	public static ROI convertToEllipseROI(Roi roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
		Rectangle2D bounds = getTransformedBoundsFromIJ(roi, xOrigin, yOrigin, downsampleFactor);
		return ROIs.createEllipseROI(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), ImagePlane.getPlaneWithChannel(c, z, t));
	}

	@Deprecated
	public static ROI convertToLineROI(Line roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToLineROI(roi, x, y, downsampleFactor, c, z, t);
	}

	public static ROI convertToLineROI(Line roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
		double x1 = convertLocationfromIJ(roi.x1d, xOrigin, downsampleFactor);
		double x2 = convertLocationfromIJ(roi.x2d, xOrigin, downsampleFactor);
		double y1 = convertLocationfromIJ(roi.y1d, yOrigin, downsampleFactor);
		double y2 = convertLocationfromIJ(roi.y2d, yOrigin, downsampleFactor);
		return ROIs.createLineROI(x1, y1, x2, y2, ImagePlane.getPlaneWithChannel(c, z, t));		
	}
	
//	public static PathPoints convertToPointROI(PolygonRoi roi, Calibration cal, double downsampleFactor) {
//		return convertToPointROI(roi, cal, downsampleFactor, -1, 0, 0);
//	}
	
	public static PathPoints convertToPointROI(PolygonRoi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToPointROI(roi, x, y, downsampleFactor, c, z, t);
	}

	public static PathPoints convertToPointROI(PolygonRoi roi, double xOrigin, double yOrigin, double downsampleFactor, final int c, final int z, final int t) {
		List<Point2> points = convertToPointsList(roi.getFloatPolygon(), xOrigin, yOrigin, downsampleFactor);
		if (points == null)
			return null;
		return ROIs.createPointsROI(points, ImagePlane.getPlaneWithChannel(c, z, t));
	}
	
	public static PolygonROI convertToPolygonROI(PolygonRoi roi, Calibration cal, double downsampleFactor) {
		return convertToPolygonROI(roi, cal, downsampleFactor, -1, 0, 0);
	}

	public static PolygonROI convertToPolygonROI(PolygonRoi roi, Calibration cal, double downsampleFactor, final int c, final int z, final int t) {
		List<Point2> points = convertToPointsList(roi.getFloatPolygon(), cal, downsampleFactor);
		if (points == null)
			return null;
		return ROIs.createPolygonROI(points, ImagePlane.getPlaneWithChannel(c, z, t));
	}
	
	public static List<Point2> convertToPointsList(FloatPolygon polygon, Calibration cal, double downsampleFactor) {
		double x = cal == null ? 0 : cal.xOrigin;
		double y = cal == null ? 0 : cal.yOrigin;
		return convertToPointsList(polygon, x, y, downsampleFactor);
	}

	public static List<Point2> convertToPointsList(FloatPolygon polygon, double xOrigin, double yOrigin, double downsampleFactor) {
		if (polygon == null)
			return null;
		List<Point2> points = new ArrayList<>();
		for (int i = 0; i < polygon.npoints; i++) {
			float x = (float)convertLocationfromIJ(polygon.xpoints[i], xOrigin, downsampleFactor);
			float y = (float)convertLocationfromIJ(polygon.ypoints[i], yOrigin, downsampleFactor);
			points.add(new Point2(x, y));
		}
		return points;
	}
	

}
