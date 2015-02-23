/*
* Copyright (c) 2012, 2015 Thomas Lively
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
* associated documentation files (the "Software"), to deal in the Software without restriction,
* including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
* and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
* subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all copies or substantial
* portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
* LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;


// ***** A NOTE ON UNITS *****
// times are generally in seconds unless otherwise noted.
// sizes and lengths are in pixels.
// velocities are in pixels/second.
// accelerations in pixels/second^2.
// angles are in degrees.
// angular velocities are in degrees/second.


// represents the game as a whole, including the game window. Contains main()
@SuppressWarnings("serial") // not important, but keeps the compiler happy. Google serialization for interesting stuff.
public class PixelPilot extends JFrame {
	private static final int TARGET_FRAMERATE = 60;

	// window and layout constants
	private static final PixelPilot INSTANCE = new PixelPilot();
	private static final String WINDOW_TITLE = "Pixel Pilot";
	private static final int WINDOW_WIDTH = 1000;
	private static final int WINDOW_HEIGHT = 750;
	private static final int GAMEOVER_X_POS = 167;
	private static final int GAMEOVER_Y_POS = 122;
	private static final int PAUSED_X_POS = 219;
	private static final int PAUSED_Y_POS = 315;
	private static final int SCORE_X_POS = 8;
	private static final int SCORE_Y_POS = 18;
	private static final int LIVES_X_OFFSET = 24;
	private static final int LIVES_X_POS = WINDOW_WIDTH - LIVES_X_OFFSET;
	private static final int LIVES_Y_POS = WINDOW_HEIGHT - 50;

	// graphics constants
	private static final Color SCORE_COLOR = Color.CYAN;
	private static final Color BG_COLOR = Color.BLACK;
	private static final Shape BG_SHAPE = new Rectangle2D.Float(0,0,1000,750);
	private static final Font FONT = new Font("SansSerif", Font.PLAIN, 15);
	private static final Image GAMEOVER_IMG = new ImageIcon(PixelPilot.class.getResource("Game Over.gif")).getImage();
	private static final Image PAUSED_IMG = new ImageIcon(PixelPilot.class.getResource("Paused.gif")).getImage();

	// fighter constants
	private static final int FIGHTER_WIDTH = 38;
	private static final int FIGHTER_HEIGHT = 48;
	private static final Image LIFE_IMG = new ImageIcon(PixelPilot.class.getResource("Fighter.gif")).getImage();
	private static final Image FIGHTER_IMG = LIFE_IMG.getScaledInstance(FIGHTER_WIDTH, FIGHTER_HEIGHT, Image.SCALE_SMOOTH);
	private static final Image THRUST_IMG = new ImageIcon(PixelPilot.class.getResource("Thrust.gif")).getImage()
			.getScaledInstance(FIGHTER_WIDTH, FIGHTER_HEIGHT, Image.SCALE_SMOOTH);
	private static final Image FIRING_IMG = new ImageIcon(PixelPilot.class.getResource("Firing.gif")).getImage()
			.getScaledInstance(FIGHTER_WIDTH, FIGHTER_HEIGHT, Image.SCALE_SMOOTH);
	public static final Image INVINCIBILITY_IMG = new ImageIcon(PixelPilot.class.getResource("Invincibility.gif")).getImage()
			.getScaledInstance(FIGHTER_WIDTH, FIGHTER_HEIGHT, Image.SCALE_SMOOTH);

	// star constants
	private static final Image[] STAR_IMGS = {
		new ImageIcon(PixelPilot.class.getResource("Star_1.gif")).getImage(),
		new ImageIcon(PixelPilot.class.getResource("Star_2.gif")).getImage(),
		new ImageIcon(PixelPilot.class.getResource("Star_3.gif")).getImage(),
		new ImageIcon(PixelPilot.class.getResource("Star_4.gif")).getImage()
	};
	private static final int STAR_NUM_MIN = 150;
	private static final int STAR_NUM_RANGE = 250;
	private static final int STAR_SIZE_MIN = 1;
	private static final int STAR_SIZE_RANGE = 5;

	// asteroid constants
	private static final Image[] ASTEROID_IMGS = {
		new ImageIcon(PixelPilot.class.getResource("Asteroid_1.gif")).getImage(),
		new ImageIcon(PixelPilot.class.getResource("Asteroid_2.gif")).getImage(),
		new ImageIcon(PixelPilot.class.getResource("Asteroid_3.gif")).getImage()
	};
	private static final int ASTEROID_SIZE_MIN = 25;
	private static final int ASTEROID_SIZE_RANGE = 30;
	private static final double ASTEROID_VEL_MIN = 50;
	private static final double ASTEROID_VEL_RANGE = 50;
	private static final double ASTEROID_VEL_HITS_MULTIPLIER = .025; 
	private static final double ASTEROID_ANGULAR_VEL_MIN = 60;
	private static final double ASTEROID_ANGULAR_VEL_RANGE = 180;

	// bullet constants
	private static final int BULLET_WIDTH = 6;
	private static final int BULLET_HEIGHT = 12;
	private static final Image BULLET_IMG = new ImageIcon(Bullet.class.getResource("Bullet.gif"))
		.getImage().getScaledInstance(BULLET_WIDTH, BULLET_HEIGHT, Image.SCALE_SMOOTH);
	private static final float BULLET_MUZZLE_VELOCITY = 1000;

	// gameplay constants
	private static final Random RAND = new Random();
	private static final int INITIAL_LIVES = 3; // start with 3 lives
	private static final int INITIAL_NEXT_LIFE = 200; // get the first bonus life at 200 points
	private static final int INITIAL_NEXT_ASTEROID = 15; // add a second target after destroying 15
	private static final double INVINCIBILITY_TIME = 3; // 3 seconds
	private static final double COOLDOWN_TIME = .35; // .35 seconds between shots
	private static final double FIRING_TIME = .05; // show firing graphic for .05 seconds
	private static final double LINEAR_ACCELERATION = 300;
	private static final double ROTATIONAL_VELOCITY= 270;

	// input variables
	private static boolean wDown = false;
	private static boolean aDown = false;
	private static boolean sDown = false;
	private static boolean dDown = false;
	private static boolean jDown = false;
	private static boolean kDown = false;
	private static boolean spDown = false;
	private static boolean enterDown = false;
	private static boolean isPaused = false; // toggled by pressing escape

	// game variables (initialized in resetGame())
	private static Fighter ship;
	private static ArrayList<Bullet> bullets;
	private static ArrayList<Asteroid> targets;
	private static Star[] stars; // use array because this doesn't grow during the game
	private static int numLives;
	private static int targetsHit;
	private static int nextLifeGain;
	private static int score;
	private static int nextAsteroid;
	private static double timeSinceLastHit;
	private static double invincibilityTimer;
	private static double cooldownTimer;


	// constructor creates game window
	private PixelPilot() {
		this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		this.setTitle(WINDOW_TITLE);
		this.setResizable(false);
		this.setLocationRelativeTo(null); // will create window in center of screen
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // closing the window will close the program
		this.add(new DrawingSpace()); // so we can draw stuff in the window
	}

	// reset game variables to their original values
	private static void resetGame() {
		numLives = INITIAL_LIVES;
		nextLifeGain = INITIAL_NEXT_LIFE;
		nextAsteroid = INITIAL_NEXT_ASTEROID;
		score = 0;
		targetsHit = 0;
		invincibilityTimer = 0;
		cooldownTimer = 0;
		timeSinceLastHit = 0;
		bullets = new ArrayList<Bullet>();
		targets = new ArrayList<Asteroid>();
		targets.add(newTarget());
		ship = new Fighter();
		stars = getNewStars();
	}

	// generate a new starfield
	private static Star[] getNewStars() {
		// randomly determine number of stars
		int numStars = RAND.nextInt(STAR_NUM_RANGE) + STAR_NUM_MIN;
		Star[] stars = new Star[numStars];
		// randomly generate parameters for each star
		for (int i = 0; i < numStars; i++) {
			stars[i] = new Star(RAND.nextInt(WINDOW_WIDTH), RAND.nextInt(WINDOW_HEIGHT),
					STAR_IMGS[RAND.nextInt(STAR_IMGS.length)]
					.getScaledInstance(RAND.nextInt(STAR_SIZE_RANGE)+STAR_SIZE_MIN, -1, Image.SCALE_SMOOTH));
		}
		return stars;
	}

	// create and return a new procedurally generated asteroid that will travel from
	// a random point on one edge of the window to a random point on the opposite edge.
	private static Asteroid newTarget() {
		double x, y, r, dx, dy, dr;
		int diameter;
		int targetX, targetY;
		Image img;

		// randomly determine diameter and angular velocity
		diameter = RAND.nextInt(ASTEROID_SIZE_RANGE) + ASTEROID_SIZE_MIN;
		r = RAND.nextInt(360);
		dr = RAND.nextDouble() * ASTEROID_ANGULAR_VEL_RANGE - ASTEROID_ANGULAR_VEL_MIN;

		// initial placement on bottom or top of window
		if (RAND.nextBoolean())
		{
			x = RAND.nextInt(WINDOW_WIDTH + diameter) - diameter;
			targetX = RAND.nextInt(WINDOW_WIDTH + diameter) - diameter;

			// spawn on top of window (y is down)
			if (RAND.nextBoolean()) {
				y = -diameter;
				targetY = WINDOW_HEIGHT;
			}
			// spawn on bottom of window
			else {
				y = WINDOW_HEIGHT;
				targetY = -diameter;
			}
		}
		// initial placement on left or right of window
		else
		{
			y = RAND.nextInt(WINDOW_HEIGHT + diameter) - diameter;
			targetY = RAND.nextInt(WINDOW_HEIGHT + diameter) - diameter;

			// spawn on left side of window
			if (RAND.nextBoolean()) {
				x = -diameter;
				targetX = WINDOW_WIDTH;
			}
			// spawn on right side of window
			else {
				x = WINDOW_WIDTH;
				targetX =  -diameter;
			}
		}

		// randomly determine speed
		double speed = RAND.nextDouble() * ASTEROID_VEL_RANGE + ASTEROID_VEL_MIN +
				targetsHit * ASTEROID_VEL_HITS_MULTIPLIER;

		// determine angle of path across window
		double theta = Math.atan2(targetY - y, targetX - x);

		// determine x and y components of velocity
		dx = Math.cos(theta) * speed;
		dy = Math.sin(theta) * speed;

		// randomly choose an asteroid image
		img = ASTEROID_IMGS[RAND.nextInt(ASTEROID_IMGS.length)].getScaledInstance(diameter, -1, Image.SCALE_SMOOTH);

		return new Asteroid(x, y, r, dx, dy, dr, diameter, img);	
	}

	// create and add to the game a pair of bullets at the proper locations
	private static void addNewBullets() {
		// offsets from the top left corner or the fighter image to the spawn positions of the bullets
		double down = 10;
		double side = 10;

		// calculate bullet velocity
		double dx = -Math.cos(Math.toRadians(ship.r + 90)) * BULLET_MUZZLE_VELOCITY+ ship.dx;
		double dy = -Math.sin(Math.toRadians(ship.r + 90)) * BULLET_MUZZLE_VELOCITY + ship.dx;

		// calculate bullet positions (trig borrowed)
		double baseX = ship.x + (FIGHTER_WIDTH - BULLET_WIDTH) / 2 - Math.cos(Math.toRadians(ship.r + 90)) * down;
		double baseY = ship.y + (FIGHTER_HEIGHT - BULLET_HEIGHT) / 2 - Math.sin(Math.toRadians(ship.r + 90)) * down;

		double x1 = baseX - Math.sin(Math.toRadians(ship.r + 90)) * side;
		double y1 = baseY + Math.cos(Math.toRadians(ship.r + 90)) * side;

		double x2 = baseX + Math.sin(Math.toRadians(ship.r + 90)) * side;
		double y2 = baseY - Math.cos(Math.toRadians(ship.r + 90)) * side;
		
		// finally create the bullets and add them to the game
		bullets.add(new Bullet(x1, y1, ship.r, dx, dy));
		bullets.add(new Bullet(x2, y2, ship.r, dx, dy));
	}

	// update game physics and resolve collisions, etc.
	private static void updateGame(double dt) {
		boolean resetShip = false;

		// update timers (this design makes it so pausing the game pauses all the time based effects)
		invincibilityTimer -= dt;
		cooldownTimer -= dt;
		timeSinceLastHit += dt;

		// process input
		boolean up = wDown && !sDown;
		boolean down = sDown && !wDown;
		boolean left = aDown && !dDown;
		boolean right = dDown && !aDown;
		boolean clock = kDown && !jDown;
		boolean cclock = jDown && !kDown;

		// accelerate the ship according to input. Trigonometry is useful afterall!
		if (up) {
			ship.dx += -Math.cos(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
			ship.dy += -Math.sin(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
		}

		if (down) {
			ship.dx += Math.cos(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
			ship.dy += Math.sin(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
		}

		if (left) {
			ship.dx += -Math.sin(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
			ship.dy += Math.cos(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
		}

		if (right) {
			ship.dx += Math.sin(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
			ship.dy += -Math.cos(Math.toRadians(ship.r + 90)) * LINEAR_ACCELERATION * dt;
		}

		// rotate ship according to input
		if (clock) {
			ship.dr = ROTATIONAL_VELOCITY;
		}
		else if (cclock) {
			ship.dr = -ROTATIONAL_VELOCITY;
		}
		else {
			ship.dr = 0;
		}

		// update ship position
		ship.update(dt);
		Rectangle2D.Double shipHitBox = ship.getHitBox();

		// create new bullets
		if (cooldownTimer <= 0 && spDown && invincibilityTimer <= 0) {
			cooldownTimer = COOLDOWN_TIME;
			addNewBullets();
		}

		// ship left window
		if (ship.x > WINDOW_WIDTH || ship.x < 0 - FIGHTER_WIDTH ||
				ship.y > WINDOW_HEIGHT || ship.y < 0 - FIGHTER_HEIGHT) {
			resetShip = true;
		}

		// update bullet positions
		for (int i = bullets.size() - 1; i >= 0; i--) {
			Bullet b = bullets.get(i);

			// update bullet position
			b.update(dt);

			// bullet left window
			if (b.x > WINDOW_WIDTH || b.x < 0 - BULLET_WIDTH ||
					b.y > WINDOW_HEIGHT || b.y < 0 - BULLET_HEIGHT) {
				bullets.set(i, bullets.get(bullets.size() - 1));
				bullets.remove(bullets.size()-1);
			}
		}

		// update targets (asteroids)
		for (int i = 0; i < targets.size(); i++) {
			Asteroid target = targets.get(i);

			// update target positions
			target.update(dt);

			Ellipse2D.Double hitCircle = target.getHitCircle();

			// target left window
			if (target.x > WINDOW_WIDTH || target.x < 0 - target.diameter ||
					target.y > WINDOW_HEIGHT || target.y < 0 - target.diameter) {
				targets.set(i, newTarget());
			}
			// target destroys ship
			else if (invincibilityTimer <= 0 && hitCircle.intersects(shipHitBox)) {
				targets.set(i, newTarget());
				resetShip = true;
			}
			// check if any bullets hit the target
			else for (int j = 0; j < bullets.size(); j++) {
				// target intersects bullet
				if (hitCircle.contains(bullets.get(j).getHitPoint())) {
					targets.set(i, newTarget());
					bullets.set(j, bullets.get(bullets.size() - 1));
					bullets.remove(bullets.size() - 1);
					targetsHit++;

					// apply bonus score for speedy combos
					if (timeSinceLastHit < 3) {
						score += 6 - timeSinceLastHit * 2;
					}
					score += 10;
					timeSinceLastHit = 0;

					// gain a bonus life
					if (score >= nextLifeGain) {
						numLives++;
						nextLifeGain *= 2;
					}

					// add an asteroid
					if (targetsHit > nextAsteroid) {
						nextAsteroid += 15 + targets.size() * 2;
						targets.add(newTarget());
					}
					break;
				}
			}
		}

		// something bad happened to the ship. If not invincible lose a life and become invincible
		if (resetShip) {
			ship = new Fighter();
			if (invincibilityTimer <= 0) {
				numLives--;
				invincibilityTimer = INVINCIBILITY_TIME;
			}	
		}
	}
	
	// draw a frame of the game
	private static void renderFrame(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(FONT);
		g2.setPaint(BG_COLOR);
		g2.fill(BG_SHAPE);

		// draw star field
		for (int i = 0; i < stars.length; i++) {
			g2.drawImage(stars[i].img, stars[i].x, stars[i].y, null);
		}

		// draw "game over" text if necessary
		if (numLives == 0) {
			g2.drawImage(GAMEOVER_IMG, GAMEOVER_X_POS, GAMEOVER_Y_POS, null);
		}

		// draw "paused" text if necessary
		else if (isPaused) {
			g2.drawImage(PAUSED_IMG, PAUSED_X_POS, PAUSED_Y_POS, null);
		}

		// draw normal frame
		else {
			// draw asteroids
			for (Asteroid a : targets) {
				a.draw(g2);
			}

			// draw ship
			ship.draw(g2);

			// draw bullets
			for (Bullet b : bullets) {
				b.draw(g2);
			}
		}

		// draw lives
		for (int i = 0; i < numLives; i++) {
			g2.drawImage(LIFE_IMG, LIVES_X_POS - i * LIVES_X_OFFSET, LIVES_Y_POS, null);
		}

		// draw score
		g2.setColor(SCORE_COLOR);
		g2.drawString("Score: " + score, SCORE_X_POS, SCORE_Y_POS);
	}

	// represents an object with position and velocity
	private static class PhysicsObject {
		public double x = 0;
		public double y = 0;
		public double r = 0;
		
		public double dx = 0;
		public double dy = 0;
		public double dr = 0;

		// take dt as an argument to make movement independent of frame rate
		public void update(double dt) {
			x += dx * dt;
			y += dy * dt;
			r = (r + 360 + dr * dt) % 360;
		}
	}

	// represents a single instance of an Asteroid
	private static class Asteroid extends PhysicsObject {

		public double diameter = 0;
		public Image img;	
		
		public Asteroid(double x, double y, double r, double dx, double dy, double dr, double diameter, Image img) {
			this.x = x;
			this.y = y;
			this.r = r;
			this.dx = dx;
			this.dy = dy;
			this.dr = dr;
			this.diameter = diameter;
			this.img = img;
		}

		public Ellipse2D.Double getHitCircle() {
			return new Ellipse2D.Double(x, y, diameter, diameter);
		}

		// rotates the camera around the center of the object, draws the image, then restores the camera position
		public void draw(Graphics2D g2) {
			AffineTransform original = g2.getTransform();
			g2.rotate(Math.toRadians(r), x + diameter / 2, y + diameter / 2);
			g2.drawImage(img, (int) x, (int) y, null);
			g2.setTransform(original);
		}
	}

	// represents a single instance of the fighter the player controls
	private static class Fighter extends PhysicsObject {

		public Fighter() {
			this.x = (WINDOW_WIDTH - FIGHTER_WIDTH) / 2;
			this.y = (WINDOW_HEIGHT - FIGHTER_HEIGHT) / 2;
			this.r = 0;
		}

		public void draw(Graphics2D g2) {
			AffineTransform original = g2.getTransform();
			
			// rotate the camera about the center of the ship
			g2.rotate(Math.toRadians(r), (int) x + FIGHTER_WIDTH / 2, (int) y + FIGHTER_HEIGHT / 2);

			// draw the invincibility image if appropriate. Also controls blinking effect
			if (invincibilityTimer > 0 && (invincibilityTimer > 1 || (int) (invincibilityTimer / .2) % 2 == 0)) {
				g2.drawImage(INVINCIBILITY_IMG, (int) x, (int) y, null);
			}

			// draw the ship itself
			g2.drawImage(FIGHTER_IMG, (int) x, (int) y, null);

			// draw the firing animation for FIRING_TIME after the ship fires
			if (COOLDOWN_TIME - cooldownTimer < FIRING_TIME && invincibilityTimer <= 0) {
				g2.drawImage(FIRING_IMG, (int) x, (int) y, null);
			}

			// if the ship is accelerating forward, show the thrust animation
			if (wDown) {
				g2.drawImage(THRUST_IMG, (int) x, (int) y, null);
			}
			
			// restpre the camera to its original position
			g2.setTransform(original);
		}
		
		public Rectangle2D.Double getHitBox() {
			return new Rectangle2D.Double(x, y, FIGHTER_WIDTH, FIGHTER_HEIGHT);
		}
	}

	// represents a single instance of a bullet emitted by the fighter
	private static class Bullet extends PhysicsObject {
		
		public Bullet(double x, double y, double r, double dx, double dy) {
			this.x = x;
			this.y = y;
			this.r = r;
			this.dx = dx;
			this.dy = dy;
		}

		public void draw(Graphics2D g2) {
			AffineTransform original = g2.getTransform();
			g2.rotate(Math.toRadians(r), (int) x + BULLET_WIDTH / 2, (int) y + BULLET_HEIGHT / 2);
			g2.drawImage(BULLET_IMG, (int) x, (int) y, null);
			g2.setTransform(original);
		}
		
		public Point2D.Double getHitPoint() {
			return new Point2D.Double(x + BULLET_WIDTH / 2, y + BULLET_WIDTH / 2);
		}
	}
	
	// represents a single star in the background starfield
	private static class Star {
		public int x;
		public int y;
		public Image img;

		public Star(int x, int y, Image img) {
			this.x = x;
			this.y = y;
			this.img = img;
		}
	}

	// the component in the window that receives keyboard input and represents the drawing canvas
	private static class DrawingSpace extends JComponent {	

		// register callbacks to respond to keyboard events
		// this kind of boilerplate garbage is unnecessary in Java 8
		public DrawingSpace() {
			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					wDown = true;
				}
			}, KeyStroke.getKeyStroke("W"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					wDown = false;
				}
			}, KeyStroke.getKeyStroke("released W"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					aDown = true;
				}
			}, KeyStroke.getKeyStroke("A"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					aDown = false;
				}
			}, KeyStroke.getKeyStroke("released A"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					sDown = true;
				}
			}, KeyStroke.getKeyStroke("S"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					sDown = false;
				}
			}, KeyStroke.getKeyStroke("released S"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					dDown = true;
				}
			}, KeyStroke.getKeyStroke("D"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					dDown = false;
				}
			}, KeyStroke.getKeyStroke("released D"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					jDown = true;
				}
			}, KeyStroke.getKeyStroke("J"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					jDown = false;
				}
			}, KeyStroke.getKeyStroke("released J"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					kDown = true;
				}
			}, KeyStroke.getKeyStroke("K"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					kDown = false;
				}
			}, KeyStroke.getKeyStroke("released K"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					spDown = true;
				}
			}, KeyStroke.getKeyStroke("SPACE"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					spDown = false;
				}
			}, KeyStroke.getKeyStroke("released SPACE"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					enterDown = true;
				}
			}, KeyStroke.getKeyStroke("ENTER"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener() { 
				public void actionPerformed(ActionEvent e) {
					enterDown = false;
				}
			}, KeyStroke.getKeyStroke("released ENTER"), WHEN_IN_FOCUSED_WINDOW);

			registerKeyboardAction(new ActionListener(){ 
				public void actionPerformed(ActionEvent e){
					isPaused = !isPaused;
				}
			}, KeyStroke.getKeyStroke("ESCAPE"), WHEN_IN_FOCUSED_WINDOW);
		}

		// called by Swing when the screen needs to be refreshed
		public void paint(Graphics g) {
			Graphics2D g2 = (Graphics2D)g;
			PixelPilot.renderFrame(g2);
		}
	}

	// displays the game window and controls the main game loop
	public static void main(String[] args) {
		INSTANCE.setVisible(true);	

		// game reset loop
		while (true) {
			resetGame();
			
			// main game loop
			long startTime = System.currentTimeMillis();

			while (true) {

				double dt = System.currentTimeMillis() - startTime;
				startTime += dt;

				// update game physics if the game is ongoing and not paused
				if (numLives > 0 && !isPaused) {
					updateGame(dt / 1000); // transform milliseconds to seconds
				}

				INSTANCE.repaint(); // calls paint() in DrawingSpace, which calls renderFrame() in PixelPilot

				// end current game and start a new one
				if (numLives <= 0 && enterDown) {
					break;
				}

				// add a wait (in milliseconds) to match the target framerate and keep the fan from going insane
				long sleepTime = (long) (1000.0 / TARGET_FRAMERATE - (System.currentTimeMillis() - startTime));
				try {
					Thread.sleep(sleepTime);
				} catch(InterruptedException e) {}				
			}
		}
	}
}