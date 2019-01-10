package spb;

import java.util.Random;

public class Position {
	private static Random rand = new Random();
	private double x;
	private double y;

	public Position() {
		x = (double) Math.round((Math.sqrt(rand.nextInt(101)) + rand.nextInt(91)) * 100D)/100D; 
		y = (double) Math.round((Math.sqrt(rand.nextInt(101)) + rand.nextInt(91)) * 100D)/100D;
	}
	
	public Position(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public Position(Position p) {
		this.x = p.getX();
		this.y = p.getY();
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(x + " " + y);
		return sb.toString();
	}
	
	public double calcDist(Position p) {
		double x = p.getX();
		double y = p.getY();
		double diffX = x - this.x;
		double diffY = y - this.y;
		return (double) Math.round((Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffY, 2))) * 100D) / 100D;
	}
	
	public Position getPoint (Position p, double d) {
		double sx = this.x;
		double sy = this.y;
		double ex = p.getX();
		double ey = p.getY();
		double diffX = ex - sx;
		double diffY = ey - sy; 
		double di = (double) Math.round((Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffY, 2))) * 100D) / 100D;
		double tx;
		double ty;
		tx = (double) Math.round((sx + d*(diffX / di)) * 100D) / 100D;
		ty = (double) Math.round((sy + d*(diffY / di)) * 100D) / 100D;
		Position trigger = new Position (tx, ty);
		return trigger;
	}
	
	public boolean equals(Object o){
		if(o==this){
			return true;
	    }
	    if(o==null || o.getClass()!=this.getClass()){
	        return false;
	    }
	    Position u =(Position) o;
	    return u.getX()==(this.x) && u.getY()==(this.y);
	}
		
}