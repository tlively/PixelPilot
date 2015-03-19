## About
PixelPilot is a small Asteroids-esque game written with standard Java libraries for educational purposes. This tutorial will guide you through the development of your own version of PixelPilot. Along the way you will learn parts of the standard Java API that will let you build more interactive and dynamic graphical applications as well as skills and techniques that will be useful for creating simple videogames in any language. By the end, you will have the skills you need to write your own 2D games in Java.

This tutorial assumes you are already familiar with programming in the Java language, though it does not assume knowledge of any specific API. In particular, you do not need to know how to write graphical programs to be able to follow along with this tutorial.

## Part 0: Setup
#### Download the code
There are two options for getting the PixelPilot code and resources.

1. Download a the code with one of the archive links at the top of this page.

2. Run `git clone https://github.com/tlively/PixelPilot.git` from the directory where you want the repository.

#### Compiling
in PixelPilot directory:

```
$ javac -d bin/ src/PixelPilot.java
```

this will place all the generated class files in PixelPilot/bin

#### Create a JAR 
In PixelPilot/bin directory

```
$ jar cmf manifest.mf PixelPilot.jar *
```

this will create a Java Archive, PixelPilot.jar, which can be run with

```
$ java -jar PixelPilot.jar
```

#### Play
goal: pilot the ship around the screen, shooting asteroids. Try to survive as long as possible and get a high score!

| Key | Action |
| :-- | :----- |
| w   | accelerate forward |
| a   | accelerate backward |
| s   | accelerate left |
| d   | accelerate right |
| j   | rotate counterclockwise |
| k   | rotate clockwise |
| space | fire |
| esc   | pause/unpause |
| enter | start a new game from the game over screen |

You start with 3 lives, which you can lose by running into an asteroid
or leaving the window. You can gain lives by reaching 200, 400, 800, etc.
points. shooting an asteroid is worth 10 points, with up to 6 bonus
points based on the time it took to shoot it. The number of asteroids
will rise, and so will their speed. When you die, you start back in the
middle of the screen. You will be invincible for 3 seconds, during which
you can move, but not shoot.

good luck, and have fun!

## Part 1: The Window
So how in the world are we going to start writing our own game? A good place to start is the **Window**. After all, we can't exactly draw our game each frame if we don't have a window to draw it on. As we will see later, we need the window in order to capture user input from the mouse and keyboard as well.

In Java, a window is represented as a *JFrame* object. As you can see in the PixelPilot source code, we have `public class PixelPilot extends JFrame`. This makes sense because in a way, the game is a window. However, for larger games it may make more sense to separate the game functionality from the window functionality.

Simply instantiating the *JFrame* will not make a window appear, though. First we must tell the window how we want it to appear and the we must command it to become visible. Since *PixelPilot* is a subclass of *JFrame*, it makes sense to complete these tasks in its constructor. Let's look at the constructor code.

    // constructor creates game window
    private PixelPilot() {
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setTitle(WINDOW_TITLE);
        this.setResizable(false);
        this.setLocationRelativeTo(null); // will create window in center of screen
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // closing the window will close the program
        this.add(new DrawingSpace()); // so we can draw stuff in the window
    }

The constructor is private since we don't want the game to be instantiated from outside of our code. *setSize*, *setTitle*, *setResizable*, *setLocationRelativeTo*, and *setDefaultCloseOperation* are all methods of *JFrame*. Most of them are self-expanatory, but if you want to learn more, you should go poke around the Java API reference for *JFrame*. We choose not to make the window resizable because drawing the game is much simpler if we can always assume that our drawing region is a constant number of pixels tall and wide. But as it turns out, just having a *JFrame* is not enough for our application to show anything interesting. For that we need to use *add* to put some content in our window.

The argument *add* takes can be any subclass of *JComponent*. All of the GUI components provided by Java are subclasses of *JComponent*, but in this case we will add an instance of our own subclass called *DrawingSpace*. This *DrawingSpace* will be the only component in our window, so the window's layout manager will let it fill up the entire window. All of our game visuals will be drawn onto this component, not onto the window directly.

Let's look at the only method that *DrawingSpace* implements.

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        PixelPilot.renderFrame(g2);
    }

*paint* is the only method that every subclass of *JComponent* must implement, because it tells the windowing system how to visually represent the component. We don't call this method directly, but rather call the *repaint* method of our *JFrame* (the *PixelPilot* instance) and let the windowing system handle calling *paint* on everything it needs to. This implementation of *paint* is very simple, as it simply casts the graphics context it receives into the more specific and useful 2D graphics context and passes responsibility for drawing to the graphics context to the game. Don't worry about the details of drawing right now, as we have some more foundational knowledge to get through first.

Even after all this work, the window will still not appear. There is one last thing we need to do after the *JFrame* has been instantiated and the *DrawingSpace* component has been added to it. We must call `setVisible(true)` on our *JFrame* instance in order to make the window appear. As you can see in the source code, *setVisible* is the first command in our *main* method. Since our instance of *PixelPilot* is stored in a static variable, we can be sure that everything in the windowing system is set up and ready to go by the time execution starts in *main*.

At this point in the tutorial, you should be able to create a visible window by instantiating a *JFrame* and using its methods to configure it properly. You should also be able to add a custom *JComponent* to the window, but it won't be able to do anything yet.

## Part 2: The Game Loop
Great, now that we've got a window to look at we need to think about how we're going to make a game with it. The most fundamental part of any game is the **game loop**. The game loop has three parts: Update the game logic, clear the screen, and render (draw) the game objects to the screen. The reason we clear the screen before rendering each frame is that when objects move on screen we want to remove them from the old location and redraw them in the new location. It turns out that it is easier and often more efficient to clear and redraw the whole screen rather than to try to clear just the part that needs updating.

With all that in mind, here is the most basic game loop possible.

    while (!game_over) {
        update();
        clear();
        draw();
    }

While this basic game loop would work, it has some some problems. First of all, we have no control over how fast this game loop runs. On a faster machine, the game itself would run faster. Clearly that is undesirable behavior. Another problem is that as long as the game is running, the processor will be working at 100% without stopping. This is very unusual for processors, and it will make your computer very very hot and make your computer's fan freak out. To prevent this from happening, we will introduce the concepts of **sleeping** and **framerate**.

Framerate is the number of frames a game renders per second. Framerates can be as low as 30 frames/second and as high as 120 frames/second. Technically they could be higher, but most monitors have a built in framerate of 60 or 120 frames per second, so telling the computer to draw more won't make the picture look any better. But how do we control the number of frames the computer draws each second?

Clearly we need some sort of timing mechanism to control when the game updates and draws itself. Consider this more sophisticated game loop.

    long startTime = System.currentTimeMillis();
    while (!game_over) {
        double dt = System.currentTimeMillis() - startTime;
        startTime += dt;
        update(dt / 1000);
        clear();
        draw();
        long sleepTime = (long) (1000.0 / TARGET_FRAMERATE - (System.currentTimeMillis() - startTime));
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {}
    }

At each iteration, this game loop calculates *dt*, the milliseconds since the last game update, and passes it to *update*. The game logic code will use this value to determine how much game state has changed since the last update. For example, *dt* is important in figuring out how far an object has moved, given its velocity, since the last frame.

Once the game loop has finished updating and drawing the scene, it calculates how long it needs to sleep before waking up to draw the next frame. `1000.0 / TARGET_FRAMERATE` is the number of milliseconds we would like to have between the start of one frame and the next, so we let the process sleep for this long, minus the time it took to do the update, clearing, and drawing. This will make the process wake up at just the right time to render the next frame.

This is not the most sophisticated game loop scheme, but it is certainly sufficient for most 2D games. If you look at the *main* method at the bottom of the PixelPilot source, you should be able to recognize this game loop structure mixed in with some high level game logic. One thing to note, however is that in PixelPilot the *clear* functionality has been rolled into the *draw* functionality which is called with `INSTANCE.repaint()`.

At this point in the tutorial you should be able to create a window and implement a good game loop. There's still nothing to look at in the window, but all that's left to do is learn how to draw to the screen. After that you will have all the skills you need to make your own games.

## Part 3: Rendering

This part is under construction. Come back soon!
        
		
